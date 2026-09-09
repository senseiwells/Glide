package me.senseiwells.glide.module

import me.senseiwells.glide.GlideMod
import net.casual.arcade.minigame.data.MinigameData
import net.casual.arcade.minigame.data.MinigameDataProvider
import net.casual.arcade.minigame.data.MinigameDataType
import net.casual.arcade.utils.file.ReadableArchive
import net.casual.arcade.utils.file.ReadableArchive.Companion.parseJson
import net.minecraft.resources.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.DimensionType
import kotlin.io.path.exists

object GlideDimensionTypeData: MinigameData, MinigameDataProvider<GlideDimensionTypeData> {
    private const val DIMENSION_TYPE_FILE = "dimension_type.json"

    override val type = MinigameDataType<GlideDimensionTypeData>(GlideMod.id("dimension_type"))

    override fun get(archive: ReadableArchive, server: MinecraftServer): GlideDimensionTypeData {
        return this
    }

    override fun type(): MinigameDataType<*> {
        return type
    }

    fun getDimensionTypeIfPresent(archive: ReadableArchive, lookup: RegistryOps.RegistryInfoLookup): DimensionType? {
        if (archive.resolve(DIMENSION_TYPE_FILE).exists()) {
            return archive.parseJson(DIMENSION_TYPE_FILE, DimensionType.DIRECT_CODEC, lookup).getOrThrow()
        }
        return null
    }
}