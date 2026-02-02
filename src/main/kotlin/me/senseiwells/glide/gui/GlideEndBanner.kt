package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.resources.utils.withShiftedDownFont
import net.casual.arcade.utils.component.color
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

open class GlideEndBanner(
    private val glide: GlideMinigame
): GlideBanner() {
    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        return this.glide.mode.getBannerShield(frame).wrap().shadowless()
    }

    override fun getBannerText(player: ServerPlayer): Component {
        val component = when {
            this.glide.round.shouldDisplayWinner() -> GlideComponents.PLAYER_WON.generate(this.glide.getWinner())
            !player.glide.finished -> GlideComponents.DNF
            else -> this.glide.mode.getBannerScore(player)
        }
        return component.color(0x444444).shadowless().withShiftedDownFont(29)
    }

    override fun updateTransition(
        players: Collection<ServerPlayer>,
        current: Int,
        total: Int,
        remaining: MinecraftTimeDuration
    ) {
        this.setTitle(GlideComponents.ENDING_IN.generate(current).withShiftedDownFont(12))
    }
}