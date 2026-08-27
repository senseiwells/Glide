package me.senseiwells.glide.gui.debug

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import net.casual.arcade.virtual.visuals.elements.PlayerSpecificElement
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object SidebarDebugGlideVelocityElement: PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        val velocity = player.glide.modifyVelocityWithLift(player.deltaMovement)
        return SidebarComponent.withNoScore(
            Component.literal("%.2f, %.2f, %.2f".format(velocity.x, velocity.y, velocity.z))
        )
    }
}