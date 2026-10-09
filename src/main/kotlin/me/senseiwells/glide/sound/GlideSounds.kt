package me.senseiwells.glide.sound

import me.senseiwells.glide.GlideMod
import net.casual.arcade.pack.sound.SoundResources
import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.utils.impl.TimedSound
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

object GlideSounds: SoundResources(GlideMod.MOD_ID) {
    val COUNTDOWN_TICK = sound(at("countdown.tick"))
    val COUNTDOWN_START = sound(at("countdown.start"))

    val BONK = sound(at("bonk"))
    val BOOST = sound(at("boost"))
    val THERMAL = sound(at("boost.thermal"))
    val RING_BLUE = sound(at("ring.blue"))
    val RING_GREEN_1 = sound(at("ring.green.1"))
    val RING_GREEN_2 = sound(at("ring.green.2"))
    val RING_YELLOW_1 = sound(at("ring.yellow.1"))
    val RING_YELLOW_2 = sound(at("ring.yellow.2"))
    val CHECKPOINT = sound(at("checkpoint"))
    val RESPAWN = sound(at("respawn"))
    val FINISH_1ST = sound(at("finish.1st"))
    val FINISH_2ND = sound(at("finish.2nd"))
    val FINISH_3RD = sound(at("finish.3rd"))
    val FINISH_NO_PODIUM = sound(at("finish"))
    val SCORE_PB = sound(at("score.pb"))
    val TIME_PB = sound(at("time.pb"))
    val CRASH = group("crash") {
        sound(at("crash.1"))
        sound(at("crash.2"))
        sound(at("crash.3"))
        sound(at("crash.4"))
    }

    val MAP_1_MUSIC = TimedSound(
        Sound(sound(at("music.map.1"), stream = true), SoundSource.MUSIC),
        2.Minutes + 12.Seconds
    )
    val MAP_2_MUSIC = TimedSound(
        Sound(sound(at("music.map.2"), stream = true), SoundSource.MUSIC),
        2.Minutes + 6.Seconds
    )
    val MAP_3_MUSIC = TimedSound(
        Sound(sound(at("music.map.3"), stream = true), SoundSource.MUSIC),
        2.Minutes + 27.Seconds
    )
    val MAP_4_MUSIC = TimedSound(
        Sound(sound(at("music.map.4"), stream = true), SoundSource.MUSIC),
        2.Minutes + 7.Seconds
    )
    val MAP_5_MUSIC = TimedSound(
        Sound(sound(at("music.map.5"), stream = true), SoundSource.MUSIC),
        2.Minutes + 5.Seconds
    )
    val MAP_6_MUSIC = TimedSound(
        Sound(sound(at("music.map.6"), stream = true), SoundSource.MUSIC),
        2.Minutes + 7.Seconds
    )
    val MAP_7_MUSIC = TimedSound(
        Sound(sound(at("music.map.7"), stream = true), SoundSource.MUSIC),
        2.Minutes + 1.Seconds
    )

    fun getFinishSound(placement: Int): SoundEvent {
        return when (placement) {
            1 -> FINISH_1ST
            2 -> FINISH_2ND
            3 -> FINISH_3RD
            else -> FINISH_NO_PODIUM
        }
    }
}