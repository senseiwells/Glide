package me.senseiwells.glide.entity.checkpoint

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.utils.ExtraEntityDataSerializers
import net.casual.arcade.utils.MinigameUtils.getMinigame
import net.casual.arcade.utils.TagUtils.contains
import net.casual.arcade.utils.TagUtils.getVec2OrNull
import net.casual.arcade.utils.TagUtils.getVec3OrNull
import net.casual.arcade.utils.TagUtils.putVec2
import net.casual.arcade.utils.TagUtils.putVec3
import net.casual.arcade.utils.location.Location
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

class RespawnCheckpoint(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Checkpoint(type, level, size) {
    var index: Int = 0
    var respawnOffset: Vec3 = Vec3.ZERO
    var respawnRotation: Vec2 = Vec2.ZERO

    val respawnLocation: Location
        get() = Location.of(this.position().add(this.respawnOffset), this.respawnRotation, this.level() as ServerLevel)

    constructor(level: Level, size: Vec3): this(GlideEntities.RESPAWN_CHECKPOINT, level, size)

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Index", Tag.TAG_ANY_NUMERIC)) {
            this.index = compound.getInt("Index")
        }
        val respawnOffset = compound.getVec3OrNull("RespawnOffset")
        if (respawnOffset != null) {
            this.respawnOffset = respawnOffset
        }
        val respawnRotation = compound.getVec2OrNull("RespawnRotation")
        if (respawnRotation != null) {
            this.respawnRotation = respawnRotation
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("Index", this.index)
        compound.putVec3("RespawnOffset", this.respawnOffset)
        compound.putVec2("RespawnRotation", this.respawnRotation)
    }

    override fun pass(player: ServerPlayer) {
        val minigame = player.getMinigame()
        if (minigame is GlideMinigame) {
            minigame.onPlayerCheckpoint(player, this)
        }
    }
}