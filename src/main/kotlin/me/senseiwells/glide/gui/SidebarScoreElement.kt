package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import net.casual.arcade.gui.elements.PlayerSpecificElement
import net.casual.arcade.gui.sidebar.SidebarComponent
import net.casual.arcade.utils.ComponentUtils
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object SidebarScoreElement: PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        return SidebarComponent.withCustomScore(
            Component.literal(player.glide.score.toString()),
            Component.empty().append(player.glide.scoreIcon).append(ComponentUtils.space(6))
        )
    }
}