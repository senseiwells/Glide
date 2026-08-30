package me.senseiwells.glide.minigame.lobby.gui

import net.casual.arcade.guis.utils.ContainerType
import net.casual.arcade.utils.ItemUtils.hideTooltip
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.min

object GlideGuis {
    const val ROW = 9

    private val TYPES = listOf(
        ContainerType.Generic9x1,
        ContainerType.Generic9x2,
        ContainerType.Generic9x3,
        ContainerType.Generic9x4,
        ContainerType.Generic9x5,
        ContainerType.Generic9x6
    )

    fun filler(): ItemStack {
        return Items.STAINED_GLASS_PANE.gray.defaultInstance.hideTooltip()
    }

    fun type(rows: Int): ContainerType {
        return TYPES[rows.coerceIn(1, TYPES.size) - 1]
    }

    fun rows(items: Int): Int {
        return ((items + ROW - 1) / ROW).coerceAtLeast(1)
    }

    fun centered(items: Int, row: Int): List<Int> {
        val spacing = if (items <= 5) 2 else 1
        val start = (ROW - ((items - 1) * spacing + 1)) / 2
        return List(items) { index -> row * ROW + start + index * spacing }
    }

    fun slots(items: Int, rows: Int): List<Int> {
        val size = min(items, rows * ROW)
        val perRow = (size + rows - 1) / rows

        val slots = ArrayList<Int>(size)
        var remaining = size
        for (row in 0..< rows) {
            val count = min(perRow, remaining)
            slots.addAll(this.centered(count, row))
            remaining -= count
        }
        return slots
    }
}
