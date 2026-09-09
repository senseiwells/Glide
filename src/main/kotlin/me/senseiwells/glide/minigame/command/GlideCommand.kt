package me.senseiwells.glide.minigame.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.glide.minigame.*
import me.senseiwells.glide.minigame.lobby.GlideLobbyMinigame
import me.senseiwells.glide.module.GlideModules
import net.casual.arcade.commands.*
import net.casual.arcade.commands.arguments.EnumArgument
import net.casual.arcade.minigame.utils.MinigameUtils.requiresAdminOrPermission
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import java.util.*

object GlideCommand: CommandTree<CommandSourceStack> {
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
        val modules = GlideModules.get(map) ?: return source.fail("No such glide map with id $map")
        val minigame = GlideMinigame.create(source.server, UUID.randomUUID(), modules, round, mode)
        for (player in players) {
            minigame.players.add(player)
        }
        if (players.isNotEmpty()) {
            minigame.start()
        }
        return source.success("Successfully created glide minigame!")
    }
}