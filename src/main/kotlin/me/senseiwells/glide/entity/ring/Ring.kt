package me.senseiwells.glide.entity.ring

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.utils.EntityUtils
import me.senseiwells.glide.utils.EntityUtils.createValue
import net.casual.arcade.entity.firework.VirtualFireworkBuilder
import net.casual.arcade.utils.PlayerUtils.location
import net.casual.arcade.utils.TagUtils.contains
import net.casual.arcade.utils.TimeUtils.Ticks
import net.minecraft.core.Direction
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
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import kotlin.math.abs

class Ring(
    type: EntityType<*>,
    level: Level,
    ringType: RingType = RingType.Blue
): Entity(type, level), PolymerEntity, GlideCollider {
    private var ringType: RingType? = null

    constructor(level: Level, type: RingType): this(GlideEntities.RING, level, type)

    init {
        this.setRingType(ringType)
        this.boundingBox = this.makeBoundingBox()
    }

    fun setRingType(type: RingType) {
        this.ringType = type
        this.entityData.set(TYPE, type.ordinal.toByte())
    }

    fun getRingType(): RingType {
        return this.ringType ?: RingType.Grey
    }

    override fun getPolymerEntityType(player: ServerPlayer?): EntityType<*> {
        return EntityType.ITEM_DISPLAY
    }

    override fun setYRot(yRot: Float) {
        super.setYRot(yRot)
        this.boundingBox = this.makeBoundingBox()
    }

    override fun setXRot(xRot: Float) {
        super.setXRot(xRot)
        this.boundingBox = this.makeBoundingBox()
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
        compound.putByte("RingType", this.getRingType().ordinal.toByte())
    }

    override fun makeBoundingBox(): AABB {
        val size = this.getRingType().getSize().toDouble() - 1
        val direction = Direction.orderedByNearest(this)[0]
        return AABB.ofSize(
            this.position(),
            abs(abs(direction.stepX) - 1) * size + 1,
            abs(abs(direction.stepY) - 1) * size + 1,
            abs(abs(direction.stepZ) - 1) * size + 1
        )
    }

    override fun intersects(box: AABB): Boolean {
        return this.boundingBox.intersects(box)
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        if (player.glide.collideWithRing(this)) {
            player.glide.score += this.getRingType().points

            val builder = VirtualFireworkBuilder()
            builder.duration(0.Ticks)

            builder.location(player.location)
            builder.explosion {
                addPrimaryColours(getRingType().color)
                shape(FireworkExplosion.Shape.LARGE_BALL)
                twinkle()
            }
            builder.build().sendTo(player)
        }
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
            data.add(DisplayTrackedData.VIEW_RANGE.createValue(3.0F))
        }
    }

    private fun getModelItem(): ItemStack {
        return this.getRingType().getItemModel()
    }

    companion object {
        private val TYPE: EntityDataAccessor<Byte> = SynchedEntityData.defineId(Ring::class.java, EntityDataSerializers.BYTE)
    }
}