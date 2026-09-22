package me.senseiwells.glide.minigame

import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.tracker.GlidePersonalBests
import me.senseiwells.glide.tracker.GlideScorePersonalBest
import me.senseiwells.glide.tracker.GlideTimePersonalBest
import me.senseiwells.glide.tracker.GlideTrail
import net.casual.arcade.utils.TimeUtils.format
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.StringRepresentable
import kotlin.math.max
import kotlin.math.min

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

    fun updateBests(
        glide: GlideMinigame,
        player: ServerPlayer,
        bests: GlidePersonalBests,
        time: MinecraftTimeDuration,
        score: Int,
        trails: List<GlideTrail>
    ): GlidePersonalBests? {
        when (this) {
            ScoreAttack -> {
                glide.stats.getOrCreateStat(player, GlideStats.BEST_SCORE).modify { v -> max(v, score) }
                if (score > bests.score.score) {
                    return bests.copy(score = GlideScorePersonalBest(score, trails))
                }
            }
            TimeAttack -> {
                glide.stats.getOrCreateStat(player, GlideStats.BEST_TIME).modify { v -> min(v, time.ticks) }
                if (time < bests.time.time) {
                    return bests.copy(time = GlideTimePersonalBest(time, trails))
                }
            }
        }
        return null
    }

    fun getUpdatedWinner(glide: GlideMinigame, leading: NameAndId?): NameAndId? {
        val comparator = when (this) {
            TimeAttack -> Comparator.comparingInt {
                glide.stats.getOrCreateStat(it.id, GlideStats.BEST_TIME).value
            }
            ScoreAttack -> Comparator.comparingInt<NameAndId> {
                glide.stats.getOrCreateStat(it.id, GlideStats.BEST_SCORE).value
            }.reversed()
        }

        val current = glide.players.all.minWithOrNull(
            Comparator.comparing(ServerPlayer::nameAndId, comparator)
        )?.nameAndId() ?: return leading

        if (leading == null) {
            return current
        }
        return listOf(current, leading).minWith(comparator)
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