package com.megatrex4.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import static com.megatrex4.ChainedPlayers.MOD_ID;

public class NetworkRegistry {
    public static final Identifier MOVEMENT_PACKET_ID = new Identifier(MOD_ID, "movement_packet");

    public static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(MOVEMENT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            server.execute(() -> {
                // Handle packet on server
                player.setPos(x, y, z);
            });
        });
    }
}

