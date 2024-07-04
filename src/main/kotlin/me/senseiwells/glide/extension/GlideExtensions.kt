package me.senseiwells.glide.extension

import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.player.PlayerExtensionEvent

object GlideExtensions {
    fun register() {
        GlobalEventHandler.register<PlayerExtensionEvent> {
            it.addExtension(PlayerGlideExtension(it.player))
        }
    }
}