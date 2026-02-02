package me.senseiwells.glide.font

import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntIntPair
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedFontResources
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.JsonUtils.int
import net.casual.arcade.utils.JsonUtils.objects
import net.casual.arcade.utils.component.LiteralWidthResolver
import net.casual.arcade.utils.component1
import net.casual.arcade.utils.component2
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.TranslatableContents
import kotlin.io.path.reader

object GlideFont: FontResources(GlideMod.id("glide_font")) {
    private const val SHIFT = -35

    val SUPPORTED_LANGS = listOf("en_us")

    val YOU_CRASHED = translatable("glide.you_crashed") {
        bitmap("en_us", at("crash/you_crashed_en_us.png"), 3, 3)
    }
    val YOU_CRASHED_BACKGROUND = bitmap(at("crash/you_crashed_background_2.png"), 500, 1000)

    val FULL_HEART = bitmap(at("heart/full.png"), 18 + SHIFT, 9)
    val HALF_HEART = bitmap(at("heart/half.png"), 18 + SHIFT, 9)
    val EMPTY_HEART = bitmap(at("heart/empty.png"), 18 + SHIFT, 9)

    object Progressbar: IndexedFontResources(GlideMod.id("progressbar")) {
        private val BG_1 = bitmap(at("progressbar/progressbar_0.png"), 5 + SHIFT, 10)
        private val BG_2 = bitmap(at("progressbar/progressbar_1.png"), 5 + SHIFT, 10)

        val BACKGROUND: MutableComponent
            get() = Component.empty().append(BG_1).append(SpacingFontResources.spaced(-1)).append(BG_2)

        val CURRENT = bitmap(at("progressbar/player_marker/current.png"), 4 + SHIFT, 7)

        init {
            for (i in 1..16) {
                indexed { bitmap(at("progressbar/player_marker/$i.png"), 3 + SHIFT, 5) }
            }
            for (i in 0..5) {
                indexed { bitmap(at("progressbar/checkpoint_$i.png"), 12 + SHIFT, 24) }
            }
        }

        fun getPlayerMarker(index: Int): Component {
            require(index in 0..15) { "Index must be between 0 and 15" }
            return this.get(index)
        }

        fun getCheckpoint(index: Int): Component {
            require(index in 0..5) { "Index must be between 0 and 5" }
            return this.get(index + 16)
        }
    }

    object Speedometer: IndexedFontResources(GlideMod.id("speedometer")) {
        val SPEEDOMETER = bitmap(at("speedometer/speedometer.png"), 8, 16)

        init {
            for (i in 0..11) {
                indexed { bitmap(at("speedometer/needle_$i.png"), 8, 16) }
            }
        }

        fun getNeedle(index: Int): Component {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index)
        }
    }

    object Timer: IndexedFontResources(GlideMod.id("timer")) {
        val TIMER = bitmap(at("timer/clock.png"), 8, 16)

        init {
            for (i in 0..11) {
                indexed { bitmap(at("timer/seconds_$i.png"), 8, 16) }
            }
            for (i in 0..11) {
                indexed { bitmap(at("timer/minutes_$i.png"), 8, 16) }
            }
        }

        fun getSecondsHand(index: Int): Component {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index)
        }

        fun getMinutesHand(index: Int): Component {
            require(index in 0..11) { "Index must be between 0 and 11" }
            return this.get(index + 12)
        }
    }

    object Score: IndexedFontResources(GlideMod.id("score")) {
        val GREY = bitmap(at("score/grey.png"), 8, 16)
        val GREEN = bitmap(at("score/green.png"), 8, 16)
        val YELLOW = bitmap(at("score/yellow.png"), 8, 16)
        val BLUE = bitmap(at("score/blue.png"), 8, 16)

        init {
            for (i in 0..20) {
                indexed { bitmap(at("score/grey_fade_$i.png"), 8, 16) }
            }
            for (i in 0..20) {
                indexed { bitmap(at("score/green_fade_$i.png"), 8, 16) }
            }
            for (i in 0..20) {
                indexed { bitmap(at("score/yellow_fade_$i.png"), 8, 16) }
            }
            for (i in 0..20) {
                indexed { bitmap(at("score/blue_fade_$i.png"), 8, 16) }
            }
        }

        fun getGreyFade(index: Int): Component {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index)
        }

        fun getGreenFade(index: Int): Component {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 21)
        }

        fun getYellowFade(index: Int): Component {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 42)
        }

        fun getBlueFade(index: Int): Component {
            require(index in 0..20) { "Index must be between 0 and 20" }
            return this.get(index + 63)
        }
    }

    object Banner: IndexedFontResources(GlideMod.id("banner_font")) {
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
                indexed { bitmap(at("banner/textbox/left.png"), ascent, height) }
                indexed { bitmap(at("banner/textbox/right.png"), ascent, height) }
                for (i in 0 until DYNAMIC_BOXES) {
                    indexed { bitmap(at("banner/textbox/${1 shl i}.png"), ascent, height) }
                }
            }
            for ((height, ascent) in this.SHIELD_SIZES) {
                indexed { bitmap(at("banner/elytra_shield.png"), ascent, height) }
                indexed { bitmap(at("banner/stopwatch_shield.png"), ascent, height) }
                indexed { bitmap(at("banner/score_shield.png"), ascent, height) }
            }

            registerTextbox(GlideComponents.SOLO_ROUND, GlideComponents::soloRoundTextbox)
            registerTextbox(GlideComponents.NUMBERED_ROUND.generate(), GlideComponents::numberedRoundTextbox)
            registerTextbox(GlideComponents.PERSONAL_BEST, GlideComponents::personalBestTextbox)
            registerTextbox(GlideComponents.HURRY_UP, GlideComponents::hurryUpTextbox)
            registerTextbox(GlideComponents.DNF, GlideComponents::dnfTextbox)
            registerTextbox(GlideComponents.PLAYER_WON.generate(), GlideComponents::playerWonTextbox)
        }

        fun elytraShield(index: Int): Component {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3)
        }

        fun stopwatchShield(index: Int): Component {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3 + 1)
        }

        fun scoreShield(index: Int): Component {
            val frame = index.coerceIn(0, this.SHIELD_SIZES.lastIndex)
            return this.get(BOX_SHIFT + frame * 3 + 2)
        }

        fun textbox(component: Component, index: Int): Component {
            val frame = index.coerceIn(0, this.BOX_SIZES.lastIndex)
            val box = this.dynamicallySizedTextbox(component, frame)
            val left = this.get(frame * BOX_COUNT)
            val right = this.get(frame * BOX_COUNT + 1)
            return Component.empty()
                .append(left)
                .append(this.dynamicallySizedSpace(frame, -6))
                .append(box)
                .append(this.dynamicallySizedSpace(frame, -6))
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
                for (arg in contents.args) {
                    dynamic.append(SpacingFontResources.composed(-1))
                    dynamic.append(when (arg) {
                        is Component -> this.dynamicallySizedTextbox(arg, frame)
                        else -> this.dynamicallySizedTextbox(arg.toString(), frame)
                    })
                }
                return dynamic
            }
            return this.dynamicallySizedTextbox(component.string, frame)
        }

        private fun dynamicallySizedTextbox(string: String, frame: Int): MutableComponent {
            val size = LiteralWidthResolver.width(string)
            var remaining = size / 2.0
            val dynamic = Component.empty()
            for (i in (0 until DYNAMIC_BOXES).reversed()) {
                val width = 1 shl i
                while (remaining >= width) {
                    // We shift a little more to the left than we have to
                    // otherwise because of floating point there may be a slight gap
                    dynamic.append(SpacingFontResources.composed(-1.2F))
                    dynamic.append(this.get(frame * BOX_COUNT + 2 + i))
                    // The textboxes are actually 1.125 (18/16) times bigger than a pixel
                    // We compensate for the error above here
                    remaining -= (width * 1.125) - 0.2F
                }
            }
            if (remaining < 1) {
                // To fix any small values, we use spacing to get the desired precision
                dynamic.append(SpacingFontResources.composed(-1 - remaining.toFloat()))
                dynamic.append(this.get(frame * BOX_COUNT + 3))
            }
            return dynamic
        }

        @Suppress("SameParameterValue")
        private fun dynamicallySizedSpace(frame: Int, size: Int = 1): Component {
            return SpacingFontResources.composed(size * (BOX_SIZES[frame].keyInt() / 18.0F))
        }

        private fun registerTextbox(component: Component, file: (String) -> String) {
            for ((i, size) in this.BOX_SIZES.withIndex()) {
                val key = GlideComponents.textboxKey(component, size.keyInt())
                val textbox = translatable(key) {
                    SUPPORTED_LANGS.forEach { bitmap(it, at(file(it)), size.valueInt(), size.keyInt()) }
                }
                this.BOXES[FramedKey(key, i)] = textbox
            }
        }

        private data class FramedKey(val key: String, val frame: Int)
    }
}