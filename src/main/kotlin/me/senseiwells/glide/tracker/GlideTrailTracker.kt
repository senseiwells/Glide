package me.senseiwells.glide.tracker

import net.casual.arcade.scheduler.MinecraftTimeDuration
import net.casual.arcade.utils.TimeUtils.Ticks
import net.minecraft.world.phys.Vec3

class GlideTrailTracker(
    private var start: MinecraftTimeDuration = MinecraftTimeDuration.ZERO
) {
    private val trails = ArrayList<GlideTrail>()

    private var current = ArrayList<Vec3>()

    fun update(point: Vec3) {
        this.current.add(point)
    }

    fun restart(time: MinecraftTimeDuration) {
        this.trails.add(GlideTrail(this.start, this.current, TICK_INTERVAL.Ticks))
        this.current = ArrayList()
        this.start = time
    }

    fun finish(): List<GlideTrail> {
        this.trails.add(GlideTrail(this.start, this.current, TICK_INTERVAL.Ticks))
        return this.trails
    }

    companion object {
        const val TICK_INTERVAL = 2
    }
}