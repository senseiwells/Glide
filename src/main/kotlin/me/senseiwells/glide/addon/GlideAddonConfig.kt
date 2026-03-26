package me.senseiwells.glide.addon

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.common.addon.AddonConfig
import me.senseiwells.glide.addon.serialization.BoosterTemplate
import me.senseiwells.glide.addon.serialization.RingTemplate
import me.senseiwells.glide.addon.serialization.ThermalTemplate
import me.senseiwells.glide.checkpoint.IndexedCheckpoints
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.serialization.codec.fieldOfAny
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import java.util.Optional

class GlideAddonConfig(
    id: Identifier,
    name: Component,
    description: Component,
    authors: List<String>,
    version: String,
    addonPack: Optional<String>,
    val spawn: Location,
    val boosters: List<BoosterTemplate>,
    val thermals: List<ThermalTemplate>,
    val checkpoints: IndexedCheckpoints,
    val rings: List<RingTemplate>
): AddonConfig(id, name, description, authors, version, "glide_map", addonPack) {
    companion object {
        val CODEC: Codec<GlideAddonConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                Identifier.CODEC.fieldOfAny("id", "addon_id").forGetter(GlideAddonConfig::addonId),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(GlideAddonConfig::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(GlideAddonConfig::description),
                Codec.STRING.listOf().fieldOf("authors").forGetter(GlideAddonConfig::authors),
                Codec.STRING.fieldOf("version").forGetter(GlideAddonConfig::version),
                Codec.STRING.optionalFieldOf("addon_pack").forGetter(GlideAddonConfig::addonPack),
                Location.CODEC.fieldOf("spawn").forGetter(GlideAddonConfig::spawn),
                BoosterTemplate.CODEC.listOf().fieldOf("boosters").forGetter(GlideAddonConfig::boosters),
                ThermalTemplate.CODEC.listOf().fieldOf("thermals").forGetter(GlideAddonConfig::thermals),
                IndexedCheckpoints.CODEC.fieldOf("checkpoints").forGetter(GlideAddonConfig::checkpoints),
                RingTemplate.CODEC.listOf().fieldOf("rings").forGetter(GlideAddonConfig::rings)
            ).apply(instance, ::GlideAddonConfig)
        }
    }
}