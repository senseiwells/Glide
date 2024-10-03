package me.senseiwells.glide.datagen

import eu.pb4.polymer.resourcepack.api.ResourcePackCreator
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.datagen.language.LanguageGenerator
import net.casual.arcade.datagen.language.NegativeWidthGenerator
import net.casual.arcade.datagen.resource.ArcadeResourceGenerator
import net.casual.arcade.resources.ArcadeResourcePacks
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Path

class GlideResourceGenerator: ArcadeResourceGenerator {
    override fun id(): String {
        return GlideMod.MOD_ID
    }

    override fun resources(): Collection<ResourcePackCreator> {
        return listOf(
            GlideMod.resourcePack.getCreator(),
            ArcadeResourcePacks.SPACES_FONT_PACK.getCreator()
        )
    }

    override fun run(client: Minecraft) {
        val fonts = this.getAssetsPath().resolve("textures").resolve("font")
        val generator = LanguageGenerator(GlideFont.SUPPORTED_LANGS).apply {
            addTextboxAndNegativeWidth(GlideComponents.SOLO_ROUND) { lang ->
                fonts.resolve(GlideComponents.soloRoundTextbox(lang))
            }
            addTextboxAndNegativeWidth(GlideComponents.NUMBERED_ROUND.generate(0)) { lang ->
                fonts.resolve(GlideComponents.numberedRoundTextbox(lang))
            }
        }
        generator.replaceLangs(client, this.getDataPath().resolve("lang"))
    }

    private fun LanguageGenerator.addTextboxAndNegativeWidth(component: Component, file: (String) -> Path) {
        add(TextboxGenerator(component, file))
        add(NegativeWidthGenerator(component))
    }
}