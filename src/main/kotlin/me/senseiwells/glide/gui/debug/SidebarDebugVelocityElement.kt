package me.senseiwells.glide.gui.debug

import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object SidebarDebugVelocityElement: PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        val velocity = player.deltaMovement
        return SidebarComponent.withNoScore(
            Component.literal("%.2f, %.2f, %.2f".format(velocity.x, velocity.y, velocity.z))
        )
    }
}