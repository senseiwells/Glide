package me.senseiwells.glide.sound

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.sound.SoundResources

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
    val FINISH_1ST = sound(at("glide.finish.1st"))
    val FINISH_2ND = sound(at("glide.finish.2nd"))
    val FINISH_3RD = sound(at("glide.finish.3rd"))
    val FINISH_NO_PODIUM = sound(at("glide.finish"))
    val CRASH = group("glide.crash") {
        sound(at("glide.crash.1"))
        sound(at("glide.crash.2"))
        sound(at("glide.crash.3"))
        sound(at("glide.crash.4"))
    }
}