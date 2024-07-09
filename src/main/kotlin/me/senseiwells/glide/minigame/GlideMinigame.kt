package me.senseiwells.glide.minigame

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.addon.GlideAddonConfig
import me.senseiwells.glide.addon.GlideAddons
import me.senseiwells.glide.checkpoint.CheckpointManager
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.events.minigame.MinigameRemovePlayerEvent
import net.casual.arcade.events.minigame.MinigameSetPlayingEvent
import net.casual.arcade.events.player.PlayerDamageEvent
import net.casual.arcade.events.player.PlayerPoseEvent
import net.casual.arcade.events.player.PlayerSlotClickEvent
import net.casual.arcade.events.player.PlayerTickEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.gui.bossbar.SuppliedBossBar
import net.casual.arcade.gui.elements.ComponentElements
import net.casual.arcade.gui.elements.SidebarElements
import net.casual.arcade.gui.sidebar.ArcadeSidebar
import net.casual.arcade.gui.sidebar.SidebarComponent
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.MinigameResources
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFlags
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.GameRuleUtils.set
import net.casual.arcade.utils.JsonUtils.string
import net.casual.arcade.utils.PlayerUtils.boostHealth
import net.casual.arcade.utils.PlayerUtils.clearPlayerInventory
import net.casual.arcade.utils.PlayerUtils.clearTitle
import net.casual.arcade.utils.PlayerUtils.resetHealth
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.setTitleAnimation
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.utils.ResourcePackUtils.afterPacksLoad
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.TimeUtils.format
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.level.storage.LevelResource
import xyz.nucleoid.fantasy.Fantasy
import xyz.nucleoid.fantasy.RuntimeWorldConfig
import xyz.nucleoid.fantasy.util.VoidChunkGenerator
import kotlin.math.min

class GlideMinigame(
    private val config: GlideAddonConfig,
    private val mode: GlideMode,
    val level: ServerLevel,
    server: MinecraftServer
): Minigame<GlideMinigame>(server) {
    override val id: ResourceLocation = ID

    private val checkpointBossbar = SuppliedBossBar()
    private val startPos = this.config.spawn.get(this.level)

    private val sidebar = ArcadeSidebar(ComponentElements.empty())
    private var progressBar: Component = Component.empty()

    private var ticks = 0

    val checkpoints: CheckpointManager
        get() = this.config.checkpoints

    init {
        this.checkpointBossbar.setTitle(ComponentElements.of("Checkpoint!".literal()))
    }

    override fun getPhases(): Collection<Phase<GlideMinigame>> {
        return GlidePhase.entries
    }

    override fun initialize() {
        super.initialize()

        this.addResources(MinigameResources.of(GlideMod.resourcePackInfo))

        // TODO: Clean this up
        this.sidebar.addRow { _ ->
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
        this.sidebar.addRow(SidebarElements.empty())
        this.sidebar.addRow { player ->
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
        if (this.mode == GlideMode.ScoreAttack) {
            this.sidebar.addRow(SidebarElements.empty())
            this.sidebar.addRow { player ->
                SidebarComponent.withCustomScore(
                    Component.literal(player.glide.score.toString()),
                    Component.empty().append(player.glide.scoreIcon).append(ComponentUtils.space(6))
                )
            }
        }
        this.sidebar.addRow(SidebarElements.empty())
        this.ui.setSidebar(this.sidebar)

        this.settings.canLookAroundWhenFrozen = false
        this.settings.tickFreezeOnPause.set(true)
        this.settings.canGetHungry.set(false)
        this.setGameRules {
            set(GameRules.RULE_KEEPINVENTORY, true)
            set(GameRules.RULE_NATURAL_REGENERATION, false)
        }

        for (booster in this.config.boosters) {
            this.level.addFreshEntity(booster.create(this.level))
        }
        for (thermal in this.config.thermals) {
            this.level.addFreshEntity(thermal.create(this.level))
        }
        for (checkpoint in this.checkpoints.all()) {
            for (beam in checkpoint.createBeaconBeams(this.level)) {
                this.level.addFreshEntity(beam)
            }
        }
        if (this.mode == GlideMode.ScoreAttack) {
            for (ring in this.config.rings) {
                this.level.addFreshEntity(ring.create(this.level))
            }
        }
    }

    fun displayCheckpointNotificationTo(player: ServerPlayer) {
        this.checkpointBossbar.addPlayer(player)
        this.scheduler.schedule(4.Seconds) {
            this.checkpointBossbar.removePlayer(player)
        }
    }

    fun markPlayerFinished(player: ServerPlayer) {
        this.players.setSpectating(player)
        player.sendSystemMessage("Finished! :)".literal())
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerTick(event: PlayerTickEvent) {
        val (player) = event

        val glide = player.glide
        glide.tick(this)
        this.sendProgress(player)

        if (player.verticalCollision) {
            player.hurt(player.damageSources().flyIntoWall(), 2.0F)
        }
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerHorizontalFlyFallCollision(event: PlayerHorizontalFlyFallCollisionEvent) {
        val (player) = event
        player.hurt(player.damageSources().flyIntoWall(), 2.0F)
        player.playSound(player.fallSounds.small, 1.0F, 1.0F)

        event.cancel()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, amount, source) = event
        if (this.players.isSpectating(player)) {
            event.cancel(0.0F)
            return
        }

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
        this.effects.freeze(player)

        // Make sure we remove this bossbar first!
        this.checkpointBossbar.removePlayer(player)
        this.sidebar.removePlayer(player)

        player.setTitleAnimation(2.Seconds, 5.Seconds)
        player.sendTitle(
            GlideFont.YOU_CRASHED.shadowless(),
            GlideFont.YOU_CRASHED_BACKGROUND.shadowless(),
        )
        player.sendSystemMessage(Component.empty(), true)

        this.scheduler.schedule(2.Seconds) {
            this.effects.unfreeze(player)
            this.sidebar.addPlayer(player)
            this.respawnPlayer(player)
        }
    }

    @Listener
    private fun onServerTick(event: ServerTickEvent) {
        if (this.phase == GlidePhase.Active) {
            this.ticks++
        }

        this.tickProgress()
    }

    @Listener
    private fun onPlayerSlotClick(event: PlayerSlotClickEvent) {
        event.cancel()
    }

    @Listener
    private fun onMinigameRemovePlayer(event: MinigameRemovePlayerEvent) {
        event.player.glide.reset()
    }

    @Listener
    private fun onMinigameSetPlaying(event: MinigameSetPlayingEvent) {
        val player = event.player

        player.glide.respawnLocation = this.startPos
        event.player.glide.reset()
        this.respawnPlayer(player)
        if (this.phase < GlidePhase.Active) {
            player.isNoGravity = true
        }

        this.start()
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerPose(event: PlayerPoseEvent) {
        val (player, _, updated) = event
        if (updated != Pose.FALL_FLYING) {
            player.kill()
        }
    }

    private fun respawnPlayer(player: ServerPlayer) {
        player.glide.respawn()
        player.clearTitle()
        player.clearPlayerInventory()
        player.setGameMode(GameType.ADVENTURE)
        player.boostHealth(-0.7)
        player.resetHealth()
        player.clearFire()
        player.teleportTo(player.glide.respawnLocation)
        player.setVelocityAndMark(0.0, 0.0, 0.0)
        val elytra = ItemStack(Items.ELYTRA)
        elytra.set(DataComponents.UNBREAKABLE, Unbreakable(true))
        player.setItemSlot(EquipmentSlot.CHEST, elytra)

        // Ensure the player has loaded the chunks
        this.scheduler.schedule(1.Ticks) {
            player.startFallFlying()
        }
    }

    private fun sendProgress(player: ServerPlayer) {
        val progressBar = Component.empty().append(this.progressBar)
        progressBar.append(ComponentUtils.space(-163)).append(ComponentUtils.space(-1, 2))

        val fullHearts = Mth.floor(player.health / 2.0)
        for (i in 0..< min(fullHearts, 3)) {
            progressBar.append(GlideFont.FULL_HEART.shadowless())
            progressBar.append(ComponentUtils.space(-2))
        }
        if (fullHearts < 3) {
            var empty = 3 - fullHearts
            if (player.health - fullHearts * 2 >= 1.0) {
                progressBar.append(GlideFont.HALF_HEART.shadowless())
                progressBar.append(ComponentUtils.space(-2))
                empty--
            }
            for (i in 0..< empty) {
                progressBar.append(GlideFont.EMPTY_HEART.shadowless())
                progressBar.append(ComponentUtils.space(-2))
            }
        }

        player.afterPacksLoad {  }

        progressBar.append(ComponentUtils.space(138))
        player.sendSystemMessage(progressBar, true)
    }

    private fun tickProgress() {
        val progressBar = Component.empty().append(this.checkpoints.progressBar)
        var lastOffset = 300
        for ((i, player) in this.players.playing.withIndex()) {
            val offset = Mth.clamp(Math.round(player.glide.progress * 300).toInt(), 0, 300)

            progressBar.append(ComponentUtils.space(offset - lastOffset))
            progressBar.append(GlideFont.Progressbar.getPlayerMarker(i).shadowless())
            lastOffset = offset + 6
        }

        progressBar.append(ComponentUtils.space(300 - lastOffset))
        this.progressBar = progressBar
    }

    companion object: MinigameFactory {
        val ID = GlideMod.id("glide_minigame")

        override fun create(context: MinigameCreationContext): Minigame<*> {
            val id = if (context.hasCustomData()) {
                ResourceLocation.parse(context.getCustomData().string("id"))
            } else {
                GlideMod.id("temple")
            }
            val addon = GlideAddons.get(id) ?: throw IllegalArgumentException()
            val handle = Fantasy.get(context.server).openTemporaryWorld(
                RuntimeWorldConfig()
                    .setGenerator(VoidChunkGenerator(context.server))
                    .setDimensionType(addon.dimensionType)
            )

            val glide = GlideMinigame(addon.config, GlideMode.ScoreAttack, handle.asWorld(), context.server)
            glide.levels.add(handle)

            val tempLocation = handle.registryKey.location()
            val dimensionDir = context.server.getWorldPath(LevelResource.ROOT).resolve("dimensions")
                .resolve(tempLocation.namespace).resolve(tempLocation.path)
            addon.extractWorld(dimensionDir)
            return glide
        }
    }
}