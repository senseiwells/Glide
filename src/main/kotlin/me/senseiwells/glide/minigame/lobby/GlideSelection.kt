package me.senseiwells.glide.minigame.lobby

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.util.ExtraCodecs
import java.util.function.Function

sealed interface GlideSelection<out T: Any> {
    data object Voting: GlideSelection<Nothing>

    data object Random: GlideSelection<Nothing>

    data class Selected<T: Any>(val value: T): GlideSelection<T>

    companion object {
        private val VOTING_CODEC: MapCodec<Voting> = MapCodec.unit(Voting)
        private val RANDOM_CODEC: MapCodec<Random> = MapCodec.unit(Random)

        fun <T: Any> codec(codec: Codec<T>): Codec<GlideSelection<T>> {
            val selected = codec.fieldOf("value").xmap({ value -> Selected(value) }, Selected<T>::value)

            val mapper = ExtraCodecs.LateBoundIdMapper<String, MapCodec<out GlideSelection<T>>>()
            mapper.put("voting", VOTING_CODEC)
            mapper.put("random", RANDOM_CODEC)
            mapper.put("selected", selected)

            return mapper.codec(Codec.STRING).dispatch({ selection ->
                when (selection) {
                    is Voting -> VOTING_CODEC
                    is Random -> RANDOM_CODEC
                    is Selected -> selected
                }
            }, Function.identity())
        }
    }
}
