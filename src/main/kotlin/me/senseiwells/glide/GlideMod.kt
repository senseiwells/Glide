package me.senseiwells.glide

import me.senseiwells.glide.addon.GlideAddons
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.extension.GlideExtensions
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.sound.GlideSounds
import me.senseiwells.glide.tracker.GlideTrail
import me.senseiwells.glide.utils.GlideRegistries
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.player.PlayerJoinEvent
import net.casual.arcade.events.server.ServerLoadedEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.minigame.Minigames
import net.casual.arcade.resources.ArcadePacks
import net.casual.arcade.resources.PackInfo
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
import net.minecraft.world.phys.Vec3
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

    lateinit var resourcePackInfo: PackInfo

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

        GlobalEventHandler.register<ServerLoadedEvent> {
            resourcePackInfo = host.getHostedPack("glide_pack")!!.toPackInfo()
        }

        val trail = listOf(
            Vec3(78.72369298093963, 6.158146761236626, 786.2425702000322),
            Vec3(78.67637642321758, 6.158146761236626, 786.2553037541774),
            Vec3(78.45644432652934, 6.158146761236626, 786.3144905884752),
            Vec3(78.09356929378403, 6.158146761236626, 786.4121453862849),
            Vec3(77.61232320886698, 6.158146761236626, 786.5416555065148),
            Vec3(77.0336444109891, 6.158146761236626, 786.6996073934483),
            Vec3(76.37825797842694, 6.158146761236626, 786.8933080355328),
            Vec3(75.66882919929104, 6.158146761236626, 787.1382541300869),
            Vec3(74.92882829290077, 6.158146761236626, 787.449260597217),
            Vec3(74.17528230807734, 6.158146761236626, 787.830069106548),
            Vec3(73.42173121214908, 6.158146761236626, 788.2803698690849),
            Vec3(72.67630314609649, 6.158146761236626, 788.7957228509744),
            Vec3(71.93799443388917, 6.158146761236626, 789.365282302489),
            Vec3(70.84126161355886, 6.158146761236626, 790.3013211499662),
            Vec3(70.11610600636234, 6.3081467671970906, 790.96927517641),
            Vec3(69.39840401329528, 6.842146788416344, 791.6683361784168),
            Vec3(68.69139172911198, 7.514386815128762, 792.3966773078511),
            Vec3(67.99587776785621, 8.23639324381872, 793.1512368950232),
            Vec3(67.31032076047448, 8.826315571260125, 793.9278235637627),
            Vec3(66.63327114246191, 9.038687609139032, 794.7228397460827),
            Vec3(65.96392933387158, 9.505141558272646, 795.5335918963236),
            Vec3(65.30226023260397, 10.153064999034234, 796.3582851388775),
            Vec3(64.65163881490443, 10.716317450821427, 797.1976765428169),
            Vec3(64.01900337929064, 10.919088333464815, 798.0546684335784),
            Vec3(63.41293452916932, 10.992085851216435, 798.9324454640758),
            Vec3(62.84131513449577, 11.01836495760702, 799.8329732934294),
            Vec3(62.06329602240914, 11.02782543590763, 801.2275678759205),
            Vec3(61.830107373364235, 11.02782543590763, 801.7043057548201),
            Vec3(61.222557968216414, 11.02782543590763, 803.167553614319),
            Vec3(60.896517357991755, 11.02782543590763, 804.1668513531704),
            Vec3(60.63158467054911, 11.02782543590763, 805.1814611609989),
            Vec3(60.426056984820775, 11.02782543590763, 806.2082818903373),
            Vec3(60.28186379787458, 11.02782543590763, 807.2439212689249),
            Vec3(60.20018717840738, 11.02782543590763, 808.284791817591),
            Vec3(60.20405333194618, 11.02782543590763, 809.3716422930952),
            Vec3(60.349353291460545, 11.02782543590763, 810.6178404428778),
            Vec3(60.637249828452326, 11.02782543590763, 811.9844265489877),
            Vec3(61.11190629642029, 11.02782543590763, 813.3750580588577),
            Vec3(61.882756904683966, 11.02782543590763, 814.5805938087663),
            Vec3(62.90181467376148, 11.02782543590763, 815.5650412693492),
            Vec3(64.11517036932962, 11.02782543590763, 816.2885509854063),
            Vec3(66.15747844770452, 11.02782543590763, 816.8052476454739),
            Vec3(67.58902713627074, 11.02782543590763, 816.7736883405184),
            Vec3(69.04782511001454, 11.02782543590763, 816.4806957199291),
            Vec3(70.52918821811454, 11.02782543590763, 815.9712101581849),
            Vec3(72.02923751505197, 11.02782543590763, 815.2824467815367),
            Vec3(73.54476084585761, 11.02782543590763, 814.4452234378093),
            Vec3(75.0730982248481, 11.02782543590763, 813.4850603882575),
            Vec3(76.61002417222194, 11.02782543590763, 812.4210619759772),
            Vec3(78.13953512016775, 11.02782543590763, 811.2573378112983),
            Vec3(79.63663439274886, 11.02782543590763, 809.9894260438548),
            Vec3(81.07584319537473, 11.02782543590763, 808.613789063875),
            Vec3(82.45904919099362, 11.02782543590763, 807.1440319398288),
            Vec3(83.61327781952869, 11.02782543590763, 805.710394364834),
            Vec3(84.52081643725563, 11.02782543590763, 804.3423521902163),
            Vec3(85.40658189965573, 11.02782543590763, 802.9145166399064),
            Vec3(86.33139085328699, 11.02782543590763, 801.4014813922482),
            Vec3(87.77745040264361, 11.02782543590763, 799.0035160411609),
            Vec3(88.71025499752261, 11.02782543590763, 797.3753842739102),
            Vec3(89.47120970756343, 11.02782543590763, 795.9840469661013),
        )

        GlobalEventHandler.register<PlayerJoinEvent> { (player) ->
            val glideTrail = GlideTrail(trail)
            glideTrail.createDisplays().forEach {
                it.startWatching(player, player.connection::send)
            }

            player.sendResourcePack(host.getHostedPack("spaces_font")!!.toPackInfo())
            player.sendResourcePack(host.getHostedPack("no_shadow")!!.toPackInfo())
        }

        Minigames.registerFactory(GlideMinigame.ID, GlideMinigame)
    }
}