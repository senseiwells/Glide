package me.senseiwells.glide.addon.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.entity.ring.Ring
import me.senseiwells.glide.entity.ring.RingType
import net.casual.arcade.utils.serialization.codec.ArcadeExtraCodecs
import net.minecraft.core.Direction.Axis
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

class RingTemplate(
    private val position: Vec3,
    private val axis: Axis,
    private val type: RingType
) {
    fun create(level: ServerLevel): Ring {
        val ring = Ring(level, this.type)
        ring.setPos(this.position)
        when (this.axis) {
            Axis.X -> ring.yRot = -90.0F
            Axis.Y -> ring.xRot = -90.0F
            Axis.Z -> ring.yRot = 0.0F
        }
        return ring
    }

    companion object {
        val CODEC: Codec<RingTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3.CODEC.fieldOf("position").forGetter(RingTemplate::position),
                Axis.CODEC.fieldOf("axis").forGetter(RingTemplate::axis),
                ArcadeExtraCodecs.enum<RingType>().fieldOf("color").forGetter(RingTemplate::type)
            ).apply(instance, ::RingTemplate)
        }
    }
}