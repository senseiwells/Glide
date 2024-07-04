package me.senseiwells.glide.entity.checkpoint

import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class ProgressCheckpoint(
    type: EntityType<*>,
    level: Level,
    size: Vec3 = DEFAULT_SIZE
): Checkpoint(type, level, size) {
    constructor(level: Level, size: Vec3): this(GlideEntities.PROGRESS_CHECKPOINT, level, size)

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        player.glide.collideWithCheckpoint(this)
    }
}