package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.resources.utils.withShiftedDownFont
import net.casual.arcade.utils.component.color
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

class GlideFinishBanner(glide: GlideMinigame): GlideEndBanner(glide) {
    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        if (!player.glide.finished) {
            return GlideFont.Banner.stopwatchShield(frame).wrap().shadowless()
        }
        return super.getBannerShield(player, frame)
    }

    override fun getBannerText(player: ServerPlayer,): Component {
        val component = when {
            !player.glide.finished -> GlideComponents.HURRY_UP
            player.glide.wasPersonalBest -> GlideComponents.PERSONAL_BEST
            else -> return super.getBannerText(player)
        }
        return component.color(0x444444).shadowless().withShiftedDownFont(29)
    }

    override fun updateTransition(
        players: Collection<ServerPlayer>,
        current: Int,
        total: Int,
        remaining: MinecraftTimeDuration
    ) {
        this.setTitle(GlideComponents.REACH_THE_END.generate(total - current).withShiftedDownFont(12))
    }
}