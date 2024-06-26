package me.senseiwells.glide.event

import net.casual.arcade.events.core.CancellableEvent
import net.casual.arcade.events.player.PlayerEvent
import net.minecraft.server.level.ServerPlayer

data class PlayerHorizontalFlyFallCollisionEvent(
    override val player: ServerPlayer,
    val damage: Float
): CancellableEvent.Default(), PlayerEvent