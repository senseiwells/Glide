package me.senseiwells.glide.item

import com.mojang.serialization.Codec
import eu.pb4.polymer.core.api.item.PolymerItem
import eu.pb4.polymer.core.api.other.PolymerComponent
import me.senseiwells.glide.GlideMod
import net.casual.arcade.utils.ResourcePackUtils.registerNextModel
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

class GlideTrailItem: Item(Properties()), PolymerItem {
    override fun getPolymerItem(itemStack: ItemStack, player: ServerPlayer?): Item {
        return Items.LEATHER_HORSE_ARMOR
    }

    override fun getPolymerCustomModelData(itemStack: ItemStack, player: ServerPlayer?): Int {
        return MODEL_ID
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
        private val MODEL_ID = GlideMod.resourcePack.getCreator().registerNextModel(
            Items.LEATHER_HORSE_ARMOR, GlideMod.id("item/trail/tinted")
        )

        val TRAIL_COLOR: DataComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            GlideMod.id("trail_color"),
            DataComponentType.builder<Int>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build()
        )

        init {
            PolymerComponent.registerDataComponent(TRAIL_COLOR)
        }
    }
}