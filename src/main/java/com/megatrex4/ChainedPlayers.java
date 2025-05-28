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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.megatrex4.MovementRestrictor.restrictMovement;

public class ChainedPlayers implements ModInitializer {

	public static String MOD_ID = "chained_players";

	public static final PlayerChainManager CHAIN_MANAGER = new PlayerChainManager();
	private final ServerMovementHandler movementHandler = new ServerMovementHandler();

	// Simple tick scheduler
	private static final List<ScheduledTask> scheduledTasks = new LinkedList<>();

	private static class ScheduledTask {
		int ticksLeft;
		Runnable task;

		ScheduledTask(int delayTicks, Runnable task) {
			this.ticksLeft = delayTicks;
			this.task = task;
		}
	}

	public static void scheduleTask(int delayTicks, Runnable task) {
		scheduledTasks.add(new ScheduledTask(delayTicks, task));
	}

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

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (CHAIN_MANAGER.isTemporarilyUnchained(newPlayer)) {
				ServerPlayerEntity partner = CHAIN_MANAGER.getTempPartner(newPlayer);
				if (partner != null && partner.isAlive()) {
					// Offset by 1 block to avoid collision
					double x = partner.getX() + 1.0;
					double y = partner.getY();
					double z = partner.getZ();
					float yaw = partner.getYaw();
					float pitch = partner.getPitch();

					// Teleport the player right after respawn
					newPlayer.teleport((ServerWorld) partner.getWorld(), x, y, z, yaw, pitch);

					// Delay re-chaining by some ticks to prevent desync
					scheduleTask(30, () -> {
						CHAIN_MANAGER.rechainPlayers(newPlayer, partner);
						System.out.println("[Player " + newPlayer.getName().getString() + "] Successfully re-chained after respawn.");
					});
				}
			}
		});

		// Register the server tick event handler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Process scheduled tasks
			Iterator<ScheduledTask> iterator = scheduledTasks.iterator();
			while (iterator.hasNext()) {
				ScheduledTask task = iterator.next();
				task.ticksLeft--;
				if (task.ticksLeft <= 0) {
					task.task.run();
					iterator.remove();
				}
			}

			for (var entry : PlayerChainManager.getChainedPlayers().entrySet()) {
				ServerPlayerEntity player1 = (ServerPlayerEntity) entry.getKey();
				ServerPlayerEntity player2 = (ServerPlayerEntity) entry.getValue();

				// Ensure players remain chained
				if (!CHAIN_MANAGER.isChained(player1)) {
					CHAIN_MANAGER.chainPlayers(player1, player2, server); // Pass server as third argument
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

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			PlayerChainManager.syncChainsToAll(server);
		});
	}
}
