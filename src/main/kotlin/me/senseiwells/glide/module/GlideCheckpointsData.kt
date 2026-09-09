package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.server.MinecraftServer

class GlideCheckpointsData(
    val checkpoints: IndexedCheckpoints
): MinigameData {
    override fun type(): MinigameDataType<*> {
        return type
    }

    companion object: MinigameDataProvider<GlideCheckpointsData> {
        private const val CHECKPOINTS_FILE = "glide_checkpoints.json"

        override val type = MinigameDataType<GlideCheckpointsData>(GlideMod.id("checkpoints"))

        override fun get(archive: ReadableArchive, server: MinecraftServer): GlideCheckpointsData {
            val checkpoints = archive.parseJson(CHECKPOINTS_FILE, IndexedCheckpoints.CODEC).getOrThrow()
            return GlideCheckpointsData(checkpoints)
        }
    }
}
