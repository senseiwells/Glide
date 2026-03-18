package me.senseiwells.glide.minigame

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.resources.utils.withShiftedDownFont
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ExtraCodecs
import java.util.function.Function

sealed interface GlideRoundMode {
    fun getCountdownTime(): MinecraftTimeDuration

    fun getStartBanner(): MutableComponent

    fun shouldDisplayWinner(): Boolean

    fun addToProgressBar(progress: MutableComponent)

    fun mayPlayerRestart(player: ServerPlayer): Boolean

    fun onRoundFinished(glide: GlideMinigame)

    sealed interface Provider {
        fun create(): GlideRoundMode

        fun codec(): MapCodec<out Provider>

        companion object {
            private val MAPPER = ExtraCodecs.LateBoundIdMapper<String, MapCodec<out Provider>>()

            val CODEC: Codec<Provider> = MAPPER.codec(Codec.STRING).dispatch(Provider::codec, Function.identity())

            init {
                MAPPER.put("solo", SoloGlideRoundMode.CODEC)
                MAPPER.put("multi", MultiGlideRoundMode.Provider.CODEC)
            }
        }
    }
}

data object SoloGlideRoundMode: GlideRoundMode, GlideRoundMode.Provider {
    val CODEC: MapCodec<SoloGlideRoundMode> = MapCodec.unit(this)

    override fun getCountdownTime(): MinecraftTimeDuration {
        return 5.Seconds
    }

    override fun getStartBanner(): MutableComponent {
        return GlideComponents.SOLO_ROUND
    }

    override fun shouldDisplayWinner(): Boolean {
        return false
    }

    override fun addToProgressBar(progress: MutableComponent) {

    }

    override fun mayPlayerRestart(player: ServerPlayer): Boolean {
        return !player.glide.finished && !player.isSpectator
    }

    override fun onRoundFinished(glide: GlideMinigame) {
        glide.complete()
    }

    override fun create(): GlideRoundMode {
        return this
    }

    override fun codec(): MapCodec<out GlideRoundMode.Provider> {
        return CODEC
    }
}

class MultiGlideRoundMode(private val rounds: Int): GlideRoundMode {
    private var round = 1

    override fun getCountdownTime(): MinecraftTimeDuration {
        return 15.Seconds
    }

    override fun getStartBanner(): MutableComponent {
        return GlideComponents.NUMBERED_ROUND.generate(this.round)
    }

    override fun shouldDisplayWinner(): Boolean {
        return this.round >= this.rounds
    }

    override fun addToProgressBar(progress: MutableComponent) {
        val completed = "${this.round}/${this.rounds}"
        val shift = completed.length * 6
        val margin = 10
        progress.append(SpacingFontResources.spaced(margin))
        progress.append(Component.literal(completed).withShiftedDownFont(38))
        progress.append(SpacingFontResources.spaced(-margin - shift))
    }

    override fun mayPlayerRestart(player: ServerPlayer): Boolean {
        return false
    }

    override fun onRoundFinished(glide: GlideMinigame) {
        if (++this.round <= this.rounds) {
            glide.restart()
        } else {
            glide.complete()
        }
    }

    class Provider(val rounds: Int): GlideRoundMode.Provider {
        override fun create(): GlideRoundMode {
            return MultiGlideRoundMode(this.rounds)
        }

        override fun codec(): MapCodec<out GlideRoundMode.Provider> {
            return CODEC
        }

        companion object {
            val CODEC: MapCodec<Provider> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.intRange(1, 10).fieldOf("rounds").forGetter(Provider::rounds)
                ).apply(instance, ::Provider)
            }
        }
    }
}