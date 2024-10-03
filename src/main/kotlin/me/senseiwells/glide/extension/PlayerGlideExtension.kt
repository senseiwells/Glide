package me.senseiwells.glide.extension

import com.google.common.collect.Iterables
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.Checkpoint
import me.senseiwells.glide.checkpoint.CheckpointData
import me.senseiwells.glide.checkpoint.FinishLine
import me.senseiwells.glide.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.accelerator.Accelerator
import me.senseiwells.glide.entity.ring.Ring
import me.senseiwells.glide.entity.ring.RingType
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.minigame.GlideMode
import me.senseiwells.glide.minigame.GlidePhase
import me.senseiwells.glide.tracker.*
import me.senseiwells.glide.utils.VecUtils.projectionScalar
import net.casual.arcade.extensions.DataExtension
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.extensions.event.PlayerExtensionEvent.Companion.getExtension
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.utils.*
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.impl.Location
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.Util
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import java.io.IOException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isRegularFile
import kotlin.math.abs
import kotlin.math.roundToInt

class PlayerGlideExtension(
    player: ServerPlayer
): PlayerExtension(player), DataExtension {
    private val personalBests = Object2ObjectOpenHashMap<ResourceLocation, GlidePersonalBests>()

    private val accelerators = Object2ObjectOpenHashMap<Accelerator, MutableInt>()

    private val trails = IntArrayList()
    private var ghost: GlideGhost? = null

    private var oldPosition: Vec3? = null

    private var lastRespawnCheckpointIndex = 0
    private var lastRespawnCheckpointTicks = 0
    private var lastCheckpointIndex = 0
    private var lastCrossedCheckpoint = CheckpointData.DEFAULT

    private var ringTypes = ArrayList<RingType>()
    private var lastSavedRingType = RingType.Grey
    private var ringTypeTicks = 0

    private val rings = ObjectOpenHashSet<UUID>()
    private val lastSavedRings = ObjectOpenHashSet<UUID>()
    private var lastSavedScore = 0

    var animationTick = 0
        private set

    var finished = false
        private set
    var combo = 0
    var scoreIcon: Component = GlideFont.Score.GREY
    var score = 0
    var progress = 0.0

    var liftForceModifier = 1.0
        private set

    var respawnLocation = Location.of()

    init {
        this.deserializeAdditionalData()
    }

    fun collideWithAccelerator(accelerator: Accelerator): Boolean {
        if (!this.accelerators.containsKey(accelerator)) {
            this.accelerators[accelerator] = MutableInt()
            return true
        }
        return false
    }

    fun collideWithRing(ring: Ring): Boolean {
        if (this.rings.add(ring.uuid)) {
            val type = ring.getRingType()
            this.addRingType(type)
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
        this.lastSavedScore = this.score
        this.lastSavedRingType = this.ringTypes.last()
        this.lastRespawnCheckpointTicks = CHECKPOINT_TICKS + 2
    }

    fun setLiftForceModifier(modifier: Double) {
        if (modifier > this.liftForceModifier) {
            this.liftForceModifier = modifier
        }
    }

    fun showTrailFor(id: ResourceLocation, mode: GlideMode, tick: Int) {
        val bests = this.personalBests[id] ?: return
        val best = when (mode) {
            GlideMode.ScoreAttack -> bests.score
            GlideMode.TimeAttack -> bests.time
        }
        if (!best.display()) {
            return
        }
        for (display in best.trails.flatMap(GlideTrail::createDisplays)) {
            display.startWatching(this.player, this.player.connection::send)
            this.trails.add(display.entityId)
        }

        val ghost = GlideGhost(best.trails)
        ghost.skip(tick)
        this.ghost = ghost
    }

    fun hideTrail() {
        if (this.trails.isNotEmpty()) {
            this.player.connection.send(ClientboundRemoveEntitiesPacket(this.trails))
            this.trails.clear()
        }
        val ghost = this.ghost
        if (ghost != null) {
            ghost.destroy(this.player)
            this.ghost = null
        }
    }

    fun checkForBest(
        id: ResourceLocation,
        mode: GlideMode,
        time: MinecraftTimeDuration,
        trails: List<GlideTrail>
    ): Boolean {
        val bests = this.personalBests[id]
        if (bests == null) {
            this.personalBests[id] = when (mode) {
                GlideMode.ScoreAttack -> GlidePersonalBests(
                    id, GlideTimePersonalBest.DEFAULT, GlideScorePersonalBest(this.score, trails)
                )
                GlideMode.TimeAttack -> GlidePersonalBests(
                    id, GlideTimePersonalBest(time, trails), GlideScorePersonalBest.DEFAULT
                )
            }
            this.serializeAdditionalData()
            return true
        }

        when (mode) {
            GlideMode.ScoreAttack -> if (this.score > bests.score.score) {
                this.personalBests[id] = bests.copy(score = GlideScorePersonalBest(this.score, trails))
                this.serializeAdditionalData()
                return true
            }
            GlideMode.TimeAttack -> if (time < bests.time.time) {
                this.personalBests[id] = bests.copy(time = GlideTimePersonalBest(time, trails))
                this.serializeAdditionalData()
                return true
            }
        }
        return false
    }

    fun tick(minigame: GlideMinigame, tick: Int) {
        if (minigame.isPhase(GlidePhase.Active)) {
            this.ghost?.tick(this.player, tick)
        }
        this.animationTick++

        if (this.lastRespawnCheckpointTicks > 0) {
            this.lastRespawnCheckpointTicks--
        }

        this.tickCollisions(minigame)

        this.liftForceModifier = 1.0
        val iterator = this.accelerators.iterator()
        for (entry in iterator) {
            val (accelerator, ticks) = entry
            if (!accelerator.accelerate(this.player, ticks)) {
                iterator.remove()
            }
        }

        this.tickProgress(minigame)

        if (this.ringTypes.size >= 2) {
            val fade = this.ringTypes[0].getFade(this.ringTypeTicks)
            val base = this.ringTypes[1].icon

            this.scoreIcon = Component.empty()
                .append(base)
                .append(ComponentUtils.space(-15))
                .append(fade)

            if (++this.ringTypeTicks > 20) {
                this.ringTypes.removeFirst()
                this.ringTypeTicks = 0
            }
            return
        }
        this.scoreIcon = this.ringTypes[0].icon
    }

    fun respawn() {
        this.oldPosition = null
        this.liftForceModifier = 1.0
        this.lastCrossedCheckpoint = CheckpointData(this.lastRespawnCheckpointIndex, this.respawnLocation.position)
        this.player.isNoGravity = false
        this.score = this.lastSavedScore
        this.rings.clear()
        this.rings.addAll(this.lastSavedRings)
        this.accelerators.clear()

        val recent = this.ringTypes.last()
        this.ringTypes.clear()
        this.ringTypes.add(recent)
        this.addRingType(this.lastSavedRingType)
        this.ringTypeTicks = 0
    }

    fun reset() {
        this.animationTick = 0
        this.finished = false
        this.lastRespawnCheckpointIndex = 0
        this.lastCheckpointIndex = 0
        this.lastSavedScore = 0
        this.scoreIcon = GlideFont.Score.GREY
        this.ringTypes.clear()
        this.ringTypes.add(RingType.Grey)
        this.lastSavedRings.clear()
        this.lastSavedRingType = RingType.Grey
        this.respawn()
        this.hideTrail()
    }

    fun addProgressComponents(minigame: GlideMinigame, progressBar: MutableComponent, marker: Int) {
        val playerOffset = Mth.clamp(Math.round(this.progress * 300).toInt(), 0, 300) - 1
        progressBar.append(ComponentUtils.space(playerOffset - 309))
        progressBar.append(GlideFont.Progressbar.getPlayerMarker(marker % 16).shadowless())
        progressBar.append(ComponentUtils.space(-7))
        progressBar.append(GlideFont.Progressbar.CURRENT.shadowless())

        var lastOffset = playerOffset + 6
        if (this.lastRespawnCheckpointIndex != 0 && this.lastRespawnCheckpointTicks in 0..CHECKPOINT_TICKS) {
            val distance = minigame.checkpoints.getPercentageDistance(this.lastRespawnCheckpointIndex)

            val index = 5 - 5 * abs(2.0F * this.lastRespawnCheckpointTicks - CHECKPOINT_TICKS) / CHECKPOINT_TICKS
            val checkpointOffset = Mth.clamp(Math.round(distance * 300).toInt(), 0, 300)

            progressBar.append(ComponentUtils.space(checkpointOffset - (playerOffset + 4)))
            progressBar.append(GlideFont.Progressbar.getCheckpoint(index.roundToInt()).shadowless())
            lastOffset = checkpointOffset + 5
        }

        progressBar.append(ComponentUtils.space(309 - lastOffset))
    }

    private fun addRingType(type: RingType) {
        if (this.ringTypes.last() != type) {
            this.ringTypes.addLast(type)
            this.combo = 0
        } else {
            this.combo++
        }
    }

    private fun tickProgress(minigame: GlideMinigame) {
        val position = this.player.position()
        val checkpoints = minigame.checkpoints
        val current = this.lastCrossedCheckpoint
        if (checkpoints.isFinish(current.index)) {
            this.progress = 100.0
            return
        }

        val previous = checkpoints.getNearest(current.previous, position).position
        val next = checkpoints.getNearest(current.next, position).position

        val distanceToCurrent = checkpoints.getDistance(current.index)
        val progress = position.projectionScalar(current.position, next)
        val delta = if (progress >= 0) {
            val distanceToNext = checkpoints.getDistance(current.next)
            (distanceToNext - distanceToCurrent) * progress
        } else {
            val distanceToPrevious = checkpoints.getDistance(current.previous)
            -(distanceToCurrent - distanceToPrevious) * (1 - position.projectionScalar(previous, current.position))
        }
        val currentDistance = distanceToCurrent + delta
        this.progress = Mth.clamp(currentDistance / checkpoints.totalLength, 0.0, 1.0)
    }

    private fun tickCollisions(minigame: GlideMinigame) {
        val boundingBox = this.player.boundingBox

        val current = this.player.position()
        val old = this.oldPosition ?: current
        val posDelta = old.distanceToSqr(current)

        val boundingBoxes = LinkedList<AABB>()
        boundingBoxes.add(boundingBox)

        // We have to lerp the hit-box if the player is travelling
        // fast enough that it could skip potential collisions.
        // We assume that colliders are *at least* 1 block thick.
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

        // We only need to check if the player has collided with the previous
        // or next checkpoints, they shouldn't be able to skip checkpoints...
        val checkpoints = Iterables.concat(
            minigame.checkpoints.get(this.lastCheckpointIndex - 1),
            minigame.checkpoints.get(this.lastCheckpointIndex + 1)
        )
        for (checkpoint in checkpoints) {
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

        this.oldPosition = current
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

    override fun getName(): String {
        return "glide_extension"
    }

    override fun deserialize(element: Tag) {
        element as CompoundTag
        this.finished = element.getBoolean("finished")
        this.lastRespawnCheckpointIndex = element.getInt("last_respawn_checkpoint_index")
        this.lastCheckpointIndex = element.getInt("last_checkpoint_index")
        this.lastCrossedCheckpoint = CheckpointData(
            element.getInt("last_crossed_checkpoint_index"),
            element.getVec3("last_crossed_checkpoint_position")
        )
        this.ringTypes.addAll(element.getIntArray("ring_types").map { RingType.entries[it] })
        this.lastSavedRingType = RingType.entries[element.getInt("last_saved_ring_type")]
        this.rings.addAll(element.getList("rings", Tag.TAG_INT_ARRAY.toInt()).map { NbtUtils.loadUUID(it) })
        this.lastSavedRings.addAll(element.getList("last_saved_rings", Tag.TAG_INT_ARRAY.toInt()).map { NbtUtils.loadUUID(it) })
        this.score = element.getInt("score")
        this.lastSavedScore = element.getInt("last_saved_score")
        this.respawnLocation = element.getLocation("respawn_location", this.player.server)
    }

    override fun serialize(): CompoundTag? {
        if (this.player.getMinigame() !is GlideMinigame) {
            return null
        }

        val tag = CompoundTag()
        tag.putBoolean("finished", this.finished)
        tag.putInt("last_respawn_checkpoint_index", this.lastRespawnCheckpointIndex)
        tag.putInt("last_checkpoint_index", this.lastCheckpointIndex)
        tag.putInt("last_crossed_checkpoint_index", this.lastCrossedCheckpoint.index)
        tag.putVec3("last_crossed_checkpoint_position", this.lastCrossedCheckpoint.position)
        tag.put("ring_types", IntArrayTag(this.ringTypes.map { it.ordinal }.toIntArray()))
        tag.putInt("last_saved_ring_type", this.lastSavedRingType.ordinal)
        tag.put("rings", this.rings.mapTo(ListTag()) { NbtUtils.createUUID(it) })
        tag.put("last_saved_rings", this.lastSavedRings.mapTo(ListTag()) { NbtUtils.createUUID(it) })
        tag.putInt("score", this.score)
        tag.putInt("last_saved_score", this.lastSavedScore)
        tag.putLocation("respawn_location", this.respawnLocation)
        return tag
    }

    // Additional data that may be large,
    // we don't want to store it in player data

    private fun deserializeAdditionalData() {
        val path = this.getAdditionalDataPath()
        if (!path.isRegularFile()) {
            return
        }
        try {
            val tag = NbtIo.read(path) ?: return
            val encoded = tag.getList("personal_bests", Tag.TAG_COMPOUND.toInt())
            val result = GlidePersonalBests.CODEC.listOf().parse(NbtOps.INSTANCE, encoded).result()
            if (result.isPresent) {
                for (best in result.get()) {
                    this.personalBests[best.map] = best
                }
            }
        } catch (e: IOException) {
            GlideMod.logger.warn("Failed to read additional data for ${this.player.scoreboardName}", e)
        }
    }

    private fun serializeAdditionalData() {
        val path = this.getAdditionalDataPath()
        path.createParentDirectories()

        val tag = CompoundTag()
        val bests = this.personalBests.values.toList()
        val result = GlidePersonalBests.CODEC.listOf().encodeStart(NbtOps.INSTANCE, bests).result()
        if (result.isPresent) {
            tag.put("personal_bests", result.get())
        }
        Util.ioPool().execute {
            try {
                NbtIo.write(tag, path)
            } catch (e: IOException) {
                GlideMod.logger.warn("Failed to write additional data for ${this.player.score}", e)
            }
        }
    }

    private fun getAdditionalDataPath(): Path {
        val root = this.player.server.getWorldPath(LevelResource.ROOT)
        return root.resolve("glide").resolve(this.player.stringUUID + ".nbt")
    }

    companion object {
        private const val CHECKPOINT_TICKS = 12

        @JvmStatic
        val ServerPlayer.glide
            get() = this.getExtension<PlayerGlideExtension>()
    }
}