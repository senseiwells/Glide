package me.senseiwells.glide.minigame.lobby.gui

import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyPhase
import me.senseiwells.glide.minigame.lobby.vote.GlideVote
import net.casual.arcade.guis.core.container.ContainerGui
import net.casual.arcade.utils.ItemUtils.enableGlint
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.utils.ItemUtils.styledLore
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class GlideVoteOptionsGui<T: Any>(
    player: ServerPlayer,
    private val lobby: GlideLobbyMinigame,
    private val vote: GlideVote<T>
): ContainerGui(player, GlideGuis.type(rows(vote.options().size) + 1), false) {
    private val positions = GlideGuis.slots(this.vote.options().size, rows(this.vote.options().size))

    private var page = 0

    init {
        this.setTitle(this.vote.title())
    }

    override fun onOpen() {
        this.loadFooter()
        this.loadPage()
    }

    override fun valid(): Boolean {
        return this.lobby.state.isAt(GlideLobbyPhase.Starting) && this.vote.enabled
    }

    private fun pages(): Int {
        val options = this.vote.options().size
        return ((options + this.positions.size - 1) / this.positions.size).coerceAtLeast(1)
    }

    private fun loadPage() {
        val options = this.vote.options()
        val tally = this.vote.tally()
        val current = this.vote.voteOf(this.player)

        val start = this.page * this.positions.size
        for ((index, slot) in this.positions.withIndex()) {
            val option = options.getOrNull(start + index)
            if (option == null) {
                this.clearSlot(slot)
                continue
            }
            this.setSlot(slot, this.display(option, tally[option] ?: 0, option == current)) {
                if (this.vote.cast(this.player, option)) {
                    this.loadPage()
                }
            }
        }
        this.loadPageControls()
    }

    private fun display(option: T, votes: Int, voted: Boolean): ItemStack {
        val lore = when {
            voted -> Component.translatable("glide.lobby.gui.voted")
            else -> Component.translatable("glide.lobby.gui.clickToVote")
        }
        val stack = this.vote.icon.named(this.vote.name(option))
            .styledLore(Component.translatable("glide.lobby.gui.votes", votes), lore)
        if (voted) {
            return stack.enableGlint()
        }
        return stack
    }

    private fun loadFooter() {
        val footer = this.getContainerSize() - GlideGuis.ROW
        for (slot in footer..< this.getContainerSize()) {
            this.setSlot(slot, GlideGuis.filler())
        }

        val key = if (this.getParent() == null) "glide.lobby.gui.close" else "glide.lobby.gui.back"
        this.setSlot(footer + GlideGuis.ROW / 2, Items.BARRIER.named(Component.translatable(key))) {
            this.openParentOrClose()
        }
    }

    private fun loadPageControls() {
        val pages = this.pages()
        if (pages <= 1) {
            return
        }
        val footer = this.getContainerSize() - GlideGuis.ROW
        val previous = this.page > 0
        val next = this.page < pages - 1
        this.setSlot(footer, this.control(previous, "glide.lobby.gui.previous")) {
            if (previous) {
                this.page--
                this.loadPage()
            }
        }
        this.setSlot(footer + GlideGuis.ROW - 1, this.control(next, "glide.lobby.gui.next")) {
            if (next) {
                this.page++
                this.loadPage()
            }
        }
    }

    private fun control(enabled: Boolean, key: String): ItemStack {
        val item = if (enabled) Items.ARROW else Items.DYE.gray
        return item.named(Component.translatable(key, this.page + 1, this.pages()))
    }

    companion object {
        private const val MAX_ROWS = 5

        fun <T: Any> of(
            player: ServerPlayer,
            lobby: GlideLobbyMinigame,
            vote: GlideVote<T>
        ): GlideVoteOptionsGui<T> {
            return GlideVoteOptionsGui(player, lobby, vote)
        }

        private fun rows(options: Int): Int {
            return GlideGuis.rows(options).coerceAtMost(MAX_ROWS)
        }
    }
}
