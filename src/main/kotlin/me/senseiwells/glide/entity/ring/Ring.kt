package me.senseiwells.glide.entity.ring

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.utils.EntityUtils
import me.senseiwells.glide.utils.EntityUtils.createValue
import net.casual.arcade.utils.TagUtils.contains
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.joml.Vector3f

class Ring(
    type: EntityType<*>,
    level: Level,
    private var ringType: RingType = RingType.Blue
): Entity(type, level), PolymerEntity, GlideCollider {
    constructor(level: Level, type: RingType): this(GlideEntities.RING, level, type)

    init {
        this.setRingType(this.ringType)
    }

    fun setRingType(type: RingType) {
        this.ringType = type
        this.entityData.set(TYPE, type.ordinal.toByte())
    }

    fun getRingType(): RingType {
        return this.ringType
    }

    override fun getPolymerEntityType(player: ServerPlayer?): EntityType<*> {
        return EntityType.ITEM_DISPLAY
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(TYPE, -1)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        if (compound.contains("RingType", Tag.TAG_ANY_NUMERIC)) {
            val type = Mth.clamp(compound.getInt("RingType"), 0, 2)
            this.setRingType(RingType.entries[type])
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putByte("RingType", this.ringType.ordinal.toByte())
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        // TODO:
    }

    override fun modifyRawTrackedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        player: ServerPlayer?,
        initial: Boolean
    ) {
        EntityUtils.replaceSynchedData(data, TYPE) { _, consumer ->
            consumer.accept(DisplayTrackedData.Item.ITEM.createValue(this.getModelItem()))
        }

        if (initial) {
            data.add(DisplayTrackedData.SCALE.createValue(Vector3f(8.0F)))
            data.add(DisplayTrackedData.BRIGHTNESS.createValue(Brightness.FULL_BRIGHT.pack()))
        }
    }

    private fun getModelItem(): ItemStack {
        return this.ringType.getItemModel()
    }

    companion object {
        private val TYPE: EntityDataAccessor<Byte> = SynchedEntityData.defineId(Ring::class.java, EntityDataSerializers.BYTE)
    }
}