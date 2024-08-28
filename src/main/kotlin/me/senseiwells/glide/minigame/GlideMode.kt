package me.senseiwells.glide.minigame

import me.senseiwells.glide.sound.GlideSounds
import net.minecraft.sounds.SoundEvent

enum class GlideMode {
    TimeAttack,
    ScoreAttack;

    fun getPersonalBestSound(): SoundEvent {
        return when (this) {
            TimeAttack -> GlideSounds.TIME_PB
            ScoreAttack -> GlideSounds.SCORE_PB
        }
    }
}