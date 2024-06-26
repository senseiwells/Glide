package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.utils.EntityUtils.addVelocityAndMark
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.utils.TagUtils.contains
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class StaticThermal(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): DynamicThermal(type, level, size), PolymerEntity {
    var targetHeight = 5.0
    val targetY: Double
        get() = this.y + this.targetHeight

    constructor(level: Level, size: Vec3): this(GlideEntities.STATIC_THERMAL, level, size)

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetHeight", Tag.TAG_ANY_NUMERIC)) {
            this.targetHeight = compound.getDouble("TargetHeight")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putDouble("TargetHeight", this.targetHeight)
    }

    override fun shouldKeepAccelerating(player: ServerPlayer): Boolean {
        return player.y < this.targetY && !player.isRemoved || super.shouldKeepAccelerating(player)
    }

    override fun accelerate(player: ServerPlayer) {
        val delta = player.deltaMovement
        if (delta.y < this.targetVelocity) {
            player.addVelocityAndMark(0.0, this.targetVelocity / 3, 0.0)
        } else {
            player.setVelocityAndMark(delta.x, this.targetVelocity, delta.z)
        }
    }

    override fun getModelItem(): ItemStack {
        return GlideItem.WEAK_THERMAL
    }
}