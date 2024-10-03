package me.senseiwells.glide.font

import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.getTranslationKeyOf
import net.minecraft.network.chat.Component

object GlideComponents {
    private const val TEXTBOX = "banner/textbox/datagen"

    val SOLO_ROUND by ComponentUtils.translatable("glide.round.solo")
    val NUMBERED_ROUND = ComponentUtils.translatableWithArgs("glide.round.numbered")

    fun soloRoundTextbox(lang: String) = "$TEXTBOX/solo_round_$lang.png"
    fun numberedRoundTextbox(lang: String) = "$TEXTBOX/numbered_round_$lang.png"

    fun textboxKey(component: Component, size: Int): String {
        val key = getTranslationKeyOf(component)
        return "$key.box.$size"
    }
}