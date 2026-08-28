package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameDataModule
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.DimensionType
import kotlin.io.path.exists

object GlideDimensionTypeData: MinigameDataModule, MinigameDataModule.Provider {
    private const val DIMENSION_TYPE_FILE = "dimension_type.json"

    override val id: Identifier = GlideMod.id("dimension_type")

    override fun get(archive: ReadableArchive, server: MinecraftServer): MinigameDataModule {
        return this
    }

    fun getDimensionTypeIfPresent(archive: ReadableArchive, lookup: RegistryOps.RegistryInfoLookup): DimensionType? {
        if (archive.resolve(DIMENSION_TYPE_FILE).exists()) {
            return archive.parseJson(DIMENSION_TYPE_FILE, DimensionType.DIRECT_CODEC, lookup).getOrThrow()
        }
        return null
    }
}