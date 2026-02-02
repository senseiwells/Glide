package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.visuals.bossbar.SuppliedBossbar
import net.casual.arcade.visuals.transition.Transition
import net.casual.arcade.visuals.utils.elements.ComponentElements
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

abstract class GlideBanner: Transition {
    private val titleBossbar = SuppliedBossbar()
    private val shieldBossbar = SuppliedBossbar()
    private val textboxBossbar = SuppliedBossbar()
    private val textBossbar = SuppliedBossbar()

    init {
        this.shieldBossbar.setTitle(::shieldTitle)
        this.textboxBossbar.setTitle(::textboxTitle)
        this.textBossbar.setTitle(::textTitle)
    }

    fun addPlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        if (title) {
            this.titleBossbar.addPlayer(player)
        }
        if (shield) {
            this.shieldBossbar.addPlayer(player)
            this.textboxBossbar.addPlayer(player)
            this.textBossbar.addPlayer(player)
        }
    }

    fun removePlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        if (title) {
            this.titleBossbar.removePlayer(player)
        }
        if (shield) {
            this.shieldBossbar.removePlayer(player)
            this.textboxBossbar.removePlayer(player)
            this.textBossbar.removePlayer(player)
        }
    }

    fun setTitle(component: Component) {
        this.titleBossbar.setTitle(ComponentElements.of(component))
    }

    override fun beforeTransition(players: Collection<ServerPlayer>, interval: MinecraftTimeDuration, updates: Int) {
        for (player in players) {
            this.addPlayer(player, shield = false)
        }
    }

    override fun afterTransition(players: Collection<ServerPlayer>) {
        for (player in players) {
            this.removePlayer(player)
        }
        this.setTitle(Component.empty())
    }

    protected abstract fun getBannerShield(player: ServerPlayer, frame: Int): Component

    protected abstract fun getBannerText(player: ServerPlayer): Component

    protected open fun getDelay(): Int {
        return 0
    }

    private fun shieldTitle(player: ServerPlayer): Component {
        val frame = player.glide.animationTick - this.getDelay()
        if (frame < 0) {
            return CommonComponents.EMPTY
        }
        return this.getBannerShield(player, frame)
    }

    private fun textboxTitle(player: ServerPlayer): Component {
        val frame = player.glide.animationTick - 20 - this.getDelay()
        if (frame < 0) {
            return CommonComponents.EMPTY
        }
        val component = this.getBannerText(player)
        return GlideFont.Banner.textbox(component, frame).wrap().shadowless()
    }

    private fun textTitle(player: ServerPlayer): Component {
        val frame = player.glide.animationTick - 20 - this.getDelay()
        if (!GlideFont.Banner.isLastTextboxFrame(frame)) {
            return CommonComponents.EMPTY
        }
        return this.getBannerText(player)
    }
}