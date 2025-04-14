package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.contains
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.utils.setVelocityAndMark
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
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

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("LiftForceModifier", Tag.TAG_ANY_NUMERIC)) {
            this.liftForceModifier = compound.getDouble("LiftForceModifier")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putDouble("LiftForceModifier", this.liftForceModifier)
    }

    override fun makeBoundingBox(position: Vec3): AABB {
        val size = this.getSize()
        return AABB.ofSize(position.add(0.0, size.y / 2, 0.0), size.x, size.y, size.z)
    }

    override fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean {
        player.glide.setLiftForceModifier(this.liftForceModifier)
        val vel = this.getVelocityClamped(player)

        if (this.targetVelocity == 0.0) { // Why do dynamic thermals behave in two different ways...
            if (this.hasLift(player)) {
                player.setVelocityAndMark(vel.x, vel.y+(0.025*this.liftForceModifier), vel.z)
                return true
            } else {
                return false
            }
        } else { //Normal thermal logic & sometimes dynamic??????
            player.isNoGravity = hasNoGravity()
            if (this.hasLift(player)) {
                if (player.glide.thermalDeltaMovement == 0.0 && player.deltaMovement.y != 0.0) {
                    player.glide.thermalDeltaMovement = (player.deltaMovement.y / this.liftForceModifier)
                }
                if (this.targetVelocity < 0.0) {
                    if (player.glide.thermalDeltaMovement <= this.targetVelocity) {
                        player.glide.thermalDeltaMovement = this.targetVelocity
                    } else {
                        player.glide.thermalDeltaMovement -= ((this.targetVelocity - player.glide.thermalDeltaMovement) / this.targetVelocity) * 0.1
                    }
                } else {
                    if (this.targetVelocity <= player.glide.thermalDeltaMovement) {
                        player.glide.thermalDeltaMovement = targetVelocity
                    } else {
                        player.glide.thermalDeltaMovement += ((this.targetVelocity - player.glide.thermalDeltaMovement) / this.targetVelocity) * 0.1
                    }
                }
            } else {
                if (this.targetVelocity < 0.0) {
                    if (player.glide.thermalDeltaMovement >= 0.0) {
                        player.glide.thermalDeltaMovement = 0.0
                        player.isNoGravity = false
                        return false
                    } else {
                        player.glide.thermalDeltaMovement += 0.025
                    }
                } else {
                    if (player.glide.thermalDeltaMovement <= 0.0) {
                        player.glide.thermalDeltaMovement = 0.0
                        player.isNoGravity = false
                        return false
                    } else {
                        player.glide.thermalDeltaMovement -= 0.025
                    }
                }
            }
            return true
        }
    }

    open fun hasLift(player: ServerPlayer): Boolean {
        return (this.boundingBox.intersects(player.boundingBox) || (this.targetVelocity < 0.0 && player.y > this.boundingBox.maxY))
    }

    open fun hasNoGravity(): Boolean {
        return false;
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

    fun getVelocityClamped(player: ServerPlayer): Vec3 {
        val initial: Vec3 = player.glide.initialEntryVelocity
        val delta: Vec3 = player.deltaMovement
        if (delta.length() > initial.length()) {
            return Vec3(
                Mth.clamp(delta.x, -initial.length(), initial.length()),
                Mth.clamp(delta.y, -initial.length(), initial.length()),
                Mth.clamp(delta.z, -initial.length(), initial.length())
            )
        }
        return delta
    }
}