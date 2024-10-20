package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.minigame.GlideMode
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.withShiftedDownFont
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

open class GlideEndBanner(private val mode: GlideMode): GlideBanner() {
    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        return this.mode.getBannerShield(frame).shadowless()
    }

    override fun getBannerText(player: ServerPlayer): Component {
        val component = when {
            !player.glide.finished -> GlideComponents.DNF
            else -> this.mode.getBannerScore(player)
        }
        return component.color(0x000000).withShiftedDownFont(29)
    }

    override fun sendCountdown(players: Collection<ServerPlayer>, current: Int, remaining: MinecraftTimeDuration) {
        this.setTitle(GlideComponents.ENDING_IN.generate(current).withShiftedDownFont(12))
    }
}