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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
			// Temporarily disable any movement restrictions or chaining for the player after respawn
			if (CHAIN_MANAGER.isChained(player)) {
				ServerPlayerEntity partner = CHAIN_MANAGER.getChainedPartner(player);
				if (partner != null) {
					// Temporarily unchain the player to allow free movement until fully synchronized
					CHAIN_MANAGER.unchainPlayers(player);
					System.out.println("[Player " + player.getName().getString() + "] Temporarily unchained after respawn.");

					if (partner.isAlive()) {
						playerDeathSync.teleportPlayerToPartner(player, partner);

						CHAIN_MANAGER.chainPlayers(player, partner);
						System.out.println("[Player " + player.getName().getString() + "] Re-chained to partner " + partner.getName().getString() + " after teleport.");
					} else {
						System.out.println("[Player " + player.getName().getString() + "] Partner is not alive, no re-chain.");
					}
				}
			}
		});

		// Register the server tick event handler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var entry : PlayerChainManager.getChainedPlayers().entrySet()) {
				ServerPlayerEntity player1 = (ServerPlayerEntity) entry.getKey();
				ServerPlayerEntity player2 = (ServerPlayerEntity) entry.getValue();

				PlayerChainManager chainManager = new PlayerChainManager();
				chainManager.chainPlayers(player1, player2);

				// Ensure players remain chained
				if (!CHAIN_MANAGER.isChained(player1)) {
					CHAIN_MANAGER.chainPlayers(player1, player2);
					System.out.println("[Player " + player1.getName().getString() + "] Re-chained with partner " + player2.getName().getString() + ".");
				}

				int distance = ModConfig.BOTH.chainLength;

//				restrictMovement(player1, player2, distance);
				MovementPacket packet1 = new MovementPacket(player1.getX(), player1.getY(), player1.getZ());
				movementHandler.handleMovementPacket(packet1, player1.networkHandler);

				MovementPacket packet2 = new MovementPacket(player2.getX(), player2.getY(), player2.getZ());
				movementHandler.handleMovementPacket(packet2, player2.networkHandler);
			}

		});
	}









}
