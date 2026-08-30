package me.senseiwells.glide.minigame.lobby.vote

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.minigame.lobby.GlideLobbyComponents
import me.senseiwells.glide.minigame.lobby.GlideSelection
import net.casual.arcade.minigame.settings.GameSetting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import java.util.*

class GlideVote<T: Any>(
    val id: String,
    private val setting: GameSetting<GlideSelection<T>>,
    private val options: () -> List<T>,
    private val namer: (T) -> Component
) {
    private val votes = Object2ObjectOpenHashMap<UUID, T>()

    val enabled: Boolean
        get() = this.setting.get() == GlideSelection.Voting

    fun options(): List<T> {
        return this.options.invoke()
    }

    fun name(option: T): Component {
        return this.namer.invoke(option)
    }

    fun selectionName(): Component {
        return when (val selection = this.setting.get()) {
            is GlideSelection.Voting -> GlideLobbyComponents.VOTING
            is GlideSelection.Random -> GlideLobbyComponents.RANDOM
            is GlideSelection.Selected -> this.name(selection.value)
        }
    }

    fun cast(player: ServerPlayer, option: T): Boolean {
        if (!this.enabled || !this.options().contains(option)) {
            return false
        }
        this.votes[player.uuid] = option
        return true
    }

    fun voteOf(player: ServerPlayer): T? {
        return this.votes[player.uuid]
    }

    fun retract(player: ServerPlayer): Boolean {
        return this.votes.remove(player.uuid) != null
    }

    fun tally(): Map<T, Int> {
        val tally = LinkedHashMap<T, Int>()
        for (option in this.options()) {
            tally[option] = 0
        }
        for (vote in this.votes.values) {
            tally.computeIfPresent(vote) { _, count -> count + 1 }
        }
        return tally
    }

    fun clear() {
        this.votes.clear()
    }

    fun resolve(random: RandomSource): T? {
        val selection = this.setting.get()
        if (selection is GlideSelection.Selected) {
            return selection.value
        }
        if (selection is GlideSelection.Voting) {
            val winner = this.winner(random)
            if (winner != null) {
                return winner
            }
        }
        val options = this.options()
        if (options.isEmpty()) {
            return null
        }
        return options[random.nextInt(options.size)]
    }

    private fun winner(random: RandomSource): T? {
        val tally = this.tally()
        val highest = tally.values.maxOrNull() ?: 0
        if (highest == 0) {
            return null
        }
        val winners = tally.entries.filter { it.value == highest }
        return winners[random.nextInt(winners.size)].key
    }
}
