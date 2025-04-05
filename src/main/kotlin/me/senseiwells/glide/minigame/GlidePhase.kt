package me.senseiwells.glide.minigame

import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.countdown
import net.casual.arcade.resources.utils.ResourcePackUtils
import net.casual.arcade.utils.TimeUtils.Seconds

enum class GlidePhase(override val id: String): Phase<GlideMinigame> {
    Waiting("waiting") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            ResourcePackUtils.afterPacksLoad(minigame.players) {
                minigame.setPhase(Countdown)
            }
        }
    },
    Countdown("countdown") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.ui.countdown.countdown(minigame, minigame.round.getCountdownTime()).then {
                minigame.setPhase(Active)
            }
        }
    },
    Active("active") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            for (player in minigame.players) {
                player.isNoGravity = false
            }
            minigame.music.start()
        }
    },
    Finish("finish") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canTakeDamage.set(false)
            minigame.endRound()
        }
    }
}