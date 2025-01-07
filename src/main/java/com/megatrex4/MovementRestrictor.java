package com.megatrex4;

import com.megatrex4.network.NetworkUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.megatrex4.ChainedPlayers.CHAIN_MANAGER;

public class MovementRestrictor {

    // Map to store the movement restriction state for each player
    private static final Map<UUID, Boolean> playerRestrictions = new HashMap<>();

    /**
     * Checks if the player's movement is restricted.
     */
    public static boolean isRestricted(ServerPlayerEntity player) {
        return playerRestrictions.getOrDefault(player.getUuid(), false); // Returns true if restricted, false otherwise
    }

    /**
     * Sets whether the player's movement is restricted.
     */
    public static void setRestrict(ServerPlayerEntity player, boolean restricted) {
        ServerPlayerEntity partner = CHAIN_MANAGER.getChainedPartner(player);

        // Attempt to retrieve or re-fetch the partner if it's null
        if (partner == null) {
            // Log the issue, but allow the restriction to still apply to the player
            System.out.println("Partner for player " + player.getName().getString() + " is null. Cannot apply movement restriction to partner.");
        } else {
            // Apply restriction to the partner as well
            playerRestrictions.put(partner.getUuid(), restricted);
            System.out.println("Movement restriction for partner " + partner.getName().getString() + " set to " + restricted);
        }

        // Apply restriction to the player
        playerRestrictions.put(player.getUuid(), restricted);
        System.out.println("Movement restriction for " + player.getName().getString() + " set to " + restricted);
    }


    /**
     * Restricts the movement of player1 based on the distance from player2.
     */
    public static void restrictMovement(ServerPlayerEntity player1, ServerPlayerEntity player2, int maxDistance) {

        // If either player has restricted movement, don't calculate movement adjustments
        if (!isRestricted(player1) && !isRestricted(player2)) {
            return; // Early exit if either player is not to be restricted
        }

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

            NetworkUtils.forceSendMovementPacket(player1, player2, player1.getX(), player1.getY(), player1.getZ());
            player1.velocityModified = true;
            player2.velocityModified = true;
        }
    }
}
