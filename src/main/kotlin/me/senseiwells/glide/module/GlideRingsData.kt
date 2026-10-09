package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.module.template.RingTemplate
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.server.MinecraftServer

class GlideRingsData(
    val rings: List<RingTemplate>
): MinigameData {
    override fun type(): MinigameDataType<*> {
        return type
    }

    companion object: MinigameDataProvider<GlideRingsData> {
        private const val RINGS_FILE = "glide_rings.json"

        override val type = MinigameDataType<GlideRingsData>(GlideMod.id("rings"))

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideRingsData {
            val rings = archive.parseJson(RINGS_FILE, RingTemplate.CODEC.listOf()).getOrThrow()
            return GlideRingsData(rings)
        }
    }
}
