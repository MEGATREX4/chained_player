package com.megatrex4.commands;

import com.megatrex4.ChainedPlayers;
import com.megatrex4.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ChainCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // Main chain command (link two players)
        dispatcher.register(CommandManager.literal("chain")
                .then(CommandManager.argument("player2", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "player2");

                            if (context.getSource().getEntity() instanceof ServerPlayerEntity) {
                                ServerPlayerEntity player1 = (ServerPlayerEntity) context.getSource().getEntity();

                                // Prevent chaining with oneself
                                if (player1.equals(player2)) {
                                    context.getSource().sendFeedback(() -> Text.translatable("command.chain.self_link_error"), false);
                                    return 0;
                                }

                                if (ModConfig.BOTH.requireConfirmation) {
                                    sendConfirmationMessage(player1, player2);
                                } else {
                                    linkPlayers(player1, player2);
                                }
                            } else {
                                linkPlayers(null, player2);
                            }

                            return 1;
                        })
                )
        );

        // Accept command
        dispatcher.register(literal("chain")
                .then(literal("accept")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                                    ServerPlayerEntity player2 = context.getSource().getPlayer();

                                    if (player1.equals(player2)) {
                                        context.getSource().sendFeedback(() -> Text.translatable("command.chain.self_link_error"), false);
                                        return 0;
                                    }

                                    linkPlayers(player1, player2);
                                    return 1;
                                })
                        )
                )
        );

        // Deny command
        dispatcher.register(literal("chain")
                .then(literal("deny")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                                    ServerPlayerEntity player2 = context.getSource().getPlayer();

                                    player2.sendMessage(Text.translatable("command.chain.deny_message", player1.getName()), false);
                                    player1.sendMessage(Text.translatable("command.chain.deny_notification", player2.getName()), false);
                                    return 1;
                                })
                        )
                )
        );

        dispatcher.register(literal("chain")
                .then(literal("force")
                        .then(argument("force", IntegerArgumentType.integer(1, 10))
                                .executes(context -> {
                                    int newInfluence = IntegerArgumentType.getInteger(context, "influence");
                                    ModConfig.BOTH.pullForce = newInfluence;
                                    context.getSource().sendFeedback(() -> Text.translatable("command.chain.length_set", newInfluence), false);

                                    ModConfig.saveConfig();
                                    return 1;
                                })
                        )
                )
        );

        // Command to change the chain length
        dispatcher.register(literal("chain")
                .then(literal("length")
                        .then(argument("length", IntegerArgumentType.integer(1, 100)) // Ensures the value is between 1 and 100
                                .executes(context -> {
                                    int newLength = IntegerArgumentType.getInteger(context, "length");
                                    ModConfig.BOTH.chainLength = newLength;
                                    context.getSource().sendFeedback(() -> Text.translatable("command.chain.length_set", newLength), false);

                                    // Save the config after changing the chain length
                                    ModConfig.saveConfig();

                                    return 1;
                                })
                        )
                )
        );

        // Command to toggle requireConfirmation
        dispatcher.register(literal("chain")
                .then(literal("request")
                        .then(argument("value", StringArgumentType.string())
                                .executes(context -> {
                                    String value = StringArgumentType.getString(context, "value");

                                    // Check for integer inputs (1 or 0)
                                    if (value.equals("1") || value.equals("0")) {
                                        ModConfig.BOTH.requireConfirmation = (value.equals("1"));
                                        context.getSource().sendFeedback(() -> Text.translatable("command.chain.confirmation_set", (value.equals("1") ? "enabled" : "disabled")), false);
                                    }
                                    // Check for boolean inputs (true or false)
                                    else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                        ModConfig.BOTH.requireConfirmation = Boolean.parseBoolean(value);
                                        context.getSource().sendFeedback(() -> Text.translatable("command.chain.confirmation_set", (value.equalsIgnoreCase("true") ? "enabled" : "disabled")), false);
                                    } else {
                                        // Invalid input
                                        context.getSource().sendFeedback(() -> Text.translatable("command.chain.invalid_value"), false);
                                    }

                                    // Save the config after changing the request confirmation setting
                                    ModConfig.saveConfig();

                                    return 1;
                                })
                        )
                )
        );
    }

    private static void sendConfirmationMessage(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        Text message = Text.translatable("command.chain.request_message", player1.getName().getString()) // Use %s placeholder here
                .append(Text.translatable("command.chain.accept")
                        .formatted(Formatting.GREEN) // Green for accept
                        .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chain accept " + player1.getName().getString()))))
                .append(Text.literal(" ")
                        .append(Text.translatable("command.chain.deny")
                                .formatted(Formatting.RED)
                                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chain deny " + player1.getName().getString())))));

        player2.sendMessage(message, false);
    }



    private static void linkPlayers(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        if (player1 != null && player2 != null) {
            ChainedPlayers.CHAIN_MANAGER.chainPlayer(player1, player2);
            player1.sendMessage(Text.translatable("command.chain.linked", player2.getName()), false);
            player2.sendMessage(Text.translatable("command.chain.linked", player1.getName()), false);
        }
    }
}
