package me.senseiwells.common.addon

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

open class AddonConfig(
    val addon_id: Identifier,
    val name: Component,
    val description: Component,
    val authors: List<String>,
    val version: String,
    val addon_type: String,
    val addon_pack: String

    //TODO update to use same addon format as Heirloom
    // see: https://github.com/Legacy-Edition-Minigames/Heirloom/blob/heirloom/src/main/java/net/kyrptonaught/heirloom/addon/type,
    // note: I think themeId in the GRF coresponds to the resource pack used? have to check.
)