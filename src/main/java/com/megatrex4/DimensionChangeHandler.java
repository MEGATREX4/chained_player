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
    private static final Set<ServerPlayerEntity> syncingPlayers = new HashSet<>();

    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(DimensionChangeHandler::onPlayerWorldChange);
        startTeleportRemovalTask();
    }

    private static void onPlayerWorldChange(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        ServerPlayerEntity partner = (ServerPlayerEntity) PlayerChainManager.getChainedPlayers().get(player);

        if (partner == null) return;

        // If either player is already syncing, don't trigger another teleport
        if (isSyncing(player) || isSyncing(partner)) {
            System.out.println("Sync already in progress for " + player.getName().getString() + " or partner.");
            return;
        }

        // Mark both as syncing
        setSyncing(player, true);
        setSyncing(partner, true);

        // If partner is not in the destination world, teleport them
        if (partner.getWorld() != destination) {
            new DelayedTeleportation(partner, destination, player.getX(), player.getY(), player.getZ()).schedule();
            System.out.println("Teleporting partner " + partner.getName().getString() + " to " + destination.getRegistryKey());
        }

        // Schedule a check to clear syncing state when both are in the same world
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (player.getWorld() == partner.getWorld()) {
                setSyncing(player, false);
                setSyncing(partner, false);
            }
        });
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

    public static boolean isSyncing(ServerPlayerEntity player) {
        return syncingPlayers.contains(player);
    }

    private static void setSyncing(ServerPlayerEntity player, boolean syncing) {
        if (syncing) {
            syncingPlayers.add(player);
        } else {
            syncingPlayers.remove(player);
        }
    }

}
