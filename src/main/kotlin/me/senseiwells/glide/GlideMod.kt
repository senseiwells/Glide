package me.senseiwells.glide

import me.senseiwells.glide.addon.GlideAddons
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.GlideExtensions
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.player.PlayerJoinEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.minigame.Minigames
import net.casual.arcade.resources.ArcadePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.hosting.DirectoryPackSupplier
import net.casual.arcade.resources.hosting.PackHost
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ResourcePackUtils.addFont
import net.casual.arcade.utils.ResourcePackUtils.addSounds
import net.casual.arcade.utils.ResourcePackUtils.sendResourcePack
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

// TODO:
//  - Behaviour of going back on checkpoints
//  - Booster / Thermal implementation
//  - UI implementation
object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    const val debug: Boolean = true

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val resourcePack = NamedResourcePackCreator.named("glide_pack") {
        addAssetSource(MOD_ID)
        addFont(GlideFont)
        addFont(GlideFont.Timer)
        addFont(GlideFont.Speedometer)
        addSounds(GlideSounds)
        packDescription = "GlidePack".literal()
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

        // TODO: For testing purposes
        val packDir = FabricLoader.getInstance().gameDir.resolve("glide_packs")
        resourcePack.buildTo(packDir)
        ArcadePacks.SPACES_FONT_PACK.buildTo(packDir)
        ArcadePacks.NO_SHADOW_PACK.buildTo(packDir)
        val host = PackHost()
        host.addPacks(DirectoryPackSupplier(packDir))
        host.start()
        GlobalEventHandler.register<ServerStoppingEvent> {
            host.shutdown()
        }

        GlobalEventHandler.register<PlayerJoinEvent> {
            it.player.sendResourcePack(host.getHostedPack("glide_pack")!!.toPackInfo())
            it.player.sendResourcePack(host.getHostedPack("spaces_font")!!.toPackInfo())
            it.player.sendResourcePack(host.getHostedPack("no_shadow")!!.toPackInfo())
        }

        Minigames.registerFactory(GlideMinigame.ID, GlideMinigame)
    }
}