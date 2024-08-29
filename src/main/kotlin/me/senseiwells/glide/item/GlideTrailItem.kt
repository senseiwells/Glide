package me.senseiwells.glide.item

import com.mojang.serialization.Codec
import eu.pb4.polymer.core.api.other.PolymerComponent
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.GlideMod.id
import net.casual.arcade.items.ArcadeModelledItem
import net.casual.arcade.items.ItemModeller
import net.casual.arcade.items.ResourcePackItemModeller
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.DyedItemColor

class GlideTrailItem private constructor(): Item(Properties()), ArcadeModelledItem {
    override fun getPolymerItem(stack: ItemStack, player: ServerPlayer?): Item {
        return Items.LEATHER_HORSE_ARMOR
    }

    override fun getModeller(): ItemModeller {
        return MODELLER
    }

    override fun getPolymerItemStack(
        stack: ItemStack,
        flag: TooltipFlag,
        lookup: HolderLookup.Provider,
        player: ServerPlayer?
    ): ItemStack {
        val copy = super.getPolymerItemStack(stack, flag, lookup, player)
        copy.set(DataComponents.DYED_COLOR, DyedItemColor(stack.getOrDefault(TRAIL_COLOR, 0xFFFFFF), false))
        return copy
    }

    companion object {
        val MODELLER = ResourcePackItemModeller(GlideTrailItem(), GlideMod.resourcePack.getCreator())
        val TRAIL by MODELLER.model(id("item/trail/tinted"))
        val GHOST by MODELLER.model(id("item/trail/ghost"))
        val RIGHT_WING by MODELLER.model(id("item/trail/ghost_wing_right"))
        val LEFT_WING by MODELLER.model(id("item/trail/ghost_wing_left"))

        val TRAIL_COLOR: DataComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("trail_color"),
            DataComponentType.builder<Int>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build()
        )

        init {
            PolymerComponent.registerDataComponent(TRAIL_COLOR)
        }
    }
}