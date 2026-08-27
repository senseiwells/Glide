package me.senseiwells.glide.tracker

import com.mojang.math.Axis
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.item.GlideTrailItem
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.utils.ColorUtils
import net.casual.arcade.observer.Observer
import net.casual.arcade.utils.MathUtils.rotationAnglesTowards
import net.casual.arcade.virtual.entity.SimpleParentVirtualEntity
import net.casual.arcade.virtual.entity.attachment.VirtualEntityAttachment
import net.casual.arcade.virtual.entity.display.SimpleVirtualItemDisplay
import net.casual.arcade.virtual.entity.location.VirtualPosition
import net.casual.arcade.virtual.entity.location.VirtualRotation
import net.casual.arcade.virtual.entity.utils.attachWithParentObservers
import net.casual.arcade.virtual.entity.utils.location
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f

class GlideGhost(
    attachment: VirtualEntityAttachment,
    private val minigame: GlideMinigame,
    private val trails: List<GlideTrail>,
): SimpleParentVirtualEntity(attachment) {
    private val ghost = this.attachWithParentObservers(::SimpleVirtualItemDisplay)
    private val rightWing = this.attachWithParentObservers(::SimpleVirtualItemDisplay)
    private val leftWing = this.attachWithParentObservers(::SimpleVirtualItemDisplay)

    private var visible: Boolean = true

    private var currentIndex = 0
    private var currentPoint: Vec3
    private var previousColor = 0xFF0000

    init {
        this.currentPoint = this.trails.firstOrNull()?.points?.firstOrNull() ?: Vec3.ZERO
        this.position = VirtualPosition.Absolute(this.currentPoint)

        this.ghost.setItemStack(GlideItems.GHOST)
        this.ghost.setLeftRotation(Axis.XP.rotationDegrees(90F))
        this.ghost.setObservableRange(Double.MAX_VALUE)

        this.rightWing.setItemStack(GlideItems.RIGHT_WING)
        this.rightWing.setTranslation(Vector3f(0.55F, 0.110F, -0.4F))
        this.rightWing.setLeftRotation(Quaternionf().rotationXYZ(ELYTRA_X_ROT, ELYTRA_Y_ROT, ELYTRA_Z_ROT))
        this.rightWing.setObservableRange(Double.MAX_VALUE)

        this.leftWing.setItemStack(GlideItems.LEFT_WING)
        this.leftWing.setTranslation(Vector3f(-0.55F, 0.110F, -0.4F))
        this.leftWing.setLeftRotation(Quaternionf().rotationXYZ(ELYTRA_X_ROT, -ELYTRA_Y_ROT, -ELYTRA_Z_ROT))
        this.leftWing.setObservableRange(Double.MAX_VALUE)
    }

    fun skip() {
        val ticks = this.minigame.ticks
        for (i in this.trails.indices) {
            if (this.trails[i].start.ticks > ticks) {
                this.currentIndex = i - 1
            }
        }
    }

    override fun updateChildren() {
        if (this.currentIndex !in this.trails.indices) {
            this.visible = false
            return
        }

        val ticks = this.minigame.ticks
        val trail = this.trails[this.currentIndex]
        val startTicks = trail.start.ticks
        if (startTicks > ticks) {
            this.visible = false
            return
        } else {
            this.visible = true
        }

        if (ticks % trail.interval.ticks == 0) {
            val interval = trail.interval.ticks
            val index = (ticks - startTicks) / interval
            if (index >= trail.points.size) {
                this.currentIndex++
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
            this.position = VirtualPosition.Absolute(point)
            this.rotation = VirtualRotation.Absolute(rotation)

            val ghostModel = GlideItems.GHOST
            ghostModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.ghost.rotation = VirtualRotation.Absolute(Vec2(-rotation.x, rotation.y + 180))
            this.ghost.setTeleportationInterpolation(interval)
            this.ghost.setItemStack(ghostModel)

            val rightModel = GlideItems.RIGHT_WING
            rightModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.rightWing.setTeleportationInterpolation(interval)
            this.rightWing.setItemStack(rightModel)

            val leftModel = GlideItems.LEFT_WING
            leftModel.set(GlideTrailItem.TRAIL_COLOR, blended)
            this.leftWing.setTeleportationInterpolation(interval)
            this.leftWing.setItemStack(leftModel)

            this.currentPoint = point
            this.previousColor = color
        }
    }

    override fun canObserve(observer: Observer): Boolean {
        return this.visible && this.location().position.closerThan(observer.location.position, 64.0)
    }

    private companion object {
        const val ELYTRA_X_ROT = 1.673035F
        const val ELYTRA_Y_ROT = 0.0F
        const val ELYTRA_Z_ROT = 2.131606F
    }
}
