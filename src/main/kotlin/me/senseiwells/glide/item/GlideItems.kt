package me.senseiwells.glide.item

import me.senseiwells.glide.GlideMod.id
import net.casual.arcade.items.ItemStackFactory
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties

object GlideItems {
    private val GLIDE_ITEM = ItemStackFactory.modeller(register("glide_item", ::GlideItem))
    private val GLIDE_TRAIL_ITEM = ItemStackFactory.modeller(register("glide_trail_item", ::GlideTrailItem))

    val STATIC_THERMAL by GLIDE_ITEM.modelled(id("thermals/weak_thermal"))
    val DYNAMIC_THERMAL by GLIDE_ITEM.modelled(id("thermals/strong_thermal"))
    val BOOST by GLIDE_ITEM.modelled(id("boosts/boost"))
    val OMNI_BOOST by GLIDE_ITEM.modelled(id("boosts/omni_boost"))
    val FAKE_BEAM by GLIDE_ITEM.modelled(id("checkpoint/fake_beam"))
    val FAKE_BEAM_SHELL by GLIDE_ITEM.modelled(id("checkpoint/fake_beam_shell"))
    val BLUE_RING by GLIDE_ITEM.modelled(id("rings/blue"))
    val YELLOW_RING by GLIDE_ITEM.modelled(id("rings/yellow"))
    val GREEN_RING by GLIDE_ITEM.modelled(id("rings/green"))

    val TRAIL by GLIDE_TRAIL_ITEM.modelled(id("trail/tinted"))
    val GHOST by GLIDE_TRAIL_ITEM.modelled(id("trail/ghost"))
    val RIGHT_WING by GLIDE_TRAIL_ITEM.modelled(id("trail/ghost_wing_right"))
    val LEFT_WING by GLIDE_TRAIL_ITEM.modelled(id("trail/ghost_wing_left"))

    fun noop() {

    }

    private fun register(path: String, provider: (Properties) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, id(path))
        val properties = Properties().setId(key)
        return Registry.register(BuiltInRegistries.ITEM, key, provider.invoke(properties))
    }
}