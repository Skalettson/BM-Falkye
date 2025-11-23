package com.bmfalkye.util;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderRegistry;

import java.util.UUID;

/**
 * Валидатор данных при загрузке из NBT/SavedData
 * Обеспечивает безопасную загрузку данных с проверкой корректности
 */
public class DataLoadValidator {
    
    /**
     * Безопасно парсит UUID из строки
     * @param uuidString строка с UUID
     * @return UUID или null, если строка невалидна
     */
    public static UUID safeParseUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            BMFalkye.LOGGER.warn("Invalid UUID string during data load: {}", uuidString);
            ModLogger.warn("Invalid UUID during load", "uuidString", uuidString, "error", e.getMessage());
            return null;
        }
    }
    
    /**
     * Валидирует и нормализует уровень игрока
     * @param level уровень из NBT
     * @return валидный уровень (1-50)
     */
    public static int validateLevel(int level) {
        if (level < 1) {
            ModLogger.warn("Invalid level during load, using default", "level", level);
            return 1;
        }
        if (level > 50) {
            ModLogger.warn("Level too high during load, capping to 50", "level", level);
            return 50;
        }
        return level;
    }
    
    /**
     * Валидирует и нормализует опыт игрока
     * @param experience опыт из NBT
     * @return валидный опыт (0-10,000,000)
     */
    public static int validateExperience(int experience) {
        if (experience < 0) {
            ModLogger.warn("Negative experience during load, setting to 0", "experience", experience);
            return 0;
        }
        if (experience > 10_000_000) {
            ModLogger.warn("Experience too high during load, capping to 10M", "experience", experience);
            return 10_000_000;
        }
        return experience;
    }
    
    /**
     * Валидирует и нормализует количество монет
     * @param coins монеты из NBT
     * @return валидное количество монет (0-1,000,000)
     */
    public static int validateCoins(int coins) {
        if (coins < 0) {
            ModLogger.warn("Negative coins during load, setting to 0", "coins", coins);
            return 0;
        }
        if (coins > 1_000_000) {
            ModLogger.warn("Coins too high during load, capping to 1M", "coins", coins);
            return 1_000_000;
        }
        return coins;
    }
    
    /**
     * Валидирует ID карты
     * @param cardId ID карты
     * @return true, если карта существует в реестре
     */
    public static boolean isValidCardId(String cardId) {
        if (cardId == null || cardId.trim().isEmpty()) {
            return false;
        }
        
        // Проверяем формат ID
        if (cardId.length() > 100) {
            ModLogger.warn("Card ID too long during load", "cardId", cardId);
            return false;
        }
        
        // Проверяем, существует ли карта в реестре
        boolean exists = CardRegistry.getCard(cardId) != null;
        if (!exists) {
            ModLogger.warn("Card ID not found in registry during load", "cardId", cardId);
        }
        return exists;
    }
    
    /**
     * Валидирует ID лидера
     * @param leaderId ID лидера
     * @return true, если лидер существует в реестре
     */
    public static boolean isValidLeaderId(String leaderId) {
        if (leaderId == null || leaderId.trim().isEmpty()) {
            return false;
        }
        
        // Проверяем формат ID
        if (leaderId.length() > 100) {
            ModLogger.warn("Leader ID too long during load", "leaderId", leaderId);
            return false;
        }
        
        // Проверяем, существует ли лидер в реестре
        boolean exists = LeaderRegistry.getLeader(leaderId) != null;
        if (!exists) {
            ModLogger.warn("Leader ID not found in registry during load", "leaderId", leaderId);
        }
        return exists;
    }
    
    /**
     * Валидирует ID достижения
     * @param achievementId ID достижения
     * @return true, если ID валиден
     */
    public static boolean isValidAchievementId(String achievementId) {
        if (achievementId == null || achievementId.trim().isEmpty()) {
            return false;
        }
        
        if (achievementId.length() > 100) {
            ModLogger.warn("Achievement ID too long during load", "achievementId", achievementId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Валидирует статистику игры
     * @param value значение статистики
     * @return валидное значение (0-1,000,000)
     */
    public static int validateStatistic(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1_000_000) {
            ModLogger.warn("Statistic value too high during load, capping", "value", value);
            return 1_000_000;
        }
        return value;
    }
    
    /**
     * Валидирует имя колоды
     * @param deckName имя колоды
     * @return валидное имя колоды или дефолтное
     */
    public static String validateDeckName(String deckName) {
        if (deckName == null || deckName.trim().isEmpty()) {
            return "Моя колода";
        }
        
        if (deckName.length() > 50) {
            ModLogger.warn("Deck name too long during load, truncating", "deckName", deckName);
            return deckName.substring(0, 50);
        }
        
        return deckName.trim();
    }
    
    /**
     * Валидирует количество карт в колоде
     * @param count количество карт
     * @return валидное количество (0-100)
     */
    public static int validateDeckCardCount(int count) {
        if (count < 0) {
            return 0;
        }
        if (count > 100) {
            ModLogger.warn("Deck card count too high during load, capping to 100", "count", count);
            return 100;
        }
        return count;
    }
}

