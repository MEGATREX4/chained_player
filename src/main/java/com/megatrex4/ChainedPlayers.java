package com.megatrex4;

import com.megatrex4.commands.ChainCommand;
import com.megatrex4.config.ModConfig;
import com.megatrex4.network.MovementPacket;
import com.megatrex4.network.NetworkRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import static com.megatrex4.MovementRestrictor.restrictMovement;

public class ChainedPlayers implements ModInitializer {

	public static String MOD_ID = "chained_players";

	public static final PlayerChainManager CHAIN_MANAGER = new PlayerChainManager();
	private final ServerMovementHandler movementHandler = new ServerMovementHandler();

	@Override
	public void onInitialize() {
		ModConfig.saveConfig();
		NetworkRegistry.registerPackets();

		// Register the command once
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ChainCommand.register(dispatcher, registryAccess);
		});

		// Register the server tick event handler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var entry : PlayerChainManager.getChainedPlayers().entrySet()) {
				ServerPlayerEntity player1 = entry.getKey();
				ServerPlayerEntity player2 = entry.getValue();

				int distance = ModConfig.BOTH.chainLength;

				restrictMovement(player1, player2, distance);
				MovementPacket packet = new MovementPacket(player1.getX(), player1.getY(), player1.getZ());
				movementHandler.handleMovementPacket(packet, player1.networkHandler);
			}
		});
	}
}
