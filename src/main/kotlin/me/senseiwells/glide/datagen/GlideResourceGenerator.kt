package me.senseiwells.glide.datagen

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import eu.pb4.polymer.resourcepack.api.ResourcePackCreator
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.datagen.language.LanguageGenerator
import net.casual.arcade.datagen.resource.ArcadeResourceGenerator
import net.casual.arcade.resources.ArcadeResourcePacks
import net.casual.arcade.utils.JsonUtils
import net.minecraft.client.Minecraft
import kotlin.io.path.writer

class GlideResourceGenerator: ArcadeResourceGenerator {
    override fun id(): String {
        return GlideMod.MOD_ID
    }

    override fun resources(): Collection<ResourcePackCreator> {
        return listOf(
            GlideMod.resourcePack.getCreator(),
            ArcadeResourcePacks.SPACING_FONT_PACK.getCreator()
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

        val array = JsonArray()
        for (char in '!'..'~') {
            val width = client.font.width(char.toString())
            if (width != 6) {
                val json = JsonObject()
                json.addProperty("char", char.code)
                json.addProperty("width", width)
                array.add(json)
            }
        }
        this.getDataPath().resolve("char_width_lookup.json").writer().use {
            JsonUtils.encode(array, it)
        }
    }
}