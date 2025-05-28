package com.megatrex4;

import com.megatrex4.client.ChainRenderer;
import com.megatrex4.client.ClientChainData;
import com.megatrex4.network.ChainSyncPacket;
import com.megatrex4.network.NetworkRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ChainedPlayersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[Client] Initializing chain renderer...");
        ChainRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(ChainSyncPacket.ID, (client, handler, buf, responseSender) -> {
            try {
                ChainSyncPacket packet = ChainSyncPacket.read(buf);
                System.out.println("[Client] Received chain sync packet with " + packet.pairs.size() + " pairs");

                client.execute(() -> {
                    ClientChainData.chainedPlayers.clear();
                    for (var pair : packet.pairs) {
                        ClientChainData.chainedPlayers.put(pair.getLeft(), pair.getRight());
                        ClientChainData.chainedPlayers.put(pair.getRight(), pair.getLeft());
                        System.out.println("[Client] Added chain pair: " + pair.getLeft() + " <-> " + pair.getRight());
                    }
                    System.out.println("[Client] Total chained players: " + ClientChainData.chainedPlayers.size());
                });
            } catch (Exception e) {
                System.err.println("[Client] Error reading chain sync packet: " + e.getMessage());
                e.printStackTrace();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkRegistry.MOVEMENT_PACKET_ID, (client, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            // Handle movement packet if needed
        });
    }
}