package me.senseiwells.glide.minigame

import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.utils.MinigameUtils.countdown
import net.casual.arcade.utils.TimeUtils.Seconds

enum class GlidePhase(override val id: String): Phase<GlideMinigame> {
    Countdown("countdown") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.ui.countdown.countdown(minigame, 5.Seconds).then {
                minigame.setPhase(Active)
            }
        }
    },
    Active("active") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            for (player in minigame.players) {
                player.isNoGravity = false
            }
        }
    },
    Finish("finish") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canTakeDamage.set(false)

            minigame.scheduler.schedule(20.Seconds) {
                minigame.complete()
            }
        }
    }
}