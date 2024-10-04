package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideMode
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.withShiftedDownFont
import net.casual.arcade.utils.TimeUtils.format
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

open class GlideEndBanner(private val mode: GlideMode): GlideBanner() {
    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        if (this.mode == GlideMode.TimeAttack) {
            return GlideFont.Banner.stopwatchShield(frame).shadowless()
        }
        return GlideFont.Banner.scoreShield(frame).shadowless()
    }

    override fun getBannerText(player: ServerPlayer, frame: Int): Component {
        val component = when {
            !player.glide.finished -> GlideComponents.DNF
            this.mode == GlideMode.ScoreAttack -> player.glide.score.toString().literal()
            else -> player.glide.finishedTime.format().literal()
        }
        return component.color(0x000000).withShiftedDownFont(4)
    }
}