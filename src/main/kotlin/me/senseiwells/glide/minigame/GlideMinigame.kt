package me.senseiwells.glide.minigame

import com.mojang.authlib.GameProfile
import it.unimi.dsi.fastutil.objects.Object2IntFunction
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.awaitCancellation
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.gui.*
import me.senseiwells.glide.gui.debug.SidebarDebugGlideVelocityElement
import me.senseiwells.glide.gui.debug.SidebarDebugVelocityElement
import me.senseiwells.glide.minigame.command.GlideMinigameCommand
import me.senseiwells.glide.module.*
import me.senseiwells.glide.pack.GlidePacks
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.tracker.GlideTrailTracker
import me.senseiwells.glide.utils.boostHealth
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.events.server.player.*
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFilter
import net.casual.arcade.minigame.data.MinigameDataSet
import net.casual.arcade.minigame.data.impl.MinigameWorldData
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.phase.MinigamePhaseLifetime
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.observer.utils.asObserver
import net.casual.arcade.pack.PackInfo
import net.casual.arcade.pack.font.spacing.SpacingFontResources
import net.casual.arcade.pack.generation.utils.add
import net.casual.arcade.pack.host.GlobalPackHost
import net.casual.arcade.pack.utils.PackInfoRef
import net.casual.arcade.pack.utils.ResourcePackUtils
import net.casual.arcade.pack.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.pack.utils.withShiftedDownFont
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.scheduler.utils.launch
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.coroutine.delay
import net.casual.arcade.utils.entity.setVelocityAndMark
import net.casual.arcade.utils.entity.teleportTo
import net.casual.arcade.utils.impl.TimedSound
import net.casual.arcade.utils.level.set
import net.casual.arcade.utils.player.*
import net.casual.arcade.utils.registries.isOf
import net.casual.arcade.utils.registries.toKey
import net.casual.arcade.utils.scoreboard.getOrCreateTeam
import net.casual.arcade.utils.server.player
import net.casual.arcade.virtual.visuals.bossbar.VirtualBossbar
import net.casual.arcade.virtual.visuals.sidebar.DynamicVirtualSidebar
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
import net.casual.arcade.virtual.visuals.utils.startObservingAndSendPackets
import net.casual.arcade.virtual.visuals.utils.stopObservingAndSendPackets
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
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
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
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
    val data: MinigameDataSet,
    val round: GlideRoundMode,
    val mode: GlideMode
): Minigame(server, uuid, ID, GlidePhase.entries) {
    // TODO: Rework this
    private val team = this.server.scoreboard.getOrCreateTeam("Glide").apply {
        setSeeFriendlyInvisibles(true)
        collisionRule = Team.CollisionRule.NEVER
    }

    private val metadata: GlideMetaData = this.data.require(GlideMetaData.type)
    private val checkpoints: IndexedCheckpoints = this.data.require(GlideCheckpointsData.type).checkpoints

    private val checkpointBossbar = VirtualBossbar()
    private val startPos = this.metadata.spawn

    private val countdown = GlideStartBanner(this)
    private val finish = GlideFinishBanner(this)
    private val end = GlideEndBanner(this)

    private val sidebar = DynamicVirtualSidebar(this.server)
    private var progressBar: Component = Component.empty()

    private val trackers = HashMap<UUID, GlideTrailTracker>()
    private val playerIndices = Object2IntOpenHashMap<UUID>()
    private val frozenPlayers = Object2ObjectOpenHashMap<UUID, Vec3>()

    private var placement = 1
    private var roundEnding = false

    private var winner: GameProfile? = null

    val level: ServerLevel
        get() = this.levels.require(LEVEL_ID)

    var ticks = 0
        private set

    init {
        this.checkpointBossbar.title.set(GlideComponents.CHECKPOINT.withShiftedDownFont(12))
    }

    fun checkpoints(): IndexedCheckpoints {
        return this.checkpoints
    }

    fun winner(): String {
        return this.winner?.name ?: "Unknown"
    }

    fun displayCheckpointNotificationTo(player: ServerPlayer) {
        this.checkpointBossbar.startObservingAndSendPackets(player.asObserver())
        // 2 Seconds, then 1 second fade
        GlobalTickedScheduler.Server.schedule(3.Seconds) {
            this.checkpointBossbar.stopObservingAndSendPackets(player.asObserver())
        }
    }

    fun markPlayerFinished(player: ServerPlayer) {
        this.players.setSpectating(player)
        player.sendSound(GlideSounds.getFinishSound(this.placement++), static = true)

        val tracker = this.trackers.remove(player.uuid) ?: return
        val updated = player.glide.updateBests(this.metadata.id, this, this.ticks.Ticks, tracker.finish())
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
            this.phases.set(GlidePhase.Finish)
        }
    }

    fun endRound() {
        this.winner = this.mode.getUpdatedWinner(this, this.winner)

        this.end.setTitle(GlideComponents.ROUND_END.withShiftedDownFont(12))
        for (player in this.players) {
            this.players.setSpectating(player)
            this.removeBannerUI(player)
            player.glide.resetAnimation()
            this.end.addPlayer(player)
        }
        val scope = this.scopes.create(MinigamePhaseLifetime.Current)
        scope.launch {
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

        this.phases.set(GlidePhase.Waiting)

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
        val scope = this.scopes.create(MinigamePhaseLifetime.Current)
        scope.launch {
            finish.transition(60.Seconds, players = players::all)
            phases.set(GlidePhase.Finish)
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
        this.initializeLevel()
        this.initializeSidebar()
        this.initializePhases()

        this.visuals.countdown = this.countdown

        this.music.suppressGameMusic(true)
        this.music.provider = GlideMusicProvider(this.getMusic())

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
            set(GameRules.LOCATOR_BAR, false)
        }

        this.commands.register(GlideMinigameCommand(this))

        this.effects.setInvisiblePredicate({ observee, observer ->
            observer != observee && observee.distanceToSqr(observer) < 6 * 6
        }, true)

        val accelerators = this.data.get(GlideAcceleratorsData.type)
        if (accelerators != null) {
            for (booster in accelerators.boosters) {
                this.level.addFreshEntity(booster.create(this.level))
            }
            for (thermal in accelerators.thermals) {
                this.level.addFreshEntity(thermal.create(this.level))
            }
        }
        for (checkpoint in this.checkpoints.all()) {
            for (beam in checkpoint.createBeaconBeams(this.level)) {
                this.level.addFreshEntity(beam)
            }
        }
        val rings = this.data.get(GlideRingsData.type)
        if (this.mode == GlideMode.ScoreAttack && rings != null) {
            for (ring in rings.rings) {
                this.level.addFreshEntity(ring.create(this.level))
            }
        }

        val packs = ArrayList(GlidePacks.get())
        val module = this.data.get(GlideResourcePackData.type)
        if (module != null) {
            packs.add(GlobalPackHost.add(module.definition).toPackInfo())
        }
        this.resources.add(object: MinigameResources {
            override fun getPacks(): Collection<PackInfo> {
                return packs.map(PackInfoRef::join)
            }
        })
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
            if (this.state > GlidePhase.Countdown) {
                if (player.verticalCollision) {
                    player.hurtServer(player.level(), player.damageSources().flyIntoWall(), 2.0F)
                }

                if (this.state.isAt(GlidePhase.Active)) {
                    val tracker = this.trackers[player.uuid] ?: return
                    if (this.ticks % GlideTrailTracker.TICK_INTERVAL == 0) {
                        tracker.update(player.position())
                    }
                }
            } else {
                player.teleportTo(this.startPos.x, this.startPos.y, this.startPos.z)
                player.startFallFlying()
            }
        }
    }

    @Listener(filters = [ListenerFilter.IsPlaying])
    private fun onPlayerHorizontalFlyFallCollision(event: PlayerHorizontalFlyFallCollisionEvent) {
        val (player) = event
        if (player.hurtServer(player.level(), player.damageSources().flyIntoWall(), 2.0F)) {
            player.playSound(player.fallSounds.small, 1.0F, 1.0F)
        }

        event.cancel()
    }

    @Listener
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, source, amount) = event
        if (this.players.isSpectating(player) || this.state < GlidePhase.Active) {
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
        this.sidebar.stopObservingAndSendPackets(player.asObserver())

        player.sendSound(GlideSounds.CRASH)
        player.setTitleAnimation(10.Ticks, 1.Seconds)
        player.sendTitle(
            GlideFont.YOU_CRASHED.wrap().shadowless(),
            GlideFont.YOU_CRASHED_BACKGROUND.wrap().shadowless(),
        )
        player.sendSystemMessage(Component.empty(), true)

        val scope = this.scopes.create(MinigamePhaseLifetime.Current)
        scope.schedule(1.Seconds) {
            this.unfreezePlayer(player)
            this.sidebar.startObservingAndSendPackets(player.asObserver())
            this.respawnPlayer(player)
            if (this.roundEnding) {
                this.finish.addPlayer(player, shield = false)
            }
        }
    }

    @Listener
    private fun onServerTick(event: ServerTickEvent) {
        if (this.state.isAt(GlidePhase.Active) && !this.paused && this.level.tickRateManager().runsNormally()) {
            this.ticks++
        }

        this.tickBanners()
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
        if (this.state < GlidePhase.Active) {
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
        event.player.glide.showTrailFor(this.metadata.id, this)
    }

    @Listener
    private fun onSetSpectating(event: MinigameSetSpectatingEvent) {
        event.player.glide.hideTrail()
    }

    @Listener(filters = [ListenerFilter.IsPlaying])
    private fun onPlayerPose(event: PlayerPoseEvent) {
        val (player, _, updated) = event
        if (updated != Pose.FALL_FLYING && !player.isSpectator) {
            player.kill(player.level())
        }
    }

    @Listener(filters = [ListenerFilter.IsPlaying])
    private fun onPlayerSneak(event: PlayerSetSneakingEvent) {
        if (!this.state.isAt(GlidePhase.Active)) {
            return
        }

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

    private fun initializeLevel() {
        var dimensionType = when {
            this.data.has(GlideDimensionTypeData.type) -> this.metadata.id.toKey(Registries.DIMENSION_TYPE)
            else -> BuiltinDimensionTypes.OVERWORLD
        }
        if (this.server.registryAccess().get(dimensionType).isEmpty) {
            GlideMod.logger.error("Custom dimension type '${this.metadata.id}' was never registered, defaulting to overworld")
            dimensionType = BuiltinDimensionTypes.OVERWORLD
        }

        this.levels.create(LEVEL_ID) {
            randomDimensionKey()
            spoofedDimensionKey(metadata.id)
            dimensionType(dimensionType)
            chunkGenerator(VoidChunkGenerator(server))
        }
        this.data.require(MinigameWorldData.type).extract(this.server, this.level.dimension())
    }

    private fun initializeSidebar() {
        var row = 0
        this.sidebar.row(row++).set(SidebarComponent.EMPTY)
        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            this.sidebar.setRow(row++, SidebarDebugGlideVelocityElement)
            this.sidebar.setRow(row++, SidebarDebugVelocityElement)
            this.sidebar.row(row++).set(SidebarComponent.EMPTY)
        }
        if (this.mode == GlideMode.ScoreAttack) {
            this.sidebar.setRow(row++, SidebarScoreElement)
            this.sidebar.row(row++).set(SidebarComponent.EMPTY)
        }

        this.sidebar.setRow(row++, SidebarVelocityElement)
        this.sidebar.row(row++).set(SidebarComponent.EMPTY)
        this.sidebar.setRow(row, PlayerSidebarTimerElement(GlobalSidebarTimerElement { this.ticks.Ticks }))

        this.visuals.setSidebar(this.sidebar)
    }

    private fun initializePhases() {
        this.phases.coroutines[GlidePhase.Waiting] = this::runWaitingLogic
        this.phases.coroutines[GlidePhase.Countdown] = this::runCountdownLogic
        this.phases.coroutines[GlidePhase.Active] = this::runActiveLogic
        this.phases.coroutines[GlidePhase.Finish] = this::runFinishLogic
    }

    private suspend fun runWaitingLogic() {
        ResourcePackUtils.awaitPacks(this.players)
    }

    private suspend fun runCountdownLogic() {
        this.visuals.countdown.transition(this.round.getCountdownTime(), players = this.players::all)
    }

    private suspend fun runActiveLogic() {
        for (player in this.players) {
            this.unfreezePlayer(player)
        }
        this.music.start()
        awaitCancellation()
    }

    private suspend fun runFinishLogic() {
        this.settings.canTakeDamage.set(false)
        this.endRound()
        awaitCancellation()
    }

    private fun respawnPlayer(player: ServerPlayer) {
        this.setPlayerPlayingState(player)
        player.sendSound(GlideSounds.RESPAWN, static = true)
        this.trackers[player.uuid]?.restart(this.ticks.Ticks)
    }

    private fun removeBannerUI(player: ServerPlayer) {
        this.checkpointBossbar.stopObservingAndSendPackets(player.asObserver())
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
        GlobalTickedScheduler.Server.schedule(1.Ticks) {
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
        repeat(min(fullHearts, 3)) {
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
            repeat(empty) {
                progressBar.append(GlideFont.EMPTY_HEART.wrap().shadowless())
                progressBar.append(SpacingFontResources.spaced(-2))
            }
        }

        progressBar.append(SpacingFontResources.spaced(145))
        this.round.addToProgressBar(progressBar)
        player.sendSystemMessage(progressBar, true)
    }

    private fun tickBanners() {
        this.countdown.tick()
        this.finish.tick()
        this.end.tick()
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

    private fun getMusic(): List<TimedSound> {
        val module = this.data.get(GlideMusicData.type)
        if (module != null) {
            return module.music
        }
        return listOf(GlideSounds.MAP_1_MUSIC, GlideSounds.MAP_2_MUSIC, GlideSounds.MAP_3_MUSIC)
    }

    companion object {
        private val LEVEL_ID = GlideMod.id("level")

        val ID = GlideMod.id("glide_minigame")

        fun create(
            server: MinecraftServer,
            uuid: UUID,
            data: MinigameDataSet,
            round: GlideRoundMode,
            mode: GlideMode
        ): GlideMinigame {
            return GlideMinigame(server, uuid, data, round, mode)
        }
    }
}