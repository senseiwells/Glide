package me.senseiwells.glide.minigame

import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.launch
import net.casual.arcade.resources.utils.ResourcePackUtils

enum class GlidePhase(override val id: String): Phase<GlideMinigame> {
    Waiting("waiting") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canLookAroundWhenFrozen = true
            ResourcePackUtils.afterPacksLoad(minigame.players) {
                minigame.setPhase(Countdown)
            }
        }
    },
    Countdown("countdown") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.launch {
                minigame.visuals.countdown.transition(minigame.round.getCountdownTime(), players = minigame.players::all)
                minigame.setPhase(Active)
            }
        }
    },
    Active("active") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canLookAroundWhenFrozen = false
            for (player in minigame.players) {
                minigame.unfreezePlayer(player)
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