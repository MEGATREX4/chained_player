package com.megatrex4;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

public class PlayerChainManager {
    private static final Map<ServerPlayerEntity, ServerPlayerEntity> chainedPlayers = new HashMap<>();

    public static Map<ServerPlayerEntity, ServerPlayerEntity> getChainedPlayers() {
        return chainedPlayers;
    }

    public void chainPlayers(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        chainedPlayers.put(player1, player2);
        chainedPlayers.put(player2, player1);
    }

    public void unchainPlayers(ServerPlayerEntity player) {
        ServerPlayerEntity partner = chainedPlayers.remove(player);
        if (partner != null) {
            chainedPlayers.remove(partner);
        }
    }

    public ServerPlayerEntity getChainedPartner(ServerPlayerEntity player) {
        return chainedPlayers.get(player);
    }

    public boolean isChained(ServerPlayerEntity player) {
        return chainedPlayers.containsKey(player);
    }
}

