package me.senseiwells.glide.addon.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.checkpoint.*
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.utils.BoundingBoxUtils.getSizeVec
import net.casual.arcade.utils.location.template.ExactLocationTemplate
import net.casual.arcade.utils.serialization.ArcadeExtraCodecs
import net.casual.arcade.utils.serialization.CodecProvider
import net.casual.arcade.utils.serialization.CodecProvider.Companion.register
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.AABB
import java.util.function.Function

interface CheckpointTemplate {
    fun create(level: ServerLevel): Checkpoint

    fun createBeaconBeams(level: ServerLevel): List<BeaconBeam> {
        return listOf()
    }

    fun codec(): MapCodec<out CheckpointTemplate>

    companion object {
        val CODEC: Codec<CheckpointTemplate> by lazy {
            GlideRegistries.CHECKPOINT_TEMPLATE.byNameCodec()
                .dispatch(CheckpointTemplate::codec, Function.identity())
        }

        internal fun bootstrap(registry: Registry<MapCodec<out CheckpointTemplate>>) {
            ProgressCheckpointTemplate.register(registry)
            FinishLineTemplate.register(registry)
            RespawnCheckpointTemplate.register(registry)
        }
    }
}

class ProgressCheckpointTemplate(
    private val index: Int,
    private val boundingBox: AABB
): CheckpointTemplate {
    override fun create(level: ServerLevel): Checkpoint {
        val checkpoint = ProgressCheckpoint(level, this.boundingBox.getSizeVec())
        checkpoint.setPos(this.boundingBox.center)
        checkpoint.index = this.index
        return checkpoint
    }

    override fun codec(): MapCodec<out CheckpointTemplate> {
        return CODEC
    }

    companion object: CodecProvider<ProgressCheckpointTemplate> {
        override val ID: ResourceLocation = GlideMod.id("progress")

        override val CODEC: MapCodec<ProgressCheckpointTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(ProgressCheckpointTemplate::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(ProgressCheckpointTemplate::boundingBox)
            ).apply(instance, ::ProgressCheckpointTemplate)
        }
    }
}

class FinishLineTemplate(
    private val index: Int,
    private val boundingBox: AABB
): CheckpointTemplate {
    override fun create(level: ServerLevel): Checkpoint {
        val finish = FinishLine(level, this.boundingBox.getSizeVec())
        finish.setPos(this.boundingBox.center)
        finish.index = this.index
        return finish
    }

    override fun codec(): MapCodec<out CheckpointTemplate> {
        return CODEC
    }

    companion object: CodecProvider<FinishLineTemplate> {
        override val ID: ResourceLocation = GlideMod.id("finish")

        override val CODEC: MapCodec<FinishLineTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(FinishLineTemplate::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(FinishLineTemplate::boundingBox)
            ).apply(instance, ::FinishLineTemplate)
        }
    }
}

class BeaconBeamTemplate(
    val length: Float,
    val location: ExactLocationTemplate
) {
    fun create(level: ServerLevel): BeaconBeam {
        val beam = BeaconBeam(level, this.length)
        beam.setPos(this.location.position)
        beam.xRot = this.location.rotation.x
        beam.yRot = this.location.rotation.y
        return beam
    }

    companion object {
        val CODEC: Codec<BeaconBeamTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.FLOAT.fieldOf("length").forGetter(BeaconBeamTemplate::length),
                ExactLocationTemplate.CODEC.fieldOf("location").forGetter(BeaconBeamTemplate::location)
            ).apply(instance, ::BeaconBeamTemplate)
        }
    }
}

class RespawnCheckpointTemplate(
    private val index: Int,
    private val boundingBox: AABB,
    private val spawn: ExactLocationTemplate,
    private val beacons: List<BeaconBeamTemplate>
): CheckpointTemplate {
    override fun create(level: ServerLevel): Checkpoint {
        val checkpoint = RespawnCheckpoint(level, this.boundingBox.getSizeVec())
        checkpoint.setPos(this.boundingBox.center)
        checkpoint.respawnRotation = this.spawn.rotation
        checkpoint.respawnOffset = this.spawn.position.subtract(checkpoint.position())
        checkpoint.index = this.index
        return checkpoint
    }

    override fun createBeaconBeams(level: ServerLevel): List<BeaconBeam> {
        return this.beacons.map { it.create(level) }
    }

    override fun codec(): MapCodec<out CheckpointTemplate> {
        return CODEC
    }

    companion object: CodecProvider<RespawnCheckpointTemplate> {
        override val ID: ResourceLocation = GlideMod.id("respawn")

        override val CODEC: MapCodec<RespawnCheckpointTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(RespawnCheckpointTemplate::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(RespawnCheckpointTemplate::boundingBox),
                ExactLocationTemplate.CODEC.fieldOf("respawn_location").forGetter(RespawnCheckpointTemplate::spawn),
                BeaconBeamTemplate.CODEC.listOf().fieldOf("beacon_beams").forGetter(RespawnCheckpointTemplate::beacons)
            ).apply(instance, ::RespawnCheckpointTemplate)
        }
    }
}