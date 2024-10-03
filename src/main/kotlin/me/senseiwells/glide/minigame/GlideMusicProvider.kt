package me.senseiwells.glide.minigame

import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.minigame.utils.MinigameMusicProvider
import net.casual.arcade.utils.impl.TimedSound
import net.minecraft.server.level.ServerPlayer

object GlideMusicProvider: MinigameMusicProvider {
    override fun getNextMusicFor(player: ServerPlayer, previous: TimedSound?): TimedSound {
        return when (previous) {
            GlideSounds.MAP_1_MUSIC -> GlideSounds.MAP_2_MUSIC
            GlideSounds.MAP_2_MUSIC -> GlideSounds.MAP_3_MUSIC
            else -> GlideSounds.MAP_1_MUSIC
        }
    }
}