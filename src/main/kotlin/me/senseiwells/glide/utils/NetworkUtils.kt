package me.senseiwells.glide.utils

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.phys.Vec3

object ExtraEntityDataSerializers {
    val VEC3: EntityDataSerializer<Vec3> = EntityDataSerializer.forValueType(ExtraByteBufCodecs.VEC3)

    init {
        EntityDataSerializers.registerSerializer(VEC3)
    }
}

object ExtraByteBufCodecs {
    val VEC3: StreamCodec<ByteBuf, Vec3> = object: StreamCodec<ByteBuf, Vec3> {
        override fun decode(byteBuf: ByteBuf): Vec3 {
            return Vec3(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble())
        }

        override fun encode(byteBuf: ByteBuf, vec: Vec3) {
            byteBuf.writeDouble(vec.x)
            byteBuf.writeDouble(vec.y)
            byteBuf.writeDouble(vec.z)
        }
    }
}