package com.bmfalkye.collection;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система коллекционных предметов (карты, стикеры, рамки)
 */
public class CollectibleSystem {
    public enum CollectibleType {
        CARD,           // Карта
        STICKER,        // Стикер
        FRAME,          // Рамка для профиля
        TITLE,          // Титул
        EMOTE,          // Эмоция
        SKIN            // Скин карты
    }
    
    // UUID игрока -> Set ID коллекционных предметов
    private static final Map<UUID, Set<String>> playerCollectibles = new ConcurrentHashMap<>();
    
    /**
     * Добавляет коллекционный предмет игроку
     */
    public static void addCollectible(ServerPlayer player, String collectibleId, CollectibleType type) {
        playerCollectibles.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet())
            .add(collectibleId);
    }
    
    /**
     * Проверяет, есть ли у игрока предмет
     */
    public static boolean hasCollectible(ServerPlayer player, String collectibleId) {
        return playerCollectibles.getOrDefault(player.getUUID(), Collections.emptySet())
            .contains(collectibleId);
    }
    
    /**
     * Получает все коллекционные предметы игрока
     */
    public static Set<String> getCollectibles(ServerPlayer player) {
        return new HashSet<>(playerCollectibles.getOrDefault(player.getUUID(), Collections.emptySet()));
    }
}

