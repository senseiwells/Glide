package me.senseiwells.glide.addon

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.common.addon.AddonConfig
import me.senseiwells.glide.addon.serialization.BoosterTemplate
import me.senseiwells.glide.addon.serialization.RingTemplate
import me.senseiwells.glide.addon.serialization.ThermalTemplate
import me.senseiwells.glide.checkpoint.Checkpoint
import me.senseiwells.glide.checkpoint.CheckpointManager
import net.casual.arcade.utils.math.location.Location
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.ResourceLocation

class GlideAddonConfig(
    id: ResourceLocation,
    name: Component,
    description: Component,
    authors: List<String>,
    version: String,
    val spawn: Location,
    val boosters: List<BoosterTemplate>,
    val thermals: List<ThermalTemplate>,
    private val allCheckpoints: List<Checkpoint>,
    val rings: List<RingTemplate>
): AddonConfig(id, name, description, authors, version) {
    val checkpoints by lazy { CheckpointManager(this.allCheckpoints) }

    companion object {
        val CODEC: Codec<GlideAddonConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(GlideAddonConfig::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(GlideAddonConfig::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(GlideAddonConfig::description),
                Codec.STRING.listOf().fieldOf("authors").forGetter(GlideAddonConfig::authors),
                Codec.STRING.fieldOf("version").forGetter(GlideAddonConfig::version),
                Location.CODEC.fieldOf("spawn").forGetter(GlideAddonConfig::spawn),
                BoosterTemplate.CODEC.listOf().fieldOf("boosters").forGetter(GlideAddonConfig::boosters),
                ThermalTemplate.CODEC.listOf().fieldOf("thermals").forGetter(GlideAddonConfig::thermals),
                Checkpoint.CODEC.listOf().fieldOf("checkpoints").forGetter(GlideAddonConfig::allCheckpoints),
                RingTemplate.CODEC.listOf().fieldOf("rings").forGetter(GlideAddonConfig::rings)
            ).apply(instance, ::GlideAddonConfig)
        }
    }
}