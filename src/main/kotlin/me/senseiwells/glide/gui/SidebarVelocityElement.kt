package me.senseiwells.glide.gui

import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth

object SidebarVelocityElement: PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        val mps = player.deltaMovement.length() * 20
        // See https://www.desmos.com/calculator/6bhuudkjpm
        val needleIndex = Mth.clamp(Mth.floor(0.24 * mps - 0.5), 0, 11)
        val speedometer = Component.empty()
            .append(GlideFont.Speedometer.SPEEDOMETER)
            .append(SpacingFontResources.spaced(-17))
            .append(GlideFont.Speedometer.getNeedle(needleIndex))
        return SidebarComponent.withCustomScore(
            Component.literal("%.2f m/s".format(mps)),
            Component.empty().append(speedometer).append(SpacingFontResources.spaced(4))
        )
    }
}