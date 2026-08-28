package me.senseiwells.glide.pack

import me.senseiwells.glide.GlideMod.MOD_ID
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.pack.generation.BuiltInResourcePacks
import net.casual.arcade.pack.generation.PackDefinition
import net.casual.arcade.pack.generation.utils.add
import net.casual.arcade.pack.host.GlobalPackHost
import net.casual.arcade.pack.utils.PackInfoRef
import net.casual.arcade.pack.utils.ResourcePackUtils.toPackInfo
import net.minecraft.network.chat.Component

object GlidePacks {
    private lateinit var hosted: List<PackInfoRef>

    val MAIN = PackDefinition("glide_pack") {
        description = Component.literal("Resource pack for the glide minigame!")
        include(MOD_ID)
        addFont(GlideFont)
        addFont(GlideFont.Timer)
        addFont(GlideFont.Speedometer)
        addFont(GlideFont.Progressbar)
        addFont(GlideFont.Score)
        addFont(GlideFont.Banner)
        addSounds(GlideSounds)
        addLangs(MOD_ID)
        generateMissingItemModels(MOD_ID)
    }

    fun get(): List<PackInfoRef> {
        if (!this::hosted.isInitialized) {
            this.hosted = listOf(
                GlobalPackHost.add(MAIN).toPackInfo(),
                GlobalPackHost.add(BuiltInResourcePacks.SPACING_FONT_PACK).toPackInfo(),
                GlobalPackHost.add(BuiltInResourcePacks.ACTION_BAR_FONT_PACK).toPackInfo()
            )
        }
        return this.hosted
    }
}