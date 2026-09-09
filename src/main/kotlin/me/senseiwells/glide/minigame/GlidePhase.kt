package me.senseiwells.glide.minigame

import net.casual.arcade.minigame.phase.MinigamePhase

enum class GlidePhase: MinigamePhase {
    Waiting,
    Countdown,
    Active,
    Finish
}