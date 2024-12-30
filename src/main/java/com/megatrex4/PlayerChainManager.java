package com.megatrex4;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class PlayerChainManager {
    private static final Map<PlayerEntity, PlayerEntity> chainedPlayers = new HashMap<>();

    public static Map<PlayerEntity, PlayerEntity> getChainedPlayers() {
        return chainedPlayers;
    }

    // Chain two players (either PlayerEntity or ServerPlayerEntity)
    public void chainPlayers(PlayerEntity player1, PlayerEntity player2) {
        chainedPlayers.put(player1, player2);
        chainedPlayers.put(player2, player1);
    }

    // Unchain a player
    public void unchainPlayers(PlayerEntity player) {
        PlayerEntity partner = chainedPlayers.remove(player);
        if (partner != null) {
            chainedPlayers.remove(partner);
        }
    }

    // Specific method for ServerPlayerEntity
    public ServerPlayerEntity getChainedPartner(ServerPlayerEntity player) {
        PlayerEntity partner = chainedPlayers.get(player);
        return (partner instanceof ServerPlayerEntity) ? (ServerPlayerEntity) partner : null;
    }

    // Check if a player is chained
    public boolean isChained(PlayerEntity player) {
        return chainedPlayers.containsKey(player);
    }
}
