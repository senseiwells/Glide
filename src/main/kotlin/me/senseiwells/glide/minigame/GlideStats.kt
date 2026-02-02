package me.senseiwells.glide.minigame

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.stats.StatType
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.minecraft.core.Holder
import net.minecraft.core.Registry

object GlideStats {
    val LAST_SNEAK_TIME = this.register("last_sneak_time", StatType.int32())

    val BEST_TIME = this.register("best_time", StatType.int32())
    val BEST_SCORE = this.register("best_score", StatType.int32())

    internal fun noop() {

    }

    private fun <T: Any> register(path: String, type: StatType<T>): Holder.Reference<StatType<T>> {
        return Registry.registerForHolder(MinigameRegistries.STAT_TYPES, GlideMod.id(path), type)
    }
}