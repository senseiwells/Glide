package me.senseiwells.glide.font

import it.unimi.dsi.fastutil.ints.IntIntPair
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedBitmapFontResources
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.component1
import net.casual.arcade.utils.component2
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    private const val SHIFT = -35

    val SUPPORTED_LANGS = listOf("en_us")

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

    object Banner: IndexedBitmapFontResources(GlideMod.id("banner_font")) {
        private val BOXES = Object2ObjectOpenHashMap<FramedKey, Component>()

        private val BOX_SIZES = listOf(
            IntIntPair.of(3, -18),
            IntIntPair.of(4, -22),
            IntIntPair.of(7, -25),
            IntIntPair.of(8, -27),
            IntIntPair.of(11, -30),
            IntIntPair.of(12, -32),
            IntIntPair.of(15, -34),
            IntIntPair.of(16, -36),
            IntIntPair.of(18, -38)
        )
        private val SHIELD_SIZES = listOf(
            IntIntPair.of(52, -15),
            IntIntPair.of(60, -11),
            IntIntPair.of(68, -7),
            IntIntPair.of(76, -3),
            IntIntPair.of(84, 1),
            IntIntPair.of(92, 5),
            IntIntPair.of(96, 7),
            IntIntPair.of(94, 6),
            IntIntPair.of(92, 5),
            IntIntPair.of(90, 4),
            IntIntPair.of(88, 3)
        )

        init {
            for ((height, ascent) in this.BOX_SIZES) {
                indexed(at("banner/textbox/left.png"), ascent, height)
                indexed(at("banner/textbox/right.png"), ascent, height)
            }
            for ((height, ascent) in this.SHIELD_SIZES) {
                indexed(at("banner/elytra_shield.png"), ascent, height)
            }

            registerTextbox(GlideComponents.SOLO_ROUND, GlideComponents::soloRoundTextbox)
            registerTextbox(GlideComponents.NUMBERED_ROUND.generate(), GlideComponents::numberedRoundTextbox)
        }

        fun elytraShield(index: Int): Component {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(this.BOX_SIZES.size * 2 + frame)
        }

        fun textbox(component: Component, index: Int): Component {
            val frame = index.coerceIn(0, this.BOX_SIZES.lastIndex)
            val key = GlideComponents.textboxKey(component, this.BOX_SIZES[frame].keyInt())
            val box = this.BOXES[FramedKey(key, frame)] ?: return Component.empty() // TODO
            val left = this.get(frame * 2)
            val right = this.get(frame * 2 + 1)
            val textbox = Component.empty()
                .append(ComponentUtils.space(1, 2))
                .append(left)
                .append(ComponentUtils.space(-1))
                .append(box)
            if (frame == 8) {
                textbox.append(ComponentUtils.negativeWidthOf(component))
                textbox.append(ComponentUtils.space(-4))
                textbox.append(component)
                textbox.append(ComponentUtils.space(2))
            }
            return textbox.append(ComponentUtils.space(-2)).append(right)
        }

        private fun registerTextbox(component: Component, file: (String) -> String) {
            for ((i, size) in this.BOX_SIZES.withIndex()) {
                val key = GlideComponents.textboxKey(component, size.keyInt())
                val textbox = translatable(key) {
                    SUPPORTED_LANGS.forEach { bitmap(it, at(file(it)), size.valueInt(), size.keyInt()) }
                }.generate()
                this.BOXES[FramedKey(key, i)] = textbox
            }
        }

        private data class FramedKey(val key: String, val frame: Int)
    }
}