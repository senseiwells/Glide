package me.senseiwells.glide.minigame

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer

sealed interface GlideRound {
    fun getStartBanner(): MutableComponent

    fun mayPlayerRestart(player: ServerPlayer): Boolean

    fun onRoundFinished(glide: GlideMinigame)
}

class SoloGlideRound: GlideRound {
    override fun getStartBanner(): MutableComponent {
        return GlideComponents.SOLO_ROUND
    }

    override fun mayPlayerRestart(player: ServerPlayer): Boolean {
        return !player.glide.finished && !player.isSpectator
    }

    override fun onRoundFinished(glide: GlideMinigame) {

    }
}

class MultiGlideRound(private val rounds: Int): GlideRound {
    private var round = 1

    override fun getStartBanner(): MutableComponent {
        return GlideComponents.NUMBERED_ROUND.generate(this.round)
    }

    override fun mayPlayerRestart(player: ServerPlayer): Boolean {
        return false
    }

    override fun onRoundFinished(glide: GlideMinigame) {
        this.round++
    }
}