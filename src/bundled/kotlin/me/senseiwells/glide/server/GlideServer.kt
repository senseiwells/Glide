package me.senseiwells.glide.server

import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.events.MinigameInitializeEvent
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.utils.player.hasPermission
import net.casual.arcade.utils.player.server
import net.casual.arcade.utils.server.players
import net.fabricmc.api.ModInitializer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.permissions.PermissionLevel
import java.util.*

object GlideServer: ModInitializer {
    private var lobby: GlideLobbyMinigame? = null

    override fun onInitialize() {
        GlobalEventHandler.Server.register<ServerStartEvent>(priority = 2_000, listener = ::onServerStart)
        GlobalEventHandler.Server.register<PlayerJoinEvent>(::onPlayerJoin)
        GlobalEventHandler.Server.register<MinigameInitializeEvent>(::onMinigameInitialize)
    }

    fun lobby(): GlideLobbyMinigame? {
        val lobby = this.lobby ?: return null
        return if (!lobby.closed) lobby else null
    }

    private fun createLobby(server: MinecraftServer): GlideLobbyMinigame {
        this.lobby()?.close()

        val lobby = GlideLobbyMinigame.create(server, UUID.randomUUID())
        lobby.start()
        this.lobby = lobby
        this.addNonPlayingToLobby(lobby)
        return lobby
    }

    private fun addNonPlayingToLobby(lobby: GlideLobbyMinigame) {
        for (player in lobby.server.players) {
            if (player.getMinigame() == null) {
                lobby.players.add(player, admin = player.hasPermission(PermissionLevel.GAMEMASTERS))
            }
        }
    }

    private fun onServerStart(event: ServerStartEvent) {
        this.createLobby(event.server)
    }

    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.getMinigame() != null) {
            return
        }
        val lobby = this.lobby() ?: this.createLobby(player.server)
        lobby.players.add(player, admin = player.hasPermission(PermissionLevel.GAMEMASTERS))
    }

    private fun onMinigameInitialize(event: MinigameInitializeEvent) {
        val minigame = event.minigame
        if (minigame is GlideLobbyMinigame && this.lobby() == null) {
            this.lobby = minigame
            this.addNonPlayingToLobby(minigame)
        }
    }
}
