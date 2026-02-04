package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

class StaticThermalAccelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): ThermalAccelerator(type, level, size), PolymerEntity {
    val targetY: Double get() = this.y + this.targetHeight

    /**
     * This represents the target height to this thermal.
     *
     * This field corresponds to the following member:
     * ```
     * ThermalAreaRuleDefinition::mTargetHeight
     * ```
     */
    var targetHeight = 5.0

    constructor(level: Level, size: Vec3): this(GlideEntities.STATIC_THERMAL, level, size)

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        this.targetHeight = input.getDoubleOr("TargetHeight", this.targetHeight)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putDouble("TargetHeight", this.targetHeight)
    }

    override fun getModelItem(): ItemStack {
        return GlideItems.STATIC_THERMAL
    }
}