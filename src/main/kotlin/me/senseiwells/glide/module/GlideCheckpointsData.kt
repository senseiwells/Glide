package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

class GlideCheckpointsData(
    val checkpoints: IndexedCheckpoints
): MinigameDataModule {
    companion object: MinigameDataModule.Provider {
        private const val CHECKPOINTS_FILE = "glide_checkpoints.json"

        override val id: Identifier = GlideMod.id("checkpoints")

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideCheckpointsData {
            val checkpoints = archive.parseJson(CHECKPOINTS_FILE, IndexedCheckpoints.CODEC).getOrThrow()
            return GlideCheckpointsData(checkpoints)
        }
    }
}
