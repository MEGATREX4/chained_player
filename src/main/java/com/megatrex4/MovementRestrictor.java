package com.megatrex4;

import net.minecraft.server.network.ServerPlayerEntity;

public class MovementRestrictor {

    public static void restrictMovement(ServerPlayerEntity player1, ServerPlayerEntity player2, int maxDistance) {
        double distance = player1.getPos().distanceTo(player2.getPos());
        if (distance > maxDistance) {
            // Calculate direction vector
            double dx = player2.getX() - player1.getX();
            double dy = player2.getY() - player1.getY();
            double dz = player2.getZ() - player1.getZ();

            // Normalize direction vector
            double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double dirX = dx / magnitude;
            double dirY = dy / magnitude;
            double dirZ = dz / magnitude;

            double ratio = (distance - maxDistance) / distance;

            double speed1 = player1.getVelocity().length();
            double speed2 = player2.getVelocity().length();

            double influence1 = speed1 < 0.01 ? 0.8 : 0.3;
            double influence2 = speed2 < 0.01 ? 0.8 : 0.3;

            double adjustX = dirX * ratio * 0.9;
            double adjustY = dirY * ratio * 0.9;
            double adjustZ = dirZ * ratio * 0.9;

            player1.addVelocity(adjustX * influence2, adjustY * influence2, adjustZ * influence2);
            player2.addVelocity(-adjustX * influence1, -adjustY * influence1, -adjustZ * influence1);

            if (player1.isOnGround() && !player2.isOnGround()) {
                player2.addVelocity(0, Math.min(0.1, -dy * ratio * 0.2), 0);
            } else if (!player1.isOnGround() && player2.isOnGround()) {
                player1.addVelocity(0, Math.min(0.1, dy * ratio * 0.2), 0);
            }

            player1.velocityModified = true;
            player2.velocityModified = true;
        }
    }
}
