package me.senseiwells.glide.font

import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntIntPair
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedBitmapFontResources
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.JsonUtils.int
import net.casual.arcade.utils.JsonUtils.objects
import net.casual.arcade.utils.component1
import net.casual.arcade.utils.component2
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.TranslatableContents
import kotlin.io.path.reader

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    private val CHAR_WIDTH_LOOKUP = Char2IntOpenHashMap()

    private const val SHIFT = -35

    val SUPPORTED_LANGS = listOf("en_us")

    val YOU_CRASHED by translatable("glide.you_crashed") {
        bitmap("en_us", at("crash/you_crashed_en_us.png"), 3, 3)
    }
    val YOU_CRASHED_BACKGROUND by bitmap(at("crash/you_crashed_background_2.png"), 500, 1000)

    val FULL_HEART by bitmap(at("heart/full.png"), 18 + SHIFT, 9)
    val HALF_HEART by bitmap(at("heart/half.png"), 18 + SHIFT, 9)
    val EMPTY_HEART by bitmap(at("heart/empty.png"), 18 + SHIFT, 9)

    init {
        CHAR_WIDTH_LOOKUP.defaultReturnValue(6)

        GlideMod.container.findPath("data/glide/char_width_lookup.json").get().reader().use {
            for (element in JsonUtils.decodeToJsonArray(it).objects()) {
                CHAR_WIDTH_LOOKUP[element.int("char").toChar()] = element.int("width")
            }
        }
    }

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

        private const val DYNAMIC_BOXES = 5
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

        private const val BOX_COUNT = (2 + DYNAMIC_BOXES)
        private val BOX_SHIFT = BOX_SIZES.size * BOX_COUNT

        init {
            for ((height, ascent) in this.BOX_SIZES) {
                indexed(at("banner/textbox/left.png"), ascent, height)
                indexed(at("banner/textbox/right.png"), ascent, height)
                for (i in 0 until DYNAMIC_BOXES) {
                    indexed(at("banner/textbox/${1 shl i}.png"), ascent, height)
                }
            }
            for ((height, ascent) in this.SHIELD_SIZES) {
                indexed(at("banner/elytra_shield.png"), ascent, height)
                indexed(at("banner/stopwatch_shield.png"), ascent, height)
                indexed(at("banner/score_shield.png"), ascent, height)
            }

            registerTextbox(GlideComponents.SOLO_ROUND, GlideComponents::soloRoundTextbox)
            registerTextbox(GlideComponents.NUMBERED_ROUND.generate(), GlideComponents::numberedRoundTextbox)
            registerTextbox(GlideComponents.PERSONAL_BEST, GlideComponents::personalBestTextbox)
            registerTextbox(GlideComponents.HURRY_UP, GlideComponents::hurryUpTextbox)
            registerTextbox(GlideComponents.DNF, GlideComponents::dnfTextbox)
        }

        fun elytraShield(index: Int): MutableComponent {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3)
        }

        fun stopwatchShield(index: Int): MutableComponent {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3 + 1)
        }

        fun scoreShield(index: Int): MutableComponent {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3 + 2)
        }

        fun textbox(component: Component, index: Int): MutableComponent {
            val frame = index.coerceIn(0, this.BOX_SIZES.lastIndex)
            val box = this.dynamicallySizedTextbox(component, frame)
            val left = this.get(frame * BOX_COUNT)
            val right = this.get(frame * BOX_COUNT + 1)
            return Component.empty()
                .append(left)
                .append(ComponentUtils.space(-6))
                .append(box)
                .append(ComponentUtils.space(-6))
                .append(right)
        }

        fun isLastTextboxFrame(index: Int): Boolean {
            return index >= 8
        }

        private fun dynamicallySizedTextbox(component: Component, frame: Int): Component {
            val contents = component.contents
            val dynamic = Component.empty()
            if (contents is TranslatableContents) {
                val key = GlideComponents.textboxKey(component, BOX_SIZES[frame].keyInt())
                val box = BOXES[FramedKey(key, frame)]
                    ?: throw IllegalStateException("Component ${contents.key} doesn't have a textbox!")
                dynamic.append(box)
                dynamic.append(ComponentUtils.space(-2))
                for (arg in contents.args) {
                    dynamic.append(when (arg) {
                        is Component -> this.dynamicallySizedTextbox(arg, frame)
                        else -> this.dynamicallySizedTextbox(arg.toString(), frame)
                    })
                    dynamic.append(ComponentUtils.space(-1))
                }
                return dynamic
            }
            return this.dynamicallySizedTextbox(component.string, frame)
        }

        private fun dynamicallySizedTextbox(string: String, frame: Int): MutableComponent {
            val size = string.sumOf { CHAR_WIDTH_LOOKUP[it] }
            var remaining = size / 2.0
            val dynamic = Component.empty()
            for (i in (0 until DYNAMIC_BOXES).reversed()) {
                val width = 1 shl i
                while (remaining >= width) {
                    dynamic.append(this.get(frame * BOX_COUNT + 2 + i))
                    dynamic.append(ComponentUtils.space(-1))
                    // The textboxes are actually 1.125 (18/16) times bigger than a pixel
                    remaining -= width * 1.125
                }
            }
            return dynamic
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