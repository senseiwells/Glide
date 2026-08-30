package me.senseiwells.glide.minigame.lobby.vote

import me.senseiwells.glide.minigame.GlideMode
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyComponents
import me.senseiwells.glide.minigame.lobby.GlideConfiguration
import me.senseiwells.glide.minigame.lobby.GlideLobbySettings
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import net.minecraft.util.RandomSource

class GlideLobbyVotes(lobby: GlideLobbyMinigame) {
    val map: GlideVote<Identifier> = GlideVote(
        "map",
        Items.FILLED_MAP,
        lobby.settings.map,
        lobby::availableMaps,
        GlideLobbyComponents::map
    )
    val mode: GlideVote<GlideMode> = GlideVote(
        "mode",
        Items.CLOCK,
        lobby.settings.mode,
        { GlideMode.entries },
        GlideLobbyComponents::mode
    )
    val rounds: GlideVote<Int> = GlideVote(
        "rounds",
        Items.PAPER,
        lobby.settings.rounds,
        { GlideLobbySettings.ROUND_OPTIONS },
        GlideLobbyComponents::rounds
    )

    fun all(): List<GlideVote<*>> {
        return listOf(this.map, this.mode, this.rounds)
    }

    fun any(): Boolean {
        return this.all().any(GlideVote<*>::enabled)
    }

    fun clear() {
        for (vote in this.all()) {
            vote.clear()
        }
    }

    fun retract(player: ServerPlayer) {
        for (vote in this.all()) {
            vote.retract(player)
        }
    }

    fun resolve(random: RandomSource): GlideConfiguration? {
        val map = this.map.resolve(random) ?: return null
        val mode = this.mode.resolve(random) ?: return null
        val rounds = this.rounds.resolve(random) ?: return null
        return GlideConfiguration(map, mode, rounds)
    }
}
