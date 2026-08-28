package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.pack.generation.PackDefinition
import net.casual.arcade.utils.file.ReadableArchive
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideResourcePackData(
    val definition: PackDefinition
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        override val id: Identifier = GlideMod.id("resource_pack")

        override fun get(archive: ReadableArchive, server: MinecraftServer): MinigameDataModule {
            return GlideResourcePackData(PackDefinition(archive.name) {
                val root = archive.resolve("resource_pack")
                include(root)
            })
        }
    }
}