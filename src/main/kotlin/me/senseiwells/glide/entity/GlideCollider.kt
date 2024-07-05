package me.senseiwells.glide.entity

import me.senseiwells.glide.minigame.GlideMinigame
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.AABB

interface GlideCollider {
    fun intersects(box: AABB): Boolean

    fun collide(player: ServerPlayer, glide: GlideMinigame)
}