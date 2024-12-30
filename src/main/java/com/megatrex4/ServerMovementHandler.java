package com.megatrex4;

import com.megatrex4.network.MovementPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class ServerMovementHandler {
    public void handleMovementPacket(MovementPacket packet, ServerPlayPacketListener listener) {
        ServerPlayNetworkHandler networkHandler = (ServerPlayNetworkHandler) listener;
        ServerPlayerEntity player = networkHandler.getPlayer();

//        System.out.println("Received MovementPacket: " + packet.getX() + ", " + packet.getY() + ", " + packet.getZ());

        // Update player position
        player.setPos(packet.getX(), packet.getY(), packet.getZ());
    }

}
