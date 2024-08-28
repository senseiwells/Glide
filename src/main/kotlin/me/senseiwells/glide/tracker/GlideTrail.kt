package me.senseiwells.glide.tracker

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement
import me.senseiwells.glide.utils.VecUtils.rotationTowards
import net.casual.arcade.scheduler.MinecraftTimeDuration
import net.minecraft.util.Brightness
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.nio.ByteBuffer

class GlideTrail(
    private val start: MinecraftTimeDuration,
    private val points: List<Vec3>,
    private val interval: MinecraftTimeDuration
) {
    fun createDisplays(): List<DisplayElement> {
        if (this.points.isEmpty()) {
            return listOf()
        }

        val elements = ArrayList<DisplayElement>(this.points.size - 1)
        val yellow = Blocks.YELLOW_STAINED_GLASS.defaultBlockState()
        val iterator = this.points.iterator()
        var previous = iterator.next()
        for (current in iterator) {
            val length = previous.distanceTo(current) * 1.01
            val element = BlockDisplayElement(yellow)
            // We use quaternions because we get more precision with them
            element.leftRotation = previous.rotationTowards(current)
            element.overridePos = previous
            element.scale = Vector3f(0.3F, 0.1F, length.toFloat())
            element.brightness = Brightness.FULL_BRIGHT
            elements.add(element)

            previous = current
        }
        return elements
    }

    companion object {
        private const val SCALE = 1_000

        private val POINTS_CODEC = Codec.BYTE_BUFFER.xmap(::deserializePoints, ::serializePoints)

        val CODEC = RecordCodecBuilder.create { instance ->
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