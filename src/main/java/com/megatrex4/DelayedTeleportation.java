package com.megatrex4;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class DelayedTeleportation {
    private final ServerPlayerEntity partner;
    private final ServerWorld destination;
    private final double x;
    private final double y;
    private final double z;

    // Constructor to accept the parameters
    public DelayedTeleportation(ServerPlayerEntity partner, ServerWorld destination, double x, double y, double z) {
        this.partner = partner;
        this.destination = destination;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Method to schedule the teleportation
    public void schedule() {
        partner.teleport(destination, x, y, z, partner.getYaw(), partner.getPitch());
    }
}
