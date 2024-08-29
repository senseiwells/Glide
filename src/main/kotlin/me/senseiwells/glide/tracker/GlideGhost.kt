package me.senseiwells.glide.tracker

import eu.pb4.polymer.virtualentity.api.ElementHolder
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement
import me.senseiwells.glide.item.GlideTrailItem
import me.senseiwells.glide.utils.ColorUtils
import me.senseiwells.glide.utils.VecUtils.rotationAnglesTowards
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f

class GlideGhost(
    private val trails: List<GlideTrail>
): HolderAttachment {
    private val holder = ElementHolder()
    private val ghost = ItemDisplayElement(GlideTrailItem.GHOST)
    private val rightWing = ItemDisplayElement(GlideTrailItem.RIGHT_WING)
    private val leftWing = ItemDisplayElement(GlideTrailItem.LEFT_WING)

    private var currentIndex = 0
    private var currentPoint: Vec3
    private var previousColor = 0xFF0000

    init {
        this.currentPoint = this.trails.firstOrNull()?.points?.firstOrNull() ?: Vec3.ZERO

        this.rightWing.translation = Vector3f(.55F, .180F, 0.05F)
        this.leftWing.translation = Vector3f(-.55F, .180F, 0.05F)

        this.rightWing.leftRotation = Quaternionf().rotationXYZ(ELYTRA_X_ROT, ELYTRA_Y_ROT, ELYTRA_Z_ROT)
        this.leftWing.leftRotation = Quaternionf().rotationXYZ(ELYTRA_X_ROT, -ELYTRA_Y_ROT, -ELYTRA_Z_ROT)

        this.holder.addElement(this.ghost)
        this.holder.addElement(this.rightWing)
        this.holder.addElement(this.leftWing)
        this.holder.attachment = this
    }

    fun destroy(player: ServerPlayer) {
        this.holder.stopWatching(player)
    }

    fun skip(tick: Int) {
        for (i in this.trails.indices) {
            if (this.trails[i].start.ticks > tick) {
                this.currentIndex = i - 1
            }
        }
    }

    fun tick(player: ServerPlayer, tick: Int) {
        if (this.currentIndex !in this.trails.indices) {
            return
        }

        val trail = this.trails[this.currentIndex]
        val startTicks = trail.start.ticks
        if (startTicks > tick) {
            return
        } else {
            this.holder.startWatching(player)
        }

        if (tick % trail.interval.ticks == 0) {
            val interval = trail.interval.ticks
            val index = (tick - startTicks) / interval
            if (index >= trail.points.size) {
                this.currentIndex++
                this.holder.stopWatching(player)
                return
            } else if (index < 0) {
                return
            }
            val point = trail.points[index]

            val length = this.currentPoint.distanceTo(point)
            val speed = length / interval * 20
            val color = ColorUtils.blend(0xFF0000, 0xFFFF00, speed.toFloat() / 25.0F)
            val blended = ColorUtils.blend(color, this.previousColor, 0.5F)
            val rotation = point.rotationAnglesTowards(this.currentPoint)

            val ghostModel = GlideTrailItem.GHOST
            ghostModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.ghost.yaw = rotation.y + 180
            this.ghost.pitch = -rotation.x
            this.ghost.teleportDuration = interval
            this.ghost.item = ghostModel

            val rightModel = GlideTrailItem.RIGHT_WING
            rightModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.rightWing.yaw = rotation.y
            this.rightWing.pitch = rotation.x
            this.rightWing.teleportDuration = interval
            this.rightWing.item = rightModel

            val leftModel = GlideTrailItem.LEFT_WING
            leftModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.leftWing.yaw = rotation.y
            this.leftWing.pitch = rotation.x
            this.leftWing.teleportDuration = interval
            this.leftWing.item = leftModel

            this.currentPoint = point
            this.previousColor = color
        }

        this.tick()
    }

    override fun holder(): ElementHolder {
        return this.holder
    }

    override fun destroy() {
        this.holder.attachment = null
    }

    override fun getPos(): Vec3 {
        return this.currentPoint
    }

    override fun getWorld(): ServerLevel? {
        return null
    }

    override fun updateCurrentlyTracking(currentlyTracking: MutableCollection<ServerGamePacketListenerImpl>) {

    }

    override fun updateTracking(tracking: ServerGamePacketListenerImpl) {

    }

    private companion object {
        const val ELYTRA_X_ROT = 1.673035F
        const val ELYTRA_Y_ROT = 0.0F
        const val ELYTRA_Z_ROT = 2.131606F
    }
}
