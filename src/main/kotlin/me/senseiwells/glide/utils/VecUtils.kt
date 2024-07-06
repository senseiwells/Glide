package me.senseiwells.glide.utils

import me.senseiwells.glide.utils.VecUtils.projectOntoLine
import net.minecraft.world.phys.Vec3

object VecUtils {
    fun Vec3.projectOntoLine(start: Vec3, end: Vec3): Vec3 {
        if (start == end) {
            return start
        }

        val direction = end.subtract(start)
        val vec = this.subtract(start)
        val projection = vec.dot(direction) / direction.lengthSqr()
        return start.add(direction.scale(projection))
    }

    fun Vec3.projectionScalar(start: Vec3, end: Vec3): Double {
        if (start == end) {
            return 0.0
        }

        val direction = end.subtract(start)
        val vec = this.subtract(start)
        return vec.dot(direction) / direction.lengthSqr()
    }
}