package me.senseiwells.glide.tracker

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.item.GlideTrailItem
import me.senseiwells.glide.utils.ColorUtils
import net.casual.arcade.utils.MathUtils.rotationTowards
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.entity.attachment.VirtualEntityAttachment
import net.casual.arcade.virtual.entity.display.SimpleVirtualItemDisplay
import net.casual.arcade.virtual.entity.location.VirtualPosition
import net.casual.arcade.virtual.entity.tracker.ObserverTracker
import net.casual.arcade.virtual.entity.utils.attachWithParentObservers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.nio.ByteBuffer

class GlideTrail(
    val start: MinecraftTimeDuration,
    val points: List<Vec3>,
    val interval: MinecraftTimeDuration
) {
    fun attachDisplays(attachment: VirtualEntityAttachment) {
        if (this.points.isEmpty()) {
            return
        }

        val iterator = this.points.iterator()
        var previous = iterator.next()
        var previousColor = 0xFF0000

        for (current in iterator) {
            val length = previous.distanceTo(current) * 1.01
            val stack = GlideItems.TRAIL
            val speed = length / this.interval.ticks * 20
            val color = ColorUtils.blend(0xFF0000, 0xFFFF00, speed.toFloat() / 25.0F)
            val blended = ColorUtils.blend(previousColor, color, 0.5F)
            previousColor = blended
            stack.set(GlideTrailItem.TRAIL_COLOR, blended)

            val item = attachment.attachWithParentObservers(::SimpleVirtualItemDisplay)
            item.setItemStack(stack)
            // We use quaternions because we get more precision with them
            item.setLeftRotation(previous.rotationTowards(current))
            item.position = VirtualPosition.Absolute(previous)
            item.setScale(Vector3f(0.3F, 0.01F, length.toFloat()))
            item.setTranslation(current.subtract(previous).scale(0.5).toVector3f())
            item.setBrightness(Brightness.FULL_BRIGHT)
            item.setObservableRange(Double.MAX_VALUE)

            previous = current
        }
    }

    companion object {
        private const val SCALE = 1_000

        private val POINTS_CODEC = Codec.BYTE_BUFFER.xmap(::deserializePoints, ::serializePoints)

        val CODEC: Codec<GlideTrail> = RecordCodecBuilder.create { instance ->
            instance.group(
                MinecraftTimeDuration.CODEC.fieldOf("start_time").forGetter(GlideTrail::start),
                POINTS_CODEC.fieldOf("points").forGetter(GlideTrail::points),
                MinecraftTimeDuration.CODEC.fieldOf("interval_time").forGetter(GlideTrail::interval)
            ).apply(instance, ::GlideTrail)
        }

        private fun serializePoints(points: List<Vec3>): ByteBuffer {
            if (points.isEmpty()) {
                return ByteBuffer.wrap(byteArrayOf())
            }

            val buffer = ByteBuffer.allocate((points.size - 1) * 3 * Short.SIZE_BYTES + 3 * Double.SIZE_BYTES)
            val iter = points.iterator()
            var previous = iter.next()
            buffer.putDouble(previous.x)
            buffer.putDouble(previous.y)
            buffer.putDouble(previous.z)
            for (current in iter) {
                val deltaX = current.x - previous.x
                val deltaY = current.y - previous.y
                val deltaZ = current.z - previous.z

                buffer.putShort((deltaX * SCALE).toInt().toShort())
                buffer.putShort((deltaY * SCALE).toInt().toShort())
                buffer.putShort((deltaZ * SCALE).toInt().toShort())
                previous = current
            }
            return buffer
        }

        private fun deserializePoints(buffer: ByteBuffer): List<Vec3> {
            if (!buffer.hasRemaining()) {
                return listOf()
            }

            val points = ArrayList<Vec3>()
            var previous = Vec3(buffer.getDouble(), buffer.getDouble(), buffer.getDouble())
            points.add(previous)
            while (buffer.hasRemaining()) {
                val deltaX = buffer.getShort().toDouble() / SCALE
                val deltaY = buffer.getShort().toDouble() / SCALE
                val deltaZ = buffer.getShort().toDouble() / SCALE

                val current = previous.add(deltaX, deltaY, deltaZ)
                points.add(current)
                previous = current
            }

            return points
        }
    }
}