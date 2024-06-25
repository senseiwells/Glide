package me.senseiwells.glide.entity.accelerator

import eu.pb4.polymer.core.api.entity.PolymerEntity
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.item.GlideItem
import me.senseiwells.glide.utils.EntityUtils.addVelocityAndMark
import me.senseiwells.glide.utils.EntityUtils.setVelocityAndMark
import net.casual.arcade.utils.TagUtils.contains
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class StaticThermal(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): DynamicThermal(type, level, size), PolymerEntity {
    private val pushing = ObjectLinkedOpenHashSet<ServerPlayer>()

    var targetHeight = 5.0
    val targetY: Double
        get() = this.y + this.targetHeight

    constructor(level: Level, size: Vec3): this(GlideEntities.STATIC_THERMAL, level, size)

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetHeight", Tag.TAG_ANY_NUMERIC)) {
            this.targetHeight = compound.getDouble("TargetHeight")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putDouble("TargetHeight", this.targetHeight)
    }

    override fun getPlayersToAccelerate(): Collection<ServerPlayer> {
        this.pushing.removeIf { player -> player.y >= this.targetY || player.isRemoved }
        this.pushing.addAll(super.getPlayersToAccelerate())
        return this.pushing
    }

    override fun accelerate(entity: Entity) {
        val delta = entity.deltaMovement
        if (delta.y < this.targetVelocity) {
            entity.addVelocityAndMark(0.0, this.targetVelocity / 3, 0.0)
        } else {
            entity.setVelocityAndMark(delta.x, this.targetVelocity, delta.z)
        }
    }

    override fun getModelItem(): ItemStack {
        return GlideItem.WEAK_THERMAL
    }
}