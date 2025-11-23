package com.bmfalkye.memory;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.util.ModLogger;

import java.util.Map;
import java.util.UUID;

/**
 * Система оптимизации памяти для долгих игровых сессий
 * Предотвращает утечки памяти и оптимизирует использование ресурсов
 */
public class MemoryOptimizer {
    
    // Максимальное количество ходов в реплее (предотвращает бесконечный рост)
    private static final int MAX_REPLAY_MOVES = 500;
    
    // Максимальное количество карт в сбросе (graveyard) перед очисткой старых
    private static final int MAX_GRAVEYARD_SIZE = 200;
    
    // Максимальное количество записей в revealedCards перед очисткой
    private static final int MAX_REVEALED_CARDS_SIZE = 100;
    
    // Максимальное время жизни сессии в миллисекундах (24 часа)
    private static final long MAX_SESSION_AGE_MS = 24 * 60 * 60 * 1000L;
    
    // Интервал проверки памяти (в миллисекундах)
    private static final long MEMORY_CHECK_INTERVAL_MS = 5 * 60 * 1000L; // 5 минут
    
    private static long lastMemoryCheck = 0;
    
    /**
     * Оптимизирует память игровой сессии
     * Вызывается периодически для очистки устаревших данных
     */
    public static void optimizeSessionMemory(FalkyeGameSession session) {
        if (session == null || session.isGameEnded()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long sessionAge = currentTime - session.getGameStartTime();
        
        // Если сессия слишком старая, принудительно завершаем её
        if (sessionAge > MAX_SESSION_AGE_MS) {
            ModLogger.warn("Session too old, forcing end",
                "sessionAge", sessionAge + "ms",
                "maxAge", MAX_SESSION_AGE_MS + "ms");
            
            // Завершаем игру с ничьей
            if (session.getPlayer1() != null) {
                session.getPlayer1().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cИгра была завершена из-за превышения времени."));
            }
            if (session.getPlayer2() != null) {
                session.getPlayer2().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cИгра была завершена из-за превышения времени."));
            }
            
            GameManager.endGame(session);
            return;
        }
        
        // Оптимизируем реплей (ограничиваем размер)
        optimizeReplayMemory(session);
        
        // Оптимизируем сбросы (graveyard)
        optimizeGraveyardMemory(session);
        
        // Оптимизируем revealedCards
        optimizeRevealedCardsMemory(session);
        
        // Оптимизируем модификаторы силы (очищаем устаревшие)
        optimizePowerModifiersMemory(session);
        
        // Оптимизируем баффы карт (очищаем истёкшие)
        optimizeCardBuffsMemory(session);
    }
    
    /**
     * Оптимизирует память реплея
     */
    private static void optimizeReplayMemory(FalkyeGameSession session) {
        java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayMove> recordedMoves = session.getRecordedMoves();
        
        if (recordedMoves.size() > MAX_REPLAY_MOVES) {
            // Оставляем только последние MAX_REPLAY_MOVES ходов
            int removeCount = recordedMoves.size() - MAX_REPLAY_MOVES;
            
            // Используем рефлексию для доступа к приватному полю
            try {
                java.lang.reflect.Field field = FalkyeGameSession.class.getDeclaredField("recordedMoves");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayMove> moves = 
                    (java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayMove>) field.get(session);
                
                // Удаляем старые ходы
                for (int i = 0; i < removeCount; i++) {
                    moves.remove(0);
                }
                
                ModLogger.logGameEvent("Replay memory optimized",
                    "removedMoves", removeCount,
                    "remainingMoves", moves.size());
            } catch (Exception e) {
                ModLogger.error("Failed to optimize replay memory", "error", e.getMessage());
            }
        }
    }
    
    /**
     * Оптимизирует память сбросов (graveyard)
     */
    private static void optimizeGraveyardMemory(FalkyeGameSession session) {
        try {
            java.lang.reflect.Field graveyard1Field = FalkyeGameSession.class.getDeclaredField("graveyard1");
            graveyard1Field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<com.bmfalkye.cards.Card> graveyard1 = 
                (java.util.List<com.bmfalkye.cards.Card>) graveyard1Field.get(session);
            
            if (graveyard1.size() > MAX_GRAVEYARD_SIZE) {
                int removeCount = graveyard1.size() - MAX_GRAVEYARD_SIZE;
                for (int i = 0; i < removeCount; i++) {
                    graveyard1.remove(0);
                }
            }
            
            java.lang.reflect.Field graveyard2Field = FalkyeGameSession.class.getDeclaredField("graveyard2");
            graveyard2Field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<com.bmfalkye.cards.Card> graveyard2 = 
                (java.util.List<com.bmfalkye.cards.Card>) graveyard2Field.get(session);
            
            if (graveyard2.size() > MAX_GRAVEYARD_SIZE) {
                int removeCount = graveyard2.size() - MAX_GRAVEYARD_SIZE;
                for (int i = 0; i < removeCount; i++) {
                    graveyard2.remove(0);
                }
            }
        } catch (Exception e) {
            ModLogger.error("Failed to optimize graveyard memory", "error", e.getMessage());
        }
    }
    
    /**
     * Оптимизирует память revealedCards
     */
    private static void optimizeRevealedCardsMemory(FalkyeGameSession session) {
        try {
            java.lang.reflect.Field field = FalkyeGameSession.class.getDeclaredField("revealedCards");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, java.util.List<String>> revealedCards = 
                (Map<UUID, java.util.List<String>>) field.get(session);
            
            for (Map.Entry<UUID, java.util.List<String>> entry : revealedCards.entrySet()) {
                java.util.List<String> cards = entry.getValue();
                if (cards.size() > MAX_REVEALED_CARDS_SIZE) {
                    // Оставляем только последние карты
                    int removeCount = cards.size() - MAX_REVEALED_CARDS_SIZE;
                    for (int i = 0; i < removeCount; i++) {
                        cards.remove(0);
                    }
                }
            }
        } catch (Exception e) {
            ModLogger.error("Failed to optimize revealedCards memory", "error", e.getMessage());
        }
    }
    
    /**
     * Оптимизирует память модификаторов силы
     * Удаляет модификаторы для карт, которых больше нет на поле
     */
    private static void optimizePowerModifiersMemory(FalkyeGameSession session) {
        try {
            java.lang.reflect.Field field = FalkyeGameSession.class.getDeclaredField("powerModifiers");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, Integer>> powerModifiers = 
                (Map<UUID, Map<String, Integer>>) field.get(session);
            
            // Получаем все карты на поле
            java.util.Set<String> cardsOnField = new java.util.HashSet<>();
            
            // Карты игрока 1
            for (com.bmfalkye.cards.Card card : session.getMeleeRow(session.getPlayer1())) {
                cardsOnField.add(card.getId());
            }
            for (com.bmfalkye.cards.Card card : session.getRangedRow(session.getPlayer1())) {
                cardsOnField.add(card.getId());
            }
            for (com.bmfalkye.cards.Card card : session.getSiegeRow(session.getPlayer1())) {
                cardsOnField.add(card.getId());
            }
            
            // Карты игрока 2
            if (session.getPlayer2() != null) {
                for (com.bmfalkye.cards.Card card : session.getMeleeRow(session.getPlayer2())) {
                    cardsOnField.add(card.getId());
                }
                for (com.bmfalkye.cards.Card card : session.getRangedRow(session.getPlayer2())) {
                    cardsOnField.add(card.getId());
                }
                for (com.bmfalkye.cards.Card card : session.getSiegeRow(session.getPlayer2())) {
                    cardsOnField.add(card.getId());
                }
            } else if (session.isPlayingWithVillager()) {
                for (com.bmfalkye.cards.Card card : session.getMeleeRow(null)) {
                    cardsOnField.add(card.getId());
                }
                for (com.bmfalkye.cards.Card card : session.getRangedRow(null)) {
                    cardsOnField.add(card.getId());
                }
                for (com.bmfalkye.cards.Card card : session.getSiegeRow(null)) {
                    cardsOnField.add(card.getId());
                }
            }
            
            // Удаляем модификаторы для карт, которых нет на поле
            for (Map<String, Integer> playerModifiers : powerModifiers.values()) {
                playerModifiers.entrySet().removeIf(entry -> !cardsOnField.contains(entry.getKey()));
            }
            
            // Удаляем пустые записи игроков
            powerModifiers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
        } catch (Exception e) {
            ModLogger.error("Failed to optimize powerModifiers memory", "error", e.getMessage());
        }
    }
    
    /**
     * Оптимизирует память баффов карт
     * Удаляет истёкшие баффы
     */
    private static void optimizeCardBuffsMemory(FalkyeGameSession session) {
        try {
            java.lang.reflect.Field field = FalkyeGameSession.class.getDeclaredField("cardBuffs");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, java.util.List<com.bmfalkye.cards.CardBuff>>> cardBuffs = 
                (Map<UUID, Map<String, java.util.List<com.bmfalkye.cards.CardBuff>>>) field.get(session);
            
            long currentTime = System.currentTimeMillis();
            
            // Удаляем истёкшие баффы
            for (Map<String, java.util.List<com.bmfalkye.cards.CardBuff>> playerBuffs : cardBuffs.values()) {
                for (java.util.List<com.bmfalkye.cards.CardBuff> buffs : playerBuffs.values()) {
                    buffs.removeIf(buff -> {
                        // Проверяем, истёк ли бафф (если есть метод для проверки)
                        // Пока просто проверяем наличие метода getExpirationTime
                        try {
                            java.lang.reflect.Method method = buff.getClass().getMethod("getExpirationTime");
                            Long expirationTime = (Long) method.invoke(buff);
                            return expirationTime != null && expirationTime < currentTime;
                        } catch (Exception e) {
                            // Если метода нет, оставляем бафф
                            return false;
                        }
                    });
                }
                
                // Удаляем пустые списки баффов
                playerBuffs.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
            
            // Удаляем пустые записи игроков
            cardBuffs.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
        } catch (Exception e) {
            ModLogger.error("Failed to optimize cardBuffs memory", "error", e.getMessage());
        }
    }
    
    /**
     * Периодическая проверка и оптимизация памяти всех активных сессий
     */
    public static void periodicMemoryCheck() {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем, нужно ли выполнять проверку
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL_MS) {
            return;
        }
        
        lastMemoryCheck = currentTime;
        
        // Оптимизируем все активные игры
        Map<UUID, FalkyeGameSession> activeGames = GameManager.getActiveGames();
        int optimizedCount = 0;
        
        for (FalkyeGameSession session : activeGames.values()) {
            if (session != null && !session.isGameEnded()) {
                optimizeSessionMemory(session);
                optimizedCount++;
            }
        }
        
        // Оптимизируем игры с villager
        Map<UUID, FalkyeGameSession> villagerGames = GameManager.getActiveVillagerGames();
        for (FalkyeGameSession session : villagerGames.values()) {
            if (session != null && !session.isGameEnded()) {
                optimizeSessionMemory(session);
                optimizedCount++;
            }
        }
        
        if (optimizedCount > 0) {
            ModLogger.logGameEvent("Periodic memory optimization completed",
                "optimizedSessions", optimizedCount);
        }
    }
    
    /**
     * Принудительная очистка памяти для завершённой сессии
     */
    public static void cleanupFinishedSession(FalkyeGameSession session) {
        if (session == null) {
            return;
        }
        
        // Очищаем все временные данные
        try {
            // Очищаем revealedCards
            java.lang.reflect.Field revealedField = FalkyeGameSession.class.getDeclaredField("revealedCards");
            revealedField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, java.util.List<String>> revealedCards = 
                (Map<UUID, java.util.List<String>>) revealedField.get(session);
            revealedCards.clear();
            
            // Очищаем collectedCombos
            java.lang.reflect.Field combosField = FalkyeGameSession.class.getDeclaredField("collectedCombos");
            combosField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, java.util.Set<String>> collectedCombos = 
                (Map<UUID, java.util.Set<String>>) combosField.get(session);
            collectedCombos.clear();
            
            // Очищаем powerModifiers
            java.lang.reflect.Field modifiersField = FalkyeGameSession.class.getDeclaredField("powerModifiers");
            modifiersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, Integer>> powerModifiers = 
                (Map<UUID, Map<String, Integer>>) modifiersField.get(session);
            powerModifiers.clear();
            
            // Очищаем cardBuffs
            java.lang.reflect.Field buffsField = FalkyeGameSession.class.getDeclaredField("cardBuffs");
            buffsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, java.util.List<com.bmfalkye.cards.CardBuff>>> cardBuffs = 
                (Map<UUID, Map<String, java.util.List<com.bmfalkye.cards.CardBuff>>>) buffsField.get(session);
            cardBuffs.clear();
            
            ModLogger.logGameEvent("Finished session memory cleaned up");
        } catch (Exception e) {
            ModLogger.error("Failed to cleanup finished session memory", "error", e.getMessage());
        }
    }
}

