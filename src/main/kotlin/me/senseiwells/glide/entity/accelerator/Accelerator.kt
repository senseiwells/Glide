package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.utils.EntityUtils
import me.senseiwells.glide.utils.EntityUtils.createValue
import net.casual.arcade.utils.TagUtils.contains
import net.casual.arcade.utils.TagUtils.getVec3OrNull
import net.casual.arcade.utils.TagUtils.getVector3fOrNull
import net.casual.arcade.utils.TagUtils.putVec3
import net.casual.arcade.utils.TagUtils.putVector3f
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

abstract class Accelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Entity(type, level), PolymerEntity {
    // Leaking `this` causes this to be nullable
    private var size: Vec3? = size

    var targetVelocity: Double = 1.5

    init {
        this.setDisplaySize(size.toVector3f())
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

    override fun getPolymerEntityType(player: ServerPlayer?): EntityType<*> {
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

    override fun makeBoundingBox(): AABB {
        val position = this.position()
        val size = this.getSize()
        return AABB.ofSize(position, size.x, size.y, size.z)
    }

    override fun tick() {
        super.tick()
        for (entity in this.getPlayersToAccelerate()) {
            this.accelerate(entity)
        }
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        EntityUtils.replaceSynchedData(data, SIZE) { _, consumer ->
            consumer.accept(DisplayTrackedData.SCALE.createValue(this.getModelScale()))
            consumer.accept(DisplayTrackedData.BRIGHTNESS.createValue(this.getModelBrightness().pack()))
        }

        if (initial) {
            data.add(DisplayTrackedData.Item.ITEM.createValue(this.getModelItem()))
        }
    }

    protected open fun getPlayersToAccelerate(): Collection<ServerPlayer> {
        return this.level().getEntitiesOfClass(ServerPlayer::class.java, this.boundingBox)
    }

    protected abstract fun accelerate(entity: Entity)

    protected abstract fun getModelScale(): Vector3f

    protected abstract fun getModelBrightness(): Brightness

    protected abstract fun getModelItem(): ItemStack

    companion object {
        val DEFAULT_SIZE = Vec3(5.0, 5.0, 5.0)

        @JvmStatic
        private val SIZE: EntityDataAccessor<Vector3f> = SynchedEntityData.defineId(Accelerator::class.java, EntityDataSerializers.VECTOR3)
    }
}