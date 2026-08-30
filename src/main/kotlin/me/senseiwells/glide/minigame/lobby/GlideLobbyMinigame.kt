package me.senseiwells.glide.minigame.lobby

import kotlinx.coroutines.Job
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.minigame.MultiGlideRoundMode
import me.senseiwells.glide.minigame.lobby.command.GlideLobbyCommand
import me.senseiwells.glide.minigame.lobby.gui.GlideLobbyCountdown
import me.senseiwells.glide.minigame.lobby.vote.GlideLobbyVotes
import me.senseiwells.glide.minigame.lobby.vote.GlideVote
import me.senseiwells.glide.module.GlideLobbyData
import me.senseiwells.glide.module.GlideModules
import me.senseiwells.glide.pack.GlidePacks
import me.senseiwells.glide.utils.unboostHealth
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.events.server.player.PlayerLeaveEvent
import net.casual.arcade.events.server.player.PlayerVoidDamageEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.data.MinigameDataModules
import net.casual.arcade.minigame.data.MinigameDataModules.Companion.get
import net.casual.arcade.minigame.data.module.MinigameWorldData
import net.casual.arcade.minigame.events.MinigameAddNewPlayerEvent
import net.casual.arcade.minigame.events.MinigameAddPlayerEvent
import net.casual.arcade.minigame.events.MinigameCloseEvent
import net.casual.arcade.minigame.events.MinigameInitializeEvent
import net.casual.arcade.minigame.events.MinigameRemovePlayerEvent
import net.casual.arcade.minigame.managers.MinigameLevelManager.SpawnLocation
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.minigame.utils.MinigameUtils.launchPhased
import net.casual.arcade.pack.PackInfo
import net.casual.arcade.pack.utils.PackInfoRef
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.entity.teleportTo
import net.casual.arcade.utils.level.resetToDefault
import net.casual.arcade.utils.level.set
import net.casual.arcade.utils.math.location.LocationWithLevel
import net.casual.arcade.utils.player.clearPlayerInventory
import net.casual.arcade.utils.player.displayName
import net.casual.arcade.utils.player.clearTitle
import net.casual.arcade.utils.player.resetExperience
import net.casual.arcade.utils.player.resetHealth
import net.casual.arcade.utils.player.resetHunger
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.visuals.elements.UniversalElement
import net.casual.arcade.virtual.visuals.sidebar.DynamicVirtualSidebar
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.gamerules.GameRules
import java.util.*
import kotlin.jvm.optionals.getOrNull

class GlideLobbyMinigame(
    server: MinecraftServer,
    uuid: UUID,
    val modules: MinigameDataModules
): Minigame(server, uuid) {
    private val data: GlideLobbyData = this.modules.get<GlideLobbyData>() ?: GlideLobbyData.DEFAULT
    override val settings: GlideLobbySettings = GlideLobbySettings(this, this.availableMaps())

    private val sidebar = DynamicVirtualSidebar(this.server)

    private var countdownJob: Job? = null
    val countdown: GlideLobbyCountdown = GlideLobbyCountdown(this)


    val level: ServerLevel = this.createLevel()
    val votes: GlideLobbyVotes = GlideLobbyVotes(this)


    var glide: GlideMinigame? = null
        private set

    override val id: Identifier = ID

    override fun phases(): Collection<Phase<GlideLobbyMinigame>> {
        return GlideLobbyPhase.entries
    }

    fun availableMaps(): List<Identifier> {
        return GlideModules.ids().toList()
    }

    fun configuration(): GlideConfiguration? {
        return this.votes.resolve(this.level.random)
    }

    fun startCountdown(delay: MinecraftTimeDuration?) {
        this.stopCountdown()

        if (delay == null || this.phase != GlideLobbyPhase.Starting || this.glide != null) {
            return
        }
        this.countdownJob = this.launchPhased {
            try {
                countdown.transition(delay, players = players::all)
            } finally {
                countdown.stop()
            }
            startGlide()
        }
    }

    fun stopCountdown() {
        this.countdownJob?.cancel()
        this.countdownJob = null
    }

    fun isReady(player: ServerPlayer): Boolean {
        return this.tags.has(player, READY)
    }

    fun readyCount(): Int {
        return this.tags.getUUIDsFor(READY).size
    }

    fun setReady(player: ServerPlayer, ready: Boolean): Boolean {
        val changed = when {
            ready -> this.tags.add(player, READY)
            else -> this.tags.remove(player, READY)
        }
        if (!changed) {
            return false
        }
        val key = if (ready) "glide.lobby.ready" else "glide.lobby.unready"
        this.chat.broadcast(
            Component.translatable(key, player.displayName(), this.readyCount(), this.players.playingPlayerCount)
        )
        this.checkAllReady()
        return true
    }

    fun clearReady() {
        this.tags.clear(READY)
    }

    private fun checkAllReady() {
        if (this.phase != GlideLobbyPhase.Starting || this.glide != null) {
            return
        }
        val playing = this.players.playing
        if (playing.isNotEmpty() && playing.all(this::isReady)) {
            this.startGlide()
        }
    }

    fun startGlide() {
        if (this.players.onlinePlayerCount == 0) {
            GlideMod.logger.error("Glide lobby failed to start, it has no players")
            this.setPhase(GlideLobbyPhase.Waiting)
            return
        }

        val configuration = this.configuration()
        if (configuration == null) {
            GlideMod.logger.error("Glide lobby failed to start, it has no available maps")
            this.setPhase(GlideLobbyPhase.Waiting)
            return
        }

        val modules = GlideModules.get(configuration.map)
        if (modules == null) {
            GlideMod.logger.error("Glide lobby failed to start, no such map ${configuration.map}")
            this.setPhase(GlideLobbyPhase.Waiting)
            return
        }

        val glide = GlideMinigame.create(
            this.server,
            UUID.randomUUID(),
            modules,
            MultiGlideRoundMode(configuration.rounds),
            configuration.mode
        )
        glide.events.register<MinigameCloseEvent> { this.returnToLobby(glide) }
        glide.events.register<PlayerLeaveEvent>(priority = 10_000) {
            if (glide.players.onlinePlayerCount == 0) {
                glide.close()
            }
        }

        this.glide = glide
        this.players.transferTo(glide)
        glide.start()

        this.setPhase(GlideLobbyPhase.Waiting)
    }

    fun teleport(player: ServerPlayer) {
        player.teleportTo(this.spawn())
    }

    private fun spawn(): LocationWithLevel<ServerLevel> {
        return this.data.spawn.get().with(this.level)
    }

    private fun returnToLobby(glide: GlideMinigame) {
        if (this.glide === glide) {
            this.glide = null
        }
        if (this.closed) {
            return
        }
        glide.players.transferTo(this)

        if (this.phase == GlideLobbyPhase.Waiting && this.players.onlinePlayerCount > 0) {
            this.setPhase(GlideLobbyPhase.Starting)
        }
    }

    @Listener
    private fun onInitialize(event: MinigameInitializeEvent) {
        this.levels.add(this.level)
        this.levels.spawn = SpawnLocation.global(this.spawn())

        this.commands.register(GlideLobbyCommand(this))

        this.settings.startDelay.addListener { _, _, value ->
            this.startCountdown(value.getOrNull())
        }

        this.sidebar.setTitle(UniversalElement.constant(Component.translatable("glide.lobby.title")))
        this.sidebar.row(0).set(SidebarComponent.EMPTY)
        this.sidebar.setRow(1, UniversalElement {
            this.row("glide.lobby.sidebar.rounds", this.votes.rounds)
        })
        this.sidebar.setRow(2, UniversalElement {
            this.row("glide.lobby.sidebar.mode", this.votes.mode)
        })
        this.sidebar.setRow(3, UniversalElement {
            this.row("glide.lobby.sidebar.map", this.votes.map)
        })
        this.sidebar.row(4).set(SidebarComponent.EMPTY)
        this.visuals.setSidebar(this.sidebar)

        val packs = GlidePacks.get()
        this.resources.add(object: MinigameResources {
            override fun getPacks(): Collection<PackInfo> {
                return packs.map(PackInfoRef::join)
            }
        })

        this.settings.pauseOnServerStop = false
        this.settings.canPvp.set(false)
        this.settings.canGetHungry.set(false)
        this.settings.canTakeDamage.set(false)
        this.settings.canBreakBlocks.set(false)
        this.settings.canPlaceBlocks.set(false)
        this.settings.canDropItems.set(false)
        this.settings.canPickupItems.set(false)
        this.settings.canInteractAll = false
    }

    @Listener
    private fun onAddNewPlayer(event: MinigameAddNewPlayerEvent) {
        val player = event.player
        player.unboostHealth()
        player.resetHealth()
        player.resetHunger()
        player.resetExperience()
        player.removeAllEffects()
        this.teleport(player)
    }

    @Listener
    private fun onAddPlayer(event: MinigameAddPlayerEvent) {
        val player = event.player

        val glide = this.glide
        if (glide != null) {
            GlobalTickedScheduler.Server.later {
                glide.players.add(player)
            }
            return
        }

        player.stopFallFlying()
        player.clearTitle()
        player.clearPlayerInventory()
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY)
        player.glide.reset()

        if (!this.players.isAdmin(player)) {
            player.setGameMode(GameType.ADVENTURE)
        }

        if (this.phase == GlideLobbyPhase.Waiting) {
            this.setPhase(GlideLobbyPhase.Starting)
        }
    }

    @Listener
    private fun onRemovePlayer(event: MinigameRemovePlayerEvent) {
        this.votes.retract(event.player)
        this.tags.remove(event.player, READY)

        if (this.players.onlinePlayerCount == 0) {
            this.setPhase(GlideLobbyPhase.Waiting)
            return
        }
        this.checkAllReady()
    }

    @Listener
    private fun onClose(event: MinigameCloseEvent) {
        this.modules.close()
    }

    @Listener
    private fun onPlayerVoidDamage(event: PlayerVoidDamageEvent) {
        this.teleport(event.player)
        event.cancel()
    }

    private fun row(key: String, vote: GlideVote<*>): SidebarComponent {
        return SidebarComponent.withCustomScore(Component.translatable(key), vote.selectionName())
    }

    private fun createLevel(): ServerLevel {
        val data = this.data
        val level = CustomLevelBuilder.build(this.server) {
            randomDimensionKey()
            spoofedDimensionKey(ID)
            dimensionType(BuiltinDimensionTypes.OVERWORLD)
            chunkGenerator(VoidChunkGenerator(server))
            defaultLevelProperties()
            viewDistance(16)
            gameRules {
                resetToDefault()
                set(GameRules.KEEP_INVENTORY, true)
                set(GameRules.SPAWN_MOBS, false)
                set(GameRules.MOB_DROPS, false)
                set(GameRules.BLOCK_DROPS, false)
                set(GameRules.ENTITY_DROPS, false)
                set(GameRules.FALL_DAMAGE, false)
                set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0)
                set(GameRules.RANDOM_TICK_SPEED, 0)
                set(GameRules.ADVANCE_WEATHER, false)
                set(GameRules.LOCATOR_BAR, false)
            }
            when {
                data.timeOfDay.isEmpty -> clockState(paused = false)
                else -> clockState(totalTicks = data.timeOfDay.get().toLong(), paused = true)
            }
        }
        this.modules.get<MinigameWorldData>()?.extract(this.server, level.dimension())
        return level
    }

    companion object {
        val ID = GlideMod.id("lobby")

        private val READY = GlideMod.id("ready")

        fun create(server: MinecraftServer, uuid: UUID): GlideLobbyMinigame {
            return GlideLobbyMinigame(server, uuid, GlideModules.lobby(server))
        }
    }
}
