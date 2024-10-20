package me.senseiwells.glide.item

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.GlideMod.id
import net.casual.arcade.items.ArcadeModelledItem
import net.casual.arcade.items.ItemModeller
import net.casual.arcade.items.ResourcePackItemModeller
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class GlideItem private constructor(): Item(Properties()), ArcadeModelledItem {
    override fun getPolymerItem(stack: ItemStack, player: ServerPlayer?): Item {
        return Items.WHITE_STAINED_GLASS
    }

    override fun getModeller(): ItemModeller {
        return MODELLER
    }

    companion object {
        val MODELLER = ResourcePackItemModeller(GlideItem(), GlideMod.resourcePack.getCreator())
        val STATIC_THERMAL by MODELLER.model(id("item/thermals/weak_thermal"))
        val DYNAMIC_THERMAL by MODELLER.model(id("item/thermals/strong_thermal"))
        val BOOST by MODELLER.model(id("item/boosts/boost"))
        val OMNI_BOOST by MODELLER.model(id("item/boosts/omni_boost"))
        val FAKE_BEAM by MODELLER.model(id("item/checkpoint/fake_beam"))
        val FAKE_BEAM_SHELL by MODELLER.model(id("item/checkpoint/fake_beam_shell"))
        val BLUE_RING by MODELLER.model(id("item/rings/blue"))
        val YELLOW_RING by MODELLER.model(id("item/rings/yellow"))
        val GREEN_RING by MODELLER.model(id("item/rings/green"))
    }
}