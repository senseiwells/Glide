package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import net.casual.arcade.pack.font.spacing.SpacingFontResources
import net.casual.arcade.virtual.visuals.elements.PlayerSpecificElement
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object SidebarScoreElement: PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        return SidebarComponent.withCustomScore(
            Component.literal(player.glide.score.toString()),
            Component.empty().append(player.glide.scoreIcon).append(SpacingFontResources.spaced(6))
        )
    }
}