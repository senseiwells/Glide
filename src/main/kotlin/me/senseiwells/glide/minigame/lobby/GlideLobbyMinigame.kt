package me.senseiwells.glide.minigame.lobby

import kotlinx.coroutines.awaitCancellation
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
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.events.server.player.PlayerLeaveEvent
import net.casual.arcade.events.server.player.PlayerVoidDamageEvent
import net.casual.arcade.events.utils.await
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.data.MinigameDataSet
import net.casual.arcade.minigame.data.impl.MinigameWorldData
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.managers.MinigameLevelManager.SpawnLocation
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.pack.PackInfo
import net.casual.arcade.pack.utils.PackInfoRef
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.entity.teleportTo
import net.casual.arcade.utils.level.resetToDefault
import net.casual.arcade.utils.level.set
import net.casual.arcade.utils.math.location.LocationWithLevel
import net.casual.arcade.utils.player.*
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
    val data: MinigameDataSet
): Minigame(server, uuid, ID, GlideLobbyPhase.entries) {
    private val lobby: GlideLobbyData = this.data.get(GlideLobbyData.type) ?: GlideLobbyData.DEFAULT
    override val settings: GlideLobbySettings = GlideLobbySettings(this, this.availableMaps())

    private val sidebar = DynamicVirtualSidebar(this.server)

    private val countdown: GlideLobbyCountdown = GlideLobbyCountdown(this)
    private var glide: GlideMinigame? = null

    val level: ServerLevel get() = this.levels.require(LEVEL_ID)
    val votes: GlideLobbyVotes = GlideLobbyVotes(this)

    fun availableMaps(): List<Identifier> {
        return GlideModules.ids().toList()
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

    fun teleport(player: ServerPlayer) {
        player.teleportTo(this.spawn())
    }

    fun initializeGlideMinigame(glide: GlideMinigame) {
        glide.scopes.root.register<MinigameCloseEvent> { this.returnToLobby(glide) }

        var emptyTicks = 0
        glide.scopes.root.register<ServerTickEvent> {
            if (glide.players.onlinePlayerCount == 0) {
                emptyTicks++
                if (emptyTicks > 10.Seconds.ticks) {
                    glide.close()
                }
            } else {
                emptyTicks = 0
            }
        }
    }

    private fun checkAllReady() {
        if (!this.state.isAt(GlideLobbyPhase.Starting)) {
            return
        }
        val playing = this.players.playing
        if (playing.isNotEmpty() && playing.all(this::isReady)) {
            this.phases.set(GlideLobbyPhase.Playing)
        }
    }

    private fun spawn(): LocationWithLevel<ServerLevel> {
        return this.lobby.spawn.get().with(this.level)
    }

    private fun returnToLobby(glide: GlideMinigame) {
        if (this.closed) {
            return
        }
        if (this.glide === glide) {
            this.glide = null
            this.phases.set(GlideLobbyPhase.Waiting)
        }
        glide.players.transferTo(this)
    }

    private fun configuration(): GlideConfiguration? {
        return this.votes.resolve(this.level.random)
    }

    private fun createGlide(): GlideMinigame? {
        if (this.players.onlinePlayerCount == 0) {
            GlideMod.logger.error("Glide lobby failed to start, it has no players")
            return null
        }

        val configuration = this.configuration()
        if (configuration == null) {
            GlideMod.logger.error("Glide lobby failed to start, it has no available maps")
            return null
        }

        val modules = GlideModules.get(configuration.map)
        if (modules == null) {
            GlideMod.logger.error("Glide lobby failed to start, no such map ${configuration.map}")
            return null
        }

        return GlideMinigame.create(
            this.server,
            UUID.randomUUID(),
            modules,
            MultiGlideRoundMode(configuration.rounds),
            configuration.mode
        )
    }

    @Listener
    private fun onInitialize(event: MinigameInitializeEvent) {
        this.initializeLevel()
        this.initializePhases()

        this.levels.spawn = SpawnLocation.global(this.spawn())

        this.commands.register(GlideLobbyCommand(this))

        this.settings.startDelay.addListener { _, _, _ ->
            if (this.state.isAt(GlideLobbyPhase.Starting)) {
                this.phases.set(GlideLobbyPhase.Starting, force = true)
            }
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
    }

    @Listener
    private fun onRemovePlayer(event: MinigameRemovePlayerEvent) {
        this.votes.retract(event.player)
        this.tags.remove(event.player, READY)

        if (this.state.isAt(GlideLobbyPhase.Playing)) {
            return
        }
        if (this.players.onlinePlayerCount == 0) {
            this.phases.set(GlideLobbyPhase.Waiting)
            return
        }
        this.checkAllReady()
    }

    @Listener
    private fun onClose(event: MinigameCloseEvent) {
        this.data.close()
    }

    @Listener
    private fun onPlayerVoidDamage(event: PlayerVoidDamageEvent) {
        this.teleport(event.player)
        event.cancel()
    }

    private fun row(key: String, vote: GlideVote<*>): SidebarComponent {
        return SidebarComponent.withCustomScore(Component.translatable(key), vote.selectionName())
    }

    private fun initializeLevel() {
        this.levels.create(LEVEL_ID) {
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
                lobby.timeOfDay.isEmpty -> clockState(paused = false)
                else -> clockState(totalTicks = lobby.timeOfDay.get().toLong(), paused = true)
            }
        }

        this.data.get(MinigameWorldData.type)?.extract(this.server, this.level.dimension())
    }

    private fun initializePhases() {
        this.phases.coroutines[GlideLobbyPhase.Waiting] = this::runWaitingLogic
        this.phases.coroutines[GlideLobbyPhase.Starting] = this::runStartingLogic
        this.phases.coroutines[GlideLobbyPhase.Playing] = this::runPlayingLogic
    }

    private suspend fun runWaitingLogic() {
        this.votes.clear()
        this.clearReady()

        while (this.players.onlinePlayerCount == 0) {
            this.events.await<MinigameAddPlayerEvent>()
        }
    }

    private suspend fun runStartingLogic() {
        try {
            val delay = this.settings.startDelay.get().getOrNull() ?: awaitCancellation()
            this.countdown.transition(delay, players = this.players::all)
        } finally {
            this.countdown.stop()
        }
    }

    private suspend fun runPlayingLogic() {
        val glide = this.createGlide()
        if (glide == null) {
            this.phases.set(GlideLobbyPhase.Waiting)
            return
        }

        this.initializeGlideMinigame(glide)

        this.glide = glide
        this.players.transferTo(glide)
        glide.start()

        awaitCancellation()
    }

    companion object {
        private val LEVEL_ID = GlideMod.id("lobby_level")

        val ID = GlideMod.id("lobby")

        private val READY = GlideMod.id("ready")

        fun create(server: MinecraftServer, uuid: UUID): GlideLobbyMinigame {
            return GlideLobbyMinigame(server, uuid, GlideModules.lobby(server))
        }
    }
}
