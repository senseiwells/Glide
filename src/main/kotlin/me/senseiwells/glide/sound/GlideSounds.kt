package me.senseiwells.glide.sound

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.sound.SoundResources
import net.minecraft.sounds.SoundEvent

object GlideSounds: SoundResources(GlideMod.MOD_ID) {
    val MUSIC_MAP_1 = sound(at("glide.music.map1"), stream = true)
    val MUSIC_MAP_3 = sound(at("glide.music.map3"), stream = true)
    val BONK = sound(at("glide.bonk"))
    val BOOST = sound(at("glide.boost"))
    val THERMAL = sound(at("glide.boost.thermal"))
    val RING_BLUE = sound(at("glide.ring.blue"))
    val RING_GREEN_1 = sound(at("glide.ring.green.1"))
    val RING_GREEN_2 = sound(at("glide.ring.green.2"))
    val RING_YELLOW_1 = sound(at("glide.ring.yellow.1"))
    val RING_YELLOW_2 = sound(at("glide.ring.yellow.2"))
    val CHECKPOINT = sound(at("glide.checkpoint"))
    val RESPAWN = sound(at("glide.respawn"))
    val FINISH_1ST = sound(at("glide.finish.1st"))
    val FINISH_2ND = sound(at("glide.finish.2nd"))
    val FINISH_3RD = sound(at("glide.finish.3rd"))
    val FINISH_NO_PODIUM = sound(at("glide.finish"))
    val SCORE_PB = sound(at("glide.score.pb"))
    val TIME_PB = sound(at("glide.time.pb"))
    val CRASH = group("glide.crash") {
        sound(at("glide.crash.1"))
        sound(at("glide.crash.2"))
        sound(at("glide.crash.3"))
        sound(at("glide.crash.4"))
    }

    fun getFinishSound(placement: Int): SoundEvent {
        return when (placement) {
            1 -> FINISH_1ST
            2 -> FINISH_2ND
            3 -> FINISH_3RD
            else -> FINISH_NO_PODIUM
        }
    }
}