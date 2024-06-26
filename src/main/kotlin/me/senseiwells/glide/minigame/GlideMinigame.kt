package me.senseiwells.glide.minigame

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.events.minigame.MinigameAddPlayerEvent
import net.casual.arcade.events.minigame.MinigameSetPlayingEvent
import net.casual.arcade.events.player.PlayerDamageEvent
import net.casual.arcade.events.player.PlayerPoseEvent
import net.casual.arcade.events.player.PlayerTickEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.gui.bossbar.SuppliedBossBar
import net.casual.arcade.gui.elements.ComponentElements
import net.casual.arcade.gui.elements.SidebarElements
import net.casual.arcade.gui.sidebar.ArcadeSidebar
import net.casual.arcade.gui.sidebar.SidebarComponent
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.During
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFlags
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.GameRuleUtils.set
import net.casual.arcade.utils.LevelUtils
import net.casual.arcade.utils.PlayerUtils.boostHealth
import net.casual.arcade.utils.PlayerUtils.clearPlayerInventory
import net.casual.arcade.utils.PlayerUtils.clearTitle
import net.casual.arcade.utils.PlayerUtils.resetHealth
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.setTitleAnimation
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.TimeUtils.format
import net.casual.arcade.utils.location.Location
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Unbreakable
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType

class GlideMinigame(server: MinecraftServer): Minigame<GlideMinigame>(server) {
    override val id: ResourceLocation = ID

    private val startPos = Location.of(279.5, 205.5, 159.5, 180F, 0F)
    private var lastCheckpoint: RespawnCheckpoint? = null
    private var player: ServerPlayer? = null

    private var ticks = 0

    private val checkpointBossbar = SuppliedBossBar()

    init {
        this.checkpointBossbar.setTitle(ComponentElements.of("Checkpoint!".literal()))
    }

    fun getPlayer(): ServerPlayer {
        return this.player!!
    }

    fun getRespawnLocation(): Location {
        val lastCheckpoint = this.lastCheckpoint
        if (lastCheckpoint != null) {
            return lastCheckpoint.respawnLocation
        }
        return this.startPos
    }

    override fun getPhases(): Collection<Phase<GlideMinigame>> {
        return GlidePhase.entries
    }

    override fun initialize() {
        super.initialize()

        val sidebar = ArcadeSidebar(ComponentElements.empty())
        sidebar.addRow { _ ->
            val elapsed = this.ticks.Ticks
            val secondsHandIndex = (elapsed.seconds / 10) % 11
            val minutesHandIndex = (elapsed.minutes / 5) % 11
            val stopwatch = Component.empty()
                .append(GlideFont.Timer.TIMER)
                .append(ComponentUtils.space(-16))
                .append(GlideFont.Timer.getSecondsHand(secondsHandIndex))
                .append(ComponentUtils.space(-16))
                .append(GlideFont.Timer.getMinutesHand(minutesHandIndex))
            SidebarComponent.withCustomScore(
                elapsed.format().literal(),
                Component.empty().append(stopwatch).append(ComponentUtils.space(5))
            )
        }
        sidebar.addRow(SidebarElements.empty())
        sidebar.addRow { player ->
            val mps = player.deltaMovement.length() * 20
            // See https://www.desmos.com/calculator/6bhuudkjpm
            val needleIndex = Mth.clamp(Mth.floor(0.24 * mps - 0.5), 0, 11)
            val speedometer = Component.empty()
                .append(GlideFont.Speedometer.SPEEDOMETER)
                .append(ComponentUtils.space(-17))
                .append(GlideFont.Speedometer.getNeedle(needleIndex))
            SidebarComponent.withCustomScore(
                "%.2f m/s".format(mps).literal(),
                Component.empty().append(speedometer).append(ComponentUtils.space(4))
            )
        }
        sidebar.addRow(SidebarElements.empty())
        this.ui.setSidebar(sidebar)
        this.levels.add(LevelUtils.overworld())

        this.setGameRules {
            set(GameRules.RULE_KEEPINVENTORY, true)
            set(GameRules.RULE_NATURAL_REGENERATION, false)
        }
    }

    fun onPlayerCheckpoint(player: ServerPlayer, checkpoint: RespawnCheckpoint) {
        val lastCheckpoint = this.lastCheckpoint
        if (lastCheckpoint != null && lastCheckpoint.index >= checkpoint.index) {
            return
        }

        player.sendSound(GlideSounds.CHECKPOINT)
        this.lastCheckpoint = checkpoint
        this.checkpointBossbar.addPlayer(player)
        this.scheduler.schedule(4.Seconds) {
            this.checkpointBossbar.removePlayer(player)
        }
    }

    fun onPlayerFinish(player: ServerPlayer) {
        if (this.phase == GlidePhase.Finish) {
            return
        }

        player.sendSound(GlideSounds.FINISH, static = true)
        this.setPhase(GlidePhase.Finish)
        player.sendSystemMessage("Finished! :)".literal())
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerTick(event: PlayerTickEvent) {
        val (player) = event
        val intersecting = player.serverLevel().getEntities(player, player.boundingBox)
        for (entity in intersecting) {
            if (entity is GlideCollider) {
                entity.collide(player, this)
            }
        }
    }

    @Listener
    private fun onPlayerHorizontalFlyFallCollision(event: PlayerHorizontalFlyFallCollisionEvent) {
        val (player) = event
        player.hurt(player.damageSources().flyIntoWall(), 2.0F)
        player.playSound(player.fallSounds.small, 1.0F, 1.0F)

        event.cancel()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, amount, source) = event
        if (source.`is`(DamageTypes.FALL) || source.`is`(DamageTypes.FLY_INTO_WALL)) {
            if (player.health > 2.0F) {
                event.cancel(2.0F)
                return
            }
        } else if (player.health > amount) {
            return
        }
        event.cancel(0.0F)
        player.setGameMode(GameType.SPECTATOR)
        player.setVelocityAndMark(0.0, 0.0, 0.0)
        this.settings.freezeEntities.set(true)

        // Make sure we remove this bossbar first!
        this.checkpointBossbar.removePlayer(player)

        player.setTitleAnimation(2.Seconds, 5.Seconds)
        player.sendTitle(
            GlideFont.YOU_CRASHED.shadowless(),
            GlideFont.YOU_CRASHED_BACKGROUND.shadowless(),
        )

        this.scheduler.schedule(2.Seconds) {
            this.settings.freezeEntities.set(false)
            this.respawnPlayer(event.player)
        }
    }

    @Listener(during = During(phases = ["active"]))
    private fun onServerTick(event: ServerTickEvent) {
        this.ticks++
    }

    @Listener
    private fun onMinigameAddPlayer(event: MinigameAddPlayerEvent) {
        if (this.player != null) {
            event.spectating = true
        }
    }

    @Listener
    private fun onMinigameSetPlaying(event: MinigameSetPlayingEvent) {
        val player = event.player
        this.player = player
        player.isNoGravity = true
        this.respawnPlayer(player)

        this.start()
    }

    @Listener
    private fun onPlayerPose(event: PlayerPoseEvent) {
        if (event.updated != Pose.FALL_FLYING) {
            event.player.kill()
        }
    }

    private fun respawnPlayer(player: ServerPlayer) {
        player.clearTitle()
        player.clearPlayerInventory()
        player.setGameMode(GameType.ADVENTURE)
        player.boostHealth(-0.7)
        player.resetHealth()
        player.teleportTo(this.getRespawnLocation())
        player.setVelocityAndMark(0.0, 0.0, 0.0)
        val elytra = ItemStack(Items.ELYTRA)
        elytra.set(DataComponents.UNBREAKABLE, Unbreakable(true))
        player.setItemSlot(EquipmentSlot.CHEST, elytra)

        // Ensure the player has loaded the chunks
        this.scheduler.schedule(1.Ticks) {
            player.startFallFlying()
        }
    }

    companion object: MinigameFactory {
        val ID = GlideMod.id("glide_minigame")

        override fun create(context: MinigameCreationContext): Minigame<*> {
            return GlideMinigame(context.server)
        }
    }
}