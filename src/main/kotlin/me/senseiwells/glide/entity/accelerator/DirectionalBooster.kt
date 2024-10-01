package me.senseiwells.glide.entity.accelerator

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.utils.EntityUtils.addVelocityAndMark
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import kotlin.math.abs

class DirectionalBooster(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Booster(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.DIRECTIONAL_BOOSTER, level, size)

    override fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean {
        val velocity = player.deltaMovement
        val normal = this.direction.normal
        val horizontalVelocity = velocity.horizontalDistance()
        if (horizontalVelocity < this.targetVelocity) {
            player.addVelocityAndMark(abs(velocity.x) * normal.x * 0.1, 0.0, abs(velocity.z) * normal.z * 0.1)
            return true
        }
        return false
    }

    override fun canStartAccelerating(player: ServerPlayer): Boolean {
        return player.direction == this.direction && super.canStartAccelerating(player)
    }

    override fun getModelItem(): ItemStack {
        return GlideItem.BOOST
    }
}