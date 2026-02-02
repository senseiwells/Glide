package me.senseiwells.glide.gui

import me.senseiwells.glide.font.GlideComponents
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.minigame.GlideRoundMode
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.resources.utils.withShiftedDownFont
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.component.color
import net.casual.arcade.utils.component.shadowless
import net.casual.arcade.utils.component.wrap
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

// TODO:
//   - When a player finishes another banner is displayed
//     - Either displaying their time
//     - Or 'personal best'
//   - After X amount of players have finished 'hurry' starts
//     - All not finished players displayed 'hurry' banner
//     - Countdown for 1 minute begins
//   - Once round ends all players shown banner either with
//     their times/score, or DNF
//   - If the round is the last round the previous banner is replaced
//     with whoever won the round 'foobar won!'
class GlideStartBanner(private val round: GlideRoundMode): GlideBanner() {
    init {
        this.setTitle(GlideComponents.STARTING.withShiftedDownFont(12))
    }

    override fun getBannerShield(player: ServerPlayer, frame: Int): Component {
        return GlideFont.Banner.elytraShield(frame).wrap().shadowless()
    }

    override fun getBannerText(player: ServerPlayer): Component {
        // return Component.literal("foo bar xyz").color(0x444444).shadowless().withShiftedDownFont(29)
        return this.round.getStartBanner().color(0x444444).shadowless().withShiftedDownFont(29)
    }

    override fun getDelay(): Int {
        return 10
    }

    override fun updateTransition(
        players: Collection<ServerPlayer>,
        current: Int,
        total: Int,
        remaining: MinecraftTimeDuration
    ) {
        this.setTitle(GlideComponents.STARTING_IN.generate(total - current).withShiftedDownFont(12))
        for (player in players) {
            player.sendSound(GlideSounds.COUNTDOWN_TICK)
        }
    }

    override fun afterTransition(players: Collection<ServerPlayer>) {
        for (player in players) {
            this.removePlayer(player, title = false)
            player.sendSound(GlideSounds.COUNTDOWN_START)
        }
        this.setTitle(GlideComponents.STARTED.withShiftedDownFont(12))
        GlobalTickedScheduler.schedule(2.Seconds) {
            super.afterTransition(players)
        }
    }
}