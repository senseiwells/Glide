package me.senseiwells.glide.minigame.lobby

import com.mojang.serialization.MapCodec
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.serialization.codec.CodecProvider
import net.minecraft.resources.Identifier

object GlideLobbyMinigameFactory: MinigameFactory, CodecProvider<GlideLobbyMinigameFactory> {
    override val id: Identifier = GlideLobbyMinigame.ID

    override val codec: MapCodec<GlideLobbyMinigameFactory> = MapCodec.unit(this)

    override fun create(context: MinigameCreationContext): Minigame {
        return GlideLobbyMinigame.create(context.server, context.uuid)
    }

    override fun codec(): MapCodec<out MinigameFactory> {
        return this.codec
    }
}
