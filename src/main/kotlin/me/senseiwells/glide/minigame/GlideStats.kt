package me.senseiwells.glide.minigame

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.stats.StatType

object GlideStats {
    val LAST_SNEAK_TIME = StatType.int32(GlideMod.id("last_sneak_time"))

    val BEST_TIME = StatType.int32(GlideMod.id("best_time"))
    val BEST_SCORE = StatType.int32(GlideMod.id("best_score"))
}