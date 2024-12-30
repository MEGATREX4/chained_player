package com.megatrex4;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DimensionChangeHandler {
    private static final Map<ServerPlayerEntity, Boolean> teleportInProgress = new HashMap<>();
    private static final Map<ServerPlayerEntity, Long> teleportStartTick = new HashMap<>(); // Map to store when teleportation started

    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(DimensionChangeHandler::onPlayerWorldChange);
        startTeleportRemovalTask();
    }

    private static void onPlayerWorldChange(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        System.out.println("Player " + player.getName().getString() + " changed dimension from " + origin.getRegistryKey() + " to " + destination.getRegistryKey());

        // Avoid teleporting the player if it's already in progress
        if (teleportInProgress.getOrDefault(player, false)) {
            System.out.println("Teleportation already in progress for " + player.getName().getString());
            return;
        }

        // Mark the teleportation as in progress for the player
        teleportInProgress.put(player, true);
        teleportStartTick.put(player, System.currentTimeMillis()); // Store the start time of teleportation

        // Get the partner from PlayerChainManager
        ServerPlayerEntity partner = PlayerChainManager.getChainedPlayers().get(player);

        if (partner != null) {
            System.out.println("Partner " + partner.getName().getString() + " teleported to " + destination.getRegistryKey());

            // If partner is in a different world, schedule the teleportation to the new world at the entering player's coordinates
            if (partner.getWorld() != destination) {
                // Use the entering player's coordinates (x, y, z)
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                // Schedule the teleportation using the DelayedTeleportation class
                new DelayedTeleportation(partner, destination, x, y, z).schedule();
                System.out.println("Scheduled partner teleportation to: " + x + ", " + y + ", " + z);
            }
        } else {
            System.out.println("No partner found for " + player.getName().getString());
        }
    }

    // Listener for server ticks
    public static void startTeleportRemovalTask() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTime = System.currentTimeMillis();

            // Create a set to hold players to remove after iteration
            Set<ServerPlayerEntity> playersToRemove = new HashSet<>(teleportInProgress.keySet()); // Copy the keys to avoid modification during iteration

            for (ServerPlayerEntity player : playersToRemove) {
                // If 2 seconds have passed since teleportation started, remove from map
                if (currentTime - teleportStartTick.getOrDefault(player, 0L) > 2000) { // 2 seconds in milliseconds
                    teleportInProgress.remove(player);
                    teleportStartTick.remove(player); // Remove start time as well
                    System.out.println("Teleportation process completed for " + player.getName().getString());
                }
            }
        });
    }

}
