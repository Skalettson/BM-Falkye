package com.bmfalkye.util;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система автоматического восстановления после некритичных ошибок
 * 
 * <p>Предоставляет механизмы для автоматического восстановления состояния игры
 * после возникновения некритичных ошибок, которые не требуют завершения игры.</p>
 * 
 * <p>Поддерживаемые типы восстановления:</p>
 * <ul>
 *   <li>Восстановление игровой сессии после ошибок обработки ходов</li>
 *   <li>Восстановление прогресса игрока после ошибок сохранения</li>
 *   <li>Восстановление состояния колоды после ошибок загрузки</li>
 *   <li>Восстановление сетевого соединения после временных сбоев</li>
 * </ul>
 * 
 * @author BM Falkye Team
 */
public class ErrorRecoverySystem {
    
    /** Максимальное количество попыток восстановления для одного игрока */
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    
    /** Время в миллисекундах, после которого счётчик попыток сбрасывается */
    private static final long RECOVERY_RESET_TIME = 60000; // 1 минута
    
    /** Отслеживание попыток восстановления для каждого игрока */
    private static final Map<UUID, RecoveryAttempt> recoveryAttempts = new ConcurrentHashMap<>();
    
    /**
     * Информация о попытках восстановления для игрока
     */
    private static class RecoveryAttempt {
        int attempts = 0;
        long lastAttemptTime = System.currentTimeMillis();
        
        void increment() {
            attempts++;
            lastAttemptTime = System.currentTimeMillis();
        }
        
        boolean canAttempt() {
            if (System.currentTimeMillis() - lastAttemptTime > RECOVERY_RESET_TIME) {
                attempts = 0;
                return true;
            }
            return attempts < MAX_RECOVERY_ATTEMPTS;
        }
    }
    
    /**
     * Типы ошибок, которые можно восстановить
     */
    public enum RecoverableErrorType {
        GAME_SESSION_ERROR,      // Ошибка игровой сессии
        PROGRESS_SAVE_ERROR,     // Ошибка сохранения прогресса
        DECK_LOAD_ERROR,         // Ошибка загрузки колоды
        NETWORK_ERROR,           // Сетевая ошибка
        DATA_VALIDATION_ERROR    // Ошибка валидации данных
    }
    
    /**
     * Пытается восстановить состояние после ошибки
     * 
     * @param player игрок, для которого произошла ошибка
     * @param errorType тип ошибки
     * @param error ошибка, которая произошла
     * @return true если восстановление успешно, false в противном случае
     */
    public static boolean attemptRecovery(ServerPlayer player, RecoverableErrorType errorType, Throwable error) {
        if (player == null || errorType == null || error == null) {
            return false;
        }
        
        UUID playerUUID = player.getUUID();
        RecoveryAttempt attempt = recoveryAttempts.computeIfAbsent(playerUUID, k -> new RecoveryAttempt());
        
        // Проверяем, можно ли делать попытку восстановления
        if (!attempt.canAttempt()) {
            ModLogger.warn("Max recovery attempts reached for player",
                "player", player.getName().getString(),
                "errorType", errorType.toString(),
                "attempts", attempt.attempts);
            return false;
        }
        
        attempt.increment();
        
        try {
            boolean recovered = false;
            
            switch (errorType) {
                case GAME_SESSION_ERROR:
                    recovered = recoverGameSession(player);
                    break;
                case PROGRESS_SAVE_ERROR:
                    recovered = recoverPlayerProgress(player);
                    break;
                case DECK_LOAD_ERROR:
                    recovered = recoverPlayerDeck(player);
                    break;
                case NETWORK_ERROR:
                    recovered = recoverNetworkConnection(player);
                    break;
                case DATA_VALIDATION_ERROR:
                    recovered = recoverDataValidation(player);
                    break;
            }
            
            if (recovered) {
                ModLogger.info("Successfully recovered from error",
                    "player", player.getName().getString(),
                    "errorType", errorType.toString(),
                    "attempt", attempt.attempts);
                
                // Сбрасываем счётчик при успешном восстановлении
                attempt.attempts = 0;
                
                // Уведомляем игрока об успешном восстановлении
                sendRecoveryMessage(player, true);
                
                return true;
            } else {
                ModLogger.warn("Recovery attempt failed",
                    "player", player.getName().getString(),
                    "errorType", errorType.toString(),
                    "attempt", attempt.attempts);
            }
        } catch (Exception e) {
            ModLogger.error("Error during recovery attempt", e,
                ErrorContextCollector.collectContext(e,
                    "player", player.getName().getString(),
                    "errorType", errorType.toString(),
                    "originalError", error.getMessage()));
        }
        
        return false;
    }
    
    /**
     * Восстанавливает игровую сессию
     */
    private static boolean recoverGameSession(ServerPlayer player) {
        try {
            FalkyeGameSession session = GameManager.getActiveGame(player);
            if (session == null) {
                return false;
            }
            
            // Проверяем валидность сессии
            if (session.getPlayer1() == null || !session.getPlayer1().isAlive()) {
                // Игрок отключился или умер - завершаем игру
                GameManager.endGame(session);
                return false;
            }
            
            // Синхронизируем состояние игры
            com.bmfalkye.network.NetworkHandler.updateGameState(player, session);
            
            return true;
        } catch (Exception e) {
            ModLogger.error("Failed to recover game session", e);
            return false;
        }
    }
    
    /**
     * Восстанавливает прогресс игрока
     */
    private static boolean recoverPlayerProgress(ServerPlayer player) {
        try {
            if (player.level() instanceof ServerLevel level) {
                PlayerProgressStorage storage = PlayerProgressStorage.get(level);
                // Принудительно сохраняем прогресс
                storage.setDirty();
                return true;
            }
            return false;
        } catch (Exception e) {
            ModLogger.error("Failed to recover player progress", e);
            return false;
        }
    }
    
    /**
     * Восстанавливает колоду игрока
     */
    private static boolean recoverPlayerDeck(ServerPlayer player) {
        try {
            if (player.level() instanceof ServerLevel level) {
                // Пытаемся загрузить колоду заново
                com.bmfalkye.storage.PlayerDeckManager deckManager = 
                    com.bmfalkye.storage.PlayerDeckManager.get(level);
                // Просто проверяем доступность
                deckManager.getPlayerDecks(player);
                return true;
            }
            return false;
        } catch (Exception e) {
            ModLogger.error("Failed to recover player deck", e);
            return false;
        }
    }
    
    /**
     * Восстанавливает сетевое соединение
     */
    private static boolean recoverNetworkConnection(ServerPlayer player) {
        try {
            // Проверяем, что соединение активно
            if (player.connection != null) {
                // Проверяем, что игрок не отключился
                return player.connection.isAcceptingMessages();
            }
            return false;
        } catch (Exception e) {
            ModLogger.error("Failed to recover network connection", e);
            return false;
        }
    }
    
    /**
     * Восстанавливает валидацию данных
     */
    private static boolean recoverDataValidation(ServerPlayer player) {
        try {
            // Пытаемся перезагрузить данные игрока
            if (player.level() instanceof ServerLevel level) {
                PlayerProgressStorage storage = PlayerProgressStorage.get(level);
                // Получаем прогресс, что заставит систему перезагрузить данные
                storage.getPlayerProgress(player);
                return true;
            }
            return false;
        } catch (Exception e) {
            ModLogger.error("Failed to recover data validation", e);
            return false;
        }
    }
    
    /**
     * Отправляет сообщение игроку о восстановлении
     */
    private static void sendRecoveryMessage(ServerPlayer player, boolean success) {
        if (player == null) {
            return;
        }
        
        if (success) {
            com.bmfalkye.util.LocalizationHelper.sendLocalizedMessage(player, 
                "error.recovery.success");
        } else {
            com.bmfalkye.util.LocalizationHelper.sendLocalizedMessage(player, 
                "error.recovery.failed");
        }
    }
    
    /**
     * Сбрасывает счётчик попыток восстановления для игрока
     */
    public static void resetRecoveryAttempts(ServerPlayer player) {
        if (player != null) {
            recoveryAttempts.remove(player.getUUID());
        }
    }
    
    /**
     * Очищает устаревшие записи о попытках восстановления
     */
    public static void cleanupExpiredAttempts() {
        long currentTime = System.currentTimeMillis();
        recoveryAttempts.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastAttemptTime > RECOVERY_RESET_TIME * 2);
    }
}

