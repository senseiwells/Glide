package me.senseiwells.glide

import me.senseiwells.glide.module.GlideModules
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.GlideExtensions
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigameFactory
import me.senseiwells.glide.minigame.GlideStats
import me.senseiwells.glide.minigame.command.GlideCommand
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.commands.register
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerRegisterCommandEvent
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.pack.generation.BuiltInResourcePacks
import net.casual.arcade.pack.generation.PackDefinition
import net.casual.arcade.pack.generation.utils.add
import net.casual.arcade.pack.host.GlobalPackHost
import net.casual.arcade.pack.utils.ResourcePackUtils.sendResourcePack
import net.casual.arcade.pack.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.Identifier
import net.casual.arcade.utils.serialization.codec.CodecProvider.Companion.register
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val container = FabricLoader.getInstance().getModContainer(MOD_ID).get()

    fun id(path: String): Identifier {
        return Identifier(MOD_ID, path)
    }

    override fun onInitialize() {
        GlideStats.noop()
        GlideItems.noop()
        GlideEntities.noop()

        GlideRegistries.load()

        GlideExtensions.registerEvents()
        GlideModules.registerEvents()

        GlideMinigameFactory.register(MinigameRegistries.MINIGAME_FACTORY)

        GlobalEventHandler.Server.register<ServerRegisterCommandEvent> {
            it.register(GlideCommand)
        }
    }
}