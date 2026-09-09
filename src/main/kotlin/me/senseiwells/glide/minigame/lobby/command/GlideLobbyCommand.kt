package me.senseiwells.glide.minigame.lobby.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.glide.minigame.command.GlideCommand
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyPhase
import me.senseiwells.glide.minigame.lobby.gui.GlideSettingsGui
import me.senseiwells.glide.minigame.lobby.gui.GlideVoteGui
import me.senseiwells.glide.minigame.lobby.gui.GlideVoteOptionsGui
import me.senseiwells.glide.minigame.lobby.vote.GlideVote
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.fail
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.success
import net.casual.arcade.minigame.utils.MinigameUtils.requiresAdminOrPermission
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

class GlideLobbyCommand(private val lobby: GlideLobbyMinigame): CommandTree<CommandSourceStack> {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return GlideCommand.create(buildContext).apply {
            literal("start") {
                requiresAdminOrPermission()
                executes(::start)
            }
            literal("settings") {
                requiresAdminOrPermission()
                executes(::openSettings)
            }
            literal("ready") {
                executes(::ready)
            }
            literal("vote") {
                executes(::openVote)
            }
        }
    }

    private fun start(context: CommandContext<CommandSourceStack>): Int {
        if (this.lobby.state.isAt(GlideLobbyPhase.Playing)) {
            return context.source.fail("The glide lobby is already playing a minigame!")
        }
        if (this.lobby.players.onlinePlayerCount == 0) {
            return context.source.fail("The glide lobby has no players!")
        }
        this.lobby.phases.set(GlideLobbyPhase.Playing)
        return context.source.success("Successfully started the glide minigame!")
    }

    private fun openSettings(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.playerOrException
        GlideSettingsGui(player, this.lobby.settings).open()
        return Command.SINGLE_SUCCESS
    }

    private fun ready(context: CommandContext<CommandSourceStack>): Int {
        if (!this.lobby.state.isAt(GlideLobbyPhase.Starting)) {
            return context.source.fail("The glide lobby isn't starting yet!")
        }
        val player = context.source.playerOrException
        if (!this.lobby.players.isPlaying(player)) {
            return context.source.fail("You aren't playing in the glide lobby!")
        }
        this.lobby.setReady(player, !this.lobby.isReady(player))
        return Command.SINGLE_SUCCESS
    }

    private fun openVote(context: CommandContext<CommandSourceStack>): Int {
        if (!this.lobby.state.isAt(GlideLobbyPhase.Starting)) {
            return context.source.fail("There isn't a glide vote running right now!")
        }
        val open = this.lobby.votes.all().filter(GlideVote<*>::enabled)
        if (open.isEmpty()) {
            return context.source.fail("There isn't anything to vote for!")
        }
        val player = context.source.playerOrException
        val gui = when (val vote = open.singleOrNull()) {
            null -> GlideVoteGui(player, this.lobby)
            else -> GlideVoteOptionsGui.of(player, this.lobby, vote)
        }
        gui.open()
        return Command.SINGLE_SUCCESS
    }
}
