package me.senseiwells.glide.gui

import me.senseiwells.glide.extension.PlayerGlideExtension.Companion.glide
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.observer.utils.asObserver
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.virtual.visuals.bossbar.DynamicVirtualBossbar
import net.casual.arcade.virtual.visuals.bossbar.VirtualBossbar
import net.casual.arcade.virtual.visuals.transition.Transition
import net.casual.arcade.virtual.visuals.utils.startObservingAndSendPackets
import net.casual.arcade.virtual.visuals.utils.stopObservingAndSendPackets
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

abstract class GlideBanner(server: MinecraftServer): Transition {
    private val titleBossbar = VirtualBossbar()
    private val shieldBossbar = DynamicVirtualBossbar(server)
    private val textboxBossbar = DynamicVirtualBossbar(server)
    private val textBossbar = DynamicVirtualBossbar(server)

    init {
        this.shieldBossbar.setTitle(::shieldTitle)
        this.textboxBossbar.setTitle(::textboxTitle)
        this.textBossbar.setTitle(::textTitle)
    }

    fun tick() {
        this.titleBossbar.tick()
        this.shieldBossbar.tick()
        this.textboxBossbar.tick()
        this.textBossbar.tick()
    }

    fun addPlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        val observer = player.asObserver()
        if (title) {
            this.titleBossbar.startObservingAndSendPackets(observer)
        }
        if (shield) {
            this.shieldBossbar.startObservingAndSendPackets(observer)
            this.textboxBossbar.startObservingAndSendPackets(observer)
            this.textBossbar.startObservingAndSendPackets(observer)
        }
    }

    fun removePlayer(player: ServerPlayer, title: Boolean = true, shield: Boolean = true) {
        val observer = player.asObserver()
        if (title) {
            this.titleBossbar.stopObservingAndSendPackets(observer)
        }
        if (shield) {
            this.shieldBossbar.stopObservingAndSendPackets(observer)
            this.textboxBossbar.stopObservingAndSendPackets(observer)
            this.textBossbar.stopObservingAndSendPackets(observer)
        }
    }

    fun setTitle(component: Component) {
        this.titleBossbar.title.set(component)
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