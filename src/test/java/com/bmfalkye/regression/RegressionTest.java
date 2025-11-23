package com.bmfalkye.regression;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderRegistry;
import com.bmfalkye.game.BetProtectionSystem;
import com.bmfalkye.util.LocalizationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Регрессионные тесты для проверки совместимости
 */
@DisplayName("Regression Tests")
public class RegressionTest {
    
    @BeforeAll
    static void setUp() {
        CardRegistry.initializeDefaultCards();
        LeaderRegistry.initializeLeaders();
    }
    
    @Test
    @DisplayName("Should maintain card registry compatibility")
    void testCardRegistryCompatibility() {
        // Проверяем, что старые карты всё ещё доступны
        Card fireDragon = CardRegistry.getCard("fire_dragon_ignisar");
        assertNotNull(fireDragon, "Fire dragon card should still exist");
        assertEquals("fire_dragon_ignisar", fireDragon.getId(), "Card ID should match");
    }
    
    @Test
    @DisplayName("Should maintain leader registry compatibility")
    void testLeaderRegistryCompatibility() {
        // Проверяем, что старые лидеры всё ещё доступны
        var leader = LeaderRegistry.getLeader("leader_fire_architect");
        assertNotNull(leader, "Fire architect leader should still exist");
        assertEquals("leader_fire_architect", leader.getId(), "Leader ID should match");
    }
    
    @Test
    @DisplayName("Should maintain bet validation rules")
    void testBetValidationCompatibility() {
        // Проверяем, что правила валидации ставок не изменились
        assertTrue(BetProtectionSystem.isValidBetAmount(100), "Standard bet should still be valid");
        assertFalse(BetProtectionSystem.isValidBetAmount(0), "Zero bet should still be invalid");
        assertFalse(BetProtectionSystem.isValidBetAmount(-1), "Negative bet should still be invalid");
    }
    
    @Test
    @DisplayName("Should maintain localization functionality")
    void testLocalizationCompatibility() {
        // Проверяем, что локализация работает
        LocalizationHelper.setLanguage("ru_ru");
        assertEquals("ru_ru", LocalizationHelper.getCurrentLanguage(), "Language should be set correctly");
        
        String localized = LocalizationHelper.getLocalizedString("test.key");
        assertNotNull(localized, "Localized string should not be null");
    }
    
    @Test
    @DisplayName("Should maintain API functionality")
    void testAPICompatibility() {
        // Проверяем, что API работает
        // API теперь работает через IEventBus, используем CardRegistry напрямую
        java.util.List<Card> cards = com.bmfalkye.cards.CardRegistry.getAllCards();
        assertNotNull(cards, "All cards should be accessible through CardRegistry");
    }
    
    @Test
    @DisplayName("Should handle edge cases in card lookup")
    void testCardLookupEdgeCases() {
        // Проверяем обработку граничных случаев
        Card nullCard = CardRegistry.getCard(null);
        assertNull(nullCard, "Null ID should return null card");
        
        Card emptyCard = CardRegistry.getCard("");
        assertNull(emptyCard, "Empty ID should return null card");
        
        Card nonExistentCard = CardRegistry.getCard("non_existent_card_id_12345");
        assertNull(nonExistentCard, "Non-existent ID should return null card");
    }
}

