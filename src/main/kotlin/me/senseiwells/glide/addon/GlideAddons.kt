package me.senseiwells.glide.addon

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object GlideAddons {
    private val path = FabricLoader.getInstance().configDir.resolve("glide-addons")

    private val addons = Object2ObjectOpenHashMap<Identifier, GlideAddon>()

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

    fun get(id: Identifier): GlideAddon? {
        return this.addons[id]
    }

    fun ids(): ObjectSet<Identifier> {
        return this.addons.keys
    }
}