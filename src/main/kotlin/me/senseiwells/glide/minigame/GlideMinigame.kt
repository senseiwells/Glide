package me.senseiwells.glide.minigame

import com.mojang.datafixers.util.Pair
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.checkpoint.RespawnCheckpoint
import net.casual.arcade.events.minigame.MinigameAddPlayerEvent
import net.casual.arcade.events.minigame.MinigameSetPlayingEvent
import net.casual.arcade.events.player.PlayerDamageEvent
import net.casual.arcade.events.player.PlayerLandEvent
import net.casual.arcade.events.player.PlayerRespawnEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.gui.bossbar.SuppliedBossBar
import net.casual.arcade.gui.elements.ComponentElements
import net.casual.arcade.gui.sidebar.ArcadeSidebar
import net.casual.arcade.gui.sidebar.SidebarComponent
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.During
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.managers.MinigameLevelManager
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.GameRuleUtils.set
import net.casual.arcade.utils.LevelUtils
import net.casual.arcade.utils.MinigameUtils.countdown
import net.casual.arcade.utils.PlayerUtils.boostHealth
import net.casual.arcade.utils.PlayerUtils.clearPlayerInventory
import net.casual.arcade.utils.PlayerUtils.resetHealth
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.location.Location
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import kotlin.time.DurationUnit

class GlideMinigame(server: MinecraftServer): Minigame<GlideMinigame>(server) {
    override val id: ResourceLocation = ID

    private val startPos = Location.of(279.5, 205.5, 159.5, 180F, 0F)
    private var player: ServerPlayer? = null
    private var checkpoint = this.startPos

    private var ticks = 0

    private val checkpointBossbar = SuppliedBossBar()

    init {
        this.checkpointBossbar.setTitle(ComponentElements.of("Checkpoint".literal()))
    }

    override fun getPhases(): Collection<Phase<GlideMinigame>> {
        return GlidePhase.entries
    }

    override fun initialize() {
        super.initialize()
        this.settings.canLookAroundWhenFrozen = true
        this.settings.freezeEntities.set(true)

        val sidebar = ArcadeSidebar(ComponentElements.of("Glide".literal()))
        sidebar.addRow { _ ->
            val seconds = this.ticks.Ticks.duration.toString(DurationUnit.SECONDS, 2)
            SidebarComponent.withNoScore(seconds.literal())
        }
        sidebar.addRow { player ->
            val mps = player.deltaMovement.length() * 20
            SidebarComponent.withNoScore("%.2f m/s".format(mps).literal())
        }
        this.ui.setSidebar(sidebar)
        this.levels.add(LevelUtils.overworld())

        this.setGameRules {
            set(GameRules.RULE_KEEPINVENTORY, true)
            set(GameRules.RULE_NATURAL_REGENERATION, false)
        }
    }

    fun onPlayerCheckpoint(player: ServerPlayer, checkpoint: RespawnCheckpoint) {
        this.checkpoint = checkpoint.respawnLocation
        this.checkpointBossbar.addPlayer(player)
        this.scheduler.schedule(4.Seconds) {
            this.checkpointBossbar.removePlayer(player)
        }
    }

    fun onPlayerFinish(player: ServerPlayer) {
        if (this.phase == GlidePhase.Finish) {
            return
        }
        this.setPhase(GlidePhase.Finish)
        player.sendSystemMessage("Finished! :)".literal())
    }

    @Listener
    private fun onPlayerLand(event: PlayerLandEvent) {
        event.player.kill()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        if (event.source.`is`(DamageTypes.FALL) || event.source.`is`(DamageTypes.FLY_INTO_WALL)) {
            event.cancel(2.0F)
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
        player.setGameMode(GameType.SURVIVAL)
        player.clearPlayerInventory()
        player.boostHealth(-0.7)
        player.resetHealth()
        player.teleportTo(this.startPos)
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack(Items.ELYTRA))
        // FIXME:
        player.connection.send(ClientboundSetEquipmentPacket(
            player.id,
            listOf(Pair(EquipmentSlot.CHEST, ItemStack(Items.ELYTRA)))
        ))

        this.start()
    }

    @Listener
    private fun onPlayerRespawn(event: PlayerRespawnEvent) {
        event.player.boostHealth(-0.7)
        event.player.resetHealth()
        event.player.teleportTo(this.checkpoint)

        event.player.tryToStartFallFlying()
    }

    companion object: MinigameFactory {
        val ID = GlideMod.id("glide_minigame")

        override fun create(context: MinigameCreationContext): Minigame<*> {
            return GlideMinigame(context.server)
        }
    }
}