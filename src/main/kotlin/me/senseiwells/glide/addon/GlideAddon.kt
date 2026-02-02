package me.senseiwells.glide.addon

import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.file.ReadableArchive
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.DimensionType
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

// TODO: None of the default glide maps use custom dimensions,
//   but we should support this in the future...
class GlideAddon(
    val archive: ReadableArchive,
    val config: GlideAddonConfig,
    val dimensionType: ResourceKey<DimensionType> = BuiltinDimensionTypes.OVERWORLD
) {
    @OptIn(ExperimentalPathApi::class)
    fun extractWorld(destination: Path) {
        this.archive.resolve("world").copyToRecursively(destination, followLinks = false, overwrite = true)
    }

    companion object {
        fun from(path: Path): GlideAddon {
            if (!path.isRegularFile() || path.extension != "lemaddon") {
                throw IllegalArgumentException("Unable to read lemaddon!")
            }
            try {
                val archive = ReadableArchive.zip(path)
                if (archive.resolve("world").notExists()) {
                    throw IllegalArgumentException("lemaddon didn't contain a world directory")
                }

                val addonsPath = archive.resolve("addon.json")
                if (!addonsPath.isReadable()) {
                    throw IllegalArgumentException("lemaddon didn't contain addon.json")
                }
                val config = JsonUtils.decodeWith(GlideAddonConfig.CODEC, addonsPath).partialOrThrow
                return GlideAddon(archive, config)
            } catch (e: IOException) {
                throw IllegalArgumentException("Invalid lemaddon!", e)
            }
        }
    }
}