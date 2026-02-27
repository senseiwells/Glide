package me.senseiwells.glide.minigame

import com.mojang.authlib.GameProfile
import it.unimi.dsi.fastutil.objects.Object2IntFunction
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.addon.GlideAddon
import me.senseiwells.glide.addon.GlideAddonConfig
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.gui.*
import me.senseiwells.glide.gui.debug.SidebarDebugGlideVelocityElement
import me.senseiwells.glide.gui.debug.SidebarDebugVelocityElement
import me.senseiwells.glide.minigame.command.ContextualGlideCommand
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.tracker.GlideTrailTracker
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.getDimensionPath
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.events.server.player.*
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.During
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFlags
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.launch
import net.casual.arcade.minigame.utils.MinigameUtils.launchPhased
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.resources.utils.withShiftedDownFont
import net.casual.arcade.scheduler.GlobalTickedScheduler
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
import net.casual.arcade.utils.TeamUtils.getOrCreateTeam
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.coroutine.delay
import net.casual.arcade.utils.isOf
import net.casual.arcade.utils.set
import net.casual.arcade.utils.setVelocityAndMark
import net.casual.arcade.utils.teleportTo
import net.casual.arcade.visuals.bossbar.SuppliedBossbar
import net.casual.arcade.visuals.sidebar.FixedSidebar
import net.casual.arcade.visuals.utils.elements.ComponentElements
import net.casual.arcade.visuals.utils.elements.SidebarElements
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.util.Unit
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.Team
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class GlideMinigame(
    server: MinecraftServer,
    uuid: UUID,
    private val config: GlideAddonConfig,
    val round: GlideRoundMode,
    val mode: GlideMode,
    val level: ServerLevel
): Minigame(server, uuid) {
    override val id: Identifier = ID

    // TODO: Rework this
    private val team = this.server.scoreboard.getOrCreateTeam("Glide") {
        setSeeFriendlyInvisibles(true)
        collisionRule = Team.CollisionRule.NEVER
    }

    private val checkpointBossbar = SuppliedBossbar(ComponentElements.of(GlideComponents.CHECKPOINT.withShiftedDownFont(12)))
    private val startPos = this.config.spawn

    private val countdown = GlideStartBanner(this.round)
    private val finish = GlideFinishBanner(this)
    private val end = GlideEndBanner(this)

    private val sidebar = FixedSidebar(ComponentElements.empty())
    private var progressBar: Component = Component.empty()

    private val trackers = HashMap<UUID, GlideTrailTracker>()
    private val playerIndices = Object2IntOpenHashMap<UUID>()
    private val frozenPlayers = Object2ObjectOpenHashMap<UUID, Vec3>()

    private var placement = 1
    private var roundEnding = false

    private var winning: GameProfile? = null

    var ticks = 0
        private set

    val checkpoints: IndexedCheckpoints
        get() = this.config.checkpoints

    fun getWinner(): String {
        return this.winning?.name ?: "Unknown"
    }

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
        val updated = player.glide.updateBests(this.config.addon_id, this, this.ticks.Ticks, tracker.finish())
        if (updated) {
            this.scheduler.schedule(1.Seconds) {
                player.sendSound(this.mode.getPersonalBestSound(), static = true)
            }
        }

        val remaining = this.players.playingPlayerCount
        if (remaining > 0) {
            if (this.placement == 2) {
                this.startEndingRound()
            } else {
                this.sendFinish(player)
            }
        } else {
            this.setPhase(GlidePhase.Finish)
        }
    }

    fun endRound() {
        this.winning = this.mode.getUpdatedWinner(this, this.winning)

        this.end.setTitle(GlideComponents.ROUND_END.withShiftedDownFont(12))
        for (player in this.players) {
            this.players.setSpectating(player)
            this.removeBannerUI(player)
            player.glide.resetAnimation()
            this.end.addPlayer(player)
        }
        this.launchPhased {
            delay(1.Seconds)
            end.transition(4.Seconds, players = players::all)
            delay(1.Seconds)
            round.onRoundFinished(this@GlideMinigame)
        }
    }

    fun restart() {
        this.placement = 1
        this.ticks = 0
        this.roundEnding = false
        this.trackers.clear()

        this.setPhase(GlidePhase.Waiting)

        for (player in this.players) {
            this.players.setSpectating(player)
            player.teleportTo(this.startPos.with(this.level))
            this.players.setPlaying(player)
        }
    }

    fun freezePlayer(player: ServerPlayer) {
        player.isNoGravity = true
        this.frozenPlayers[player.uuid] = player.position()
    }

    fun unfreezePlayer(player: ServerPlayer) {
        player.isNoGravity = false
        this.frozenPlayers.remove(player.uuid)
    }

    fun startEndingRound() {
        if (this.roundEnding) {
            return
        }

        this.roundEnding = true
        for (player in this.players) {
            this.sendFinish(player)
        }
        this.launch {
            finish.transition(60.Seconds, players = players::all)
            setPhase(GlidePhase.Finish)
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
        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            this.sidebar.addRow(SidebarElements.empty())
            this.sidebar.addRow(SidebarDebugVelocityElement)
            this.sidebar.addRow(SidebarDebugGlideVelocityElement)
        }

        this.sidebar.addRow(SidebarElements.empty())
        this.visuals.setSidebar(this.sidebar)

        this.visuals.countdown = this.countdown

        this.music.suppressGameMusic(true)
        this.music.provider = GlideMusicProvider

        this.settings.tickFreezeOnPause.set(true)
        this.settings.canGetHungry.set(false)
        this.settings.canPvp.set(false)
        this.levels.setGameRules {
            set(GameRules.KEEP_INVENTORY, true)
            set(GameRules.NATURAL_HEALTH_REGENERATION, false)
            set(GameRules.SPAWN_MOBS, false)
            set(GameRules.RANDOM_TICK_SPEED, 0)
            set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0)
            set(GameRules.ADVANCE_WEATHER, false)
            set(GameRules.ADVANCE_TIME, false)
        }

        this.commands.register(ContextualGlideCommand(this))

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

        val frozenPosition = this.frozenPlayers[player.uuid]
        if (frozenPosition != null) {
            player.setVelocityAndMark(0.0, 0.0, 0.0)
            player.teleportTo(frozenPosition)
        }

        val glide = player.glide
        glide.tick(this)

        if (this.players.isPlaying(player)) {
            if (this.phase > GlidePhase.Countdown) {
                if (player.verticalCollision) {
                    player.hurtServer(player.level(), player.damageSources().flyIntoWall(), 2.0F)
                }

                if (this.phase == GlidePhase.Active) {
                    val tracker = this.trackers[player.uuid] ?: return
                    if (this.ticks % GlideTrailTracker.TICK_INTERVAL == 0) {
                        tracker.update(player.position())
                    }
                }
            } else {
                player.teleportTo(this.startPos.x, this.startPos.y, this.startPos.z)
            }
        }
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerHorizontalFlyFallCollision(event: PlayerHorizontalFlyFallCollisionEvent) {
        val (player) = event
        player.hurtServer(player.level(), player.damageSources().flyIntoWall(), 2.0F)
        player.playSound(player.fallSounds.small, 1.0F, 1.0F)

        event.cancel()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, source, amount) = event
        if (this.players.isSpectating(player) || this.phase < GlidePhase.Active) {
            event.cancel()
            return
        }

        if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.FLY_INTO_WALL)) {
            if (player.health > 2.0F) {
                event.amount = 2.0F
                return
            }
        } else if (player.health > amount) {
            return
        }
        event.cancel()
        player.setGameMode(GameType.SPECTATOR)
        player.pose = Pose.STANDING
        player.setVelocityAndMark(0.0, 0.0, 0.0)
        this.freezePlayer(player)

        this.removeBannerUI(player)
        this.sidebar.removePlayer(player)

        player.sendSound(GlideSounds.CRASH)
        player.setTitleAnimation(10.Ticks, 1.Seconds)
        player.sendTitle(
            GlideFont.YOU_CRASHED.wrap().shadowless(),
            GlideFont.YOU_CRASHED_BACKGROUND.wrap().shadowless(),
        )
        player.sendSystemMessage(Component.empty(), true)

        this.scheduler.schedulePhased(1.Seconds) {
            this.unfreezePlayer(player)
            this.sidebar.addPlayer(player)
            this.respawnPlayer(player)
            if (this.roundEnding) {
                this.finish.addPlayer(player, shield = false)
            }
        }
    }

    @Listener
    private fun onServerTick(event: ServerTickEvent) {
        if (this.phase == GlidePhase.Active && !this.paused && this.level.tickRateManager().runsNormally()) {
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
            player.verticalCollision = false
            this.freezePlayer(player)

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
        event.player.glide.showTrailFor(this.config.addon_id, this)
    }

    @Listener
    private fun onSetSpectating(event: MinigameSetSpectatingEvent) {
        event.player.glide.hideTrail()
    }

    @Listener(flags = ListenerFlags.IS_PLAYING)
    private fun onPlayerPose(event: PlayerPoseEvent) {
        val (player, _, updated) = event
        if (updated != Pose.FALL_FLYING && !player.isSpectator) {
            player.kill(player.level())
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
        player.teleportTo(player.glide.respawnLocation.with(this.level))
        player.setVelocityAndMark(0.0, 0.0, 0.0)

        // Ensure the player has loaded the chunks
        GlobalTickedScheduler.schedule(1.Ticks) {
            val elytra = ItemStack(Items.ELYTRA)
            elytra.set(DataComponents.UNBREAKABLE, Unit.INSTANCE)
            player.setItemSlot(EquipmentSlot.CHEST, elytra)

            player.teleportTo(player.glide.respawnLocation.with(this.level))
            player.setVelocityAndMark(0.0, 0.0, 0.0)
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
        this.round.addToProgressBar(progressBar)
        player.sendSystemMessage(progressBar, true)
    }

    private fun tickProgress() {
        val progressBar = Component.empty().append(this.checkpoints.progressBar)
        var lastOffset = 309
        for (player in this.players.playing) {
            val index = this.playerIndices.getInt(player.uuid) % 16
            val offset = Mth.clamp((player.glide.progress * 300).roundToInt(), 0, 300)

            progressBar.append(SpacingFontResources.spaced(offset - lastOffset))
            progressBar.append(GlideFont.Progressbar.getPlayerMarker(index).wrap().shadowless())
            lastOffset = offset + 6
        }

        progressBar.append(SpacingFontResources.spaced(309 - lastOffset))
        this.progressBar = progressBar
    }

    companion object {
        val ID = GlideMod.id("glide_minigame")

        fun create(
            server: MinecraftServer,
            uuid: UUID,
            addon: GlideAddon,
            round: GlideRoundMode,
            mode: GlideMode
        ): GlideMinigame {
            val level = CustomLevelBuilder.build(server) {
                randomDimensionKey()
                spoofedDimensionKey(addon.config.addon_id)
                dimensionType(addon.dimensionType)
                chunkGenerator(VoidChunkGenerator(server))
            }

            addon.extractWorld(server.getDimensionPath(level.dimension()))
            return GlideMinigame(server, uuid, addon.config, round, mode, level)
        }
    }
}