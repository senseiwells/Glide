package me.senseiwells.glide.minigame

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.TimeUtils.format
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
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

    fun getBannerShield(frame: Int): MutableComponent {
        return when (this) {
            TimeAttack -> GlideFont.Banner.stopwatchShield(frame)
            ScoreAttack -> GlideFont.Banner.scoreShield(frame)
        }
    }

    fun getBannerScore(player: ServerPlayer): MutableComponent {
        return when (this) {
            TimeAttack -> player.glide.finishedTime.format().literal()
            ScoreAttack -> player.glide.score.toString().literal()
        }
    }
}