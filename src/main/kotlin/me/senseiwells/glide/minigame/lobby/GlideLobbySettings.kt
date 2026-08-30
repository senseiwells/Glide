package me.senseiwells.glide.minigame.lobby

import com.mojang.serialization.Codec
import me.senseiwells.glide.minigame.GlideMode
import net.casual.arcade.minigame.settings.GameSetting
import net.casual.arcade.minigame.settings.MinigameSettings
import net.casual.arcade.minigame.settings.display.MenuGameSetting
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder.Companion.time
import net.casual.arcade.minigame.utils.item
import net.casual.arcade.utils.ItemUtils.styledLore
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items

class GlideLobbySettings(
    minigame: GlideLobbyMinigame,
    maps: Collection<Identifier>
): MinigameSettings(minigame, Component.translatable("glide.lobby.settings")) {
    val map: GameSetting<GlideSelection<Identifier>> = this.register(
        selection(Identifier.CODEC, maps, GlideLobbyComponents::map) {
            name = "map"
            display = item(Items.FILLED_MAP, Component.translatable("glide.lobby.settings.map.name"))
                .styledLore(Component.translatable("glide.lobby.settings.map.desc"))
            value = GlideSelection.Random
        }
    )

    val mode: GameSetting<GlideSelection<GlideMode>> = this.register(
        selection(GlideMode.CODEC, GlideMode.entries, GlideLobbyComponents::mode, GlideMode::getSerializedName) {
            name = "mode"
            display = item(Items.CLOCK, Component.translatable("glide.lobby.settings.mode.name"))
                .styledLore(Component.translatable("glide.lobby.settings.mode.desc"))
            value = GlideSelection.Selected(GlideMode.TimeAttack)
        }
    )

    val rounds: GameSetting<GlideSelection<Int>> = this.register(
        selection(Codec.INT, ROUND_OPTIONS, GlideLobbyComponents::rounds) {
            name = "rounds"
            display = item(Items.PAPER, Component.translatable("glide.lobby.settings.rounds.name"))
                .styledLore(Component.translatable("glide.lobby.settings.rounds.desc"))
            value = GlideSelection.Selected(3)
        }
    )

    val votingDuration: GameSetting<MinecraftTimeDuration> = this.register(time {
        name = "voting_duration"
        display = item(Items.BELL, Component.translatable("glide.lobby.settings.votingDuration.name"))
            .styledLore(Component.translatable("glide.lobby.settings.votingDuration.desc"))
        value = 30.Seconds
        for (seconds in intArrayOf(15, 30, 60, 120)) {
            option("${seconds}s", Component.literal("${seconds}s"), seconds.Seconds)
        }
    })

    val countdownDuration: GameSetting<MinecraftTimeDuration> = this.register(time {
        name = "countdown_duration"
        display = item(Items.REPEATER, Component.translatable("glide.lobby.settings.countdownDuration.name"))
            .styledLore(Component.translatable("glide.lobby.settings.countdownDuration.desc"))
        value = 10.Seconds
        for (seconds in intArrayOf(5, 10, 20, 30)) {
            option("${seconds}s", Component.literal("${seconds}s"), seconds.Seconds)
        }
    })

    companion object {
        val ROUND_OPTIONS: List<Int> = listOf(1, 3, 5, 7, 10)

        private fun <T: Any> selection(
            codec: Codec<T>,
            options: Iterable<T>,
            namer: (T) -> Component,
            id: (T) -> String = Any::toString,
            block: MenuGameSettingBuilder<GlideSelection<T>>.() -> Unit
        ): MenuGameSetting<GlideSelection<T>> {
            val selection = GlideSelection.codec(codec)
            val builder = MenuGameSettingBuilder { name, value, values ->
                GameSetting(name, value, values, selection)
            }
            builder.apply(block)
            builder.option("by_vote", GlideLobbyComponents.BY_VOTE, GlideSelection.ByVote)
            builder.option("random", GlideLobbyComponents.RANDOM, GlideSelection.Random)
            for (option in options) {
                builder.option(id.invoke(option), namer.invoke(option), GlideSelection.Selected(option))
            }
            return builder.build()
        }
    }
}
