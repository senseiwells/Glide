package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.utils.intersects
import net.casual.arcade.utils.entity.SynchedDataUtils
import net.casual.arcade.utils.entity.createValue
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.virtual.entity.utils.EntityDataAccessors
import net.fabricmc.fabric.api.networking.v1.context.PacketContext
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.joml.Vector3fc

sealed class Accelerator(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Entity(type, level), PolymerEntity {
    // Leaking `this` causes this to be nullable
    private var size: Vec3? = size

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

    fun setDisplaySize(size: Vector3fc) {
        this.entityData.set(SIZE, Vector3f(size))
    }

    fun getDisplaySize(): Vector3fc {
        return this.entityData.get(SIZE)
    }

    open fun shouldAcceleratePlayer(player: ServerPlayer, swept: Collection<AABB>): Boolean {
        return this.boundingBox.intersects(swept)
    }

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityTypes.ITEM_DISPLAY
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(SIZE, Vector3f())
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        input.read("Size", Vec3.CODEC).ifPresent(this::setSize)
        input.read("DisplaySize", ExtraCodecs.VECTOR3F).ifPresent(this::setDisplaySize)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.store("Size", Vec3.CODEC, this.getSize())
        output.store("DisplaySize", ExtraCodecs.VECTOR3F, this.getDisplaySize())
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

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        SynchedDataUtils.replaceSynchedData(data, SIZE) { _, consumer ->
            consumer.accept(EntityDataAccessors.Display.SCALE.createValue(this.getModelScale()))
            consumer.accept(EntityDataAccessors.Display.BRIGHTNESS_OVERRIDE.createValue(this.getModelBrightness().pack()))
        }

        if (initial) {
            data.add(EntityDataAccessors.Display.Item.ITEM_STACK.createValue(this.getModelItem()))
            data.add(EntityDataAccessors.Display.VIEW_RANGE.createValue(3.0F))
        }
    }

    abstract fun getSound(): Sound

    protected abstract fun getModelScale(): Vector3fc

    protected abstract fun getModelBrightness(): Brightness

    protected abstract fun getModelItem(): ItemStack

    companion object {
        val DEFAULT_SIZE = Vec3(5.0, 5.0, 5.0)

        @JvmStatic
        private val SIZE: EntityDataAccessor<Vector3fc> = SynchedEntityData.defineId(Accelerator::class.java, EntityDataSerializers.VECTOR3)
    }
}