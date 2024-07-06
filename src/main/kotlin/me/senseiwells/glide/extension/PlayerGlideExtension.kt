package me.senseiwells.glide.extension

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.checkpoint.Checkpoint
import me.senseiwells.glide.checkpoint.CheckpointData
import me.senseiwells.glide.checkpoint.FinishLine
import me.senseiwells.glide.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.accelerator.Accelerator
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.extensions.DataExtension
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.utils.PlayerUtils.getExtension
import net.casual.arcade.utils.location.Location
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import java.util.*

class PlayerGlideExtension(
    player: ServerPlayer
): PlayerExtension(player), DataExtension {
    private val accelerators = Object2ObjectOpenHashMap<Accelerator, MutableInt>()

    private var oldPosition: Vec3? = null

    private var finished = false

    private var lastRespawnCheckpointIndex = 0
    var lastCheckpointIndex = 0
        private set
    var lastCrossedCheckpoint = CheckpointData.DEFAULT
        private set

    var liftForceModifier = 1.0
        private set

    var respawnLocation = Location.of()

    fun collideWithAccelerator(accelerator: Accelerator): Boolean {
        if (!this.accelerators.containsKey(accelerator)) {
            this.accelerators[accelerator] = MutableInt()
            return true
        }
        return false
    }

    fun collideWithCheckpoint(checkpoint: Checkpoint): Boolean {
        this.lastCrossedCheckpoint = CheckpointData(checkpoint.index, this.player.position())
        if (this.lastCheckpointIndex + 1 == checkpoint.index) {
            this.lastCheckpointIndex = checkpoint.index
            return true
        }
        return false
    }

    fun collideWithFinishLine(finish: FinishLine): Boolean {
        if (!this.finished) {
            this.finished = true
            return true
        }
        return false
    }

    fun collideWithRespawnCheckpoint(checkpoint: RespawnCheckpoint, spawn: Location) {
        this.respawnLocation = spawn
        this.lastRespawnCheckpointIndex = checkpoint.index
    }

    fun setLiftForceModifier(modifier: Double) {
        if (modifier > this.liftForceModifier) {
            this.liftForceModifier = modifier
        }
    }

    fun tick(minigame: GlideMinigame) {
        val boundingBox = this.player.boundingBox

        val current = this.player.position()
        val old = this.oldPosition ?: current
        val posDelta = old.distanceToSqr(current)

        val boundingBoxes = LinkedList<AABB>()
        boundingBoxes.add(boundingBox)

        // The player is travelling more than one of their hit-boxes + 1 block
        val maxPosDelta = Mth.square(1 + boundingBox.xsize)
        if (posDelta > maxPosDelta) {
            val halfWidth = boundingBox.xsize / 2.0

            val splits = Mth.ceil(posDelta / maxPosDelta)
            val delta = 1 / splits.toDouble()
            for (i in 1..< splits) {
                val between = old.lerp(current, i * delta)
                boundingBoxes.add(AABB(
                    between.x - halfWidth,
                    between.y,
                    between.z - halfWidth,
                    between.x + halfWidth,
                    between.y + boundingBox.ysize,
                    between.z + halfWidth
                ))
            }
        }

        for (checkpoint in minigame.getAllCheckpoints()) {
            if (this.checkCollision(checkpoint, boundingBoxes, minigame)) {
                break
            }
        }

        // This should be big enough for any intersecting entities
        val intersecting = this.player.serverLevel().getEntities(this.player, boundingBox.inflate(40.0))
        for (entity in intersecting) {
            if (entity is GlideCollider) {
                this.checkCollision(entity, boundingBoxes, minigame)
            }
        }

        this.liftForceModifier = 1.0
        val iterator = this.accelerators.iterator()
        for (entry in iterator) {
            val (accelerator, ticks) = entry
            if (!accelerator.accelerate(this.player, ticks)) {
                iterator.remove()
            }
        }
        this.oldPosition = current
    }

    fun respawn() {
        this.liftForceModifier = 1.0
        this.lastCrossedCheckpoint = CheckpointData(this.lastRespawnCheckpointIndex, this.respawnLocation.position)
        this.player.isNoGravity = false
        this.accelerators.clear()
    }

    fun reset() {
        this.finished = false
        this.lastCheckpointIndex = 0
        this.respawn()
    }

    override fun getName(): String {
        return "glide_extension"
    }

    override fun deserialize(element: Tag) {

    }

    override fun serialize(): Tag? {
        return null
    }

    private fun checkCollision(collider: GlideCollider, boxes: List<AABB>, minigame: GlideMinigame): Boolean {
        for (box in boxes) {
            if (collider.intersects(box)) {
                collider.collide(this.player, minigame)
                return true
            }
        }
        return false
    }

    companion object {
        @JvmStatic
        val ServerPlayer.glide
            get() = this.getExtension(PlayerGlideExtension::class.java)
    }
}