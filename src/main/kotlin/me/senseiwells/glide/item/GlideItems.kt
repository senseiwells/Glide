package me.senseiwells.glide.item

import me.senseiwells.glide.GlideMod.id
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object GlideItems {
    val GLIDE_ITEM: Item = Items.registerItem(id("glide_item"), GlideItem.MODELLER.item())

    fun noop() {

    }
}