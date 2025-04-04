package me.senseiwells.glide

import me.senseiwells.glide.addon.GlideAddons
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.GlideExtensions
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigameFactory
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.host.GlobalPackHost
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.resources.ArcadeResourcePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.utils.ResourcePackUtils.addFont
import net.casual.arcade.resources.utils.ResourcePackUtils.addLangsFromData
import net.casual.arcade.resources.utils.ResourcePackUtils.addMissingItemModels
import net.casual.arcade.resources.utils.ResourcePackUtils.addPack
import net.casual.arcade.resources.utils.ResourcePackUtils.addSounds
import net.casual.arcade.resources.utils.ResourcePackUtils.sendResourcePack
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.codec.CodecProvider.Companion.register
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val container = FabricLoader.getInstance().getModContainer(MOD_ID).get()

    val resourcePack = NamedResourcePackCreator.named("glide_pack") {
        addAssetSource(MOD_ID)
        addFont(GlideFont)
        addFont(GlideFont.Timer)
        addFont(GlideFont.Speedometer)
        addFont(GlideFont.Progressbar)
        addFont(GlideFont.Score)
        addFont(GlideFont.Banner)
        addSounds(GlideSounds)
        addLangsFromData(MOD_ID)
        addMissingItemModels(MOD_ID)
        packDescription = Component.literal("GlidePack")
    }

    fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
    }

    override fun onInitialize() {
        GlideItems.noop()
        GlideEntities.noop()

        GlideRegistries.load()
        GlideExtensions.register()
        GlideAddons.load()

        GlideMinigameFactory.register(MinigameRegistries.MINIGAME_FACTORY)

        val packDir = FabricLoader.getInstance().gameDir.resolve("glide_packs")
        val glidePack by GlobalPackHost.addPack(packDir, resourcePack).toPackInfo()
        val spacesPack by GlobalPackHost.addPack(packDir, ArcadeResourcePacks.SPACING_FONT_PACK).toPackInfo()
        val shiftedPack by GlobalPackHost.addPack(packDir, ArcadeResourcePacks.ACTION_BAR_FONT_PACK).toPackInfo()

        GlobalEventHandler.Server.register<PlayerJoinEvent> { (player) ->
            player.sendResourcePack(glidePack)
            player.sendResourcePack(spacesPack)
            player.sendResourcePack(shiftedPack)
        }
    }
}