package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.utils.*
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.impl.Sound
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import org.joml.Vector3f
import xyz.nucleoid.packettweaker.PacketContext

abstract class Accelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Entity(type, level), PolymerEntity, GlideCollider {
    // Leaking `this` causes this to be nullable
    private var size: Vec3? = size

    var targetVelocity: Double = 1.5

    init {
        this.setDisplaySize(size.scale(0.95).toVector3f())
        this.boundingBox = this.makeBoundingBox()
    }

    fun setSize(vec: Vec3) {
        this.size = vec
        this.boundingBox = this.makeBoundingBox()
    }

    fun getSize(): Vec3 {
        return this.size ?: DEFAULT_SIZE
    }

    fun setDisplaySize(size: Vector3f) {
        this.entityData.set(SIZE, size)
    }

    fun getDisplaySize(): Vector3f {
        return this.entityData.get(SIZE)
    }

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityType.ITEM_DISPLAY
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(SIZE, Vector3f())
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        val size = compound.getVec3OrNull("Size")
        if (size != null) {
            this.setSize(size)
        }
        val displaySize = compound.getVector3fOrNull("DisplaySize")
        if (displaySize != null) {
            this.setDisplaySize(displaySize)
        }
        if (compound.contains("TargetVelocity", Tag.TAG_ANY_NUMERIC)) {
            this.targetVelocity = compound.getDouble("TargetVelocity")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putVec3("Size", this.getSize())
        compound.putVector3f("DisplaySize", this.getDisplaySize())
        compound.putDouble("TargetVelocity", this.targetVelocity)
    }

    override fun makeBoundingBox(position: Vec3): AABB {
        val size = this.getSize()
        return AABB.ofSize(position, size.x, size.y, size.z)
    }

    override fun tick() {

    }

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, f: Float): Boolean {
        return false
    }

    override fun intersects(box: AABB): Boolean {
        return this.boundingBox.intersects(box)
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        if (this.canStartAccelerating(player) && player.glide.collideWithAccelerator(this)) {
            player.sendSound(this.getSound())
        }
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        SynchedDataUtils.replaceSynchedData(data, SIZE) { _, consumer ->
            consumer.accept(DisplayTrackedData.SCALE.createValue(this.getModelScale()))
            consumer.accept(DisplayTrackedData.BRIGHTNESS.createValue(this.getModelBrightness().pack()))
        }

        if (initial) {
            data.add(DisplayTrackedData.Item.ITEM.createValue(this.getModelItem()))
            data.add(DisplayTrackedData.VIEW_RANGE.createValue(3.0F))
        }
    }

    open fun canStartAccelerating(player: ServerPlayer): Boolean {
        return true
    }

    abstract fun accelerate(player: ServerPlayer, ticks: MutableInt): Boolean

    protected abstract fun getSound(): Sound

    protected abstract fun getModelScale(): Vector3f

    protected abstract fun getModelBrightness(): Brightness

    protected abstract fun getModelItem(): ItemStack

    companion object {
        val DEFAULT_SIZE = Vec3(5.0, 5.0, 5.0)

        @JvmStatic
        private val SIZE: EntityDataAccessor<Vector3f> = SynchedEntityData.defineId(Accelerator::class.java, EntityDataSerializers.VECTOR3)
    }
}