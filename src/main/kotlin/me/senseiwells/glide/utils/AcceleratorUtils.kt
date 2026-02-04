package me.senseiwells.glide.utils

import me.senseiwells.glide.entity.accelerator.*
import net.minecraft.core.Vec3i

fun Accelerator.getLiftForceModifier(): Double {
    return when (this) {
        is ThermalAccelerator -> this.liftForceModifier
        else -> 1.0
    }
}

fun Accelerator.getStaticLift(): Double {
    return when (this) {
        is ThermalAccelerator -> this.targetVerticalVelocity
        else -> 0.0
    }
}

fun Accelerator.getTargetY(): Double {
    return when (this) {
        is StaticThermalAccelerator -> this.targetY
        else -> 0.0
    }
}

fun Accelerator.getSpeedBoost(): Double {
    return when (this) {
        is BoosterAccelerator -> this.targetHorizontalVelocity
        else -> 0.0
    }
}

fun Accelerator.getBoostDirection(): Vec3i? {
    return when (this) {
        is DirectionalBoosterAccelerator -> this.direction.unitVec3i
        else -> null
    }
}