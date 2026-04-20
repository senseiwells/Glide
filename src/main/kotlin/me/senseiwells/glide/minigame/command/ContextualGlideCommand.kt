package me.senseiwells.glide.minigame.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.minigame.GlidePhase
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.success
import net.casual.arcade.utils.player.username
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument

class ContextualGlideCommand(private val glide: GlideMinigame): CommandTree<CommandSourceStack> {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return GlideCommand.create(buildContext).apply {
            literal("start-ending-round") {
                executes(::startEndingRound)
            }
            literal("end-round") {
                executes(::endRound)
            }
            literal("mark-as-finished") {
                argument("target", EntityArgument.player()) {
                    executes(::markAsFinished)
                }
            }
            literal("restart") {
                executes(::restartGlide)
            }
        }
    }

    private fun startEndingRound(context: CommandContext<CommandSourceStack>): Int {
        this.glide.startEndingRound()
        return context.source.success("Successfully started ending the glide round!")
    }

    private fun endRound(context: CommandContext<CommandSourceStack>): Int {
        this.glide.setPhase(GlidePhase.Finish)
        return context.source.success("Successfully ending the glide round!")
    }

    private fun markAsFinished(context: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(context, "target")
        this.glide.markPlayerFinished(target)
        return context.source.success("Successfully marked ${target.username} as finished!")
    }

    private fun restartGlide(context: CommandContext<CommandSourceStack>): Int {
        this.glide.restart()
        return context.source.success("Successfully restarted glide!")
    }
}