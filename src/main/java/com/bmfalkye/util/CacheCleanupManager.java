package com.bmfalkye.util;

import com.bmfalkye.client.ClientPacketHandler;
import com.bmfalkye.client.gui.CardTextures;
import com.bmfalkye.statistics.StatisticsCache;

/**
 * Менеджер для централизованной очистки всех кэшей в моде
 * Обеспечивает периодическую очистку кэшей для оптимизации производительности
 */
public class CacheCleanupManager {
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 10 * 60 * 1000; // 10 минут
    
    /**
     * Выполняет очистку всех кэшей
     * Вызывается периодически для поддержания производительности
     */
    public static void cleanupAllCaches() {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем, не слишком ли часто вызываем очистку
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        lastCleanupTime = currentTime;
        
        try {
            // Очищаем кэш текстур (клиентская сторона)
            cleanupTextureCaches();
            
            // Очищаем кэш статистики (серверная сторона)
            cleanupStatisticsCache();
            
            ModLogger.logGameEvent("Cache cleanup completed", 
                "textureCache", "cleared",
                "statisticsCache", "cleaned");
                
        } catch (Exception e) {
            ModLogger.error("Error during cache cleanup", e,
                "error", e.getMessage());
        }
    }
    
    /**
     * Очищает кэши текстур
     */
    private static void cleanupTextureCaches() {
        try {
            // Очищаем кэш текстур карт (клиентская сторона)
            // Проверяем, что мы на клиенте
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                CardTextures.clearCache();
                
                // Очищаем кэш коллекции карт
                ClientPacketHandler.clearCardCollectionCache();
            }
            
            ModLogger.logGameEvent("Texture caches cleared");
        } catch (Exception e) {
            ModLogger.warn("Error clearing texture caches", 
                "error", e.getMessage());
        }
    }
    
    /**
     * Очищает кэш статистики
     */
    private static void cleanupStatisticsCache() {
        try {
            // Очищаем истёкшие записи из кэша статистики
            StatisticsCache.cleanup();
            
            ModLogger.logGameEvent("Statistics cache cleaned");
        } catch (Exception e) {
            ModLogger.warn("Error cleaning statistics cache", 
                "error", e.getMessage());
        }
    }
    
    /**
     * Принудительная очистка всех кэшей
     * Используется при критических ситуациях (нехватка памяти, перезагрузка ресурсов)
     */
    public static void forceCleanupAllCaches() {
        try {
            // Очищаем кэш текстур (клиентская сторона)
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                CardTextures.clearCache();
                ClientPacketHandler.clearCardCollectionCache();
            }
            
            // Полностью очищаем кэш статистики
            StatisticsCache.clear();
            
            // Сбрасываем время последней очистки
            lastCleanupTime = 0;
            
            ModLogger.logGameEvent("Force cache cleanup completed");
        } catch (Exception e) {
            ModLogger.error("Error during force cache cleanup", e,
                "error", e.getMessage());
        }
    }
    
    /**
     * Получает время последней очистки
     */
    public static long getLastCleanupTime() {
        return lastCleanupTime;
    }
    
    /**
     * Проверяет, нужно ли выполнить очистку
     */
    public static boolean shouldCleanup() {
        return System.currentTimeMillis() - lastCleanupTime >= CLEANUP_INTERVAL_MS;
    }
}

