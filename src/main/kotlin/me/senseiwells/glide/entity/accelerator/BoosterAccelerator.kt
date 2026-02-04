package me.senseiwells.glide.entity.accelerator

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
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc

open class BoosterAccelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size) {
    /**
     * The target horizontal velocity for this booster.
     *
     * This field corresponds to the following member:
     * ```
     * ThermalAreaRuleDefinition::mSpeedBoost
     * ```
     */
    var targetHorizontalVelocity: Double = 1.5

    constructor(level: Level, size: Vec3): this(GlideEntities.BOOSTER, level, size)

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        this.targetHorizontalVelocity = input.getDoubleOr("TargetVelocity", this.targetHorizontalVelocity)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putDouble("TargetVelocity", this.targetHorizontalVelocity)
    }

    override fun getSound(): Sound {
        return Sound(GlideSounds.BOOST)
    }

    override fun getModelScale(): Vector3fc {
        return this.getDisplaySize()
    }

    override fun getModelBrightness(): Brightness {
        return Brightness.FULL_BRIGHT
    }

    override fun getModelItem(): ItemStack {
        return GlideItems.OMNI_BOOST
    }
}