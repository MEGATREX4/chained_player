package com.megatrex4;

import com.megatrex4.commands.ChainCommand;
import com.megatrex4.config.ModConfig;
import com.megatrex4.network.MovementPacket;
import com.megatrex4.network.NetworkRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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

        DimensionChangeHandler.register();

		ServerPlayerEvents.AFTER_RESPAWN.register((server, player, alive) -> {
			if (CHAIN_MANAGER.isChained(player)) {
				ServerPlayerEntity partner = CHAIN_MANAGER.getChainedPartner(player);
				if (partner != null && partner.isAlive()) {
					// Teleport the player to their partner's position
					player.teleport(partner.getX(), partner.getY(), partner.getZ());
				} else {
					// If no partner is alive, teleport the player to their spawn point
					if (player.getSpawnPointPosition() != null) {
						player.teleport(player.getSpawnPointPosition().getX(), player.getSpawnPointPosition().getY(), player.getSpawnPointPosition().getZ());
					} else {
						// Default to world spawn if no spawn point is set
						player.teleport(player.getWorld().getSpawnPos().getX(), player.getWorld().getSpawnPos().getY(), player.getWorld().getSpawnPos().getZ());
					}
				}
			}
		});

		// Register the server tick event handler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var entry : PlayerChainManager.getChainedPlayers().entrySet()) {
				ServerPlayerEntity player1 = entry.getKey();
				ServerPlayerEntity player2 = entry.getValue();

				PlayerChainManager chainManager = new PlayerChainManager();
				chainManager.chainPlayers(player1, player2);


				int distance = ModConfig.BOTH.chainLength;

//				restrictMovement(player1, player2, distance);
				MovementPacket packet = new MovementPacket(player1.getX(), player1.getY(), player1.getZ());
				movementHandler.handleMovementPacket(packet, player1.networkHandler);
			}

		});
	}
}
