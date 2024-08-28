package me.senseiwells.glide.tracker

import net.casual.arcade.scheduler.MinecraftTimeDuration
import net.casual.arcade.utils.TimeUtils.Ticks

interface GlidePersonalBest {
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
        val DEFAULT = GlideScorePersonalBest(0, listOf())
    }
}

class GlideTimePersonalBest(
    val time: MinecraftTimeDuration = Int.MAX_VALUE.Ticks,
    override val trails: List<GlideTrail> = listOf()
): GlidePersonalBest {
    override fun display(): Boolean {
        return this.time.ticks != Int.MAX_VALUE && super.display()
    }

    companion object {
        val DEFAULT = GlideTimePersonalBest()
    }
}