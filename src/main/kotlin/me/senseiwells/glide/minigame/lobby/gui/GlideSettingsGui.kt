package me.senseiwells.glide.minigame.lobby.gui

import me.senseiwells.glide.minigame.lobby.GlideLobbySettings
import net.casual.arcade.guis.core.container.ContainerGui
import net.casual.arcade.guis.utils.SlotClickAction
import net.casual.arcade.utils.ItemUtils.named
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items

class GlideSettingsGui(
    player: ServerPlayer,
    private val settings: GlideLobbySettings
): ContainerGui(player, GlideGuis.type(GlideGuis.rows(settings.displays().size) + 1), false) {
    private val displays = this.settings.displays()
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
            this.setSlot(slot, setting.displayWithLore()) { action ->
                if (action.isMouse && action != SlotClickAction.MouseDoubleClick) {
                    setting.cycle(if (action.isRight) -1 else 1)
                    this.load()
                }
            }
        }
        for (slot in footer..< this.getContainerSize()) {
            this.setSlot(slot, GlideGuis.filler())
        }
        this.setSlot(footer + GlideGuis.ROW / 2, Items.BARRIER.named(Component.translatable("glide.lobby.gui.close"))) {
            this.openParentOrClose()
        }
    }
}
