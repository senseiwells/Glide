package me.senseiwells.glide.datagen

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.pack.GlidePacks
import net.casual.arcade.datagen.language.LanguageGenerator
import net.casual.arcade.datagen.resource.ArcadeResourceGenerator
import net.casual.arcade.pack.generation.BuiltInResourcePacks
import net.casual.arcade.pack.generation.PackDefinition
import net.minecraft.client.Minecraft

class GlideResourceGenerator: ArcadeResourceGenerator {
    override fun id(): String {
        return GlideMod.MOD_ID
    }

    override fun resources(): Collection<PackDefinition> {
        return listOf(
            GlidePacks.MAIN,
            BuiltInResourcePacks.SPACING_FONT_PACK
        )
    }

    override fun run(client: Minecraft) {
        val fonts = this.getAssetsPath().resolve("textures").resolve("font")
        val generator = LanguageGenerator(GlideFont.SUPPORTED_LANGS).apply {
            add(TextboxGenerator(GlideComponents.SOLO_ROUND) { lang ->
                fonts.resolve(GlideComponents.soloRoundTextbox(lang))
            })
            add(TextboxGenerator(GlideComponents.NUMBERED_ROUND.generate("")) { lang ->
                fonts.resolve(GlideComponents.numberedRoundTextbox(lang))
            })
            add(TextboxGenerator(GlideComponents.PERSONAL_BEST) { lang ->
                fonts.resolve(GlideComponents.personalBestTextbox(lang))
            })
            add(TextboxGenerator(GlideComponents.HURRY_UP) { lang ->
                fonts.resolve(GlideComponents.hurryUpTextbox(lang))
            })
            add(TextboxGenerator(GlideComponents.DNF) { lang ->
                fonts.resolve(GlideComponents.dnfTextbox(lang))
            })
            add(TextboxGenerator(GlideComponents.PLAYER_WON.generate("")) { lang ->
                fonts.resolve(GlideComponents.playerWonTextbox(lang))
            })
        }
        generator.replaceLangs(client, this.getDataPath().resolve("lang"))
    }
}