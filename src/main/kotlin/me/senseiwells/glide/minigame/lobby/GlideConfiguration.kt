package me.senseiwells.glide.minigame.lobby

import me.senseiwells.glide.minigame.GlideMode
import net.minecraft.resources.Identifier

data class GlideConfiguration(
    val map: Identifier,
    val mode: GlideMode,
    val rounds: Int
)
