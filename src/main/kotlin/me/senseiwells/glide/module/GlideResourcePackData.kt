package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.pack.generation.PackDefinition
import net.casual.arcade.utils.file.ReadableArchive
import net.minecraft.server.MinecraftServer

class GlideResourcePackData(
    val definition: PackDefinition
): MinigameData {
    override fun type(): MinigameDataType<*> {
        return type
    }

    companion object: MinigameDataProvider<GlideResourcePackData> {
        override val type = MinigameDataType<GlideResourcePackData>(GlideMod.id("resource_pack"))

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideResourcePackData {
            return GlideResourcePackData(PackDefinition(archive.name) {
                val root = archive.resolve("resource_pack")
                include(root)
            })
        }
    }
}