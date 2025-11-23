package com.bmfalkye.game;

import com.bmfalkye.util.ModLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для восстановления игровых сессий после отключения игрока
 * Обеспечивает надежное восстановление состояния игры при переподключении
 */
public class ReconnectManager {
    // Храним информацию о сессиях для восстановления (UUID игрока -> данные сессии)
    private static final Map<UUID, ReconnectData> pendingReconnects = new ConcurrentHashMap<>();
    private static final long RECONNECT_TIMEOUT_MS = 5 * 60 * 1000; // 5 минут
    
    /**
     * Сохраняет данные сессии для возможного восстановления
     * Вызывается при отключении игрока во время активной игры
     */
    public static void saveSessionForReconnect(ServerPlayer player, FalkyeGameSession session) {
        if (player == null || session == null) {
            ModLogger.warn("Attempted to save reconnect data with null player or session");
            return;
        }
        
        // Валидация сессии перед сохранением
        if (!isSessionValidForReconnect(session)) {
            ModLogger.warn("Session is not valid for reconnect", 
                "player", player.getName().getString(),
                "gameEnded", session.isGameEnded(),
                "roundEnded", session.isRoundEnded());
            return;
        }
        
        UUID playerId = player.getUUID();
        ReconnectData data = new ReconnectData(session, System.currentTimeMillis());
        pendingReconnects.put(playerId, data);
        
        ModLogger.logGameEvent("Session saved for reconnect", 
            "player", player.getName().getString(),
            "round", session.getCurrentRound(),
            "isVillagerGame", session.isPlayingWithVillager());
        
        // Уведомляем второго игрока (если есть) об отключении
        notifyOpponentAboutDisconnect(session, player);
    }
    
    /**
     * Пытается восстановить сессию для игрока после переподключения
     * @return true если сессия успешно восстановлена, false в противном случае
     */
    public static boolean tryReconnect(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        UUID playerId = player.getUUID();
        ReconnectData data = pendingReconnects.get(playerId);
        
        if (data == null) {
            return false; // Нет сохранённой сессии
        }
        
        // Проверяем, не истёк ли таймаут
        long timeSinceSave = System.currentTimeMillis() - data.getSaveTime();
        if (timeSinceSave > RECONNECT_TIMEOUT_MS) {
            ModLogger.logGameEvent("Reconnect timeout expired", 
                "player", player.getName().getString(),
                "timeSinceSave", (timeSinceSave / 1000) + " seconds");
            pendingReconnects.remove(playerId);
            return false;
        }
        
        FalkyeGameSession session = data.getSession();
        
        // Валидация сессии перед восстановлением
        if (!validateSessionForReconnect(session, player)) {
            pendingReconnects.remove(playerId);
            return false;
        }
        
        // Восстанавливаем сессию в зависимости от роли игрока
        boolean reconnected = false;
        
        if (session.getPlayer1() != null && session.getPlayer1().getUUID().equals(playerId)) {
            // Игрок - player1
            reconnected = restoreSessionForPlayer1(player, session);
        } else if (session.getPlayer2() != null && session.getPlayer2().getUUID().equals(playerId)) {
            // Игрок - player2
            reconnected = restoreSessionForPlayer2(player, session);
        } else {
            // Игрок не участвует в сессии
            ModLogger.warn("Player not found in session for reconnect", 
                "player", player.getName().getString());
            pendingReconnects.remove(playerId);
            return false;
        }
        
        if (reconnected) {
            pendingReconnects.remove(playerId);
            ModLogger.logGameEvent("Session reconnected successfully", 
                "player", player.getName().getString(),
                "round", session.getCurrentRound());
        }
        
        return reconnected;
    }
    
    /**
     * Восстанавливает сессию для player1
     */
    private static boolean restoreSessionForPlayer1(ServerPlayer player, FalkyeGameSession session) {
        try {
            // Обновляем ссылку на игрока в сессии (на случай, если объект изменился)
            // Примечание: FalkyeGameSession может не иметь сеттера для player1,
            // поэтому мы просто обновляем регистрацию в GameManager
            
            // Регистрируем сессию в GameManager
            UUID playerId = player.getUUID();
            GameManager.getActiveGames().put(playerId, session);
            
            if (session.isPlayingWithVillager()) {
                GameManager.getActiveVillagerGames().put(playerId, session);
            }
            
            // Открываем игровой экран с использованием безопасной отправки пакетов
            com.bmfalkye.network.NetworkHandler.openGameScreen(player, session);
            
            // Отправляем уведомление об успешном реконнекте
            player.sendSystemMessage(Component.literal("§a§lИгра восстановлена! Добро пожаловать обратно!"));
            com.bmfalkye.network.NetworkHandler.addActionLog(player, "§aИгра восстановлена после переподключения");
            
            // Уведомляем второго игрока (если есть) о реконнекте
            notifyOpponentAboutReconnect(session, player);
            
            return true;
            
        } catch (Exception e) {
            ModLogger.error("Failed to restore session for player1", e,
                "player", player.getName().getString());
            return false;
        }
    }
    
    /**
     * Восстанавливает сессию для player2
     */
    private static boolean restoreSessionForPlayer2(ServerPlayer player, FalkyeGameSession session) {
        try {
            // Регистрируем сессию в GameManager
            UUID playerId = player.getUUID();
            GameManager.getActiveGames().put(playerId, session);
            
            // Открываем игровой экран
            com.bmfalkye.network.NetworkHandler.openGameScreen(player, session);
            
            // Отправляем уведомление об успешном реконнекте
            player.sendSystemMessage(Component.literal("§a§lИгра восстановлена! Добро пожаловать обратно!"));
            com.bmfalkye.network.NetworkHandler.addActionLog(player, "§aИгра восстановлена после переподключения");
            
            // Уведомляем первого игрока о реконнекте
            notifyOpponentAboutReconnect(session, player);
            
            return true;
            
        } catch (Exception e) {
            ModLogger.error("Failed to restore session for player2", e,
                "player", player.getName().getString());
            return false;
        }
    }
    
    /**
     * Валидирует сессию перед сохранением для реконнекта
     */
    private static boolean isSessionValidForReconnect(FalkyeGameSession session) {
        if (session == null) {
            return false;
        }
        
        // Не сохраняем завершённые игры
        if (session.isGameEnded()) {
            return false;
        }
        
        // Проверяем, что есть хотя бы один игрок
        if (session.getPlayer1() == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Валидирует сессию перед восстановлением
     */
    private static boolean validateSessionForReconnect(FalkyeGameSession session, ServerPlayer player) {
        if (session == null) {
            ModLogger.warn("Session is null during reconnect validation", 
                "player", player.getName().getString());
            return false;
        }
        
        // Проверяем, что сессия ещё активна
        if (session.isGameEnded()) {
            ModLogger.logGameEvent("Session ended during reconnect", 
                "player", player.getName().getString());
            player.sendSystemMessage(Component.literal("§cИгра уже завершена. Восстановление невозможно."));
            return false;
        }
        
        // Проверяем, что сессия не слишком старая
        // (это дополнительная проверка, основная проверка таймаута уже выполнена)
        
        // Проверяем, что игрок действительно участвует в сессии
        UUID playerId = player.getUUID();
        boolean isPlayer1 = session.getPlayer1() != null && session.getPlayer1().getUUID().equals(playerId);
        boolean isPlayer2 = session.getPlayer2() != null && session.getPlayer2().getUUID().equals(playerId);
        
        if (!isPlayer1 && !isPlayer2) {
            ModLogger.warn("Player not found in session during reconnect", 
                "player", player.getName().getString());
            return false;
        }
        
        // Проверяем, что второй игрок (если есть) всё ещё онлайн или это игра с villager
        if (!session.isPlayingWithVillager() && session.getPlayer2() != null) {
            ServerPlayer player2 = session.getPlayer2();
            if (player2 == null || !player2.isAlive() || 
                (player2.level() instanceof ServerLevel level && 
                 level.getServer().getPlayerList().getPlayer(player2.getUUID()) == null)) {
                // Второй игрок тоже отключился
                // GameTickHandler обработает зависшие игры и завершит их при необходимости
                ModLogger.warn("Opponent also disconnected", 
                    "player", player.getName().getString());
                // Разрешаем реконнект - GameTickHandler обработает зависшие игры
            }
        }
        
        return true;
    }
    
    /**
     * Уведомляет противника об отключении игрока
     */
    private static void notifyOpponentAboutDisconnect(FalkyeGameSession session, ServerPlayer disconnectedPlayer) {
        if (session.isPlayingWithVillager()) {
            // Для игр с villager не нужно уведомлять
            return;
        }
        
        ServerPlayer opponent = null;
        if (session.getPlayer1() != null && !session.getPlayer1().getUUID().equals(disconnectedPlayer.getUUID())) {
            opponent = session.getPlayer1();
        } else if (session.getPlayer2() != null && !session.getPlayer2().getUUID().equals(disconnectedPlayer.getUUID())) {
            opponent = session.getPlayer2();
        }
        
        if (opponent != null && opponent.isAlive()) {
            opponent.sendSystemMessage(Component.literal("§e" + disconnectedPlayer.getName().getString() + 
                " отключился. Игра будет восстановлена при переподключении."));
            com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                "§eПротивник отключился. Ожидание переподключения...");
        }
    }
    
    /**
     * Уведомляет противника о реконнекте игрока
     */
    private static void notifyOpponentAboutReconnect(FalkyeGameSession session, ServerPlayer reconnectedPlayer) {
        if (session.isPlayingWithVillager()) {
            // Для игр с villager не нужно уведомлять
            return;
        }
        
        ServerPlayer opponent = null;
        if (session.getPlayer1() != null && !session.getPlayer1().getUUID().equals(reconnectedPlayer.getUUID())) {
            opponent = session.getPlayer1();
        } else if (session.getPlayer2() != null && !session.getPlayer2().getUUID().equals(reconnectedPlayer.getUUID())) {
            opponent = session.getPlayer2();
        }
        
        if (opponent != null && opponent.isAlive()) {
            opponent.sendSystemMessage(Component.literal("§a" + reconnectedPlayer.getName().getString() + 
                " переподключился! Игра продолжается."));
            com.bmfalkye.network.NetworkHandler.addActionLog(opponent, 
                "§aПротивник переподключился!");
            
            // Обновляем состояние игры для противника
            com.bmfalkye.network.NetworkHandler.updateGameStateImmediate(opponent, session);
        }
    }
    
    /**
     * Удаляет сохранённую сессию (когда игра завершена)
     */
    public static void removeReconnectData(UUID playerId) {
        ReconnectData removed = pendingReconnects.remove(playerId);
        if (removed != null) {
            ModLogger.logGameEvent("Reconnect data removed", "playerId", playerId.toString());
        }
    }
    
    /**
     * Удаляет сохранённую сессию для всех игроков в сессии
     */
    public static void removeReconnectDataForSession(FalkyeGameSession session) {
        if (session == null) {
            return;
        }
        
        if (session.getPlayer1() != null) {
            removeReconnectData(session.getPlayer1().getUUID());
        }
        
        if (session.getPlayer2() != null) {
            removeReconnectData(session.getPlayer2().getUUID());
        }
    }
    
    /**
     * Очищает истёкшие данные восстановления
     */
    public static void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;
        
        for (Map.Entry<UUID, ReconnectData> entry : pendingReconnects.entrySet()) {
            long timeSinceSave = currentTime - entry.getValue().getSaveTime();
            if (timeSinceSave > RECONNECT_TIMEOUT_MS) {
                UUID playerId = entry.getKey();
                pendingReconnects.remove(playerId);
                removed++;
                
                ModLogger.logGameEvent("Expired reconnect data removed", 
                    "playerId", playerId.toString(),
                    "age", (timeSinceSave / 1000) + " seconds");
            }
        }
        
        if (removed > 0) {
            ModLogger.logGameEvent("Cleaned up expired reconnect data", "count", String.valueOf(removed));
        }
    }
    
    /**
     * Получает количество ожидающих реконнекта сессий
     */
    public static int getPendingReconnectCount() {
        return pendingReconnects.size();
    }
    
    /**
     * Проверяет, есть ли сохранённая сессия для игрока
     */
    public static boolean hasPendingReconnect(UUID playerId) {
        return pendingReconnects.containsKey(playerId);
    }
    
    /**
     * Данные для восстановления сессии
     */
    private static class ReconnectData {
        private final FalkyeGameSession session;
        private final long saveTime;
        
        public ReconnectData(FalkyeGameSession session, long saveTime) {
            this.session = session;
            this.saveTime = saveTime;
        }
        
        public FalkyeGameSession getSession() {
            return session;
        }
        
        public long getSaveTime() {
            return saveTime;
        }
    }
}

