package me.senseiwells.glide.font

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedBitmapFontResources
import net.casual.arcade.utils.ComponentUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    val YOU_CRASHED by bitmap(at("you_crashed.png"), 3, 3)
    val YOU_CRASHED_BACKGROUND by bitmap(at("you_crashed_background_2.png"), 500, 1000)

    object Progressbar: IndexedBitmapFontResources(GlideMod.id("progressbar")) {
        private const val SHIFT = -20

        private val BG_1 by bitmap(at("progressbar/progressbar_1.png"), 5 + SHIFT, 10)
        private val BG_2 by bitmap(at("progressbar/progressbar_2.png"), 5 + SHIFT, 10)
        val CHECKPOINT by bitmap(at("progressbar/checkpoint_icon.png"), 7 + SHIFT, 14)

        val BACKGROUND: MutableComponent
            get() = Component.empty().append(BG_1).append(ComponentUtils.space(-1)).append(BG_2)

        init {
            for (i in 1..16) {
                indexed(at("progressbar/player_marker/$i.png"), 3 + SHIFT, 5)
            }
        }

        fun getPlayerMarker(index: Int): MutableComponent {
            require(index in 0..15) { "Index must be between 0 and 15" }
            return this.get(index)
        }
    }

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