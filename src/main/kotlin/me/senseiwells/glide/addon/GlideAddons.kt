package me.senseiwells.glide.addon

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object GlideAddons {
    private val path = FabricLoader.getInstance().configDir.resolve("glide-addons")

    private val addons = HashMap<ResourceLocation, GlideAddon>()

    fun load() {
        if (!this.path.exists()) {
            this.path.createDirectories()
            return
        }
        for (path in this.path.listDirectoryEntries("*.lemaddon")) {
            val addon = GlideAddon.from(path)
            this.addons[addon.config.id] = addon
        }
    }

    fun get(id: ResourceLocation): GlideAddon? {
        return this.addons[id]
    }
}