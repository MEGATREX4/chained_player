package com.megatrex4;

import com.megatrex4.network.MovementPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class ChainMovementHandler {

    private final MinecraftClient client = MinecraftClient.getInstance();

    public void sendMovementUpdate(double x, double y, double z) {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            MovementPacket packet = new MovementPacket(x, y, z);
//            System.out.println("Sending MovementPacket: " + x + ", " + y + ", " + z);
            client.getNetworkHandler().sendPacket(packet);
        }
    }

}
