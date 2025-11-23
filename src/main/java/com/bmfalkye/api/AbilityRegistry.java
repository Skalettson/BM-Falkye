package com.bmfalkye.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр способностей карт
 */
public class AbilityRegistry {
    private static final Map<String, Ability> cardAbilities = new HashMap<>();
    
    /**
     * Регистрирует способность для карты
     */
    public static void registerCardAbility(String cardId, Ability ability) {
        cardAbilities.put(cardId, ability);
    }
    
    /**
     * Получает способность карты
     */
    public static Ability getCardAbility(String cardId) {
        return cardAbilities.get(cardId);
    }
    
    /**
     * Проверяет, есть ли способность у карты
     */
    public static boolean hasAbility(String cardId) {
        return cardAbilities.containsKey(cardId);
    }
}

