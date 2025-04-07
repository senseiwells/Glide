package me.senseiwells.glide.addon.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.entity.accelerator.*
import net.casual.arcade.utils.BoundingBoxUtils.getSizeVec
import net.casual.arcade.utils.codec.ArcadeExtraCodecs
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.AABB
import java.util.*

class BoosterTemplate(
    private val boundingBox: AABB,
    private val targetVelocity: Double,
    private val direction: Direction,
    private val isDirectional: Boolean
) {
    fun create(level: ServerLevel): Accelerator {
        val size = this.boundingBox.getSizeVec()
        val booster = if (this.isDirectional) DirectionalBooster(level, size) else Booster(level, size)
        booster.setPos(this.boundingBox.center)
        booster.yRot = this.direction.toYRot()
        booster.targetVelocity = this.targetVelocity
        return booster
    }

    companion object {
        val CODEC: Codec<BoosterTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(BoosterTemplate::boundingBox),
                Codec.DOUBLE.fieldOf("target_velocity").forGetter(BoosterTemplate::targetVelocity),
                Direction.CODEC.fieldOf("direction").forGetter(BoosterTemplate::direction),
                Codec.BOOL.optionalFieldOf("directional", false).forGetter(BoosterTemplate::isDirectional)
            ).apply(instance, ::BoosterTemplate)
        }
    }
}

class ThermalTemplate(
    private val boundingBox: AABB,
    private val targetVelocity: Double,
    private val liftForceModifier: Double,
    private val targetY: Optional<Double>
) {
    fun create(level: ServerLevel): Accelerator {
        val size = this.boundingBox.getSizeVec()
        val thermal = if (this.targetY.isPresent) {
            val static = StaticThermal(level, size)
            static.targetHeight = this.targetY.get() - this.boundingBox.minY
            static
        } else {
            DynamicThermal(level, size)
        }
        thermal.setPos(this.boundingBox.bottomCenter)
        thermal.targetVelocity = targetVelocity
        thermal.liftForceModifier = this.liftForceModifier
        return thermal
    }

    companion object {
        val CODEC: Codec<ThermalTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(ThermalTemplate::boundingBox),
                Codec.DOUBLE.optionalFieldOf("target_velocity", 0.0).forGetter(ThermalTemplate::targetVelocity),
                Codec.DOUBLE.optionalFieldOf("lift_multiplier", 1.0).forGetter(ThermalTemplate::liftForceModifier),
                Codec.DOUBLE.optionalFieldOf("target_y").forGetter(ThermalTemplate::targetY)
            ).apply(instance, ::ThermalTemplate)
        }
    }
}
