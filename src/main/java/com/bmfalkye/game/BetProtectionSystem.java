package com.bmfalkye.game;

import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система защиты от эксплойтов в системе ставок
 */
public class BetProtectionSystem {
    // Максимальная ставка (защита от переполнения)
    private static final int MAX_BET_AMOUNT = 100000;
    
    // Минимальная ставка
    private static final int MIN_BET_AMOUNT = 1;
    
    // Отслеживание заблокированных ставок: UUID игрока -> сумма заблокированной ставки
    private static final Map<UUID, Integer> lockedBets = new ConcurrentHashMap<>();
    
    // Отслеживание времени блокировки ставки: UUID игрока -> время блокировки (мс)
    private static final Map<UUID, Long> betLockTime = new ConcurrentHashMap<>();
    
    // Таймаут блокировки ставки (5 минут)
    private static final long BET_LOCK_TIMEOUT = 5 * 60 * 1000;
    
    /**
     * Блокирует ставку с защитой от эксплойтов
     * @param player игрок
     * @param betAmount сумма ставки
     * @return true если успешно заблокировано, false если ошибка
     */
    public static boolean lockBetSafely(ServerPlayer player, int betAmount) {
        if (player == null) {
            ModLogger.warn("Attempt to lock bet for null player");
            return false;
        }
        
        UUID playerUUID = player.getUUID();
        
        // Проверка валидности суммы ставки
        if (!isValidBetAmount(betAmount)) {
            ModLogger.warn("Invalid bet amount", "player", player.getName().getString(), "amount", betAmount);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cНеверная сумма ставки! Минимум: " + MIN_BET_AMOUNT + ", Максимум: " + MAX_BET_AMOUNT));
            return false;
        }
        
        // Проверка на уже заблокированную ставку
        if (lockedBets.containsKey(playerUUID)) {
            int existingBet = lockedBets.get(playerUUID);
            ModLogger.warn("Player already has locked bet", 
                "player", player.getName().getString(), 
                "existingBet", existingBet,
                "newBet", betAmount);
            
            // Проверяем, не истёк ли таймаут
            Long lockTime = betLockTime.get(playerUUID);
            if (lockTime != null && System.currentTimeMillis() - lockTime > BET_LOCK_TIMEOUT) {
                // Таймаут истёк - разблокируем старую ставку
                ModLogger.logGameEvent("Bet lock timeout expired, unlocking old bet",
                    "player", player.getName().getString(),
                    "oldBet", existingBet);
                unlockBet(playerUUID);
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cУ вас уже есть активная ставка! Сначала завершите текущую игру."));
                return false;
            }
        }
        
        // Проверка на активную игру
        if (GameManager.getActiveGame(player) != null) {
            ModLogger.warn("Player attempted to bet while in active game",
                "player", player.getName().getString());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cВы уже в игре! Нельзя делать ставку во время активной игры."));
            return false;
        }
        
        // Блокируем ставку
        lockedBets.put(playerUUID, betAmount);
        betLockTime.put(playerUUID, System.currentTimeMillis());
        
        ModLogger.logGameEvent("Bet locked safely", 
            "player", player.getName().getString(),
            "amount", betAmount);
        
        return true;
    }
    
    /**
     * Разблокирует ставку игрока
     */
    public static void unlockBet(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        
        Integer betAmount = lockedBets.remove(playerUUID);
        betLockTime.remove(playerUUID);
        
        if (betAmount != null) {
            ModLogger.logGameEvent("Bet unlocked", 
                "playerUUID", playerUUID.toString(),
                "amount", betAmount);
        }
    }
    
    /**
     * Разблокирует ставку игрока (по объекту игрока)
     */
    public static void unlockBet(ServerPlayer player) {
        if (player != null) {
            unlockBet(player.getUUID());
        }
    }
    
    /**
     * Проверяет, заблокирована ли ставка у игрока
     */
    public static boolean hasLockedBet(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return lockedBets.containsKey(player.getUUID());
    }
    
    /**
     * Получает сумму заблокированной ставки
     */
    public static int getLockedBetAmount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return lockedBets.getOrDefault(player.getUUID(), 0);
    }
    
    /**
     * Проверяет, что ставка игрока соответствует заблокированной
     */
    public static boolean validateBetAmount(ServerPlayer player, int betAmount) {
        if (player == null) {
            return false;
        }
        
        UUID playerUUID = player.getUUID();
        Integer lockedBet = lockedBets.get(playerUUID);
        
        if (lockedBet == null) {
            // Нет заблокированной ставки - это нормально для новой ставки
            return isValidBetAmount(betAmount);
        }
        
        // Проверяем, что ставка совпадает с заблокированной
        if (lockedBet != betAmount) {
            ModLogger.warn("Bet amount mismatch", 
                "player", player.getName().getString(),
                "lockedBet", lockedBet,
                "providedBet", betAmount);
            return false;
        }
        
        return true;
    }
    
    /**
     * Валидирует сумму ставки
     */
    public static boolean isValidBetAmount(int betAmount) {
        return betAmount >= MIN_BET_AMOUNT && betAmount <= MAX_BET_AMOUNT;
    }
    
    /**
     * Получает максимальную сумму ставки
     */
    public static int getMaxBetAmount() {
        return MAX_BET_AMOUNT;
    }
    
    /**
     * Получает минимальную сумму ставки
     */
    public static int getMinBetAmount() {
        return MIN_BET_AMOUNT;
    }
    
    /**
     * Очищает истёкшие блокировки ставок (вызывается периодически)
     */
    public static void cleanupExpiredLocks() {
        long currentTime = System.currentTimeMillis();
        java.util.Iterator<Map.Entry<UUID, Long>> iterator = betLockTime.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerUUID = entry.getKey();
            Long lockTime = entry.getValue();
            
            if (lockTime != null && currentTime - lockTime > BET_LOCK_TIMEOUT) {
                Integer betAmount = lockedBets.remove(playerUUID);
                iterator.remove();
                
                ModLogger.logGameEvent("Expired bet lock cleaned up",
                    "playerUUID", playerUUID.toString(),
                    "betAmount", betAmount != null ? betAmount : 0,
                    "age", (currentTime - lockTime) + "ms");
            }
        }
    }
    
    /**
     * Очищает все блокировки ставок (для тестирования или сброса)
     */
    public static void clearAllLocks() {
        int count = lockedBets.size();
        lockedBets.clear();
        betLockTime.clear();
        ModLogger.logGameEvent("All bet locks cleared", "count", count);
    }
    
    /**
     * Проверяет, может ли игрок сделать ставку (не в игре, нет активной блокировки)
     */
    public static boolean canPlayerBet(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        // Проверка на активную игру
        if (GameManager.getActiveGame(player) != null) {
            return false;
        }
        
        // Проверка на заблокированную ставку
        UUID playerUUID = player.getUUID();
        if (lockedBets.containsKey(playerUUID)) {
            // Проверяем таймаут
            Long lockTime = betLockTime.get(playerUUID);
            if (lockTime != null && System.currentTimeMillis() - lockTime > BET_LOCK_TIMEOUT) {
                // Таймаут истёк - можно делать новую ставку
                unlockBet(playerUUID);
                return true;
            }
            return false;
        }
        
        return true;
    }
}

