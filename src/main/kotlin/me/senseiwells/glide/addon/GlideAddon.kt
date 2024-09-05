package me.senseiwells.glide.addon

import net.casual.arcade.utils.JsonUtils
import net.lingala.zip4j.ZipFile
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.DimensionType
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class GlideAddon(
    private val zip: ZipFile,
    val config: GlideAddonConfig,
    val dimensionType: ResourceKey<DimensionType> = BuiltinDimensionTypes.OVERWORLD
) {
    fun extractWorld(destination: Path) {
        this.zip.extractFile("world/", destination.parent.absolutePathString(), destination.name)
    }

    companion object {
        fun from(path: Path): GlideAddon {
            if (!path.isRegularFile() || path.extension != "lemaddon") {
                throw IllegalArgumentException("Unable to read lemaddon!")
            }
            try {
                val zip = ZipFile(path.toFile())
                val worldHeader = zip.getFileHeader("world/")
                if (worldHeader == null || !worldHeader.isDirectory) {
                    throw IllegalArgumentException("lemaddon didn't contain a world directory")
                }

                val addonHeader = zip.getFileHeader("addon.json")
                    ?: throw IllegalArgumentException("lemaddon didn't contain addon.json")
                val config = zip.getInputStream(addonHeader).use {
                    JsonUtils.decodeWith(GlideAddonConfig.CODEC, it).partialOrThrow
                }
                return GlideAddon(zip, config)

                // TODO: None of the default glide maps use custom dimensions,
                //   but we should support this in the future...
                // val dimensionTypeHeader = zip.getFileHeader("dimension_type.json")
                //     ?: throw java.lang.IllegalArgumentException("lemaddon didn't contain dimension_type.json")
                // val dimensionType = zip.getInputStream(dimensionTypeHeader).use {
                //     JsonUtils.decodeWith(DimensionType.CODEC, it).partialOrThrow
                // }
            } catch (e: IOException) {
                throw IllegalArgumentException("Invalid lemaddon!", e)
            }
        }
    }
}