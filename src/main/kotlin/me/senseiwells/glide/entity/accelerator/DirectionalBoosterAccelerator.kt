package me.senseiwells.glide.entity.accelerator

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItems
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class DirectionalBoosterAccelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): BoosterAccelerator(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.DIRECTIONAL_BOOSTER, level, size)

    override fun shouldAcceleratePlayer(player: ServerPlayer): Boolean {
        val diff = Mth.abs(Mth.wrapDegrees(player.yRot - this.direction.toYRot()))
        return diff < 90 && super.shouldAcceleratePlayer(player)
    }

    override fun getModelItem(): ItemStack {
        return GlideItems.BOOST
    }
}