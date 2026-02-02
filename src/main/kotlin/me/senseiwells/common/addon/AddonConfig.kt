package me.senseiwells.common.addon

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

open class AddonConfig(
    val id: Identifier,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String
)