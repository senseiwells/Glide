package me.senseiwells.glide.module

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import me.senseiwells.glide.GlideMod
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.registry.RegistryEventHandler
import net.casual.arcade.events.server.registry.RegistryLoadedFromResourcesEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.data.MinigameDataModule.Provider.Companion.register
import net.casual.arcade.minigame.data.MinigameDataModules
import net.casual.arcade.minigame.data.MinigameDataModules.Companion.get
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.utils.file.ReadableArchive
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.DimensionType
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

object GlideModules {
    private val builtin = GlideMod.container.findPath("data/glide/maps").get()
    private val builtinLobby = GlideMod.container.findPath("data/glide/lobby").get()

    private val modules = Object2ObjectOpenHashMap<Identifier, MinigameDataModules>()

    fun registerEvents() {
        GlobalEventHandler.Server.register<ServerStartEvent>(::onServerStart)
        RegistryEventHandler.register(Registries.DIMENSION_TYPE, ::onRegisterDimensionTypes)

        GlideAcceleratorsData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideCheckpointsData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideLobbyData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideDimensionTypeData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideMetadataData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideMusicData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideResourcePackData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
        GlideRingsData.register(MinigameRegistries.MINIGAME_DATA_MODULE_PROVIDER)
    }

    fun get(id: Identifier): MinigameDataModules? {
        return this.modules[id]
    }

    fun ids(): ObjectSet<Identifier> {
        return this.modules.keys
    }

    fun lobby(server: MinecraftServer): MinigameDataModules {
        return MinigameDataModules.from(ReadableArchive.from(this.builtinLobby), server)
    }

    private fun onServerStart(event: ServerStartEvent) {
        for (path in this.paths()) {
            val archive = ReadableArchive.from(path)
            val module = MinigameDataModules.from(archive, event.server)
            val data = module.get<GlideMetadataData>()
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
            event.register(GlideMetadataData.getId(archive), dimension)
        }
    }

    private fun paths(): List<Path> {
        // TODO: Allow custom paths
        return this.builtin.listDirectoryEntries()
    }
}