package me.senseiwells.glide.event

import net.casual.arcade.events.common.CancellableEvent
import net.casual.arcade.events.server.player.PlayerEvent
import net.minecraft.server.level.ServerPlayer

data class PlayerHorizontalFlyFallCollisionEvent(
    override val player: ServerPlayer,
    val damage: Float
): CancellableEvent.Default(), PlayerEvent