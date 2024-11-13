package me.senseiwells.glide.minigame

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.addon.GlideAddons
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.exception.MinigameCreationException
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.codec.CodecProvider
import net.minecraft.resources.ResourceLocation

class GlideMinigameFactory(
    private val map: ResourceLocation,
    private val mode: GlideMode
): MinigameFactory {
    override fun codec(): MapCodec<out MinigameFactory> {
        return CODEC
    }

    override fun create(context: MinigameCreationContext): Minigame {
        val addon = GlideAddons.get(this.map) ?: throw MinigameCreationException("No such map ${this.map} available")
        return GlideMinigame.create(context.server, context.uuid, addon, this.mode)
    }

    companion object: CodecProvider<GlideMinigameFactory> {
        private val DEFAULT_MAP = GlideMod.id("temple")

        override val ID: ResourceLocation = GlideMinigame.ID

        override val CODEC: MapCodec<out GlideMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ResourceLocation.CODEC.optionalFieldOf("map", DEFAULT_MAP).forGetter(GlideMinigameFactory::map),
                GlideMode.CODEC.optionalFieldOf("mode", GlideMode.TimeAttack).forGetter(GlideMinigameFactory::mode)
            ).apply(instance, ::GlideMinigameFactory)
        }
    }
}