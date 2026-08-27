package me.senseiwells.glide.gui

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import net.casual.arcade.virtual.visuals.elements.PlayerSpecificElement
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
import net.minecraft.server.level.ServerPlayer
import java.util.*

class PlayerSidebarTimerElement(
    private val global: GlobalSidebarTimerElement
): PlayerSpecificElement<SidebarComponent> {
    private var last = Object2ObjectOpenHashMap<UUID, SidebarComponent>()

    override fun get(player: ServerPlayer): SidebarComponent {
        val timer: SidebarComponent
        if (!player.glide.finished) {
            timer = this.global.get(player)
            this.last[player.uuid] = timer
        } else {
            timer = this.last.getOrDefault(player.uuid, SidebarComponent())
        }
        return timer
    }
}