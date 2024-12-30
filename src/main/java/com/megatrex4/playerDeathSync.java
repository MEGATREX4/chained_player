package com.megatrex4;

import com.megatrex4.config.ModConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class playerDeathSync {


    public static void teleportPlayerToPartner(ServerPlayerEntity player, ServerPlayerEntity partner) {
        if (partner != null && partner.isAlive()) {
            if (player.getWorld() != partner.getWorld()) {
                BlockPos partnerPos = new BlockPos((int)partner.getX(), (int)partner.getY(), (int)partner.getZ());

                BlockPos validPos = findNearestSolidBlock(player, partner, partnerPos.getX(), partnerPos.getY(), partnerPos.getZ(), ModConfig.BOTH.chainLength);

                if (validPos != null) {
                    new DelayedTeleportation(player, (ServerWorld) partner.getWorld(), validPos.getX(), validPos.getY(), validPos.getZ()).schedule();
                } else {
                    new DelayedTeleportation(player, (ServerWorld) partner.getWorld(), partner.getX(), partner.getY(), partner.getZ()).schedule();
                }
            } else {
                BlockPos validPos = findNearestSolidBlock(player, partner, partner.getX(), partner.getY(), partner.getZ(), ModConfig.BOTH.chainLength);

                if (validPos != null) {
                    new DelayedTeleportation(player, (ServerWorld) partner.getWorld(), validPos.getX(), validPos.getY(), validPos.getZ()).schedule();
                } else {
                    new DelayedTeleportation(player, (ServerWorld) partner.getWorld(), partner.getX(), partner.getY(), partner.getZ()).schedule();
                }
            }
        }
    }

    private static BlockPos findNearestSolidBlock(ServerPlayerEntity player, ServerPlayerEntity partner, double partnerX, double partnerY, double partnerZ, double maxDistance) {
        // Determine which world to search in
        World world = (partner != null && partner.getWorld() != player.getWorld())
                ? player.getServer().getWorld(partner.getWorld().getRegistryKey())
                : player.getWorld();

        // Convert the partner's position from double to int for BlockPos
        BlockPos partnerPos = new BlockPos((int) partnerX, (int) partnerY, (int) partnerZ);
        BlockPos nearestPos = null;

        // Start the search with the smallest radius first
        for (int distance = 1; distance <= (int) maxDistance; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int y = -distance; y <= distance; y++) {
                    for (int z = -distance; z <= distance; z++) {
                        BlockPos currentPos = partnerPos.add(x, y, z);

                        if (world.getBlockState(currentPos).isSolid() && world.getBlockState(currentPos.up()).isAir()) {
                            nearestPos = currentPos;
                            break;
                        }
                    }
                    if (nearestPos != null) break;
                }
                if (nearestPos != null) break;
            }
            if (nearestPos != null) break;
        }

        return nearestPos;
    }


}
