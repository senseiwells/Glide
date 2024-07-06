package me.senseiwells.glide.font

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedBitmapFontResources
import net.minecraft.network.chat.MutableComponent

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    val YOU_CRASHED by bitmap(at("you_crashed.png"), 3, 3)
    val YOU_CRASHED_BACKGROUND by bitmap(at("you_crashed_background_2.png"), 500, 1000)

    object Speedometer: IndexedBitmapFontResources(GlideMod.id("speedometer")) {
        val SPEEDOMETER by bitmap(at("speedometer/speedometer.png"), 8, 16)

        init {
            for (i in 0..11) {
                indexed(at("speedometer/needle_$i.png"), 8, 16)
            }
        }

        fun getNeedle(index: Int): MutableComponent {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index)
        }
    }

    object Timer: IndexedBitmapFontResources(GlideMod.id("timer")) {
        val TIMER by bitmap(at("timer/clock.png"), 8, 16)

        init {
            for (i in 0..11) {
                indexed(at("timer/seconds_$i.png"), 8, 16)
            }
            for (i in 0..11) {
                indexed(at("timer/minutes_$i.png"), 8, 16)
            }
        }

        fun getSecondsHand(index: Int): MutableComponent {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index)
        }

        fun getMinutesHand(index: Int): MutableComponent {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index + 12)
        }
    }
}