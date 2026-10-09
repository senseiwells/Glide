package me.senseiwells.glide.module

import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.utils.impl.TimedSound
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

class GlideMusicData(
    val music: List<TimedSound>
): MinigameData {
    override fun type(): MinigameDataType<*> {
        return type
    }

    companion object: MinigameDataProvider<GlideMusicData> {
        private const val MUSIC_FILE = "glide_music.json"

        private val TIMED_MUSIC_CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                Identifier.CODEC.fieldOf("sound").forGetter { timed -> timed.sound.event.location },
                MinecraftTimeDuration.CODEC.fieldOf("duration").forGetter(TimedSound::duration)
            ).apply(instance, ::createTimedMusic)
        }

        override val type = MinigameDataType<GlideMusicData>(GlideMod.id("music"))

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideMusicData {
            val music = archive.parseJson(MUSIC_FILE, TIMED_MUSIC_CODEC.listOf(), server).getOrThrow()
            return GlideMusicData(music)
        }

        private fun createTimedMusic(id: Identifier, duration: MinecraftTimeDuration): TimedSound {
            val sound = Sound(SoundEvent.createVariableRangeEvent(id), SoundSource.MUSIC)
            return TimedSound(sound, duration)
        }
    }
}