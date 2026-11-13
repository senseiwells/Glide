package me.senseiwells.glide.module

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import me.senseiwells.glide.GlideMod
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.registry.RegistryEventHandler
import net.casual.arcade.events.server.registry.RegistryLoadedFromResourcesEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.data.MinigameDataProvider.Companion.register
import net.casual.arcade.minigame.data.MinigameDataSet
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.utils.file.ReadableArchive
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.DimensionType
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

object GlideModules {
    private val builtin = GlideMod.container.findPath("data/glide/maps").get()
    private val builtinLobby = GlideMod.container.findPath("data/glide/lobby").get()

    private val custom = FabricLoader.getInstance().gameDir.resolve("glide/maps")

    private val modules = Object2ObjectOpenHashMap<Identifier, MinigameDataSet>()

    fun registerEvents() {
        GlobalEventHandler.Server.register<ServerStartEvent>(::onServerStart)
        RegistryEventHandler.register(Registries.DIMENSION_TYPE, ::onRegisterDimensionTypes)

        GlideAcceleratorsData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideCheckpointsData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideLobbyData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideDimensionTypeData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideMetaData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideMusicData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideResourcePackData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
        GlideRingsData.register(MinigameRegistries.MINIGAME_DATA_PROVIDER)
    }

    fun get(id: Identifier): MinigameDataSet? {
        return this.modules[id]
    }

    fun ids(): ObjectSet<Identifier> {
        return this.modules.keys
    }

    fun lobby(server: MinecraftServer): MinigameDataSet {
        return MinigameDataSet.from(ReadableArchive.from(this.builtinLobby), server)
    }

    private fun onServerStart(event: ServerStartEvent) {
        for (path in this.paths()) {
            val archive = ReadableArchive.from(path)
            val module = MinigameDataSet.from(archive, event.server)
            val data = module.get(GlideMetaData.type)
            if (data == null) {
                GlideMod.logger.error("Failed to load glide map '${archive.name}', no glide_metadata.json was found!")
                module.close()
                continue
            }
            this.modules[data.id] = module
        }
    }

    private fun onRegisterDimensionTypes(event: RegistryLoadedFromResourcesEvent<DimensionType>) {
        for (path in this.paths()) {
            val archive = ReadableArchive.from(path)
            val dimension = GlideDimensionTypeData.getDimensionTypeIfPresent(archive, event.context) ?: continue
            event.register(GlideMetaData.getId(archive), dimension)
        }
    }

    private fun paths(): List<Path> {
        return (this.builtin.listDirectoryEntries() + this.customs())
            .filter { it.name != ".DS_Store" }
    }

    private fun customs(): List<Path> {
        if (this.custom.isDirectory()) {
            return this.custom.listDirectoryEntries()
        }
        return listOf()
    }
}