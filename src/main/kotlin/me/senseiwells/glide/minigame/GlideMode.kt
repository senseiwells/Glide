package me.senseiwells.glide.minigame

import com.mojang.serialization.Codec
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.TimeUtils.format
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.StringRepresentable

enum class GlideMode: StringRepresentable {
    TimeAttack,
    ScoreAttack;

    fun getPersonalBestSound(): SoundEvent {
        return when (this) {
            TimeAttack -> GlideSounds.TIME_PB
            ScoreAttack -> GlideSounds.SCORE_PB
        }
    }

    fun getBannerShield(frame: Int): Component {
        return when (this) {
            TimeAttack -> GlideFont.Banner.stopwatchShield(frame)
            ScoreAttack -> GlideFont.Banner.scoreShield(frame)
        }
    }

    fun getBannerScore(player: ServerPlayer): MutableComponent {
        return when (this) {
            TimeAttack -> Component.literal(player.glide.finishedTime.format())
            ScoreAttack -> Component.literal(player.glide.score.toString())
        }
    }

    override fun getSerializedName(): String {
        return when (this) {
            TimeAttack -> "time_attack"
            ScoreAttack -> "score_attack"
        }
    }

    companion object {
        val CODEC: Codec<GlideMode> = StringRepresentable.fromEnum(GlideMode::values)
    }
}