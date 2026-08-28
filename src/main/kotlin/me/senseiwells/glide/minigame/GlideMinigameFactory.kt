package me.senseiwells.glide.minigame

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.module.GlideModules
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.exception.MinigameCreationException
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.serialization.codec.CodecProvider
import net.minecraft.resources.Identifier

class GlideMinigameFactory(
    private val map: Identifier,
    private val round: GlideRoundMode.Provider,
    private val mode: GlideMode
): MinigameFactory {
    override fun codec(): MapCodec<out MinigameFactory> {
        return codec
    }

    override fun create(context: MinigameCreationContext): Minigame {
        val addon = GlideModules.get(this.map) ?: throw MinigameCreationException("No such map ${this.map} available")
        return GlideMinigame.create(context.server, context.uuid, addon, this.round.create(), this.mode)
    }

    companion object: CodecProvider<GlideMinigameFactory> {
        private val DEFAULT_MAP = GlideMod.id("temple")

        override val id = GlideMinigame.ID

        override val codec: MapCodec<out GlideMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Identifier.CODEC.optionalFieldOf("map", DEFAULT_MAP).forGetter(GlideMinigameFactory::map),
                GlideRoundMode.Provider.CODEC.optionalFieldOf("round", SoloGlideRoundMode).forGetter(GlideMinigameFactory::round),
                GlideMode.CODEC.optionalFieldOf("mode", GlideMode.TimeAttack).forGetter(GlideMinigameFactory::mode)
            ).apply(instance, ::GlideMinigameFactory)
        }
    }
}