package com.megatrex4;

import net.minecraft.server.network.ServerPlayerEntity;

public class MovementRestrictor {

    public static void restrictMovement(ServerPlayerEntity player1, ServerPlayerEntity player2, int maxDistance) {
        double distance = player1.getPos().distanceTo(player2.getPos());
        if (distance > maxDistance) {

            double dx = player2.getX() - player1.getX();
            double dy = player2.getY() - player1.getY();
            double dz = player2.getZ() - player1.getZ();

            // Normalize direction vector
            double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double dirX = dx / magnitude;
            double dirY = dy / magnitude;
            double dirZ = dz / magnitude;

            double ratio = (distance - maxDistance) / distance;

            // Check velocities to determine movement status
            double speed1 = player1.getVelocity().length();
            double speed2 = player2.getVelocity().length();

            // Influence calculation: stationary players have higher influence
            double influence1 = speed1 < 0.01 ? 1.0 : 0.5; // Stationary player1 gets more influence
            double influence2 = speed2 < 0.01 ? 1.0 : 0.5; // Stationary player2 gets more influence

            // Adjust positions based on stationary status and influence
            if (speed1 < 0.01 && speed2 >= 0.01) {
                // Player1 is stationary, pull them more towards player2
                player1.addVelocity(dirX * ratio * influence2, dirY * ratio * influence2, dirZ * ratio * influence2);
            } else if (speed2 < 0.01 && speed1 >= 0.01) {
                // Player2 is stationary, pull them more towards player1
                player2.addVelocity(-dirX * ratio * influence1, -dirY * ratio * influence1, -dirZ * ratio * influence1);
            } else {
                // Both players are moving, apply balanced force
                player1.addVelocity(dirX * ratio * 0.5, dirY * ratio * 0.5, dirZ * ratio * 0.5);
                player2.addVelocity(-dirX * ratio * 0.5, -dirY * ratio * 0.5, -dirZ * ratio * 0.5);
            }

            // Handle airborne player scenario
            if (player1.isOnGround() && !player2.isOnGround()) {
                // Suspend player2 slightly above the ground
                player2.addVelocity(0, Math.min(0.1, -dy * ratio), 0);
            } else if (!player1.isOnGround() && player2.isOnGround()) {
                // Suspend player1 slightly above the ground
                player1.addVelocity(0, Math.min(0.1, dy * ratio), 0);
            }

            // Mark players for velocity updates
            player1.velocityModified = true;
            player2.velocityModified = true;
        }
    }
}
