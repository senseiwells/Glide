package me.senseiwells.glide.checkpoint

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.checkpoint.BeaconBeam
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.minigame.template.location.ExactLocationTemplate
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.codec.ArcadeExtraCodecs
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.codec.CodecProvider.Companion.register
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Function

interface Checkpoint: GlideCollider {
    val index: Int
    val boundingBox: AABB

    val shouldMarkOnProgressBar: Boolean

    val position: Vec3
        get() = this.boundingBox.center


    fun codec(): MapCodec<out Checkpoint>

    fun createBeaconBeams(level: ServerLevel): List<BeaconBeam> {
        return listOf()
    }

    override fun intersects(box: AABB): Boolean {
        return this.boundingBox.intersects(box)
    }

    companion object {
        val CODEC: Codec<Checkpoint> by lazy {
            GlideRegistries.CHECKPOINT_TEMPLATE.byNameCodec()
                .dispatch(Checkpoint::codec, Function.identity())
        }

        internal fun bootstrap(registry: Registry<MapCodec<out Checkpoint>>) {
            ProgressCheckpoint.register(registry)
            FinishLine.register(registry)
            RespawnCheckpoint.register(registry)
        }
    }
}

class ProgressCheckpoint(
    override val index: Int,
    override val boundingBox: AABB
): Checkpoint {
    override val shouldMarkOnProgressBar: Boolean = false

    override fun codec(): MapCodec<out Checkpoint> {
        return CODEC
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        player.glide.collideWithCheckpoint(this)
    }

    companion object: CodecProvider<ProgressCheckpoint> {
        override val ID: ResourceLocation = GlideMod.id("progress")

        override val CODEC: MapCodec<ProgressCheckpoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(ProgressCheckpoint::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(ProgressCheckpoint::boundingBox)
            ).apply(instance, ::ProgressCheckpoint)
        }
    }
}

class FinishLine(
    override val index: Int,
    override val boundingBox: AABB
): Checkpoint {
    override val shouldMarkOnProgressBar: Boolean = false

    override fun codec(): MapCodec<out Checkpoint> {
        return CODEC
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        if (player.glide.collideWithCheckpoint(this)) {
            player.glide.collideWithFinishLine(this)
            glide.markPlayerFinished(player)
        }
    }

    companion object: CodecProvider<FinishLine> {
        override val ID: ResourceLocation = GlideMod.id("finish")

        override val CODEC: MapCodec<FinishLine> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(FinishLine::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(FinishLine::boundingBox)
            ).apply(instance, ::FinishLine)
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

class RespawnCheckpoint(
    override val index: Int,
    override val boundingBox: AABB,
    private val spawn: ExactLocationTemplate,
    private val beacons: List<BeaconBeamTemplate>
): Checkpoint {
    override val shouldMarkOnProgressBar: Boolean = true

    override fun createBeaconBeams(level: ServerLevel): List<BeaconBeam> {
        return this.beacons.map { it.create(level) }
    }

    override fun codec(): MapCodec<out Checkpoint> {
        return CODEC
    }

    override fun collide(player: ServerPlayer, glide: GlideMinigame) {
        if (player.glide.collideWithCheckpoint(this)) {
            player.sendSound(GlideSounds.CHECKPOINT, static = true)
            glide.displayCheckpointNotificationTo(player)
            player.glide.collideWithRespawnCheckpoint(this, this.spawn.get(glide.level))
        }
    }

    companion object: CodecProvider<RespawnCheckpoint> {
        override val ID: ResourceLocation = GlideMod.id("respawn")

        override val CODEC: MapCodec<RespawnCheckpoint> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(RespawnCheckpoint::index),
                ArcadeExtraCodecs.AABB.fieldOf("bounding_box").forGetter(RespawnCheckpoint::boundingBox),
                ExactLocationTemplate.CODEC.fieldOf("respawn_location").forGetter(RespawnCheckpoint::spawn),
                BeaconBeamTemplate.CODEC.listOf().fieldOf("beacon_beams").forGetter(RespawnCheckpoint::beacons)
            ).apply(instance, ::RespawnCheckpoint)
        }
    }
}