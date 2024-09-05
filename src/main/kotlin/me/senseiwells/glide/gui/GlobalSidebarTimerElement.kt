package me.senseiwells.glide.gui

import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.gui.elements.UniversalElement
import net.casual.arcade.gui.sidebar.SidebarComponent
import net.casual.arcade.scheduler.MinecraftTimeDuration
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.TimeUtils.format
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class GlobalSidebarTimerElement(
    private val time: () -> MinecraftTimeDuration
): UniversalElement<SidebarComponent> {
    override fun get(server: MinecraftServer): SidebarComponent {
        val elapsed = this.time.invoke()
        val secondsHandIndex = (elapsed.seconds / 10) % 11
        val minutesHandIndex = (elapsed.minutes / 5) % 11
        val stopwatch = Component.empty()
            .append(GlideFont.Timer.TIMER)
            .append(ComponentUtils.space(-16))
            .append(GlideFont.Timer.getSecondsHand(secondsHandIndex))
            .append(ComponentUtils.space(-16))
            .append(GlideFont.Timer.getMinutesHand(minutesHandIndex))
        return SidebarComponent.withCustomScore(
            elapsed.format().literal(),
            Component.empty().append(stopwatch).append(ComponentUtils.space(5))
        )
    }
}