package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.impl.Sound
import net.minecraft.util.Brightness
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

open class ThermalAccelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size), PolymerEntity {
    /**
     * The target vertical velocity for this thermal.
     *
     * This field corresponds to the following member:
     * ```
     * ThermalAreaRuleDefinition::mStaticLift
     * ```
     */
    var targetVerticalVelocity = 1.5

    /**
     * How much this thermal modifies the lift force.
     *
     * This field corresponds to the following member:
     * ```
     * ThermalAreaRuleDefinition::mLiftForceModifier
     * ```
     */
    var liftForceModifier = 1.0

    constructor(level: Level, size: Vec3): this(GlideEntities.DYNAMIC_THERMAL, level, size)

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        this.liftForceModifier = input.getDoubleOr("LiftForceModifier", this.liftForceModifier)
        this.targetVerticalVelocity = input.getDoubleOr("TargetVelocity", this.targetVerticalVelocity)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putDouble("LiftForceModifier", this.liftForceModifier)
        output.putDouble("TargetVelocity", this.targetVerticalVelocity)
    }

    override fun makeBoundingBox(position: Vec3): AABB {
        val size = this.getSize()
        return AABB.ofSize(position.add(0.0, size.y / 2, 0.0), size.x, size.y, size.z)
    }

    override fun getSound(): Sound {
        return Sound(GlideSounds.THERMAL)
    }

    override fun getModelScale(): Vector3f {
        val size = this.getDisplaySize()
        return Vector3f(size.x(), 1.0F, size.z())
    }

    override fun getModelBrightness(): Brightness {
        val height = this.getDisplaySize().y().toInt()
        return Brightness(height % 16, height / 16)
    }

    override fun getModelItem(): ItemStack {
        if (this.targetVerticalVelocity < 0) {
            return ItemStack.EMPTY
        }
        return GlideItems.DYNAMIC_THERMAL
    }
}