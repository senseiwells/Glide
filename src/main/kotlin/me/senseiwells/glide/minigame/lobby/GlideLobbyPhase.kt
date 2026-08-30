package me.senseiwells.glide.minigame.lobby

import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.launchPhased
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.coroutine.delay

enum class GlideLobbyPhase(override val id: String): Phase<GlideLobbyMinigame> {
    Waiting("waiting"),
    Voting("voting") {
        override fun start(minigame: GlideLobbyMinigame, previous: Phase<GlideLobbyMinigame>) {
            minigame.votes.clear()
            minigame.launchPhased {
                delay(minigame.settings.votingDuration.get())
                minigame.setPhase(Countdown)
            }
        }
    },
    Countdown("countdown") {
        override fun start(minigame: GlideLobbyMinigame, previous: Phase<GlideLobbyMinigame>) {
            minigame.launchPhased {
                minigame.visuals.countdown.transition(
                    minigame.settings.countdownDuration.get(),
                    players = minigame.players::all
                )
                delay(1.Seconds)
                minigame.startGlide()
            }
        }
    }
}
