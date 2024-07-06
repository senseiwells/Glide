package me.senseiwells.common.addon

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

open class AddonConfig(
    val id: ResourceLocation,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String
)