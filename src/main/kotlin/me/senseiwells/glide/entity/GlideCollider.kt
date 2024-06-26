package me.senseiwells.glide.entity

import me.senseiwells.glide.minigame.GlideMinigame
import net.minecraft.server.level.ServerPlayer

interface GlideCollider {
    fun collide(player: ServerPlayer, glide: GlideMinigame)
}