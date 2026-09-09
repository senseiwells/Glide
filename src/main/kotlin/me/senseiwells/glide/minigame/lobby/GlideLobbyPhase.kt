package me.senseiwells.glide.minigame.lobby

import net.casual.arcade.minigame.phase.MinigamePhase
import kotlin.jvm.optionals.getOrNull

enum class GlideLobbyPhase: MinigamePhase {
    Waiting,
    Starting
}
