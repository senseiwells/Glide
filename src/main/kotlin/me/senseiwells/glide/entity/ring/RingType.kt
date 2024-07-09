package me.senseiwells.glide.entity.ring

import me.senseiwells.glide.font.GlideFont.Score
import me.senseiwells.glide.item.GlideItem
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

enum class RingType(
    val points: Int,
    val color: Int,
    val icon: Component,
    private val fade: (Int) -> Component,
    private val model: () -> ItemStack
) {
    Blue(7, 0x63CECA, Score.BLUE, Score::getBlueFade, GlideItem::BLUE_RING),
    Yellow(5, 0xDFBA25, Score.YELLOW, Score::getYellowFade, GlideItem::YELLOW_RING),
    Green(3, 0x49D671, Score.GREEN, Score::getGreenFade, GlideItem::GREEN_RING),
    Grey(0, 0x000000, Score.GREY, Score::getGreyFade, ItemStack::EMPTY);

    fun getSize(): Int {
        return this.points + 2
    }

    fun getIconFade(ticks: Int): Component {
        return this.fade.invoke(ticks)
    }

    fun getItemModel(): ItemStack {
        return this.model.invoke()
    }
}