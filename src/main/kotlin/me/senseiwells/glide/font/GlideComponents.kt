package me.senseiwells.glide.font

import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.getTranslationKeyOf
import net.minecraft.network.chat.Component

object GlideComponents {
    private const val TEXTBOX = "banner/textbox/datagen"

    val SOLO_ROUND by ComponentUtils.translatable("glide.round.solo")
    val NUMBERED_ROUND = ComponentUtils.translatableWithArgs("glide.round.numbered")
    val STARTING by ComponentUtils.translatable("glide.round.starting")
    val STARTING_IN = ComponentUtils.translatableWithArgs("glide.round.starting.in")
    val STARTED by ComponentUtils.translatable("glide.round.started")
    val REACH_THE_END = ComponentUtils.translatableWithArgs("glide.round.reachTheEnd")
    val ROUND_END by ComponentUtils.translatable("glide.round.end")
    val ENDING_IN = ComponentUtils.translatableWithArgs("glide.round.ending.in")

    val PERSONAL_BEST by ComponentUtils.translatable("glide.personalBest")
    val HURRY_UP by ComponentUtils.translatable("glide.hurryUp")
    val DNF by ComponentUtils.translatable("glide.dnf")
    val PLAYER_WON = ComponentUtils.translatableWithArgs("glide.playerWon")

    val CHECKPOINT by ComponentUtils.translatable("glide.checkpoint")

    fun soloRoundTextbox(lang: String) = "$TEXTBOX/solo_round_$lang.png"
    fun numberedRoundTextbox(lang: String) = "$TEXTBOX/numbered_round_$lang.png"
    fun personalBestTextbox(lang: String) = "$TEXTBOX/personal_best_$lang.png"
    fun hurryUpTextbox(lang: String) = "$TEXTBOX/hurry_up_$lang.png"
    fun dnfTextbox(lang: String) = "$TEXTBOX/dnf_$lang.png"
    fun playerWonTextbox(lang: String) = "$TEXTBOX/player_won_$lang.png"

    fun textboxKey(component: Component, size: Int): String {
        val key = getTranslationKeyOf(component)
        return "$key.box.$size"
    }
}