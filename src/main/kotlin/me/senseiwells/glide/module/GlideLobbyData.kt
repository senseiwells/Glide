package me.senseiwells.glide.module

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.casual.arcade.utils.math.location.providers.LocationProvider
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import java.util.*

class GlideLobbyData(
    val spawn: LocationProvider = LocationProvider.DEFAULT,
    val timeOfDay: Optional<Int> = Optional.empty()
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val LOBBY_DATA_FILE = "glide_lobby_data.json"

        val DEFAULT = GlideLobbyData()

        override val id: Identifier = GlideMod.id("lobby_data")

        val codec: Codec<GlideLobbyData> = RecordCodecBuilder.create { instance ->
            instance.group(
                LocationProvider.CODEC.optionalFieldOf("spawn", LocationProvider.DEFAULT).forGetter(GlideLobbyData::spawn),
                Codec.INT.optionalFieldOf("time_of_day").forGetter(GlideLobbyData::timeOfDay)
            ).apply(instance, ::GlideLobbyData)
        }

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideLobbyData {
            return archive.parseJson(LOBBY_DATA_FILE, this.codec, server).getOrThrow()
        }
    }
}
