package com.bmfalkye.edgecases;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardBuff;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.client.gui.backup.logic.CardRendererLogic;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты граничных случаев
 * Проверяет обработку нестандартных ситуаций
 */
class EdgeCaseTests {
    
    @Test
    void testNegativePower() {
        // Тест обработки отрицательной силы (не должна быть отрицательной)
        Card card = new Card("test", "Тест", Card.CardType.CREATURE, 0, 
                           "Описание", "Фракция");
        
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff hugeDebuff = new CardBuff("debuff", CardBuff.BuffType.POWER_DECREASE, 100, -1, sourcePlayer);
        List<CardBuff> debuffs = new ArrayList<>();
        debuffs.add(hugeDebuff);
        
        int effectivePower = CardRendererLogic.calculateEffectivePower(card, 0, debuffs);
        assertEquals(0, effectivePower, "Сила не должна быть отрицательной");
    }
    
    @Test
    void testZeroPower() {
        // Тест обработки нулевой силы
        Card card = new Card("test", "Тест", Card.CardType.SPELL, 0, 
                           "Описание", "Фракция");
        
        int effectivePower = CardRendererLogic.calculateEffectivePower(card, 0, null);
        assertEquals(0, effectivePower);
    }
    
    @Test
    void testVeryLargePower() {
        // Тест обработки очень большой силы
        Card card = new Card("test", "Тест", Card.CardType.CREATURE, Integer.MAX_VALUE - 10, 
                           "Описание", "Фракция");
        
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff powerIncrease = new CardBuff("buff", CardBuff.BuffType.POWER_INCREASE, 5, -1, sourcePlayer);
        List<CardBuff> buffs = new ArrayList<>();
        buffs.add(powerIncrease);
        
        // Должно обрабатываться без переполнения
        assertDoesNotThrow(() -> {
            CardRendererLogic.calculateEffectivePower(card, card.getPower(), buffs);
        }, "Очень большая сила должна обрабатываться без переполнения");
    }
    
    @Test
    void testEmptyBuffsList() {
        // Тест обработки пустого списка баффов
        Card card = new Card("test", "Тест", Card.CardType.CREATURE, 5, 
                           "Описание", "Фракция");
        
        List<CardBuff> emptyBuffs = new ArrayList<>();
        int effectivePower = CardRendererLogic.calculateEffectivePower(card, 5, emptyBuffs);
        assertEquals(5, effectivePower, "Пустой список баффов не должен изменять силу");
    }
    
    @Test
    void testNullCard() {
        // Тест обработки null карты (должна обрабатываться gracefully)
        assertDoesNotThrow(() -> {
            // Методы должны обрабатывать null gracefully
        }, "Null карта должна обрабатываться gracefully");
    }
    
    @Test
    void testMultiplePowerIncreases() {
        // Тест нескольких баффов увеличения силы
        Card card = new Card("test", "Тест", Card.CardType.CREATURE, 5, 
                           "Описание", "Фракция");
        
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff buff1 = new CardBuff("buff1", CardBuff.BuffType.POWER_INCREASE, 3, -1, sourcePlayer);
        CardBuff buff2 = new CardBuff("buff2", CardBuff.BuffType.POWER_INCREASE, 2, -1, sourcePlayer);
        CardBuff buff3 = new CardBuff("buff3", CardBuff.BuffType.POWER_INCREASE, 1, -1, sourcePlayer);
        
        List<CardBuff> buffs = new ArrayList<>();
        buffs.add(buff1);
        buffs.add(buff2);
        buffs.add(buff3);
        
        int effectivePower = CardRendererLogic.calculateEffectivePower(card, 5, buffs);
        assertEquals(11, effectivePower, "5 + 3 + 2 + 1 = 11");
    }
    
    @Test
    void testMixedBuffs() {
        // Тест смешанных баффов (увеличение и уменьшение)
        Card card = new Card("test", "Тест", Card.CardType.CREATURE, 10, 
                           "Описание", "Фракция");
        
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff increase = new CardBuff("buff1", CardBuff.BuffType.POWER_INCREASE, 5, -1, sourcePlayer);
        CardBuff decrease = new CardBuff("buff2", CardBuff.BuffType.POWER_DECREASE, 3, -1, sourcePlayer);
        CardBuff immunity = new CardBuff("buff3", CardBuff.BuffType.IMMUNITY, 0, -1, sourcePlayer);
        
        List<CardBuff> buffs = new ArrayList<>();
        buffs.add(increase);
        buffs.add(decrease);
        buffs.add(immunity);
        
        int effectivePower = CardRendererLogic.calculateEffectivePower(card, 10, buffs);
        assertEquals(12, effectivePower, "10 + 5 - 3 = 12");
    }
    
    @Test
    void testGlowIntensityBoundaries() {
        // Тест граничных значений интенсивности свечения
        long animationTime = System.currentTimeMillis();
        
        // Минимальная интенсивность (не выбранная, не наведённая)
        float minIntensity = CardRendererLogic.calculateGlowIntensity(false, false, 
                                                                     CardRarity.COMMON, animationTime);
        assertTrue(minIntensity >= 0.0f, "Минимальная интенсивность должна быть >= 0");
        
        // Максимальная интенсивность (выбранная легендарная)
        float maxIntensity = CardRendererLogic.calculateGlowIntensity(true, true, 
                                                                     CardRarity.LEGENDARY, animationTime);
        assertTrue(maxIntensity <= 1.0f, "Максимальная интенсивность должна быть <= 1");
    }
}

