package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.visuals.bossbar.SuppliedBossbar
import net.casual.arcade.visuals.elements.ComponentElements
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

abstract class GlideBanner {
    private val titleBossbar = SuppliedBossbar()
    private val shieldBossbar = SuppliedBossbar()
    private val textboxBossbar = SuppliedBossbar()

    init {
        this.shieldBossbar.setTitle(::shieldTitle)
        this.textboxBossbar.setTitle(::textboxTitle)
    }

    fun addPlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        if (title) {
            this.titleBossbar.addPlayer(player)
        }
        if (shield) {
            this.shieldBossbar.addPlayer(player)
            this.textboxBossbar.addPlayer(player)
        }
    }

    fun removePlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        if (title) {
            this.titleBossbar.removePlayer(player)
        }
        if (shield) {
            this.shieldBossbar.removePlayer(player)
            this.textboxBossbar.removePlayer(player)
        }
    }

    protected fun setTitle(component: Component) {
        this.titleBossbar.setTitle(ComponentElements.of(component))
    }

    protected abstract fun getBannerShield(player: ServerPlayer, frame: Int): Component

    protected abstract fun getBannerText(player: ServerPlayer, frame: Int): Component

    protected open fun getDelay(): Int {
        return 0
    }

    private fun shieldTitle(player: ServerPlayer): Component {
        val frame = player.glide.animationTick - this.getDelay()
        if (frame < 0) {
            return Component.empty()
        }
        return this.getBannerShield(player, frame)
    }

    private fun textboxTitle(player: ServerPlayer): Component {
        val frame = player.glide.animationTick - 20 - this.getDelay()
        if (frame < 0) {
            return Component.empty()
        }
        return Component.empty()
            .append(GlideFont.Banner.textbox(this.getBannerText(player, frame), frame))
            .shadowless()
    }
}