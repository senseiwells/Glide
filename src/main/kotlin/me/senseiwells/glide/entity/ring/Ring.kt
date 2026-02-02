package me.senseiwells.glide.entity.ring

import eu.pb4.polymer.core.api.entity.PolymerEntity
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.SynchedDataUtils
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.createValue
import net.casual.arcade.utils.math.location.LocationWithLevel.Companion.locationWithLevel
import net.casual.arcade.visuals.entity.firework.VirtualFireworkBuilder
import net.minecraft.core.Direction
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import xyz.nucleoid.packettweaker.PacketContext
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

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
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

    override fun readAdditionalSaveData(input: ValueInput) {
        val type = input.getInt("RingType").map { Mth.clamp(it, 0, 2) }
        if (type.isPresent) {
            this.setRingType(RingType.entries[type.get()])
        }
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.putInt("RingType", this.getRingType().ordinal)
    }

    override fun makeBoundingBox(position: Vec3): AABB {
        val size = this.getRingType().getSize().toDouble() - 3
        val direction = Direction.orderedByNearest(this)[0]
        return AABB.ofSize(
            position,
            abs(abs(direction.stepX) - 1) * size + 1,
            abs(abs(direction.stepY) - 1) * size + 1,
            abs(abs(direction.stepZ) - 1) * size + 1
        )
    }

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, f: Float): Boolean {
        return false
    }

    override fun intersects(box: AABB): Boolean {
        return this.boundingBox.intersects(box)
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        if (player.glide.collideWithRing(this)) {
            player.glide.score += this.getRingType().points
            player.sendSound(this.getRingType().getSound(player.glide.combo), static = true)

            val builder = VirtualFireworkBuilder(player.level())
            builder.duration(0.Ticks)

            builder.location(player.locationWithLevel)
            builder.explosion {
                addPrimaryColors(getRingType().color)
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
        SynchedDataUtils.replaceSynchedData(data, TYPE) { _, consumer ->
            consumer.accept(DisplayTrackedData.Item.ITEM.createValue(this.getModelItem()))
        }

        if (initial) {
            data.add(DisplayTrackedData.SCALE.createValue(Vector3f(8.0F)))
            data.add(DisplayTrackedData.BRIGHTNESS.createValue(Brightness.FULL_BRIGHT.pack()))
            data.add(DisplayTrackedData.VIEW_RANGE.createValue(3.0F))
        }
    }

    private fun getModelItem(): ItemStack {
        return this.getRingType().getModel()
    }

    companion object {
        private val TYPE: EntityDataAccessor<Byte> = SynchedEntityData.defineId(Ring::class.java, EntityDataSerializers.BYTE)
    }
}