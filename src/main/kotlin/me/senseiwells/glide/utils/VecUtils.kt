package me.senseiwells.glide.utils

import me.senseiwells.glide.utils.VecUtils.rotationAnglesTowards
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f

object VecUtils {
    private val DEFAULT_ROTATION = Vector3f(0.0F, 0.0F, 1.0F)

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

    fun Vec3.rotationAnglesTowards(other: Vec3): Vec2 {
        val direction = other.subtract(this)
        val horizontal = direction.horizontalDistance()
        val xRot = Mth.wrapDegrees(-Mth.atan2(direction.y, horizontal).toFloat() * 180.0F / Mth.PI)
        val yRot = Mth.wrapDegrees(Mth.atan2(direction.z, direction.x).toFloat() * 180.0F / Mth.PI - 90.0F)
        return Vec2(xRot, yRot)
    }

    fun Vec3.rotationTowards(other: Vec3): Quaternionf {
        val direction = other.subtract(this).toVector3f()
        return Quaternionf().rotationTo(DEFAULT_ROTATION, direction)
    }
}