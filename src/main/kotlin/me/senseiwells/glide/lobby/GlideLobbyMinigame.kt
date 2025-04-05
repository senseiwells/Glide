package me.senseiwells.glide.lobby

import net.casual.arcade.minigame.area.PlaceableArea
import net.casual.arcade.minigame.lobby.LobbyMinigame
import net.casual.arcade.utils.math.location.LocationWithLevel
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import java.util.*

class GlideLobbyMinigame(
    server: MinecraftServer,
    uuid: UUID,
    area: PlaceableArea,
    spawn: LocationWithLevel<ServerLevel>
): LobbyMinigame(server, uuid, area, spawn) {
}