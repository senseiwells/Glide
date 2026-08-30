package me.senseiwells.glide.minigame.lobby.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.glide.minigame.command.GlideCommand
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyPhase
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.fail
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.success
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

class GlideLobbyCommand(private val lobby: GlideLobbyMinigame): CommandTree<CommandSourceStack> {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return GlideCommand.create(buildContext).apply {
            literal("start") {
                executes(::start)
            }
            literal("skip-voting") {
                executes(::skipVoting)
            }
        }
    }

    private fun start(context: CommandContext<CommandSourceStack>): Int {
        if (this.lobby.phase != GlideLobbyPhase.Waiting) {
            return context.source.fail("The glide lobby has already started!")
        }
        if (this.lobby.glide != null) {
            return context.source.fail("The glide lobby is already playing a minigame!")
        }
        this.lobby.startTransition()
        return context.source.success("Successfully started the glide lobby!")
    }

    private fun skipVoting(context: CommandContext<CommandSourceStack>): Int {
        if (this.lobby.phase != GlideLobbyPhase.Voting) {
            return context.source.fail("The glide lobby isn't currently voting!")
        }
        this.lobby.setPhase(GlideLobbyPhase.Countdown)
        return context.source.success("Successfully skipped the glide lobby voting!")
    }
}
