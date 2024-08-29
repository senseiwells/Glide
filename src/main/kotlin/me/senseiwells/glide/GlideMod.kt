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

object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    val logger: Logger = LogManager.getLogger(MOD_ID)


    val resourcePack = NamedResourcePackCreator.named("glide_pack") {
        addAssetSource(MOD_ID)
        addFont(GlideFont)
        addFont(GlideFont.Timer)
        addFont(GlideFont.Speedometer)
        addFont(GlideFont.Progressbar)
        addFont(GlideFont.Score)
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

        // GlideGRFConverter.convertSingle(
        //     id("temple"),
        //     Path.of("/Users/sensei/Documents/Coding/Kotlin/Glide/src/main/resources/rules/temple.json"),
        //     Path.of("/Users/sensei/Documents/Coding/Kotlin/Glide/src/main/resources/rules/addon.json"),
        // )
        // exitProcess(0)

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

        GlobalEventHandler.register<PlayerJoinEvent> { (player) ->
            // val pos = player.position().add(0.0, 1.0, 0.0)
            //
            // val yaw = player.yRot
            // val pitch = player.xRot
            //
            // val ghost = ItemDisplayElement(GlideTrailItem.GHOST)
            // ghost.overridePos = pos
            // ghost.yaw = yaw
            // ghost.pitch = pitch
            //
            // val left = ItemDisplayElement(GlideTrailItem.LEFT_WING)
            // left.leftRotation = Quaternionf().rotationXYZ(ELYTRA_X_ROT, -ELYTRA_Y_ROT, -ELYTRA_Z_ROT)
            // left.translation = Vector3f(.55F, .25F, -0.1F)
            // left.overridePos = pos
            // left.yaw = yaw
            // left.pitch = pitch
            //
            // val right = ItemDisplayElement(GlideTrailItem.RIGHT_WING)
            // right.leftRotation = Quaternionf().rotationXYZ(ELYTRA_X_ROT, ELYTRA_Y_ROT, ELYTRA_Z_ROT)
            // right.translation = Vector3f(-.55F, .251F, -0.1F)
            // right.overridePos = pos
            // right.yaw = yaw
            // right.pitch = pitch
            //
            // ghost.startWatching(player, player.connection::send)
            // right.startWatching(player, player.connection::send)
            // left.startWatching(player, player.connection::send)

            player.sendResourcePack(host.getHostedPack("spaces_font")!!.toPackInfo())
            player.sendResourcePack(host.getHostedPack("no_shadow")!!.toPackInfo())
            player.sendResourcePack(host.getHostedPack("glide_pack")!!.toPackInfo())
        }

        Minigames.registerFactory(GlideMinigame.ID, GlideMinigame)
    }

    // private fun calculateElytraRot(delta: Vec3): Vector3f {
    //     var xRot = Mth.PI / 12
    //     var zRot = -Mth.PI / 12
    //     var spread = 1.0f
    //     if (delta.y < 0.0) {
    //         val vec32 = delta.normalize()
    //         spread = 1.0f - (1-vec32.y).pow(1.5).toFloat()
    //     }
    //
    //     xRot = spread * (Mth.PI / 9) + (1.0f - spread) * xRot
    //     zRot = spread * (-Mth.PI / 2) + (1.0f - spread) * zRot
    //     return Vector3f(xRot + Mth.HALF_PI, 0.0F, zRot)
    // }
    //
    // const val ELYTRA_X_ROT = 1.673035F
    // const val ELYTRA_Y_ROT = 0.0F
    // const val ELYTRA_Z_ROT = 2.131606F
}