package com.bmfalkye.game;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Таймер хода (1 минута 30 секунд)
 * Оптимизирован для производительности с использованием кэширования и потокобезопасных структур
 */
public class TurnTimer {
    private static final int TURN_TIME_SECONDS = 90; // 1 минута 30 секунд
    private static final Map<UUID, Long> turnStartTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> timeOutCounts = new ConcurrentHashMap<>(); // Счётчик пропусков по таймауту
    
    // Кэш для результатов проверки времени (обновляется каждые 5 секунд)
    private static final Map<UUID, CachedTimeResult> timeCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5000; // 5 секунд
    
    public static void startTurn(FalkyeGameSession session) {
        UUID currentPlayerUUID = session.getCurrentPlayerUUID();
        if (currentPlayerUUID != null) {
            turnStartTimes.put(currentPlayerUUID, System.currentTimeMillis());
        }
    }
    
    /**
     * Получает оставшееся время с использованием кэша для оптимизации
     */
    public static int getRemainingTime(FalkyeGameSession session) {
        UUID currentPlayerUUID = session.getCurrentPlayerUUID();
        if (currentPlayerUUID == null) {
            return 0;
        }
        
        // Проверяем кэш
        CachedTimeResult cached = timeCache.get(currentPlayerUUID);
        long currentTime = System.currentTimeMillis();
        
        if (cached != null && (currentTime - cached.cacheTime) < CACHE_TTL_MS) {
            // Используем кэшированное значение, но обновляем его с учётом прошедшего времени
            long timeSinceCache = (currentTime - cached.cacheTime) / 1000;
            int remaining = cached.remainingTime - (int)timeSinceCache;
            return Math.max(0, remaining);
        }
        
        // Вычисляем новое значение
        Long startTime = turnStartTimes.get(currentPlayerUUID);
        if (startTime == null) {
            int remaining = TURN_TIME_SECONDS;
            timeCache.put(currentPlayerUUID, new CachedTimeResult(remaining, currentTime));
            return remaining;
        }
        
        long elapsed = (currentTime - startTime) / 1000;
        int remaining = TURN_TIME_SECONDS - (int)elapsed;
        remaining = Math.max(0, remaining);
        
        // Сохраняем в кэш
        timeCache.put(currentPlayerUUID, new CachedTimeResult(remaining, currentTime));
        
        return remaining;
    }
    
    public static boolean isTimeUp(FalkyeGameSession session) {
        return getRemainingTime(session) <= 0;
    }
    
    public static void checkAndAutoPass(FalkyeGameSession session) {
        if (isTimeUp(session) && !session.isRoundEnded() && !session.isGameEnded()) {
            ServerPlayer currentPlayer = session.getCurrentPlayer();
            if (currentPlayer != null) {
                // Увеличиваем счётчик пропусков
                UUID playerUUID = currentPlayer.getUUID();
                int timeOutCount = timeOutCounts.getOrDefault(playerUUID, 0) + 1;
                timeOutCounts.put(playerUUID, timeOutCount);
                
                // Автоматически пасуем при истечении времени
                session.pass(currentPlayer);
                currentPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cВремя вышло! Автоматический пас. Пропусков: " + timeOutCount + "/3"));
                
                // Если 3 пропуска - проигрыш
                if (timeOutCount >= 3) {
                    currentPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cВы проиграли из-за 3 пропусков по таймауту!"));
                    // Завершаем игру с поражением
                    session.forceGameEnd(currentPlayer, false);
                }
            } else if (session.isPlayingWithVillager() && session.isVillagerTurn()) {
                // Для villager тоже пасуем при истечении времени
                session.passVillager();
            }
        }
    }
    
    /**
     * Сбросить счётчик пропусков для игрока
     */
    public static void resetTimeOutCount(UUID playerUUID) {
        timeOutCounts.remove(playerUUID);
    }
    
    /**
     * Получить количество пропусков игрока
     */
    public static int getTimeOutCount(UUID playerUUID) {
        return timeOutCounts.getOrDefault(playerUUID, 0);
    }
    
    public static void endTurn(FalkyeGameSession session) {
        UUID currentPlayerUUID = session.getCurrentPlayerUUID();
        if (currentPlayerUUID != null) {
            turnStartTimes.remove(currentPlayerUUID);
            timeCache.remove(currentPlayerUUID); // Очищаем кэш
        }
    }
    
    /**
     * Очищает кэш для указанного игрока
     */
    public static void clearCache(UUID playerUUID) {
        timeCache.remove(playerUUID);
    }
    
    /**
     * Очищает все истёкшие записи из кэша
     */
    public static void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        timeCache.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue().cacheTime) > CACHE_TTL_MS * 2); // Удаляем записи старше 10 секунд
    }
    
    /**
     * Очищает все данные таймера для указанного игрока
     */
    public static void clearPlayerData(UUID playerUUID) {
        turnStartTimes.remove(playerUUID);
        timeCache.remove(playerUUID);
        // timeOutCounts не очищаем, так как они нужны для отслеживания пропусков
    }
    
    /**
     * Очищает все данные таймера (используется при выключении сервера)
     */
    public static void clearAll() {
        turnStartTimes.clear();
        timeOutCounts.clear();
        timeCache.clear();
    }
    
    /**
     * Кэшированный результат проверки времени
     */
    private static class CachedTimeResult {
        final int remainingTime;
        final long cacheTime;
        
        CachedTimeResult(int remainingTime, long cacheTime) {
            this.remainingTime = remainingTime;
            this.cacheTime = cacheTime;
        }
    }
}

