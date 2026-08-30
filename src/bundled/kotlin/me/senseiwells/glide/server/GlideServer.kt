package me.senseiwells.glide.server

import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.utils.player.hasPermission
import net.fabricmc.api.ModInitializer
import net.minecraft.server.permissions.PermissionLevel
import java.util.UUID

object GlideServer: ModInitializer {
    private var lobby: GlideLobbyMinigame? = null

    override fun onInitialize() {
        GlobalEventHandler.Server.register<ServerStartEvent>(priority = 2_000, listener = ::onServerStart)
        GlobalEventHandler.Server.register<PlayerJoinEvent>(::onPlayerJoin)
    }

    fun lobby(): GlideLobbyMinigame? {
        return this.lobby?.takeIf { !it.closed }
    }

    private fun onServerStart(event: ServerStartEvent) {
        val lobby = GlideLobbyMinigame.create(event.server, UUID.randomUUID())
        lobby.start()
        this.lobby = lobby
    }

    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.getMinigame() != null) {
            return
        }
        val lobby = this.lobby() ?: return
        lobby.players.add(player, admin = player.hasPermission(PermissionLevel.GAMEMASTERS))
    }
}
