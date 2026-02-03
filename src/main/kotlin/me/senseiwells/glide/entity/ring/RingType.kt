package me.senseiwells.glide.entity.ring

import com.mojang.serialization.Codec
import me.senseiwells.glide.font.GlideFont.Score
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.utils.convertCasing
import net.casual.arcade.utils.string.PascalCase
import net.casual.arcade.utils.string.SnakeCase
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.ItemStack

enum class RingType(
    val points: Int,
    val color: Int,
    val icon: Component
): StringRepresentable {
    Blue(7, 0x63CECA, Score.BLUE) {
        override fun getModel(): ItemStack {
            return GlideItems.BLUE_RING
        }

        override fun getFade(delta: Int): Component {
            return Score.getBlueFade(delta)
        }

        override fun getSound(combo: Int): SoundEvent {
            return GlideSounds.RING_BLUE
        }
    },
    Yellow(5, 0xDFBA25, Score.YELLOW) {
        override fun getModel(): ItemStack {
            return GlideItems.YELLOW_RING
        }

        override fun getFade(delta: Int): Component {
            return Score.getYellowFade(delta)
        }

        override fun getSound(combo: Int): SoundEvent {
            return if (combo % 2 == 0) GlideSounds.RING_YELLOW_1 else GlideSounds.RING_YELLOW_2
        }
    },
    Green(3, 0x49D671, Score.GREEN) {
        override fun getModel(): ItemStack {
            return GlideItems.GREEN_RING
        }

        override fun getFade(delta: Int): Component {
            return Score.getGreenFade(delta)
        }

        override fun getSound(combo: Int): SoundEvent {
            return if (combo % 2 == 0) GlideSounds.RING_GREEN_1 else GlideSounds.RING_GREEN_2
        }
    },
    Grey(0, 0x000000, Score.GREY) {
        override fun getModel(): ItemStack {
            return ItemStack.EMPTY
        }

        override fun getFade(delta: Int): Component {
            return Score.getGreyFade(delta)
        }

        override fun getSound(combo: Int): SoundEvent {
            return SoundEvents.EMPTY
        }
    };

    abstract fun getModel(): ItemStack

    abstract fun getFade(delta: Int): Component

    abstract fun getSound(combo: Int): SoundEvent

    fun getSize(): Int {
        return 12 - this.points
    }

    override fun getSerializedName(): String {
        return this.name.convertCasing(PascalCase, SnakeCase)
    }

    companion object {
        val CODEC: Codec<RingType> = StringRepresentable.fromEnum(RingType::values)
    }
}