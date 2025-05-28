package com.megatrex4;

import com.megatrex4.network.ChainSyncPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerChainManager {
    private static final Logger LOGGER = Logger.getLogger(PlayerChainManager.class.getName());
    private static final Map<PlayerEntity, PlayerEntity> chainedPlayers = new HashMap<>();
    private static final Map<PlayerEntity, PlayerEntity> tempUnchained = new HashMap<>();

    private static final PlayerChainManager INSTANCE = new PlayerChainManager();

    public static PlayerChainManager getInstance() {
        return INSTANCE;
    }

    public static Map<PlayerEntity, PlayerEntity> getChainedPlayers() {
        return chainedPlayers;
    }

    public static Map<PlayerEntity, PlayerEntity> getTempUnchainedPlayers() {
        return tempUnchained;
    }

    // Log the current state of chainedPlayers and tempUnchained
    private void logState(String action) {
        LOGGER.info(() -> String.format(
                "[%s] ChainedPlayers size: %d, TempUnchained size: %d",
                action,
                chainedPlayers.size(),
                tempUnchained.size()
        ));
    }


    // Chain two players
    public void chainPlayers(PlayerEntity player1, PlayerEntity player2, MinecraftServer server) {
        if (isTemporarilyUnchained(player1)) {
            tempUnchained.remove(player1);
        }
        if (isTemporarilyUnchained(player2)) {
            tempUnchained.remove(player2);
        }

        // Chain the players
        chainedPlayers.put(player1, player2);
        chainedPlayers.put(player2, player1);

        // Restrict movement of both players
        MovementRestrictor.setRestrict((ServerPlayerEntity) player1, true);
        MovementRestrictor.setRestrict((ServerPlayerEntity) player2, true);

        // Sync chains to all players
        syncChainsToAll(server);

        logState("chainPlayers");
    }

    // Unchain a player
    public void unchainPlayers(PlayerEntity player, MinecraftServer server) {
        PlayerEntity partner = chainedPlayers.remove(player);
        if (partner != null) {
            chainedPlayers.remove(partner);
        }

        // Remove movement restriction for both players
        MovementRestrictor.setRestrict((ServerPlayerEntity) player, false);
        if (partner != null) {
            MovementRestrictor.setRestrict((ServerPlayerEntity) partner, false);
        }

        // Sync chains to all players
        syncChainsToAll(server);

        logState("unchainPlayers");
    }

    // Temporarily unchain a player
    public void temporarilyUnchainPlayers(PlayerEntity player) {
        PlayerEntity partner = chainedPlayers.remove(player);
        if (partner != null) {
            tempUnchained.put(player, partner);
            chainedPlayers.remove(partner); // This should already be done by the first remove
        }

        // Remove movement restriction for both players
        MovementRestrictor.setRestrict((ServerPlayerEntity) player, false);
        if (partner != null) {
            MovementRestrictor.setRestrict((ServerPlayerEntity) partner, false);
        }

        logState("temporarilyUnchainPlayers");
    }

    // Add parameters to rechain specific players
    public void rechainPlayers(ServerPlayerEntity player, ServerPlayerEntity partner) {
        if (!isChained(player) && partner != null && !isChained(partner)) {
            // Restrict movement of both players when they are rechained
            MovementRestrictor.setRestrict(player, true);
            MovementRestrictor.setRestrict(partner, true);

            // Rechain players
            chainedPlayers.put(player, partner);
            chainedPlayers.put(partner, player);

            // Remove both players from tempUnchained if they were temporarily unchained
            tempUnchained.remove(player);
            tempUnchained.remove(partner);

            logState("Re-chain");
        }
    }



    // Specific method for ServerPlayerEntity
    public ServerPlayerEntity getChainedPartner(ServerPlayerEntity player) {
        return (ServerPlayerEntity) chainedPlayers.get(player);
    }

    public ServerPlayerEntity getTempPartner(ServerPlayerEntity player) {
        return (ServerPlayerEntity) tempUnchained.get(player);
    }





    // Check if a player is chained
    public boolean isChained(PlayerEntity player) {
        return chainedPlayers.containsKey(player);
    }

    // Check if a player is temporarily unchained
    public boolean isTemporarilyUnchained(PlayerEntity player) {
        return tempUnchained.containsKey(player);
    }

    // Replace the syncChainsToAll method in PlayerChainManager.java with this:

    public static void syncChainsToAll(MinecraftServer server) {
        List<ChainSyncPacket.Pair<UUID, UUID>> pairs = new ArrayList<>();
        for (var entry : chainedPlayers.entrySet()) {
            UUID uuid1 = entry.getKey().getUuid();
            UUID uuid2 = entry.getValue().getUuid();
            if (uuid1.compareTo(uuid2) < 0) { // Only send each pair once
                pairs.add(new ChainSyncPacket.Pair<>(uuid1, uuid2));
            }
        }

        ChainSyncPacket packet = new ChainSyncPacket(pairs);

        System.out.println("[Chain Sync] Sending " + pairs.size() + " chain pairs to clients");

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Create a new buffer for each player to avoid reuse issues
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            packet.write(buf);
            ServerPlayNetworking.send(player, ChainSyncPacket.ID, buf);
        }
    }
}
