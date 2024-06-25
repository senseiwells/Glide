package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.utils.EntityUtils.addVelocityAndMark
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

open class DynamicThermal(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size), PolymerEntity {
    constructor(level: Level, size: Vec3): this(GlideEntities.DYNAMIC_THERMAL, level, size)

    override fun makeBoundingBox(): AABB {
        val size = this.getSize()
        val position = this.position().add(0.0, size.y / 2, 0.0)
        return AABB.ofSize(position, size.x, size.y, size.z)
    }

    override fun accelerate(entity: Entity) {
        val delta = entity.deltaMovement
        if (delta.y < this.targetVelocity) {
            entity.addVelocityAndMark(0.0, (this.targetVelocity - 1) / 4.0, 0.0)
        } else if (delta.y == this.targetVelocity) {
            // TODO: Why?
            entity.addVelocityAndMark(0.0, -1.0, 0.0)
        }
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
        return GlideItem.STRONG_THERMAL
    }
}