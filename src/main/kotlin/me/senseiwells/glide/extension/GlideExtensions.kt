package me.senseiwells.glide.extension

import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.extensions.event.PlayerExtensionEvent

object GlideExtensions {
    fun register() {
        GlobalEventHandler.register<PlayerExtensionEvent> {
            it.addExtension(::PlayerGlideExtension)
        }
    }
}