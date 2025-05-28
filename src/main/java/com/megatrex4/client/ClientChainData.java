package com.megatrex4.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChainData {
    public static final Map<UUID, UUID> chainedPlayers = new ConcurrentHashMap<>();
}