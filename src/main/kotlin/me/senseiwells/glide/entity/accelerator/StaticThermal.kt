package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.item.GlideItems
import net.casual.arcade.utils.addVelocityAndMark
import net.casual.arcade.utils.addVelocitySmooth
import net.casual.arcade.utils.contains
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt

class StaticThermal(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): DynamicThermal(type, level, size), PolymerEntity {
    var targetHeight = 5.0
    val targetY: Double
        get() = this.y + this.targetHeight

    constructor(level: Level, size: Vec3): this(GlideEntities.STATIC_THERMAL, level, size)

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        this.targetHeight = input.getDoubleOr("TargetHeight", this.targetHeight)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putDouble("TargetHeight", this.targetHeight)
    }

    override fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean {
        player.glide.setLiftForceModifier(this.liftForceModifier)
        val currentVelocityY = player.deltaMovement.y

        player.isNoGravity = true
        if (player.y > this.targetY) {
            if (ticks.incrementAndGet() <= 20) {
                if (currentVelocityY > this.targetVelocity) {
                    player.isNoGravity = false
                }

                player.addVelocitySmooth(0.0, -0.008, 0.0)
                return true
            } else {
                player.isNoGravity = false
                return false
            }
        } else {
            ticks.setValue(0)
            if (currentVelocityY < this.targetVelocity) {
                player.addVelocitySmooth(
                    0.0,
                    (this.targetVelocity - currentVelocityY) / this.targetVelocity * 0.1,
                    0.0
                )
            } else {
                player.isNoGravity = false
            }
            return true
        }
    }

    override fun getModelItem(): ItemStack {
        return GlideItems.STATIC_THERMAL
    }
}