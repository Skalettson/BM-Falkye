package com.bmfalkye.cards;

import com.bmfalkye.client.gui.backup.logic.CardRendererLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для CardRendererLogic с реальными картами
 */
class CardRendererLogicIntegrationTest {
    
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
    void testCalculateEffectivePower_Integration() {
        // Интеграционный тест расчёта эффективной силы
        int basePower = testCard.getPower();
        assertEquals(5, basePower);
        
        // Без баффов
        int effectivePower1 = CardRendererLogic.calculateEffectivePower(testCard, basePower, null);
        assertEquals(basePower, effectivePower1);
        
        // С баффом увеличения силы
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff powerIncrease = new CardBuff("buff1", CardBuff.BuffType.POWER_INCREASE, 3, -1, sourcePlayer);
        buffs.add(powerIncrease);
        
        int effectivePower2 = CardRendererLogic.calculateEffectivePower(testCard, basePower, buffs);
        assertEquals(basePower + 3, effectivePower2);
    }
    
    @Test
    void testGetPowerColorCode_Integration() {
        // Интеграционный тест цветов силы
        int basePower = testCard.getPower();
        
        // Увеличенная сила
        String color1 = CardRendererLogic.getPowerColorCode(basePower + 2, basePower);
        assertEquals("§a", color1);
        
        // Уменьшенная сила
        String color2 = CardRendererLogic.getPowerColorCode(basePower - 2, basePower);
        assertEquals("§c", color2);
        
        // Базовая сила
        String color3 = CardRendererLogic.getPowerColorCode(basePower, basePower);
        assertEquals("§f", color3);
    }
    
    @Test
    void testGetTypeIcon_Integration() {
        // Интеграционный тест иконок типов
        String icon = CardRendererLogic.getTypeIcon(testCard);
        assertNotNull(icon);
        assertFalse(icon.isEmpty());
    }
    
    @Test
    void testCalculateGlowIntensity_Integration() {
        // Интеграционный тест интенсивности свечения
        long animationTime = System.currentTimeMillis();
        
        // Выбранная карта
        float intensity1 = CardRendererLogic.calculateGlowIntensity(true, false, testCard.getRarity(), animationTime);
        assertTrue(intensity1 > 0.0f);
        
        // Наведённая карта
        float intensity2 = CardRendererLogic.calculateGlowIntensity(false, true, testCard.getRarity(), animationTime);
        assertTrue(intensity2 > 0.0f);
        
        // Обычная карта
        float intensity3 = CardRendererLogic.calculateGlowIntensity(false, false, testCard.getRarity(), animationTime);
        assertTrue(intensity3 >= 0.0f);
    }
    
    @Test
    void testLegendaryCardGlow() {
        // Тест свечения для легендарной карты
        Card legendaryCard = new Card("legendary", "Легендарная", 
                                     Card.CardType.CREATURE, 10, 
                                     "Описание", "Фракция", 
                                     CardRarity.LEGENDARY, 100);
        
        long animationTime = System.currentTimeMillis();
        float intensity = CardRendererLogic.calculateGlowIntensity(false, false, 
                                                                   legendaryCard.getRarity(), animationTime);
        
        // Легендарные карты должны иметь пульсирующее свечение
        assertTrue(intensity >= 0.0f && intensity <= 1.0f);
    }
}

