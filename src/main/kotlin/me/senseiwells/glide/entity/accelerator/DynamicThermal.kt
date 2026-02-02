package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.addVelocitySmooth
import net.casual.arcade.utils.impl.Sound
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import org.joml.Vector3f

open class DynamicThermal(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size), PolymerEntity {
    var liftForceModifier = 1.0

    constructor(level: Level, size: Vec3): this(GlideEntities.DYNAMIC_THERMAL, level, size)

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        this.liftForceModifier = input.getDoubleOr("LiftForceModifier", this.liftForceModifier)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putDouble("LiftForceModifier", this.liftForceModifier)
    }

    override fun makeBoundingBox(position: Vec3): AABB {
        val size = this.getSize()
        return AABB.ofSize(position.add(0.0, size.y / 2, 0.0), size.x, size.y, size.z)
    }

    override fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean {
        player.glide.setLiftForceModifier(this.liftForceModifier)

        if (this.targetVelocity < 0) {
            if (player.deltaMovement.y > this.targetVelocity) {
                player.addVelocitySmooth(
                    0.0,
                    (this.targetVelocity - player.deltaMovement.y) / this.targetVelocity * -0.1,
                    0.0
                )
            }
        } else if (this.targetVelocity > 0) {
            if (player.deltaMovement.y < this.targetVelocity) {
                player.addVelocitySmooth(
                    0.0,
                    (this.targetVelocity - player.deltaMovement.y) / this.targetVelocity * 0.1,
                    0.0
                )
            }
        } else {
            player.addVelocitySmooth(0.0, 0.025, 0.0)
        }
        return this.boundingBox.intersects(player.boundingBox)
    }

    override fun getSound(): Sound {
        return Sound(GlideSounds.THERMAL)
    }

    override fun getModelScale(): Vector3f {
        val size = this.getDisplaySize()
        return Vector3f(size.x, 1.0F, size.z)
    }

    override fun getModelBrightness(): Brightness {
        val height = this.getDisplaySize().y.toInt()
        return Brightness(height % 16, height / 16)
    }

    override fun getModelItem(): ItemStack {
        if (this.targetVelocity < 0) {
            return ItemStack.EMPTY
        }
        return GlideItems.DYNAMIC_THERMAL
    }
}