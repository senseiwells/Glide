package me.senseiwells.glide.entity.checkpoint

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.utils.EntityUtils
import me.senseiwells.glide.utils.EntityUtils.createValue
import me.senseiwells.glide.utils.ExtraEntityDataSerializers
import net.casual.arcade.utils.TagUtils.getVec3OrNull
import net.casual.arcade.utils.TagUtils.putVec3
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

abstract class Checkpoint(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Entity(type, level), PolymerEntity {
    init {
        this.setSize(size)
    }

    fun setSize(size: Vec3) {
        this.entityData.set(SIZE, size)
        this.boundingBox = this.makeBoundingBox()
    }

    fun getSize(): Vec3 {
        return this.entityData.get(SIZE)
    }

    override fun getPolymerEntityType(player: ServerPlayer?): EntityType<*> {
        return EntityType.BLOCK_DISPLAY
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(SIZE, Vec3.ZERO)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        val size = compound.getVec3OrNull("Size")
        if (size != null) {
            this.setSize(size)
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putVec3("Size", this.getSize())
    }

    override fun makeBoundingBox(): AABB {
        val position = this.position()
        val size = this.getSize()
        return AABB.ofSize(position, size.x, size.y, size.z)
    }

    override fun tick() {
        val intersecting = this.level().getEntitiesOfClass(ServerPlayer::class.java, this.boundingBox)
        for (entity in intersecting) {
            this.pass(entity)
        }
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        EntityUtils.replaceSynchedData(data, SIZE) { vec, consumer ->
            consumer.accept(DisplayTrackedData.SCALE.createValue(vec.toVector3f()))
            consumer.accept(DisplayTrackedData.TRANSLATION.createValue(vec.scale(0.5).reverse().toVector3f()))
        }

        if (initial) {
            // data.add(DisplayTrackedData.Block.BLOCK_STATE.createValue(Blocks.RED_STAINED_GLASS.defaultBlockState()))
            data.add(DisplayTrackedData.BRIGHTNESS.createValue(Brightness.FULL_BRIGHT.pack()))
        }
    }

    protected abstract fun pass(player: ServerPlayer)

    companion object {
        val DEFAULT_SIZE = Vec3(5.0, 5.0, 5.0)

        @JvmStatic
        private val SIZE: EntityDataAccessor<Vec3> = SynchedEntityData.defineId(Checkpoint::class.java, ExtraEntityDataSerializers.VEC3)
    }
}