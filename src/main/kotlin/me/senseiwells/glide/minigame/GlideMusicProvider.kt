package me.senseiwells.glide.minigame

import net.casual.arcade.minigame.utils.MinigameMusicProvider
import net.casual.arcade.utils.impl.TimedSound
import net.minecraft.server.level.ServerPlayer

class GlideMusicProvider(val music: List<TimedSound>): MinigameMusicProvider {
    override fun getNextMusicFor(player: ServerPlayer, previous: TimedSound?): TimedSound {
        if (previous == null) {
            return this.music.first()
        }

        val index = this.music.indexOf(previous)
        return this.music[(index + 1) % this.music.size]
    }
}