package me.senseiwells.glide.extension

import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.utils.register
import net.casual.arcade.extensions.event.PlayerExtensionEvent

object GlideExtensions {
    fun registerEvents() {
        GlobalEventHandler.Server.register<PlayerExtensionEvent> {
            it.addExtension(::PlayerGlideExtension)
        }
    }
}