package me.senseiwells.glide.minigame.lobby

import net.casual.arcade.minigame.phase.Phase
import kotlin.jvm.optionals.getOrNull

enum class GlideLobbyPhase(override val id: String): Phase<GlideLobbyMinigame> {
    Waiting("waiting"),
    Starting("starting") {
        override fun start(minigame: GlideLobbyMinigame, previous: Phase<GlideLobbyMinigame>) {
            minigame.votes.clear()
            minigame.clearReady()
            minigame.startCountdown(minigame.settings.startDelay.get().getOrNull())
        }

        override fun end(minigame: GlideLobbyMinigame, next: Phase<GlideLobbyMinigame>) {
            minigame.stopCountdown()
        }
    }
}
