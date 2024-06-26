package me.senseiwells.glide.entity.accelerator

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.EntityUtils.addVelocityAndMark
import net.casual.arcade.utils.impl.Sound
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

open class Booster(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.BOOSTER, level, size)

    override fun accelerate(player: ServerPlayer) {
        if (player.deltaMovement.lengthSqr() < this.targetVelocity * this.targetVelocity) {
            val acceleration = player.lookAngle.normalize().scale(0.2)
            player.addVelocityAndMark(acceleration.x, 0.0, acceleration.z)
        }
    }

    override fun getSound(): Sound {
        return Sound(GlideSounds.BOOST)
    }

    override fun getModelScale(): Vector3f {
        return this.getDisplaySize()
    }

    override fun getModelBrightness(): Brightness {
        return Brightness.FULL_BRIGHT
    }

    override fun getModelItem(): ItemStack {
        return GlideItem.OMNI_BOOST
    }
}