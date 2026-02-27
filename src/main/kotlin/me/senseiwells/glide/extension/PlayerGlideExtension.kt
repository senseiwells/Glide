package me.senseiwells.glide.extension

import com.google.common.collect.Iterables
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.Checkpoint
import me.senseiwells.glide.checkpoint.CheckpointData
import me.senseiwells.glide.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.accelerator.Accelerator
import me.senseiwells.glide.entity.accelerator.ThermalAccelerator
import me.senseiwells.glide.entity.ring.Ring
import me.senseiwells.glide.entity.ring.RingType
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.minigame.GlideMode
import me.senseiwells.glide.tracker.*
import me.senseiwells.glide.utils.*
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.extensions.SerializableExtension
import net.casual.arcade.extensions.utils.getExtension
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.MathUtils.projectionScalar
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.server
import net.casual.arcade.utils.addVelocityAndMark
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.entity.attachment.SimpleVirtualEntityAttachment
import net.casual.arcade.virtual.entity.attachment.anchor.ManualAttachmentAnchor
import net.casual.arcade.virtual.entity.utils.attach
import net.minecraft.core.UUIDUtil
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
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
): PlayerExtension(player), SerializableExtension {
    private val personalBests = Object2ObjectOpenHashMap<Identifier, GlidePersonalBests>()

    private val accelerators = Reference2ObjectOpenHashMap<Accelerator, MutableInt>()

    private var trailAttachment: SimpleVirtualEntityAttachment? = null

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

    // These are all the properties we need for the
    // accelerator physics, and match up directly with
    // the decomp `LivingEntity` members
    // The decomp has 2 members that are seemingly unused.
    // Or at least the methods that use them haven't been
    // decomped yet, those are `bool_3E0` and `mIsInThermalArea`.

    // LivingEntity::mThermalArea
    private var accelerator: Accelerator? = null
    // LivingEntity::mHasPendingThermalEntry
    private var pendingAcceleratorEntry = true
    // LivingEntity::mIsApplyingStaticLift
    private var isApplyingStaticLift = false
    // LivingEntity::mIsApplyingLift
    private var isApplyingLift = false
    // LivingEntity::mLiftDurationTimer
    private var liftDurationTimer = 0
    // LivingEntity::mTargetLiftVelocity
    private var targetLiftVelocity = 0.0
    // LivingEntity::mStaticLiftTargetHeight
    private var staticLiftTargetY = 0.0
    // LivingEntity::mIsUpdraft
    private var isUpdraft = false
    // LivingEntity::mIsSpeedBoosting
    private var isSpeedBoosting = false
    // LivingEntity::mTargetBoostSpeed
    private var targetBoostSpeed = 0.0
    // LivingEntity::mAdditionalGlideVelocity
    private var additionalGlideVelocity = 0.0
    // LivingEntity::mAppliedLiftVelocity
    private var appliedLiftVelocity = 0.0
    // LivingEntity::qword_3E8 (This is an educated guess - Sensei)
    private var liftForceModifier = 1.0

    //Janky non-decomp sourced parity vars
    var initialEntryVelocity = Vec3.ZERO
    var respawnSpeedBoost = 0.0

    var isAppliedLiftDirty = false
        private set

    var animationTick = 0
        private set

    var finished: Boolean = false
        private set
    var finishedTime: MinecraftTimeDuration = MinecraftTimeDuration.ZERO
        private set
    var wasPersonalBest: Boolean = false
        private set
    var combo = 0
    var scoreIcon: Component = GlideFont.Score.GREY
    var score = 0
    var progress = 0.0

    var respawnLocation: Location = Location.DEFAULT

    init {
        this.deserializeAdditionalData()
    }

    fun modifyVelocityWithLift(original: Vec3): Vec3 {
        val glideVerticalVelocity = this.getGlideVerticalVelocity()
        if (glideVerticalVelocity != 0.0) {
            var clamped = original
            if (original.horizontal().length() > this.initialEntryVelocity.horizontal().length()) {
                clamped = original.horizontal().normalize().scale(this.initialEntryVelocity.horizontal().length())
            }
            return Vec3(clamped.x, glideVerticalVelocity, clamped.z)
        }
        return original
    }

    fun getGlideVerticalVelocity(): Double {
        return this.appliedLiftVelocity + this.additionalGlideVelocity
    }

    fun getLiftForceModifier(): Double {
        return this.liftForceModifier
    }

    fun getGlideNetworkScale(): Double {
        return 0.9
    }

    fun checkForCollisions() {
        val glide = this.player.getMinigame() as? GlideMinigame ?: return
        if (this.player.level() != glide.level) {
            return
        }

        val swept = this.calculateSweptHitbox()
        this.checkColliders(glide, swept)
        this.checkAccelerators(glide, swept)
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

    fun collideWithFinishLine(): Boolean {
        if (!this.finished) {
            this.finished = true
            return true
        }
        return false
    }

    fun collideWithRespawnCheckpoint(checkpoint: RespawnCheckpoint, spawn: Location, speedBoost: Double) {
        this.respawnLocation = spawn
        this.lastRespawnCheckpointIndex = checkpoint.index
        this.lastSavedScore = this.score
        this.lastSavedRingType = this.ringTypes.last()
        this.lastRespawnCheckpointTicks = CHECKPOINT_TICKS + 2
        this.respawnSpeedBoost = speedBoost
    }

    fun showTrailFor(id: Identifier, glide: GlideMinigame) {
        val bests = this.personalBests[id] ?: return
        val best = when (glide.mode) {
            GlideMode.ScoreAttack -> bests.score
            GlideMode.TimeAttack -> bests.time
        }
        if (!best.display() || this.trailAttachment != null) {
            return
        }

        val anchor = ManualAttachmentAnchor(Location.DEFAULT, this.player.level())
        val attachment = SimpleVirtualEntityAttachment(anchor)
        attachment.interactable = false

        for (trail in best.trails) {
            trail.attachDisplays(attachment)
        }

        val ghost = attachment.attach { a -> GlideGhost(a, glide, best.trails) }
        ghost.skip()
        this.trailAttachment = attachment

        attachment.startObservingAttached(this.player)
    }

    fun hideTrail() {
        val attachment = this.trailAttachment ?: return
        attachment.stopObservingAttached(this.player)
        this.trailAttachment = null
    }

    fun updateBests(
        id: Identifier,
        glide: GlideMinigame,
        time: MinecraftTimeDuration,
        trails: List<GlideTrail>
    ): Boolean {
        this.finishedTime = time

        val bests = this.personalBests[id]
            ?: GlidePersonalBests(id, GlideTimePersonalBest.DEFAULT, GlideScorePersonalBest.DEFAULT)

        val updated = glide.mode.updateBests(glide, this.player, bests, time, this.score, trails)
        if (updated != null) {
            this.personalBests[id] = updated
            this.wasPersonalBest = true
            this.serializeAdditionalData()
            return true
        }
        this.wasPersonalBest = false
        return false
    }

    fun tick(minigame: GlideMinigame) {
        this.animationTick++
        if (!minigame.players.isPlaying(this.player)) {
            return
        }

        this.trailAttachment?.tick()

        if (this.lastRespawnCheckpointTicks > 0) {
            this.lastRespawnCheckpointTicks--
        }

        this.tickProgress(minigame)

        if (this.ringTypes.size >= 2) {
            val fade = this.ringTypes[0].getFade(this.ringTypeTicks)
            val base = this.ringTypes[1].icon

            this.scoreIcon = Component.empty()
                .append(base)
                .append(SpacingFontResources.spaced(-15))
                .append(fade)

            if (++this.ringTypeTicks > 20) {
                this.ringTypes.removeFirst()
                this.ringTypeTicks = 0
            }
            return
        }
        if (this.ringTypes.isNotEmpty()) {
            this.scoreIcon = this.ringTypes[0].icon
        }
    }

    fun respawn() {
        this.oldPosition = null
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

        this.accelerator = null
        this.pendingAcceleratorEntry = true
        this.isApplyingStaticLift = false
        this.isApplyingLift = false
        this.liftDurationTimer = 0
        this.targetLiftVelocity = 0.0
        this.staticLiftTargetY = 0.0
        this.isUpdraft = false
        this.isSpeedBoosting = false
        this.targetBoostSpeed = 0.0
        this.additionalGlideVelocity = 0.0
        this.appliedLiftVelocity = 0.0
        this.liftForceModifier = 1.0

        this.initialEntryVelocity = Vec3.ZERO

        if (this.respawnSpeedBoost != 0.0) {
            this.isSpeedBoosting = true
            this.targetBoostSpeed = this.respawnSpeedBoost
        }
    }

    fun reset() {
        this.resetAnimation()
        this.finished = false
        this.finishedTime = MinecraftTimeDuration.ZERO
        this.lastRespawnCheckpointIndex = 0
        this.lastCheckpointIndex = 0
        this.lastSavedScore = 0
        this.scoreIcon = GlideFont.Score.GREY
        this.ringTypes.clear()
        this.ringTypes.add(RingType.Grey)
        this.lastSavedRings.clear()
        this.lastSavedRingType = RingType.Grey
        this.respawnSpeedBoost = 0.0
        this.respawn()
        this.hideTrail()
    }

    fun resetAnimation() {
        this.animationTick = 0
    }

    fun addProgressComponents(minigame: GlideMinigame, progressBar: MutableComponent, marker: Int) {
        val playerOffset = Mth.clamp((this.progress * 300).roundToInt(), 0, 300) - 1
        progressBar.append(SpacingFontResources.spaced(playerOffset - 309))
        progressBar.append(GlideFont.Progressbar.getPlayerMarker(marker % 16).wrap().shadowless())
        progressBar.append(SpacingFontResources.spaced(-7))
        progressBar.append(GlideFont.Progressbar.CURRENT.wrap().shadowless())

        var lastOffset = playerOffset + 6
        if (this.lastRespawnCheckpointIndex != 0 && this.lastRespawnCheckpointTicks in 0..CHECKPOINT_TICKS) {
            val distance = minigame.checkpoints.getPercentageDistance(this.lastRespawnCheckpointIndex)

            val index = 5 - 5 * abs(2.0F * this.lastRespawnCheckpointTicks - CHECKPOINT_TICKS) / CHECKPOINT_TICKS
            val checkpointOffset = Mth.clamp(Math.round(distance * 300).toInt(), 0, 300)

            progressBar.append(SpacingFontResources.spaced(checkpointOffset - (playerOffset + 4)))
            progressBar.append(GlideFont.Progressbar.getCheckpoint(index.roundToInt()).wrap().shadowless())
            lastOffset = checkpointOffset + 5
        }

        progressBar.append(SpacingFontResources.spaced(309 - lastOffset))
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

    private fun checkColliders(minigame: GlideMinigame, swept: Collection<AABB>) {
        // We only need to check if the player has collided with the previous
        // or next checkpoints, they shouldn't be able to skip checkpoints...
        val checkpoints = Iterables.concat(
            minigame.checkpoints.get(this.lastCheckpointIndex - 1),
            minigame.checkpoints.get(this.lastCheckpointIndex + 1)
        )
        for (checkpoint in checkpoints) {
            if (this.checkCollision(checkpoint, swept, minigame)) {
                break
            }
        }

        // This should be big enough for any intersecting entities
        val intersecting = this.player.level().getEntities(this.player, this.player.boundingBox.inflate(16.0))
        for (entity in intersecting) {
            if (entity is GlideCollider) {
                this.checkCollision(entity, swept, minigame)
            }
        }
    }

    private fun checkAccelerators(minigame: GlideMinigame, swept: Collection<AABB>) {
        this.liftForceModifier = 1.0

        val accelerators = minigame.level.getEntities(ACCELERATOR_TYPE_TEST) { true }
        val accelerator = accelerators.firstOrNull { accelerator ->
            accelerator.shouldAcceleratePlayer(this.player, swept)
        }

        var boostDirection: Vec3i? = null
        var boundingBox: AABB? = null
        if (accelerator != null) {
            if (this.pendingAcceleratorEntry) {
                this.pendingAcceleratorEntry = false
                // Janky check for if thermal is static, this doesn't need to be set for natural thermals or boosters.
                if (accelerator is ThermalAccelerator && accelerator.liftForceModifier != 1.0) { this.initialEntryVelocity = player.deltaMovement }
                this.player.sendSound(accelerator.getSound())
            }

            this.liftForceModifier = accelerator.getLiftForceModifier()

            val staticLift = accelerator.getStaticLift()
            if (!this.isApplyingStaticLift) {
                if (staticLift != 0.0) {
                    this.isApplyingLift = true
                    this.liftDurationTimer = 0
                }
                this.targetLiftVelocity = staticLift
            }

            val targetY = accelerator.getTargetY()
            if (targetY > 0.0) {
                this.isApplyingStaticLift = true
                this.staticLiftTargetY = targetY
            }

            this.isUpdraft = staticLift >= 0.0

            val speedBoost = accelerator.getSpeedBoost()
            if (speedBoost > 0.0) {
                this.isSpeedBoosting = true
                this.targetBoostSpeed = speedBoost
                boostDirection = accelerator.getBoostDirection()
            }

            boundingBox = accelerator.boundingBox

            @Suppress("ControlFlowWithEmptyBody")
            if (this.accelerator != accelerator) {
                // I presume this is for some client-side syncing that we don't need - Sensei
                // player->GetGameMode()->RecordThermalEntered()
            }

            this.accelerator = accelerator
        } else if (this.accelerator != null) {
            this.pendingAcceleratorEntry = true
            // This update count is used in one of the accelerators
            // use conditions, but I think it's unused - Sensei
            // mThermalArea->updateUseCount();
            this.accelerator = null
        }

        if (this.isApplyingStaticLift) {
            if (this.player.y >= this.staticLiftTargetY || this.player.verticalCollision) {
                this.isApplyingStaticLift = false
                this.isApplyingLift = false
            } else {
                this.isApplyingLift = true
            }
        }

        val previousAppliedLift = this.appliedLiftVelocity
        if (this.isApplyingLift) {
            if (this.appliedLiftVelocity == 0.0 && this.player.deltaMovement.y != 0.0) {
                this.appliedLiftVelocity = this.player.deltaMovement.y
            }
            if (this.isUpdraft) {
                if (this.appliedLiftVelocity < this.targetLiftVelocity) {
                    this.appliedLiftVelocity += (this.targetLiftVelocity - this.appliedLiftVelocity) / this.targetLiftVelocity * 0.1
                }
            } else {
                if (this.appliedLiftVelocity > this.targetLiftVelocity) {
                    this.appliedLiftVelocity += (this.targetLiftVelocity - this.appliedLiftVelocity) / this.targetLiftVelocity * -0.1
                }
            }

            if (!this.isApplyingStaticLift) {
                this.liftDurationTimer += 1
                if (this.liftDurationTimer > 20) {
                    this.isApplyingLift = false
                    this.liftDurationTimer = 0
                }
            }
        } else if (this.isUpdraft) {
            this.appliedLiftVelocity = if (this.appliedLiftVelocity > 0.0) this.appliedLiftVelocity - 0.025 else 0.0
        } else {
            this.appliedLiftVelocity = if (this.appliedLiftVelocity < 0.0) this.appliedLiftVelocity - 0.025 else 0.0
        }
        this.isAppliedLiftDirty = previousAppliedLift != this.appliedLiftVelocity

        if (this.isSpeedBoosting) {
            val deltaMovement = this.player.deltaMovement
            val horizSpeed = deltaMovement.horizontalDistance()
            if (horizSpeed < this.targetBoostSpeed) {
                if (boostDirection != null && (boundingBox?.intersects(this.player.boundingBox) ?: true)) {
                    val xBoost = abs(deltaMovement.x) * 0.1 * boostDirection.x
                    val zBoost = abs(deltaMovement.z) * 0.1 * boostDirection.z
                    this.player.addVelocityAndMark(xBoost, 0.0, zBoost)

                    val newDeltaMovement = this.player.deltaMovement
                    if (newDeltaMovement.x * boostDirection.x < 0.0 || newDeltaMovement.z * boostDirection.z < 0.0) {
                        this.isSpeedBoosting = false
                    }
                } else {
                    this.player.addVelocityAndMark(deltaMovement.x * 0.1, 0.0, deltaMovement.z * 0.1)
                }
            } else {
                this.isSpeedBoosting = false
            }
        }
    }

    private fun calculateSweptHitbox(): ArrayList<AABB> {
        val boundingBox = this.player.boundingBox

        val current = this.player.position()
        val old = this.oldPosition ?: current
        val posDelta = old.distanceToSqr(current)

        val swept = ArrayList<AABB>()
        swept.add(boundingBox)

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
                swept.add(AABB(
                    between.x - halfWidth,
                    between.y,
                    between.z - halfWidth,
                    between.x + halfWidth,
                    between.y + boundingBox.ysize,
                    between.z + halfWidth
                ))
            }
        }

        this.oldPosition = current
        return swept
    }

    private fun checkCollision(collider: GlideCollider, boxes: Collection<AABB>, minigame: GlideMinigame): Boolean {
        for (box in boxes) {
            if (collider.intersects(box)) {
                collider.collide(this.player, minigame)
                return true
            }
        }
        return false
    }

    override fun id(): Identifier {
        return GlideMod.id("glide_extension")
    }

    override fun deserialize(input: ValueInput) {
        this.finished = input.getBooleanOr("finished", false)
        this.lastRespawnCheckpointIndex = input.getIntOr("last_respawn_checkpoint_index", 0)
        this.lastCheckpointIndex = input.getIntOr("last_checkpoint_index", 0)
        this.lastCrossedCheckpoint = CheckpointData(
            input.getIntOr("last_crossed_checkpoint_index", 0),
            input.read("last_crossed_checkpoint_position", Vec3.CODEC).orElse(Vec3.ZERO)
        )
        this.ringTypes.addAll(input.read("ring_types", RingType.CODEC.listOf()).orElse(listOf()))
        this.lastSavedRingType = input.read("last_saved_ring_type", RingType.CODEC).orElse(RingType.Grey)
        this.rings.addAll(input.read("rings", UUIDUtil.CODEC_SET).orElse(setOf()))
        this.lastSavedRings.addAll(input.read("last_saved_rings", UUIDUtil.CODEC_SET).orElse(setOf()))
        this.score = input.getIntOr("score", 0)
        this.lastSavedScore = input.getIntOr("last_saved_score", 0)
        this.respawnLocation = input.read("respawn_location", Location.CODEC).orElse(this.respawnLocation)
    }

    override fun serialize(output: ValueOutput) {
        if (this.player.getMinigame() !is GlideMinigame) {
            return
        }

        output.putBoolean("finished", this.finished)
        output.putInt("last_respawn_checkpoint_index", this.lastRespawnCheckpointIndex)
        output.putInt("last_checkpoint_index", this.lastCheckpointIndex)
        output.putInt("last_crossed_checkpoint_index", this.lastCrossedCheckpoint.index)
        output.store("last_crossed_checkpoint_position", Vec3.CODEC, this.lastCrossedCheckpoint.position)
        output.store("ring_types", RingType.CODEC.listOf(), this.ringTypes)
        output.store("last_saved_ring_type", RingType.CODEC, this.lastSavedRingType)
        output.store("rings", UUIDUtil.CODEC_SET, this.rings)
        output.store("last_saved_rings", UUIDUtil.CODEC_SET, this.lastSavedRings)
        output.putInt("score", this.score)
        output.putInt("last_saved_score", this.lastSavedScore)
        output.store("respawn_location", Location.CODEC, this.respawnLocation)
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
            val result = tag.read("personal_bests", GlidePersonalBests.CODEC.listOf())
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
        tag.store("personal_bests", GlidePersonalBests.CODEC.listOf(), bests)
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
        private val ACCELERATOR_TYPE_TEST = EntityTypeTest.forClass<Entity, _>(Accelerator::class.java)

        private const val CHECKPOINT_TICKS = 12

        @JvmStatic
        val ServerPlayer.glide: PlayerGlideExtension
            get() = this.getExtension()
    }
}