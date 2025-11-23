package com.bmfalkye.system;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardBuff;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.client.gui.backup.logic.CardRendererLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Комплексные системные интеграционные тесты
 * Проверяет работу всех систем вместе
 */
@DisplayName("System Integration Tests")
class SystemIntegrationTest {
    
    private Card testCard;
    private List<CardBuff> buffs;
    
    @BeforeEach
    void setUp() {
        testCard = new Card("test_card", "Тестовая карта", 
                          Card.CardType.CREATURE, 5, 
                          "Описание", "Дом Пламени", 
                          CardRarity.COMMON, 10);
        buffs = new ArrayList<>();
    }
    
    @Test
    void testCompleteCardRenderingFlow() {
        // Полный поток рендеринга карты
        int basePower = testCard.getPower();
        
        // 1. Расчёт эффективной силы
        int effectivePower = CardRendererLogic.calculateEffectivePower(testCard, basePower, buffs);
        assertTrue(effectivePower >= 0);
        
        // 2. Получение цвета силы
        String colorCode = CardRendererLogic.getPowerColorCode(effectivePower, basePower);
        assertNotNull(colorCode);
        
        // 3. Получение иконки типа
        String typeIcon = CardRendererLogic.getTypeIcon(testCard);
        assertNotNull(typeIcon);
        
        // 4. Расчёт интенсивности свечения
        long animationTime = System.currentTimeMillis();
        float glowIntensity = CardRendererLogic.calculateGlowIntensity(false, false, 
                                                                       testCard.getRarity(), animationTime);
        assertTrue(glowIntensity >= 0.0f && glowIntensity <= 1.0f);
    }
    
    @Test
    void testCardWithMultipleBuffs() {
        // Тест карты с несколькими баффами
        UUID sourcePlayer = UUID.randomUUID();
        
        CardBuff powerIncrease = new CardBuff("buff1", CardBuff.BuffType.POWER_INCREASE, 5, -1, sourcePlayer);
        CardBuff powerDecrease = new CardBuff("buff2", CardBuff.BuffType.POWER_DECREASE, 2, -1, sourcePlayer);
        CardBuff immunity = new CardBuff("buff3", CardBuff.BuffType.IMMUNITY, 0, -1, sourcePlayer);
        
        buffs.add(powerIncrease);
        buffs.add(powerDecrease);
        buffs.add(immunity);
        
        int basePower = testCard.getPower();
        int effectivePower = CardRendererLogic.calculateEffectivePower(testCard, basePower, buffs);
        
        // 5 + 5 - 2 = 8
        assertEquals(8, effectivePower);
    }
    
    @Test
    void testAllRaritiesRendering() {
        // Тест рендеринга всех редкостей
        CardRarity[] rarities = CardRarity.values();
        
        for (CardRarity rarity : rarities) {
            Card card = new Card("test_" + rarity.name(), "Тест", 
                               Card.CardType.CREATURE, 5, 
                               "Описание", "Фракция", 
                               rarity, 10);
            
            long animationTime = System.currentTimeMillis();
            float glowIntensity = CardRendererLogic.calculateGlowIntensity(false, false, 
                                                                           card.getRarity(), animationTime);
            
            assertTrue(glowIntensity >= 0.0f && glowIntensity <= 1.0f, 
                      "Интенсивность свечения для " + rarity + " должна быть в диапазоне [0, 1]");
        }
    }
    
    @Test
    void testAllCardTypesRendering() {
        // Тест рендеринга всех типов карт
        Card.CardType[] types = Card.CardType.values();
        
        for (Card.CardType type : types) {
            Card card = new Card("test_" + type.name(), "Тест", 
                               type, 5, 
                               "Описание", "Фракция");
            
            String typeIcon = CardRendererLogic.getTypeIcon(card);
            assertNotNull(typeIcon, "Иконка типа для " + type + " не должна быть null");
            assertFalse(typeIcon.isEmpty(), "Иконка типа для " + type + " не должна быть пустой");
        }
    }
    
    @Test
    void testEdgeCases() {
        // Тест граничных случаев
        
        // Карта с нулевой силой
        Card zeroPowerCard = new Card("zero", "Нулевая", 
                                    Card.CardType.SPELL, 0, 
                                    "Описание", "Фракция");
        int effectivePower1 = CardRendererLogic.calculateEffectivePower(zeroPowerCard, 0, null);
        assertEquals(0, effectivePower1);
        
        // Карта с большим дебаффом (не должна стать отрицательной)
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff hugeDebuff = new CardBuff("debuff", CardBuff.BuffType.POWER_DECREASE, 100, -1, sourcePlayer);
        List<CardBuff> debuffs = new ArrayList<>();
        debuffs.add(hugeDebuff);
        
        int effectivePower2 = CardRendererLogic.calculateEffectivePower(zeroPowerCard, 0, debuffs);
        assertEquals(0, effectivePower2, "Сила не должна быть отрицательной");
    }
    
    @Test
    @DisplayName("Should integrate API with card registry")
    void testAPIIntegration() {
        // API теперь работает через IEventBus, используем CardRegistry напрямую
        List<Card> cards = com.bmfalkye.cards.CardRegistry.getAllCards();
        assertNotNull(cards, "All cards should be accessible through CardRegistry");
    }
    
    @Test
    @DisplayName("Should integrate localization with error messages")
    void testLocalizationIntegration() {
        com.bmfalkye.util.LocalizationHelper.setLanguage("ru_ru");
        String localized = com.bmfalkye.util.LocalizationHelper.getLocalizedString("error.bm_falkye.generic", "test error");
        assertNotNull(localized, "Localized error message should not be null");
    }
    
    @Test
    @DisplayName("Should integrate bet protection with validation")
    void testBetProtectionIntegration() {
        assertTrue(com.bmfalkye.game.BetProtectionSystem.isValidBetAmount(100), 
            "Bet protection should validate bet amounts");
        assertFalse(com.bmfalkye.game.BetProtectionSystem.isValidBetAmount(0), 
            "Bet protection should reject zero bets");
    }
}

