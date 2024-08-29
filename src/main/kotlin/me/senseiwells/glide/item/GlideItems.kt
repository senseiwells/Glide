package me.senseiwells.glide.item

import me.senseiwells.glide.GlideMod.id
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object GlideItems {
    val GLIDE_ITEM: Item = Items.registerItem(id("glide_item"), GlideItem.MODELLER.item())
    val GLIDE_TRAIL_ITEM: Item = Items.registerItem(id("glide_trail_item"), GlideTrailItem.MODELLER.item())

    fun noop() {

    }
}