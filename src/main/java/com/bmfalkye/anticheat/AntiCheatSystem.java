package com.bmfalkye.anticheat;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система проверки на честность игры
 * Обнаруживает и предотвращает читерство
 */
public class AntiCheatSystem {
    
    /**
     * Типы нарушений
     */
    public enum ViolationType {
        TOO_FAST_ACTION,        // Слишком быстрое действие (бот)
        INVALID_CARD_PLAY,      // Попытка сыграть невалидную карту
        OUT_OF_TURN_ACTION,     // Действие не в свой ход
        DUPLICATE_ACTION,       // Дублирование действия
        SUSPICIOUS_PATTERN,     // Подозрительный паттерн действий
        INVALID_STATE_ACCESS    // Доступ к невалидному состоянию
    }
    
    /**
     * Уровень серьёзности нарушения
     */
    public enum ViolationSeverity {
        LOW,        // Низкая (предупреждение)
        MEDIUM,     // Средняя (временная блокировка)
        HIGH,       // Высокая (длительная блокировка)
        CRITICAL    // Критическая (бан)
    }
    
    // Отслеживание времени последних действий: UUID игрока -> время последнего действия (мс)
    private static final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<>();
    
    // Отслеживание количества действий: UUID игрока -> количество действий в окне
    private static final Map<UUID, Integer> actionCount = new ConcurrentHashMap<>();
    
    // Отслеживание нарушений: UUID игрока -> количество нарушений
    private static final Map<UUID, Integer> violationCount = new ConcurrentHashMap<>();
    
    // Отслеживание времени окна для подсчёта действий: UUID игрока -> время начала окна
    private static final Map<UUID, Long> actionWindowStart = new ConcurrentHashMap<>();
    
    // Минимальное время между действиями (мс) - защита от ботов
    private static final long MIN_ACTION_INTERVAL = 100; // 100мс = минимум 10 действий в секунду
    
    // Максимальное количество действий в окне времени
    private static final int MAX_ACTIONS_PER_WINDOW = 20;
    
    // Время окна для подсчёта действий (мс)
    private static final long ACTION_WINDOW_MS = 5000; // 5 секунд
    
    // Пороги для нарушений
    private static final int LOW_VIOLATION_THRESHOLD = 3;
    private static final int MEDIUM_VIOLATION_THRESHOLD = 5;
    private static final int HIGH_VIOLATION_THRESHOLD = 10;
    private static final int CRITICAL_VIOLATION_THRESHOLD = 20;
    
    /**
     * Проверяет действие игрока на честность
     * @param player игрок
     * @param actionType тип действия
     * @param session игровая сессия
     * @return результат проверки
     */
    public static CheatCheckResult checkAction(ServerPlayer player, String actionType, FalkyeGameSession session) {
        if (player == null) {
            return new CheatCheckResult(false, ViolationType.INVALID_STATE_ACCESS, 
                ViolationSeverity.LOW, "Player is null");
        }
        
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        // Проверка 1: Скорость действий (защита от ботов)
        CheatCheckResult speedCheck = checkActionSpeed(playerUUID, currentTime);
        if (!speedCheck.isValid()) {
            recordViolation(playerUUID, speedCheck.getViolationType(), speedCheck.getSeverity());
            return speedCheck;
        }
        
        // Проверка 2: Количество действий в окне
        CheatCheckResult windowCheck = checkActionWindow(playerUUID, currentTime);
        if (!windowCheck.isValid()) {
            recordViolation(playerUUID, windowCheck.getViolationType(), windowCheck.getSeverity());
            return windowCheck;
        }
        
        // Проверка 3: Валидность состояния игры
        if (session != null) {
            CheatCheckResult stateCheck = checkGameState(player, session, actionType);
            if (!stateCheck.isValid()) {
                recordViolation(playerUUID, stateCheck.getViolationType(), stateCheck.getSeverity());
                return stateCheck;
            }
        }
        
        // Действие валидно - обновляем статистику
        lastActionTime.put(playerUUID, currentTime);
        incrementActionCount(playerUUID, currentTime);
        
        return new CheatCheckResult(true, null, null, "Action is valid");
    }
    
    /**
     * Проверяет скорость действий (защита от ботов)
     */
    private static CheatCheckResult checkActionSpeed(UUID playerUUID, long currentTime) {
        Long lastTime = lastActionTime.get(playerUUID);
        
        if (lastTime != null) {
            long timeSinceLastAction = currentTime - lastTime;
            
            if (timeSinceLastAction < MIN_ACTION_INTERVAL) {
                // Слишком быстрое действие
                int violationCount = getViolationCount(playerUUID);
                ViolationSeverity severity = determineSeverity(violationCount);
                
                ModLogger.warn("Suspicious action speed detected",
                    "playerUUID", playerUUID.toString(),
                    "timeSinceLastAction", timeSinceLastAction + "ms",
                    "minInterval", MIN_ACTION_INTERVAL + "ms",
                    "violationCount", violationCount);
                
                return new CheatCheckResult(false, ViolationType.TOO_FAST_ACTION, 
                    severity, "Action too fast: " + timeSinceLastAction + "ms < " + MIN_ACTION_INTERVAL + "ms");
            }
        }
        
        return new CheatCheckResult(true, null, null, "Speed check passed");
    }
    
    /**
     * Проверяет количество действий в окне времени
     */
    private static CheatCheckResult checkActionWindow(UUID playerUUID, long currentTime) {
        Long windowStart = actionWindowStart.get(playerUUID);
        
        if (windowStart == null || currentTime - windowStart > ACTION_WINDOW_MS) {
            // Начинаем новое окно
            actionWindowStart.put(playerUUID, currentTime);
            actionCount.put(playerUUID, 1);
            return new CheatCheckResult(true, null, null, "New action window");
        }
        
        int count = actionCount.getOrDefault(playerUUID, 0);
        if (count >= MAX_ACTIONS_PER_WINDOW) {
            // Слишком много действий в окне
            int violationCount = getViolationCount(playerUUID);
            ViolationSeverity severity = determineSeverity(violationCount);
            
            ModLogger.warn("Too many actions in window",
                "playerUUID", playerUUID.toString(),
                "actionCount", count,
                "maxActions", MAX_ACTIONS_PER_WINDOW,
                "violationCount", violationCount);
            
            return new CheatCheckResult(false, ViolationType.SUSPICIOUS_PATTERN,
                severity, "Too many actions: " + count + " >= " + MAX_ACTIONS_PER_WINDOW);
        }
        
        return new CheatCheckResult(true, null, null, "Window check passed");
    }
    
    /**
     * Проверяет валидность состояния игры
     */
    private static CheatCheckResult checkGameState(ServerPlayer player, FalkyeGameSession session, String actionType) {
        // Проверка 1: Игра должна быть активна
        if (session.isGameEnded()) {
            return new CheatCheckResult(false, ViolationType.INVALID_STATE_ACCESS,
                ViolationSeverity.MEDIUM, "Game is ended");
        }
        
        // Проверка 2: Игрок должен быть участником игры
        if (!session.getPlayer1().equals(player) && 
            (session.getPlayer2() == null || !session.getPlayer2().equals(player))) {
            return new CheatCheckResult(false, ViolationType.INVALID_STATE_ACCESS,
                ViolationSeverity.HIGH, "Player is not a participant");
        }
        
        // Проверка 3: Для действий, требующих хода, проверяем, чей ход
        if (requiresTurn(actionType)) {
            if (!session.isPlayerTurn(player)) {
                return new CheatCheckResult(false, ViolationType.OUT_OF_TURN_ACTION,
                    ViolationSeverity.MEDIUM, "Not player's turn");
            }
        }
        
        return new CheatCheckResult(true, null, null, "Game state check passed");
    }
    
    /**
     * Определяет, требует ли действие хода игрока
     */
    private static boolean requiresTurn(String actionType) {
        return actionType != null && (
            actionType.equals("playCard") ||
            actionType.equals("pass") ||
            actionType.equals("useLeader") ||
            actionType.equals("surrender")
        );
    }
    
    /**
     * Увеличивает счётчик действий в окне
     */
    private static void incrementActionCount(UUID playerUUID, long currentTime) {
        Long windowStart = actionWindowStart.get(playerUUID);
        
        if (windowStart == null || currentTime - windowStart > ACTION_WINDOW_MS) {
            actionWindowStart.put(playerUUID, currentTime);
            actionCount.put(playerUUID, 1);
        } else {
            actionCount.merge(playerUUID, 1, Integer::sum);
        }
    }
    
    /**
     * Записывает нарушение
     */
    private static void recordViolation(UUID playerUUID, ViolationType type, ViolationSeverity severity) {
        violationCount.merge(playerUUID, 1, Integer::sum);
        
        int count = violationCount.get(playerUUID);
        
        ModLogger.warn("Cheat violation recorded",
            "playerUUID", playerUUID.toString(),
            "type", type.toString(),
            "severity", severity.toString(),
            "totalViolations", count);
        
        // Если критическое нарушение - логируем как ошибку
        if (severity == ViolationSeverity.CRITICAL) {
            ModLogger.error("CRITICAL cheat violation",
                "playerUUID", playerUUID.toString(),
                "type", type.toString(),
                "totalViolations", count);
        }
    }
    
    /**
     * Получает количество нарушений игрока
     */
    public static int getViolationCount(UUID playerUUID) {
        return violationCount.getOrDefault(playerUUID, 0);
    }
    
    /**
     * Определяет серьёзность нарушения на основе истории
     */
    private static ViolationSeverity determineSeverity(int violationCount) {
        if (violationCount >= CRITICAL_VIOLATION_THRESHOLD) {
            return ViolationSeverity.CRITICAL;
        } else if (violationCount >= HIGH_VIOLATION_THRESHOLD) {
            return ViolationSeverity.HIGH;
        } else if (violationCount >= MEDIUM_VIOLATION_THRESHOLD) {
            return ViolationSeverity.MEDIUM;
        } else {
            return ViolationSeverity.LOW;
        }
    }
    
    /**
     * Проверяет валидность карты в руке игрока
     */
    public static boolean validateCardInHand(ServerPlayer player, FalkyeGameSession session, String cardId) {
        if (player == null || session == null || cardId == null) {
            return false;
        }
        
        java.util.List<com.bmfalkye.cards.Card> hand = session.getHand(player);
        for (com.bmfalkye.cards.Card card : hand) {
            if (card.getId().equals(cardId)) {
                return true;
            }
        }
        
        // Карта не найдена в руке - записываем нарушение
        UUID playerUUID = player.getUUID();
        recordViolation(playerUUID, ViolationType.INVALID_CARD_PLAY, ViolationSeverity.MEDIUM);
        
        ModLogger.warn("Player tried to play card not in hand",
            "player", player.getName().getString(),
            "cardId", cardId);
        
        return false;
    }
    
    /**
     * Проверяет, может ли игрок выполнить действие
     */
    public static boolean canPlayerAct(ServerPlayer player, FalkyeGameSession session, String actionType) {
        if (player == null || session == null) {
            return false;
        }
        
        CheatCheckResult result = checkAction(player, actionType, session);
        
        if (!result.isValid()) {
            // Отправляем предупреждение игроку
            if (result.getSeverity() == ViolationSeverity.HIGH || 
                result.getSeverity() == ViolationSeverity.CRITICAL) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c§lВНИМАНИЕ: Обнаружена подозрительная активность!"));
            }
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Сбрасывает статистику игрока (при выходе из игры)
     */
    public static void resetPlayerStats(UUID playerUUID) {
        lastActionTime.remove(playerUUID);
        actionCount.remove(playerUUID);
        actionWindowStart.remove(playerUUID);
        // НЕ сбрасываем violationCount - он должен накапливаться
    }
    
    /**
     * Очищает статистику нарушений игрока (для админов)
     */
    public static void clearViolations(UUID playerUUID) {
        violationCount.remove(playerUUID);
        ModLogger.logGameEvent("Violations cleared for player", "playerUUID", playerUUID.toString());
    }
    
    /**
     * Получает статистику игрока
     */
    public static PlayerCheatStats getPlayerStats(UUID playerUUID) {
        return new PlayerCheatStats(
            playerUUID,
            getViolationCount(playerUUID),
            lastActionTime.get(playerUUID),
            actionCount.getOrDefault(playerUUID, 0)
        );
    }
    
    /**
     * Результат проверки на читерство
     */
    public static class CheatCheckResult {
        private final boolean valid;
        private final ViolationType violationType;
        private final ViolationSeverity severity;
        private final String message;
        
        public CheatCheckResult(boolean valid, ViolationType violationType, 
                              ViolationSeverity severity, String message) {
            this.valid = valid;
            this.violationType = violationType;
            this.severity = severity;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public ViolationType getViolationType() {
            return violationType;
        }
        
        public ViolationSeverity getSeverity() {
            return severity;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Статистика читерства игрока
     */
    public static class PlayerCheatStats {
        private final UUID playerUUID;
        private final int violationCount;
        private final Long lastActionTime;
        private final int actionCount;
        
        public PlayerCheatStats(UUID playerUUID, int violationCount, Long lastActionTime, int actionCount) {
            this.playerUUID = playerUUID;
            this.violationCount = violationCount;
            this.lastActionTime = lastActionTime;
            this.actionCount = actionCount;
        }
        
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        
        public int getViolationCount() {
            return violationCount;
        }
        
        public Long getLastActionTime() {
            return lastActionTime;
        }
        
        public int getActionCount() {
            return actionCount;
        }
    }
}

