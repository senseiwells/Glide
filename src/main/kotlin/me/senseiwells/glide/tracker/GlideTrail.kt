package me.senseiwells.glide.tracker

import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement
import me.senseiwells.glide.utils.VecUtils.rotationTowards
import net.casual.arcade.scheduler.MinecraftTimeDuration
import net.casual.arcade.utils.TagUtils.getVec3OrNull
import net.casual.arcade.utils.TagUtils.putVec3
import net.casual.arcade.utils.TimeUtils.Ticks
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Brightness
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.nio.ByteBuffer

class GlideTrail(
    private val start: MinecraftTimeDuration,
    private val points: List<Vec3>
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

    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt("StartTime", this.start.ticks)
        if (this.points.isEmpty()) {
            return tag
        }

        val iter = this.points.iterator()
        var previous = iter.next()
        tag.putVec3("StartPosition", previous)
        val buffer = ByteBuffer.allocate((this.points.size - 1) * 3 * Short.SIZE_BYTES)
        for (current in iter) {
            val deltaX = current.x - previous.x
            val deltaY = current.y - previous.y
            val deltaZ = current.z - previous.z

            buffer.putShort((deltaX * SCALE).toInt().toShort())
            buffer.putShort((deltaY * SCALE).toInt().toShort())
            buffer.putShort((deltaZ * SCALE).toInt().toShort())
            previous = current
        }
        tag.putByteArray("PositionDeltas", buffer.array())
        return tag
    }

    companion object {
        private const val SCALE = 1_000

        fun deserialize(tag: CompoundTag): GlideTrail {
            val start = tag.getInt("StartTime").Ticks
            var previous = tag.getVec3OrNull("StartPosition") ?: return GlideTrail(start, listOf())

            val points = ArrayList<Vec3>()
            points.add(previous)
            val buffer = ByteBuffer.wrap(tag.getByteArray("PositionDeltas"))
            while (buffer.hasRemaining()) {
                val deltaX = buffer.getShort().toDouble() / SCALE
                val deltaY = buffer.getShort().toDouble() / SCALE
                val deltaZ = buffer.getShort().toDouble() / SCALE

                val current = previous.add(deltaX, deltaY, deltaZ)
                points.add(current)
                previous = current
            }

            return GlideTrail(start, points)
        }
    }
}