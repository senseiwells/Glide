package me.senseiwells.glide.minigame.lobby.gui

import me.senseiwells.glide.minigame.lobby.GlideLobbySettings
import net.casual.arcade.guis.core.container.ContainerGui
import net.casual.arcade.minigame.utils.addSettingDisplay
import net.casual.arcade.utils.ItemUtils.named
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items

class GlideSettingsGui(
    player: ServerPlayer,
    private val settings: GlideLobbySettings
): ContainerGui(player, GlideGuis.type(GlideGuis.rows(enabled.size) + 1), false) {
    private val displays = enabled.map { property -> property.get(this.settings) }
    private val positions = GlideGuis.slots(this.displays.size, GlideGuis.rows(this.displays.size))

    init {
        this.setTitle(Component.translatable("glide.lobby.settings.glide"))
    }

    override fun onOpen() {
        this.load()
    }

    private fun load() {
        val footer = this.getContainerSize() - GlideGuis.ROW
        for ((index, slot) in this.positions.withIndex()) {
            val setting = this.displays.getOrNull(index)
            if (setting == null) {
                this.clearSlot(slot)
                continue
            }
            this.addSettingDisplay(slot, setting)
        }
        for (slot in footer..< this.getContainerSize()) {
            this.setSlot(slot, GlideGuis.filler())
        }
        this.setSlot(footer + GlideGuis.ROW / 2, Items.BARRIER.named(Component.translatable("glide.lobby.gui.close"))) {
            this.openParentOrClose()
        }
    }

    companion object {
        private val enabled = listOf(GlideLobbySettings::map, GlideLobbySettings::mode, GlideLobbySettings::rounds, GlideLobbySettings::startDelay)
    }
}
