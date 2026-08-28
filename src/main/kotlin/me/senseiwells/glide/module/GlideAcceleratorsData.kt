package me.senseiwells.glide.module

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.module.template.BoosterTemplate
import me.senseiwells.glide.module.template.ThermalTemplate
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideAcceleratorsData(
    val boosters: List<BoosterTemplate>,
    val thermals: List<ThermalTemplate>
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val ACCELERATORS_FILE = "glide_accelerators.json"

        override val id: Identifier = GlideMod.id("accelerators")

        val codec: Codec<GlideAcceleratorsData> = RecordCodecBuilder.create { instance ->
            instance.group(
                BoosterTemplate.CODEC.listOf().fieldOf("boosters").forGetter(GlideAcceleratorsData::boosters),
                ThermalTemplate.CODEC.listOf().fieldOf("thermals").forGetter(GlideAcceleratorsData::thermals)
            ).apply(instance, ::GlideAcceleratorsData)
        }

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideAcceleratorsData {
            return archive.parseJson(ACCELERATORS_FILE, this.codec).getOrThrow()
        }
    }
}
