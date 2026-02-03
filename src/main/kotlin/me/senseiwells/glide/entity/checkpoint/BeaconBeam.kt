package me.senseiwells.glide.entity.checkpoint

import eu.pb4.polymer.core.api.entity.PolymerEntity
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItems
import net.casual.arcade.utils.SynchedDataUtils
import net.casual.arcade.utils.createValue
import net.casual.arcade.virtual.entity.attachment.SimpleVirtualEntityAttachment
import net.casual.arcade.virtual.entity.display.SimpleVirtualItemDisplay
import net.casual.arcade.virtual.entity.utils.EntityDataAccessors
import net.casual.arcade.virtual.entity.utils.attachWithParentObservers
import net.casual.arcade.virtual.entity.utils.createVirtualEntityAttachment
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.joml.Vector3f
import xyz.nucleoid.packettweaker.PacketContext

class BeaconBeam(
    type: EntityType<*>,
    level: Level,
    length: Float = 10.0F
): Entity(type, level), PolymerEntity {
    private val shell: SimpleVirtualItemDisplay

    constructor(level: Level, length: Float): this(GlideEntities.BEACON_BEAM, level, length)

    init {
        val attachment = this.createVirtualEntityAttachment(::SimpleVirtualEntityAttachment)
        this.shell = attachment.attachWithParentObservers(::SimpleVirtualItemDisplay)
        this.shell.setBrightness(this.getModelBrightness())

        this.setLength(length)
    }

    fun setLength(length: Float) {
        this.entityData.set(LENGTH, length)
        this.shell.setScale(this.getModelScale())
        this.shell.setTranslation(this.getModelTranslation())
    }

    fun getLength(): Float {
        return this.entityData.get(LENGTH)
    }

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityType.ITEM_DISPLAY
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(LENGTH, 0.0F)
    }

    override fun tick() {

    }

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, f: Float): Boolean {
        return false
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        this.setLength(input.getFloatOr("BeamLength", this.getLength()))
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.putFloat("BeamLength", this.getLength())
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        SynchedDataUtils.replaceSynchedData(data, LENGTH) { _, consumer ->
            consumer.accept(EntityDataAccessors.Display.SCALE.createValue(this.getModelScale()))
            consumer.accept(EntityDataAccessors.Display.TRANSLATION.createValue(this.getModelTranslation()))
        }

        if (initial) {
            data.add(EntityDataAccessors.Display.Item.ITEM_STACK.createValue(GlideItems.FAKE_BEAM))
            data.add(EntityDataAccessors.Display.BRIGHTNESS_OVERRIDE.createValue(this.getModelBrightness().pack()))
            data.add(EntityDataAccessors.Display.VIEW_RANGE.createValue(3.0F))
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