package me.senseiwells.glide.minigame.lobby.gui

import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyPhase
import me.senseiwells.glide.minigame.lobby.vote.GlideVote
import net.casual.arcade.guis.core.container.ContainerGui
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.utils.ItemUtils.styledLore
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class GlideVoteGui(
    player: ServerPlayer,
    private val lobby: GlideLobbyMinigame
): ContainerGui(player, GlideGuis.type(GlideGuis.rows(open(lobby).size) + 1), false) {
    private val votes = open(this.lobby)
    private val positions = GlideGuis.slots(this.votes.size, GlideGuis.rows(this.votes.size))

    init {
        this.setTitle(Component.translatable("glide.lobby.gui.vote"))
    }

    override fun onOpen() {
        this.load()
    }

    override fun valid(): Boolean {
        return this.lobby.state.isAt(GlideLobbyPhase.Starting)
    }

    private fun load() {
        for ((index, slot) in this.positions.withIndex()) {
            val vote = this.votes[index]
            this.setSlot(slot, this.display(vote)) {
                val gui = GlideVoteOptionsGui.of(this.player, this.lobby, vote)
                gui.setParent(this)
                gui.open()
            }
        }
        this.loadFooter()
    }

    private fun display(vote: GlideVote<*>): ItemStack {
        val lore = when (val voted = vote.voteNameOf(this.player)) {
            null -> Component.translatable("glide.lobby.gui.notVoted")
            else -> Component.translatable("glide.lobby.gui.yourVote", voted)
        }
        return vote.icon.named(vote.title()).styledLore(lore)
    }

    private fun loadFooter() {
        val footer = this.getContainerSize() - GlideGuis.ROW
        for (slot in footer..< this.getContainerSize()) {
            this.setSlot(slot, GlideGuis.filler())
        }

        this.setSlot(footer + GlideGuis.ROW / 2, Items.BARRIER.named(Component.translatable("glide.lobby.gui.close"))) {
            this.openParentOrClose()
        }
    }

    companion object {
        private fun open(lobby: GlideLobbyMinigame): List<GlideVote<*>> {
            return lobby.votes.all().filter(GlideVote<*>::enabled)
        }
    }
}
