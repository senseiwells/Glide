package me.senseiwells.glide.gui

import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.pack.font.spacing.SpacingFontResources
import net.casual.arcade.utils.Identifier
import net.casual.arcade.utils.TimeUtils.format
import net.casual.arcade.utils.component.font
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.visuals.elements.UniversalElement
import net.casual.arcade.virtual.visuals.sidebar.SidebarComponent
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
            .append(SpacingFontResources.spaced(-16))
            .append(GlideFont.Timer.getSecondsHand(secondsHandIndex))
            .append(SpacingFontResources.spaced(-16))
            .append(GlideFont.Timer.getMinutesHand(minutesHandIndex))
        return SidebarComponent.withCustomScore(
            Component.literal(elapsed.format()).font(Identifier("glide", "big_numbers")),
            Component.empty().append(stopwatch).append(SpacingFontResources.spaced(5))
        )
    }
}