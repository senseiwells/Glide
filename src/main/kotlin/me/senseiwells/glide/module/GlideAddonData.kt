package me.senseiwells.glide.module

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.module.template.BoosterTemplate
import me.senseiwells.glide.module.template.RingTemplate
import me.senseiwells.glide.module.template.ThermalTemplate
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.serialization.codec.fieldOfAny
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideAddonData(
    val id: Identifier,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String,
    val spawn: Location,
    val boosters: List<BoosterTemplate>,
    val thermals: List<ThermalTemplate>,
    val checkpoints: IndexedCheckpoints,
    val rings: List<RingTemplate>
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val ADDON_FILE = "addon.json"
        private val ID_CODEC = Identifier.CODEC.fieldOfAny("id", "addon_id")

        override val id: Identifier = GlideMod.id("addon")

        val codec: Codec<GlideAddonData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ID_CODEC.forGetter(GlideAddonData::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(GlideAddonData::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(GlideAddonData::description),
                Codec.STRING.listOf().fieldOf("authors").forGetter(GlideAddonData::authors),
                Codec.STRING.fieldOf("version").forGetter(GlideAddonData::version),
                Location.CODEC.fieldOf("spawn").forGetter(GlideAddonData::spawn),
                BoosterTemplate.CODEC.listOf().fieldOf("boosters").forGetter(GlideAddonData::boosters),
                ThermalTemplate.CODEC.listOf().fieldOf("thermals").forGetter(GlideAddonData::thermals),
                IndexedCheckpoints.CODEC.fieldOf("checkpoints").forGetter(GlideAddonData::checkpoints),
                RingTemplate.CODEC.listOf().fieldOf("rings").forGetter(GlideAddonData::rings)
            ).apply(instance, ::GlideAddonData)
        }

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideAddonData {
            return this.get(archive)
        }

        fun get(archive: ReadableArchive): GlideAddonData {
            return archive.parseJson(ADDON_FILE, this.codec).getOrThrow()
        }

        fun getId(archive: ReadableArchive): Identifier {
            return archive.parseJson(ADDON_FILE, ID_CODEC.codec()).getOrThrow()
        }
    }
}