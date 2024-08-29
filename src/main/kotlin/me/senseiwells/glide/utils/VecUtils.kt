package me.senseiwells.glide.utils

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import kotlin.math.asin
import kotlin.math.atan2

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

    fun Vec3.rotationAnglesTowards(other: Vec3): Vec2 {
        val direction = other.subtract(this)
        val horizontal = direction.horizontalDistance()
        val xRot = Mth.wrapDegrees(-Mth.atan2(direction.y, horizontal).toFloat() * Mth.RAD_TO_DEG)
        val yRot = Mth.wrapDegrees(Mth.atan2(direction.z, direction.x).toFloat() * Mth.RAD_TO_DEG - 90.0F)
        return Vec2(xRot, yRot)
    }

    fun Vec3.rotationTowards(other: Vec3): Quaternionf {
        // This stupid math took me like 8 hours to figure out.
        // I hate Quaternions - Sensei
        val normalized = other.subtract(this).normalize()
        val projected = Vec3(normalized.x, 0.0, normalized.z).normalize()

        val yaw = atan2(projected.x, projected.z).toFloat()
        val pitch = asin(-normalized.y).toFloat()

        return Quaternionf().rotateY(yaw).mul(Quaternionf().rotateX(pitch))
    }
}