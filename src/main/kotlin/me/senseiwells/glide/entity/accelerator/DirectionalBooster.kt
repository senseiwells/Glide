package me.senseiwells.glide.entity.accelerator

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class DirectionalBooster(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Booster(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.DIRECTIONAL_BOOSTER, level, size)

    override fun accelerate(player: ServerPlayer) {
        if (player.direction.opposite == this.direction) {
            super.accelerate(player)
        }
    }

    override fun getModelItem(): ItemStack {
        return GlideItem.BOOST
    }
}