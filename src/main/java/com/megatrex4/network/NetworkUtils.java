package com.megatrex4.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class NetworkUtils {
    /**
     * Force sends the movement packet to the player and their partner.
     *
     * @param player   The main player to send the packet to.
     * @param partner  The partner player to send the packet to.
     * @param x        The x-coordinate to update to.
     * @param y        The y-coordinate to update to.
     * @param z        The z-coordinate to update to.
     */
    public static void forceSendMovementPacket(ServerPlayerEntity player, ServerPlayerEntity partner, double x, double y, double z) {
        MovementPacket packet = new MovementPacket(x, y, z);

        // Send packet to the main player
        ServerPlayNetworking.send(player, NetworkRegistry.MOVEMENT_PACKET_ID, packet.toPacketByteBuf());

        // Send packet to the partner
        if (partner != null) {
            ServerPlayNetworking.send(partner, NetworkRegistry.MOVEMENT_PACKET_ID, packet.toPacketByteBuf());
        }
    }
}