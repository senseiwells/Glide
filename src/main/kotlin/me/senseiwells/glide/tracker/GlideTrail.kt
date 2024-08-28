package me.senseiwells.glide.tracker

import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement
import me.senseiwells.glide.utils.VecUtils.rotationTowards
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

class GlideTrail(
    private val points: List<Vec3>
) {
    fun createDisplays(): List<DisplayElement> {
        if (this.points.isEmpty()) {
            return listOf()
        }

        val elements = ArrayList<DisplayElement>(this.points.size - 1)
        val yellow = Blocks.YELLOW_CONCRETE.defaultBlockState()
        val iterator = this.points.iterator()
        var previous = iterator.next()
        for (current in iterator) {
            val length = previous.distanceTo(current) * 1.01
            val element = BlockDisplayElement(yellow)
            // We use quaternions because we get more precision with them
            element.leftRotation = previous.rotationTowards(current)
            element.overridePos = previous
            element.translation = Vector3f(-0.15F, -0.05F, 0.0F)
            element.scale = Vector3f(0.3F, 0.1F, length.toFloat())
            elements.add(element)

            previous = current
        }
        return elements
    }
}