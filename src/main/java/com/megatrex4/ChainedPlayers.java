package com.megatrex4;

import com.megatrex4.commands.ChainCommand;
import com.megatrex4.config.ModConfig;
import com.megatrex4.network.MovementPacket;
import com.megatrex4.network.NetworkRegistry;
import com.megatrex4.network.NetworkUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

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

		ServerLivingEntityEvents.AFTER_DEATH.register((livingEntity, source) -> {
			if (livingEntity instanceof ServerPlayerEntity player) {
				if (CHAIN_MANAGER.isChained(player)) {
					ServerPlayerEntity partner = CHAIN_MANAGER.getChainedPartner(player);
					if (partner != null) {
						MovementRestrictor.setRestrict(player, false);
						CHAIN_MANAGER.temporarilyUnchainPlayers(player);
						CHAIN_MANAGER.temporarilyUnchainPlayers(partner);
						System.out.println("[Player " + player.getName().getString() + "] Temporarily unchained due to death.");
					}
				}
			}
		});

		DimensionChangeHandler.register();

		ServerPlayerEvents.AFTER_RESPAWN.register((server, player, alive) -> {
			if (TeleportationManager.isInTeleportationState(player)) {
				System.out.println("[Player " + player.getName().getString() + "] Waiting for teleportation to complete before re-chaining.");
				return;
			}

			// Ensure that the player is not in a "stuck" state before re-chaining
			if (CHAIN_MANAGER.isTemporarilyUnchained(player)) {
				ServerPlayerEntity partner = CHAIN_MANAGER.getTempPartner(player);
				if (partner != null && partner.isAlive()) {
					double maxDistance = ModConfig.BOTH.chainLength;

					// Teleport and re-chain the players
					playerDeathSync.teleportPlayerToPartner(player, partner);
					TeleportationManager.scheduleTeleportationCheck(player, partner, maxDistance);
					NetworkUtils.forceSendMovementPacket(player, partner, player.getX(), player.getY(), player.getZ());

					// Ensure that the movement restrictions are properly cleared or reset
					player.velocityModified = false;
					partner.velocityModified = false;
				} else {
					System.out.println("[Player " + player.getName().getString() + "] Partner not available for re-chaining.");
				}
			}
		});






		// Register the server tick event handler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var entry : PlayerChainManager.getChainedPlayers().entrySet()) {
				ServerPlayerEntity player1 = (ServerPlayerEntity) entry.getKey();
				ServerPlayerEntity player2 = (ServerPlayerEntity) entry.getValue();

				// Ensure players remain chained
				if (!CHAIN_MANAGER.isChained(player1)) {
					CHAIN_MANAGER.chainPlayers(player1, player2);
					System.out.println("[Player " + player1.getName().getString() + "] Re-chained with partner " + player2.getName().getString() + ".");
				}

				int distance = ModConfig.BOTH.chainLength;

				restrictMovement(player1, player2, distance);
				MovementPacket packet1 = new MovementPacket(player1.getX(), player1.getY(), player1.getZ());
				movementHandler.handleMovementPacket(packet1, player1.networkHandler);

				MovementPacket packet2 = new MovementPacket(player2.getX(), player2.getY(), player2.getZ());
				movementHandler.handleMovementPacket(packet2, player2.networkHandler);
			}

		});
	}
}
