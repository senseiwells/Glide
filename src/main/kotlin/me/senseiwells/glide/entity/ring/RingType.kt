package me.senseiwells.glide.entity.ring

import me.senseiwells.glide.item.GlideItem
import net.minecraft.world.item.ItemStack

enum class RingType(val points: Int, private val model: () -> ItemStack) {
    Blue(7, GlideItem::BLUE_RING),
    Yellow(5, GlideItem::YELLOW_RING),
    Green(3, GlideItem::GREEN_RING);

    fun getItemModel(): ItemStack {
        return this.model.invoke()
    }
}