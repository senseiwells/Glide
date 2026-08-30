package me.senseiwells.glide.minigame.lobby.gui

import me.senseiwells.glide.minigame.lobby.GlideLobbyComponents
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.visuals.bossbar.VirtualBossbar
import net.casual.arcade.virtual.visuals.transition.Transition
import net.minecraft.server.level.ServerPlayer

class GlideLobbyCountdown(private val lobby: GlideLobbyMinigame): Transition {
    private val bossbar = VirtualBossbar()

    fun stop() {
        this.lobby.visuals.removeBossbar(this.bossbar)
    }

    override fun beforeTransition(players: Collection<ServerPlayer>, interval: MinecraftTimeDuration, updates: Int) {
        this.lobby.visuals.addBossbar(this.bossbar)
    }

    override fun updateTransition(
        players: Collection<ServerPlayer>,
        current: Int,
        total: Int,
        remaining: MinecraftTimeDuration
    ) {
        this.bossbar.title.set(GlideLobbyComponents.STARTING_IN.generate(total - current))
    }

    override fun afterTransition(players: Collection<ServerPlayer>) {
        this.stop()
    }
}
