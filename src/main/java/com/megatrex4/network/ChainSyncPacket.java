package com.megatrex4.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import java.util.*;

import static com.megatrex4.ChainedPlayers.MOD_ID;

public class ChainSyncPacket {
    // Make sure this matches your MOD_ID
    public static final Identifier ID = new Identifier(MOD_ID, "chain_sync");
    public final List<Pair<UUID, UUID>> pairs;

    public ChainSyncPacket(List<Pair<UUID, UUID>> pairs) {
        this.pairs = pairs;
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(pairs.size());
        for (var pair : pairs) {
            buf.writeUuid(pair.getLeft());
            buf.writeUuid(pair.getRight());
        }
    }

    public static ChainSyncPacket read(PacketByteBuf buf) {
        int size = buf.readInt();
        List<Pair<UUID, UUID>> pairs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            pairs.add(new Pair<>(buf.readUuid(), buf.readUuid()));
        }
        return new ChainSyncPacket(pairs);
    }

    // Simple Pair class if not using Apache Commons
    public static class Pair<L, R> {
        private final L left;
        private final R right;
        public Pair(L left, R right) { this.left = left; this.right = right; }
        public L getLeft() { return left; }
        public R getRight() { return right; }
    }
}