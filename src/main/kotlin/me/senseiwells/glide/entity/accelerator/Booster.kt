package me.senseiwells.glide.entity.accelerator

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.utils.impl.Sound
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import org.joml.Vector3f

open class Booster(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Accelerator(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.BOOSTER, level, size)

    override fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean {
        val velocity = player.deltaMovement
        val horizontalVelocity = velocity.horizontalDistance()
        if (horizontalVelocity < this.targetVelocity) {
            player.setVelocityAndMark(velocity.x * 1.1, velocity.y, velocity.z * 1.1)
            return true
        }
        return this.intersects(player.boundingBox)
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
        return GlideItems.OMNI_BOOST
    }
}