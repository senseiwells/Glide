package me.senseiwells.glide.entity.checkpoint

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.ElementHolder
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.utils.EntityUtils
import me.senseiwells.glide.utils.EntityUtils.createValue
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import org.joml.Vector3f

class BeaconBeam(
    type: EntityType<*>,
    level: Level,
    length: Float = 10.0F
): Entity(type, level), PolymerEntity {
    private val shell = ItemDisplayElement(GlideItem.FAKE_BEAM_SHELL)
    private val holder = ElementHolder()

    constructor(level: Level, length: Float): this(GlideEntities.BEACON_BEAM, level, length)

    init {
        this.shell.scale = this.getModelScale()
        this.shell.brightness = this.getModelBrightness()
        this.holder.addElement(this.shell)
        EntityAttachment.ofTicking(this.holder, this)

        this.setLength(length)
    }

    fun setLength(length: Float) {
        this.entityData.set(LENGTH, length)
        this.shell.scale = this.getModelScale()
        this.shell.translation = this.getModelTranslation()
    }

    fun getLength(): Float {
        return this.entityData.get(LENGTH)
    }

    override fun getPolymerEntityType(player: ServerPlayer?): EntityType<*> {
        return EntityType.ITEM_DISPLAY
    }

    override fun setYRot(yRot: Float) {
        super.setYRot(yRot)
        this.shell.yaw = yRot
    }

    override fun setXRot(xRot: Float) {
        super.setXRot(xRot)
        this.shell.pitch = xRot
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(LENGTH, 0.0F)
    }

    override fun tick() {

    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        if (compound.contains("BeamLength", 99)) {
            this.setLength(compound.getFloat("BeamLength"))
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putFloat("BeamLength", this.getLength())
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        EntityUtils.replaceSynchedData(data, LENGTH) { _, consumer ->
            consumer.accept(DisplayTrackedData.SCALE.createValue(this.getModelScale()))
            consumer.accept(DisplayTrackedData.TRANSLATION.createValue(this.getModelTranslation()))
        }

        if (initial) {
            data.add(DisplayTrackedData.Item.ITEM.createValue(GlideItem.FAKE_BEAM))
            data.add(DisplayTrackedData.BRIGHTNESS.createValue(this.getModelBrightness().pack()))
        }
    }

    private fun getModelTranslation(): Vector3f {
        return Vector3f(0.0F, 0.0F, this.getLength() / 2)
    }

    private fun getModelScale(): Vector3f {
        return Vector3f(1.0F, 1.0F, this.getLength())
    }

    private fun getModelBrightness(): Brightness {
        return Brightness.FULL_BRIGHT
    }

    companion object {
        @JvmStatic
        private val LENGTH: EntityDataAccessor<Float> = SynchedEntityData.defineId(BeaconBeam::class.java, EntityDataSerializers.FLOAT)
    }
}