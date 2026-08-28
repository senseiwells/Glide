package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.module.template.RingTemplate
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideRingsData(
    val rings: List<RingTemplate>
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val RINGS_FILE = "glide_rings.json"

        override val id: Identifier = GlideMod.id("rings")

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideRingsData {
            val rings = archive.parseJson(RINGS_FILE, RingTemplate.CODEC.listOf()).getOrThrow()
            return GlideRingsData(rings)
        }
    }
}
