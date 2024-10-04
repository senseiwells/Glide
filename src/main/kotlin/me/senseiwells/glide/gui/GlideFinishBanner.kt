package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideMode
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.withShiftedDownFont
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

class GlideFinishBanner(mode: GlideMode): GlideEndBanner(mode) {
    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        if (!player.glide.finished) {
            return GlideFont.Banner.stopwatchShield(frame).shadowless()
        }
        return super.getBannerShield(player, frame)
    }

    override fun getBannerText(player: ServerPlayer, frame: Int): Component {
        val component = when {
            !player.glide.finished -> GlideComponents.HURRY_UP
            player.glide.wasPersonalBest -> GlideComponents.PERSONAL_BEST
            else -> return super.getBannerText(player, frame)
        }
        return component.color(0x000000).withShiftedDownFont(4)
    }
}