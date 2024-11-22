package me.senseiwells.glide.minigame

import it.unimi.dsi.fastutil.objects.Object2IntFunction
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.addon.GlideAddon
import me.senseiwells.glide.addon.GlideAddonConfig
import me.senseiwells.glide.checkpoint.CheckpointManager
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.gui.*
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.tracker.GlideTrailTracker
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.getDimensionPath
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.events.player.*
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.During
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFlags
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.countdown
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.withShiftedDownFont
import net.casual.arcade.utils.ComponentUtils.wrap
import net.casual.arcade.utils.PlayerUtils.addToTeam
import net.casual.arcade.utils.PlayerUtils.boostHealth
import net.casual.arcade.utils.PlayerUtils.clearPlayerInventory
import net.casual.arcade.utils.PlayerUtils.clearTitle
import net.casual.arcade.utils.PlayerUtils.player
import net.casual.arcade.utils.PlayerUtils.removeFromTeam
import net.casual.arcade.utils.PlayerUtils.resetHealth
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.setTitleAnimation
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.utils.TeamUtils.getOrCreateTeam
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.set
import net.casual.arcade.visuals.bossbar.SuppliedBossbar
import net.casual.arcade.visuals.elements.ComponentElements
import net.casual.arcade.visuals.elements.SidebarElements
import net.casual.arcade.visuals.sidebar.FixedSidebar
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
import net.minecraft.world.scores.Team
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class GlideMinigame(
    server: MinecraftServer,
    uuid: UUID,
    private val config: GlideAddonConfig,
    private val round: GlideRound,
    private val mode: GlideMode,
    val level: ServerLevel
): Minigame(server, uuid) {
    override val id: ResourceLocation = ID

    // TODO: Rework this
    private val team = this.server.scoreboard.getOrCreateTeam("Glide") {
        setSeeFriendlyInvisibles(true)
        collisionRule = Team.CollisionRule.NEVER
    }

    private val checkpointBossbar = SuppliedBossbar(ComponentElements.of(GlideComponents.CHECKPOINT.withShiftedDownFont(12)))
    private val startPos = this.config.spawn.get(this.level)

    private val countdown = GlideStartBanner(this.round)
    private val finish = GlideFinishBanner(this.mode)
    private val end = GlideEndBanner(this.mode)

    private val sidebar = FixedSidebar(ComponentElements.empty())
    private var progressBar: Component = Component.empty()

    private val trackers = HashMap<UUID, GlideTrailTracker>()
    private val playerIndices = Object2IntOpenHashMap<UUID>()

    private var placement = 1
    private var ticks = 0

    private var roundEnding = false

    val checkpoints: CheckpointManager
        get() = this.config.checkpoints

    override fun phases(): Collection<Phase<GlideMinigame>> {
        return GlidePhase.entries
    }

    fun displayCheckpointNotificationTo(player: ServerPlayer) {
        this.checkpointBossbar.addPlayer(player)
        // 2 Seconds, then 1 second fade
        GlobalTickedScheduler.schedule(3.Seconds) {
            this.checkpointBossbar.removePlayer(player)
        }
    }

    fun markPlayerFinished(player: ServerPlayer) {
        this.players.setSpectating(player)
        player.sendSound(GlideSounds.getFinishSound(this.placement++), static = true)

        val tracker = this.trackers.remove(player.uuid) ?: return
        val best = player.glide.checkForBest(this.config.id, this.mode, this.ticks.Ticks, tracker.finish())
        if (best) {
            this.scheduler.schedule(1.Seconds) {
                player.sendSound(this.mode.getPersonalBestSound(), static = true)
            }
        }

        val remaining = this.players.playingPlayerCount
        if (remaining > 0) {
            if (this.placement == 2) {
                this.endingRound()
            } else {
                this.sendFinish(player)
            }
        } else {
            this.setPhase(GlidePhase.Finish)
        }
    }

    fun endRound() {
        this.end.setTitle(GlideComponents.ROUND_END.withShiftedDownFont(12))
        for (player in this.players) {
            this.removeBannerUI(player)
            player.glide.resetAnimation()
            this.end.addPlayer(player)
        }
        this.scheduler.schedule(1.Seconds) {
            this.end.countdown(this, 4.Seconds)
        }
    }

    private fun endingRound() {
        this.roundEnding = true
        for (player in this.players) {
            this.sendFinish(player)
        }
        this.finish.countdown(this, 60.Seconds).then {
            this.setPhase(GlidePhase.Finish)
        }
    }

    private fun sendFinish(player: ServerPlayer) {
        this.removeBannerUI(player)
        player.glide.resetAnimation()
        this.finish.addPlayer(player)
        this.scheduler.schedule(5.Seconds) {
            this.finish.removePlayer(player, title = false)
        }
    }

    @Listener
    private fun onInitialize(event: MinigameInitializeEvent) {
        this.levels.add(this.level)
        this.sidebar.addRow(PlayerSidebarTimerElement(GlobalSidebarTimerElement { this.ticks.Ticks }))
        this.sidebar.addRow(SidebarElements.empty())
        this.sidebar.addRow(SidebarVelocityElement)
        if (this.mode == GlideMode.ScoreAttack) {
            this.sidebar.addRow(SidebarElements.empty())
            this.sidebar.addRow(SidebarScoreElement)
        }
        this.sidebar.addRow(SidebarElements.empty())
        this.ui.setSidebar(this.sidebar)

        this.ui.countdown = this.countdown

        this.music.suppressGameMusic(true)
        this.music.provider = GlideMusicProvider

        this.settings.canLookAroundWhenFrozen = false
        this.settings.tickFreezeOnPause.set(true)
        this.settings.canGetHungry.set(false)
        this.settings.canPvp.set(false)
        this.levels.setGameRules {
            set(GameRules.RULE_KEEPINVENTORY, true)
            set(GameRules.RULE_NATURAL_REGENERATION, false)
            set(GameRules.RULE_DOMOBSPAWNING, false)
            set(GameRules.RULE_RANDOMTICKING, 0)
            set(GameRules.RULE_DOFIRETICK, false)
            set(GameRules.RULE_WEATHER_CYCLE, false)
            set(GameRules.RULE_DAYLIGHT, false)
        }

        this.effects.setInvisiblePredicate({ observee, observer ->
            observer != observee && observee.distanceToSqr(observer) < 6 * 6
        }, true)

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

    @Listener
    private fun onPlayerTick(event: PlayerTickEvent) {
        val (player) = event

        val glide = player.glide
        glide.tick(this, this.ticks)

        if (this.players.isPlaying(player)) {
            if (player.verticalCollision) {
                player.hurtServer(player.serverLevel(), player.damageSources().flyIntoWall(), 2.0F)
            }

            if (this.phase == GlidePhase.Active) {
                val tracker = this.trackers[player.uuid] ?: return
                if (this.ticks % GlideTrailTracker.TICK_INTERVAL == 0) {
                    tracker.update(player.position())
                }
            }
        }
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerHorizontalFlyFallCollision(event: PlayerHorizontalFlyFallCollisionEvent) {
        val (player) = event
        player.hurtServer(player.serverLevel(), player.damageSources().flyIntoWall(), 2.0F)
        player.playSound(player.fallSounds.small, 1.0F, 1.0F)

        event.cancel()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, source, amount) = event
        if (this.players.isSpectating(player)) {
            event.cancel()
            return
        }

        if (source.`is`(DamageTypes.FALL) || source.`is`(DamageTypes.FLY_INTO_WALL)) {
            if (player.health > 2.0F) {
                event.amount = 2.0F
                return
            }
        } else if (player.health > amount) {
            return
        }
        event.cancel()
        player.setGameMode(GameType.SPECTATOR)
        player.setVelocityAndMark(0.0, 0.0, 0.0)
        this.effects.freeze(player)

        this.removeBannerUI(player)
        this.sidebar.removePlayer(player)

        player.sendSound(GlideSounds.CRASH)
        player.setTitleAnimation(10.Ticks, 1.Seconds)
        player.sendTitle(
            GlideFont.YOU_CRASHED.wrap().shadowless(),
            GlideFont.YOU_CRASHED_BACKGROUND.wrap().shadowless(),
        )
        player.sendSystemMessage(Component.empty(), true)

        this.scheduler.schedule(1.Seconds) {
            this.effects.unfreeze(player)
            this.sidebar.addPlayer(player)
            this.respawnPlayer(player)
            if (this.roundEnding) {
                this.finish.addPlayer(player, shield = false)
            }
        }
    }

    @Listener
    private fun onServerTick(event: ServerTickEvent) {
        if (this.phase == GlidePhase.Active && !this.paused) {
            this.ticks++
        }

        this.tickProgress()

        for ((uuid, index) in this.playerIndices) {
            val player = event.server.player(uuid) ?: continue
            if (!player.isSpectator && !this.players.isSpectating(player)) {
                this.sendProgress(player, index)
            }
        }
        for (player in this.players.spectating) {
            player.sendSystemMessage(this.progressBar, true)
        }
    }

    @Listener
    private fun onPlayerSlotClick(event: PlayerSlotClickEvent) {
        event.cancel()
    }

    @Listener
    private fun onMinigameRemovePlayer(event: MinigameRemovePlayerEvent) {
        val player = event.player
        player.glide.reset()

        this.trackers.remove(player.uuid)

        this.removeBannerUI(player)

        if (player.team === this.team) {
            player.removeFromTeam()
        }
    }

    @Listener
    private fun onMinigameSetPlaying(event: MinigameSetPlayingEvent) {
        val player = event.player

        player.glide.respawnLocation = this.startPos
        player.glide.reset()
        this.setPlayerPlayingState(player)
        if (this.phase < GlidePhase.Active) {
            player.isNoGravity = true

            this.countdown.addPlayer(player)
        }
        player.addToTeam(this.team)

        this.trackers[player.uuid] = GlideTrailTracker(this.ticks.Ticks)
        this.playerIndices.computeIfAbsent(player.uuid, Object2IntFunction {
            this.playerIndices.size
        })
    }

    @Listener
    private fun onLoadPlaying(event: MinigameLoadPlayingEvent) {
        event.player.glide.showTrailFor(this.config.id, this.mode, this.ticks)
    }

    @Listener
    private fun onSetSpectating(event: MinigameSetSpectatingEvent) {
        event.player.glide.hideTrail()
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerPose(event: PlayerPoseEvent) {
        val (player, _, updated) = event
        if (updated != Pose.FALL_FLYING) {
            player.kill(event.player.serverLevel())
        }
    }

    @Listener(flags = ListenerFlags.IS_PLAYING, during = During(phases = ["active"]))
    private fun onPlayerSneak(event: PlayerSetSneakingEvent) {
        val (player, sneaking) = event
        if (this.round.mayPlayerRestart(player) && sneaking) {
            val last = this.stats.getOrCreateStat(player, GlideStats.LAST_SNEAK_TIME)
            if (abs(this.server.tickCount - last.value) < 7) {
                this.respawnPlayer(event.player)
            } else {
                last.modify { this.server.tickCount }
            }
        }
    }

    private fun respawnPlayer(player: ServerPlayer) {
        this.setPlayerPlayingState(player)
        player.sendSound(GlideSounds.RESPAWN, static = true)
        this.trackers[player.uuid]?.restart(this.ticks.Ticks)
    }

    private fun removeBannerUI(player: ServerPlayer) {
        this.checkpointBossbar.removePlayer(player)
        this.countdown.removePlayer(player)
        this.finish.removePlayer(player)
        this.end.removePlayer(player)
    }

    private fun setPlayerPlayingState(player: ServerPlayer) {
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
        GlobalTickedScheduler.schedule(1.Ticks) {
            player.startFallFlying()
        }
    }

    private fun sendProgress(player: ServerPlayer, index: Int) {
        val progressBar = Component.empty().append(this.progressBar)
        player.glide.addProgressComponents(this, progressBar, index)

        progressBar.append(SpacingFontResources.spaced(-171)).append(SpacingFontResources.spaced(-0.5F))

        val fullHearts = Mth.floor(player.health / 2.0)
        for (i in 0..< min(fullHearts, 3)) {
            progressBar.append(GlideFont.FULL_HEART.wrap().shadowless())
            progressBar.append(SpacingFontResources.spaced(-2))
        }
        if (fullHearts < 3) {
            var empty = 3 - fullHearts
            if (player.health - fullHearts * 2 >= 1.0) {
                progressBar.append(GlideFont.HALF_HEART.wrap().shadowless())
                progressBar.append(SpacingFontResources.spaced(-2))
                empty--
            }
            for (i in 0..< empty) {
                progressBar.append(GlideFont.EMPTY_HEART.wrap().shadowless())
                progressBar.append(SpacingFontResources.spaced(-2))
            }
        }

        progressBar.append(SpacingFontResources.spaced(145))
        player.sendSystemMessage(progressBar, true)
    }

    private fun tickProgress() {
        val progressBar = Component.empty().append(this.checkpoints.progressBar)
        var lastOffset = 309
        for ((i, player) in this.players.playing.withIndex()) {
            val offset = Mth.clamp(Math.round(player.glide.progress * 300).toInt(), 0, 300)

            progressBar.append(SpacingFontResources.spaced(offset - lastOffset))
            progressBar.append(GlideFont.Progressbar.getPlayerMarker(i).wrap().shadowless())
            lastOffset = offset + 6
        }

        progressBar.append(SpacingFontResources.spaced(309 - lastOffset))
        this.progressBar = progressBar
    }

    companion object {
        val ID = GlideMod.id("glide_minigame")

        fun create(server: MinecraftServer, uuid: UUID, addon: GlideAddon, mode: GlideMode): GlideMinigame {
            val level = CustomLevelBuilder.build(server) {
                randomDimensionKey()
                dimensionType(addon.dimensionType)
                chunkGenerator(VoidChunkGenerator(server))
            }

            addon.extractWorld(server.getDimensionPath(level.dimension()))
            return GlideMinigame(server, uuid, addon.config, SoloGlideRound(), mode, level)
        }
    }
}