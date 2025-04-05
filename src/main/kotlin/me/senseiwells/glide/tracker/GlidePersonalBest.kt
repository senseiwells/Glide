package me.senseiwells.glide.tracker

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.resources.ResourceLocation

data class GlidePersonalBests(
    val map: ResourceLocation,
    val time: GlideTimePersonalBest,
    val score: GlideScorePersonalBest
) {
    companion object {
        val CODEC: Codec<GlidePersonalBests> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("map").forGetter(GlidePersonalBests::map),
                GlideTimePersonalBest.CODEC.fieldOf("time").forGetter(GlidePersonalBests::time),
                GlideScorePersonalBest.CODEC.fieldOf("score").forGetter(GlidePersonalBests::score)
            ).apply(instance, ::GlidePersonalBests)
        }
    }
}

sealed interface GlidePersonalBest {
    val trails: List<GlideTrail>

    fun display(): Boolean {
        return this.trails.isNotEmpty()
    }
}

class GlideScorePersonalBest(
    val score: Int,
    override val trails: List<GlideTrail>
): GlidePersonalBest {
    companion object {
        val DEFAULT = GlideScorePersonalBest(-1, listOf())

        val CODEC: Codec<GlideScorePersonalBest> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("score").forGetter(GlideScorePersonalBest::score),
                GlideTrail.CODEC.listOf().fieldOf("trails").forGetter(GlideScorePersonalBest::trails)
            ).apply(instance, ::GlideScorePersonalBest)
        }
    }
}

class GlideTimePersonalBest(
    val time: MinecraftTimeDuration,
    override val trails: List<GlideTrail>
): GlidePersonalBest {
    override fun display(): Boolean {
        return this.time.ticks != Int.MAX_VALUE && super.display()
    }

    companion object {
        val DEFAULT = GlideTimePersonalBest(Int.MAX_VALUE.Ticks, listOf())

        val CODEC: Codec<GlideTimePersonalBest> = RecordCodecBuilder.create { instance ->
            instance.group(
                MinecraftTimeDuration.CODEC.fieldOf("time").forGetter(GlideTimePersonalBest::time),
                GlideTrail.CODEC.listOf().fieldOf("trails").forGetter(GlideTimePersonalBest::trails)
            ).apply(instance, ::GlideTimePersonalBest)
        }
    }
}