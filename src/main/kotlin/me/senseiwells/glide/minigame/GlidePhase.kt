package me.senseiwells.glide.minigame

import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.utils.MinigameUtils.countdown
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.TimeUtils.Seconds
import net.minecraft.sounds.SoundSource

enum class GlidePhase(override val id: String): Phase<GlideMinigame> {
    Waiting("waiting") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canLookAroundWhenFrozen = true
        }
    },
    Countdown("countdown") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.ui.countdown.countdown(minigame, 5.Seconds).then {
                minigame.setPhase(Active)
            }
        }
    },
    Active("active") {
        override fun start(minigame: GlideMinigame, previous: Phase<GlideMinigame>) {
            minigame.settings.canLookAroundWhenFrozen = false
            for (player in minigame.players) {
                player.isNoGravity = false
            }
            minigame.music.start()
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