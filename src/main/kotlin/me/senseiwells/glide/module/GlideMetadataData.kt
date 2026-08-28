package me.senseiwells.glide.module

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.serialization.codec.fieldOfAny
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideMetadataData(
    val id: Identifier,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String,
    val spawn: Location
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val METADATA_FILE = "glide_metadata.json"
        private val ID_CODEC = Identifier.CODEC.fieldOfAny("id", "addon_id")

        override val id: Identifier = GlideMod.id("metadata")

        val codec: Codec<GlideMetadataData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ID_CODEC.forGetter(GlideMetadataData::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(GlideMetadataData::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(GlideMetadataData::description),
                Codec.STRING.listOf().fieldOf("authors").forGetter(GlideMetadataData::authors),
                Codec.STRING.fieldOf("version").forGetter(GlideMetadataData::version),
                Location.CODEC.fieldOf("spawn").forGetter(GlideMetadataData::spawn)
            ).apply(instance, ::GlideMetadataData)
        }

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideMetadataData {
            return this.get(archive)
        }

        fun get(archive: ReadableArchive): GlideMetadataData {
            return archive.parseJson(METADATA_FILE, this.codec).getOrThrow()
        }

        fun getId(archive: ReadableArchive): Identifier {
            return archive.parseJson(METADATA_FILE, ID_CODEC.codec()).getOrThrow()
        }
    }
}
