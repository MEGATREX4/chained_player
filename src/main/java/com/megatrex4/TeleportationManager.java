package com.megatrex4;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class TeleportationManager {
    private static final Map<ServerPlayerEntity, TeleportationState> teleportationStates = new HashMap<>();

    public static void setInTeleportationState(ServerPlayerEntity player, boolean state) {
        teleportationStates.put(player, new TeleportationState(state));
    }

    public static boolean isInTeleportationState(ServerPlayerEntity player) {
        return teleportationStates.getOrDefault(player, new TeleportationState(false)).isInTeleportation();
    }

    public static void onTeleportationDone(ServerPlayerEntity player) {
        teleportationStates.put(player, new TeleportationState(false));
    }

    public static boolean isWithinRadius(ServerPlayerEntity player, ServerPlayerEntity partner, double radius) {
        if (player == null || partner == null) return false;

        double squaredDistance = player.squaredDistanceTo(partner);
        double squaredRadius = radius * radius;
        return squaredDistance <= squaredRadius;
    }

    public static void scheduleTeleportationCheck(ServerPlayerEntity player, ServerPlayerEntity partner, double maxDistance) {
        teleportationStates.put(player, new TeleportationState(true));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TeleportationState state = teleportationStates.get(player);
            if (state != null && state.isInTeleportation()) {
                if (isWithinRadius(player, partner, maxDistance)) {
                    onTeleportationDone(player);

                    // Use the existing instance
                    PlayerChainManager manager = ChainedPlayers.CHAIN_MANAGER;
                    if (manager != null) {
                        manager.rechainPlayers(player, partner);
                        System.out.println("[Player " + player.getName().getString() + "] Successfully re-chained.");
                    }
                }
            }
        });
    }


    private static class TeleportationState {
        private final boolean inTeleportation;

        public TeleportationState(boolean inTeleportation) {
            this.inTeleportation = inTeleportation;
        }

        public boolean isInTeleportation() {
            return inTeleportation;
        }
    }
}
