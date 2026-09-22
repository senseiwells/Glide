package me.senseiwells.glide.minigame.lobby.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.minigame.GlideMode
import me.senseiwells.glide.minigame.GlideRoundMode
import me.senseiwells.glide.minigame.MultiGlideRoundMode
import me.senseiwells.glide.minigame.SoloGlideRoundMode
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.minigame.lobby.GlideLobbyPhase
import me.senseiwells.glide.minigame.lobby.gui.GlideSettingsGui
import me.senseiwells.glide.minigame.lobby.gui.GlideVoteGui
import me.senseiwells.glide.minigame.lobby.gui.GlideVoteOptionsGui
import me.senseiwells.glide.minigame.lobby.vote.GlideVote
import me.senseiwells.glide.module.GlideModules
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.arguments.EnumArgument
import net.casual.arcade.commands.fail
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.success
import net.casual.arcade.commands.suggests
import net.casual.arcade.minigame.utils.MinigameUtils.requiresAdminOrPermission
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

class GlideLobbyCommand(private val lobby: GlideLobbyMinigame): CommandTree<CommandSourceStack> {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("glide") {
            literal("create") {
                requiresAdminOrPermission()
                literal("lobby") {
                    executes { ctx -> createGlideLobby(ctx, listOf(ctx.source.playerOrException)) }
                    argument("players", EntityArgument.players()) {
                        executes(::createGlideLobby)
                    }
                }
                literal("map") {
                    argument("map", IdentifierArgument.id()) {
                        suggests { _ -> GlideModules.ids().map(Any::toString) }
                        argument("mode", EnumArgument.enumeration<GlideMode>()) {
                            literal("solo") {
                                executes { ctx -> createSoloGlideMinigame(ctx, ctx.source.playerOrException) }
                                argument("player", EntityArgument.player()) {
                                    executes(::createSoloGlideMinigame)
                                }
                            }
                            literal("multi") {
                                executes { ctx -> createMultiGlideMinigame(ctx, listOf(ctx.source.playerOrException), 3) }
                                argument("rounds", IntegerArgumentType.integer(1, 10)) {
                                    executes { ctx -> createMultiGlideMinigame(ctx, listOf(ctx.source.playerOrException)) }
                                    argument("players", EntityArgument.players()) {
                                        executes(::createMultiGlideMinigame)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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


    private fun createGlideLobby(
        context: CommandContext<CommandSourceStack>,
        players: Collection<ServerPlayer> = EntityArgument.getPlayers(context, "players")
    ): Int {
        val source = context.source
        val lobby = GlideLobbyMinigame.create(source.server, UUID.randomUUID())
        lobby.start()
        for (player in players) {
            lobby.players.add(player)
        }
        return source.success("Successfully created glide lobby!")
    }

    private fun createSoloGlideMinigame(
        context: CommandContext<CommandSourceStack>,
        player: ServerPlayer = EntityArgument.getPlayer(context, "player")
    ): Int {
        return this.createGlideMinigame(context, SoloGlideRoundMode, players = listOf(player))
    }

    private fun createMultiGlideMinigame(
        context: CommandContext<CommandSourceStack>,
        players: Collection<ServerPlayer> = EntityArgument.getPlayers(context, "players"),
        rounds: Int = IntegerArgumentType.getInteger(context, "rounds"),
    ): Int {
        return this.createGlideMinigame(context, MultiGlideRoundMode(rounds), players = players)
    }

    private fun createGlideMinigame(
        context: CommandContext<CommandSourceStack>,
        round: GlideRoundMode,
        map: Identifier = IdentifierArgument.getId(context, "map"),
        mode: GlideMode = EnumArgument.getEnumeration(context, "mode"),
        players: Collection<ServerPlayer>
    ): Int {
        val source = context.source
        if (players.isEmpty()) {
            return source.fail("No players specified to play glide")
        }
        val modules = GlideModules.get(map) ?: return source.fail("No such glide map with id $map")
        val minigame = GlideMinigame.create(source.server, UUID.randomUUID(), modules, round, mode)
        this.lobby.initializeGlideMinigame(minigame)
        for (player in players) {
            minigame.players.add(player)
        }
        minigame.start()
        return source.success("Successfully created glide minigame!")
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
