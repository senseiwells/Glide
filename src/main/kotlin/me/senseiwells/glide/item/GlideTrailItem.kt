package me.senseiwells.glide.item

import com.mojang.serialization.Codec
import eu.pb4.polymer.core.api.item.PolymerItem
import eu.pb4.polymer.core.api.other.PolymerComponent
import me.senseiwells.glide.GlideMod.id
import net.fabricmc.fabric.api.networking.v1.context.PacketContext
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.DyedItemColor

class GlideTrailItem(properties: Properties): Item(properties), PolymerItem {
    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.LEATHER_HORSE_ARMOR
    }

    override fun getPolymerItemStack(
        stack: ItemStack,
        flag: TooltipFlag,
        context: PacketContext,
        lookup: HolderLookup.Provider
    ): ItemStack {
        val copy = super.getPolymerItemStack(stack, flag, context, lookup)
        copy.set(DataComponents.DYED_COLOR, DyedItemColor(stack.getOrDefault(TRAIL_COLOR, 0xFFFFFF)))
        return copy
    }

    companion object {
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