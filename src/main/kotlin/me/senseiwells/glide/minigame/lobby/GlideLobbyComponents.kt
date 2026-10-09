package me.senseiwells.glide.minigame.lobby

import me.senseiwells.glide.minigame.GlideMode
import me.senseiwells.glide.module.GlideMetaData
import me.senseiwells.glide.module.GlideModules
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.string.SnakeCase
import net.casual.arcade.utils.string.TitleCase
import net.casual.arcade.utils.string.convertCasing
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object GlideLobbyComponents {
    val STARTING_IN = ComponentUtils.translatableWithArgs("glide.lobby.starting.in")

    val BY_VOTE = Component.translatable("glide.lobby.byVote")
    val RANDOM = Component.translatable("glide.lobby.random")

    fun map(map: Identifier): Component {
        val name = GlideModules.get(map)?.get(GlideMetaData.type)?.name
        if (name == null || name.string.isEmpty()) {
            return Component.literal(map.path.convertCasing(SnakeCase, TitleCase))
        }
        return name
    }

    fun mode(mode: GlideMode): Component {
        return Component.literal(mode.serializedName.convertCasing(SnakeCase, TitleCase))
    }

    fun rounds(rounds: Int): Component {
        return Component.translatable("glide.lobby.rounds", rounds)
    }
}
