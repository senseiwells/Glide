package me.senseiwells.glide.font

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedBitmapFontResources
import net.casual.arcade.utils.ComponentUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    private const val SHIFT = -35

    val YOU_CRASHED by translatable("glide.you_crashed") {
        bitmap("en_us", at("crash/you_crashed_en_us.png"), 3, 3)
    }
    val YOU_CRASHED_BACKGROUND by bitmap(at("crash/you_crashed_background_2.png"), 500, 1000)

    val FULL_HEART by bitmap(at("heart/full.png"), 18 + SHIFT, 9)
    val HALF_HEART by bitmap(at("heart/half.png"), 18 + SHIFT, 9)
    val EMPTY_HEART by bitmap(at("heart/empty.png"), 18 + SHIFT, 9)

    object Progressbar: IndexedBitmapFontResources(GlideMod.id("progressbar")) {
        private val BG_1 by bitmap(at("progressbar/progressbar_0.png"), 5 + SHIFT, 10)
        private val BG_2 by bitmap(at("progressbar/progressbar_1.png"), 5 + SHIFT, 10)

        val BACKGROUND: MutableComponent
            get() = Component.empty().append(BG_1).append(ComponentUtils.space(-1)).append(BG_2)

        val CURRENT by bitmap(at("progressbar/player_marker/current.png"), 4 + SHIFT, 7)

        init {
            for (i in 1..16) {
                indexed(at("progressbar/player_marker/$i.png"), 3 + SHIFT, 5)
            }
            for (i in 0..5) {
                indexed(at("progressbar/checkpoint_$i.png"), 12 + SHIFT, 24)
            }
        }

        fun getPlayerMarker(index: Int): MutableComponent {
            require(index in 0..15) { "Index must be between 0 and 15" }
            return this.get(index)
        }

        fun getCheckpoint(index: Int): MutableComponent {
            require(index in 0..5) { "Index must be between 0 and 5" }
            return this.get(index + 16)
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

    object Score: IndexedBitmapFontResources(GlideMod.id("score")) {
        val GREY by bitmap(at("score/grey.png"), 8, 16)
        val GREEN by bitmap(at("score/green.png"), 8, 16)
        val YELLOW by bitmap(at("score/yellow.png"), 8, 16)
        val BLUE by bitmap(at("score/blue.png"), 8, 16)

        init {
            for (i in 0..20) {
                indexed(at("score/grey_fade_$i.png"), 8, 16)
            }
            for (i in 0..20) {
                indexed(at("score/green_fade_$i.png"), 8, 16)
            }
            for (i in 0..20) {
                indexed(at("score/yellow_fade_$i.png"), 8, 16)
            }
            for (i in 0..20) {
                indexed(at("score/blue_fade_$i.png"), 8, 16)
            }
        }

        fun getGreyFade(index: Int): MutableComponent {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index)
        }

        fun getGreenFade(index: Int): MutableComponent {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 21)
        }

        fun getYellowFade(index: Int): MutableComponent {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 42)
        }

        fun getBlueFade(index: Int): MutableComponent {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 63)
        }
    }
}