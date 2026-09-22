package me.senseiwells.glide.module

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.serialization.codec.fieldOfAny
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideMetaData(
    val id: Identifier,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String,
    val spawn: Location
): MinigameData {
    override fun type(): MinigameDataType<*> {
        return type
    }

    companion object: MinigameDataProvider<GlideMetaData> {
        private const val METADATA_FILE = "glide_metadata.json"
        private val ID_CODEC = Identifier.CODEC.fieldOfAny("id", "addon_id")

        override val type = MinigameDataType<GlideMetaData>(GlideMod.id("metadata"))

        val codec: Codec<GlideMetaData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ID_CODEC.forGetter(GlideMetaData::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(GlideMetaData::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(GlideMetaData::description),
                Codec.STRING.listOf().fieldOf("authors").forGetter(GlideMetaData::authors),
                Codec.STRING.fieldOf("version").forGetter(GlideMetaData::version),
                Location.CODEC.fieldOf("spawn").forGetter(GlideMetaData::spawn)
            ).apply(instance, ::GlideMetaData)
        }

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideMetaData {
            return this.get(archive)
        }

        fun get(archive: ReadableArchive): GlideMetaData {
            return archive.parseJson(METADATA_FILE, this.codec).getOrThrow()
        }

        fun getId(archive: ReadableArchive): Identifier {
            return archive.parseJson(METADATA_FILE, ID_CODEC.codec()).getOrThrow()
        }
    }
}
