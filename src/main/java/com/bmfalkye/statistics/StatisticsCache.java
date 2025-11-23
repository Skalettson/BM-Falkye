package com.bmfalkye.statistics;

import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.util.CacheManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Кэш для статистики игроков
 */
public class StatisticsCache {
    private static final CacheManager<UUID, PlayerProgress> STATS_CACHE = 
        new CacheManager<>(10 * 60 * 1000, 200); // 10 минут TTL, максимум 200 записей
    
    /**
     * Получает статистику из кэша или загружает её
     */
    public static PlayerProgress getStatistics(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        PlayerProgress cached = STATS_CACHE.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Загружаем из хранилища
        ServerLevel level = (ServerLevel) player.level();
        if (level == null) {
            return null;
        }
        
        com.bmfalkye.storage.PlayerProgressStorage storage = 
            com.bmfalkye.storage.PlayerProgressStorage.get(level);
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        if (progress != null) {
            STATS_CACHE.put(playerId, progress);
        }
        
        return progress;
    }
    
    /**
     * Инвалидирует кэш для игрока
     */
    public static void invalidate(ServerPlayer player) {
        STATS_CACHE.remove(player.getUUID());
    }
    
    /**
     * Обновляет кэш для игрока
     */
    public static void update(ServerPlayer player, PlayerProgress progress) {
        STATS_CACHE.put(player.getUUID(), progress);
    }
    
    /**
     * Очищает весь кэш
     */
    public static void clear() {
        STATS_CACHE.clear();
    }
    
    /**
     * Очищает истёкшие записи
     */
    public static void cleanup() {
        STATS_CACHE.cleanup();
    }
}

