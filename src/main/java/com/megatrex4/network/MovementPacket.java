package com.megatrex4.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class MovementPacket implements Packet<ServerPlayPacketListener> {
    private final double x, y, z;

    public MovementPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getter methods for the coordinates
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public void apply(ServerPlayPacketListener listener) {
        // Correct way to get player from ServerPlayNetworkHandler
        ServerPlayNetworkHandler networkHandler = (ServerPlayNetworkHandler) listener;
        ServerPlayerEntity player = networkHandler.getPlayer();

        // Update the player's position
        player.setPos(x, y, z);
    }
}
