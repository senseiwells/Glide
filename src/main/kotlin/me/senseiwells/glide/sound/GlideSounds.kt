package me.senseiwells.glide.sound

import me.senseiwells.glide.GlideMod
import net.casual.arcade.resources.sound.SoundResources

object GlideSounds: SoundResources(GlideMod.MOD_ID) {
    val MUSIC_MAP_1 = sound(at("glide.music.map1"), stream = true)
    val MUSIC_MAP_3 = sound(at("glide.music.map3"), stream = true)
    val BONK = sound(at("glide.bonk"))
    val BOOST = sound(at("glide.boost"))
    val THERMAL_BOOST = sound(at("glide.boost.thermal"))
    val CHECKPOINT = sound(at("glide.checkpoint"))
    val FINISH = sound(at("glide.finish"))
    val FINISH_BEST = sound(at("glide.finish.best"))
}