package me.senseiwells.glide.checkpoint

import net.minecraft.world.phys.Vec3
import kotlin.math.max

data class CheckpointData(
    val index: Int,
    val position: Vec3
) {
    val previous: Int
        get() = max(this.index - 1, 0)
    val next: Int
        get() = this.index + 1

    companion object {
        val DEFAULT = CheckpointData(0, Vec3.ZERO)
    }
}
