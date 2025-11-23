package com.bmfalkye.cards;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для enum {@link CardRarity}.
 * 
 * <p>Проверяет свойства редкости карт, включая:
 * <ul>
 *   <li>Все уровни редкости</li>
 *   <li>Шанс выпадения</li>
 *   <li>Отображаемые имена</li>
 *   <li>Цвета для отображения</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
class CardRarityTest {
    
    /**
     * Тест всех уровней редкости.
     */
    @Test
    void testAllRaritiesExist() {
        assertNotNull(CardRarity.COMMON);
        assertNotNull(CardRarity.RARE);
        assertNotNull(CardRarity.EPIC);
        assertNotNull(CardRarity.LEGENDARY);
    }
    
    /**
     * Тест шансов выпадения редкостей.
     * Проверяет, что шансы убывают с ростом редкости.
     */
    @Test
    void testDropChances() {
        float commonChance = CardRarity.COMMON.getDropChance();
        float rareChance = CardRarity.RARE.getDropChance();
        float epicChance = CardRarity.EPIC.getDropChance();
        float legendaryChance = CardRarity.LEGENDARY.getDropChance();
        
        // Проверяем, что шансы находятся в диапазоне [0, 1]
        assertTrue(commonChance >= 0 && commonChance <= 1);
        assertTrue(rareChance >= 0 && rareChance <= 1);
        assertTrue(epicChance >= 0 && epicChance <= 1);
        assertTrue(legendaryChance >= 0 && legendaryChance <= 1);
        
        // Проверяем, что шансы убывают с ростом редкости
        assertTrue(commonChance > rareChance);
        assertTrue(rareChance > epicChance);
        assertTrue(epicChance > legendaryChance);
    }
    
    /**
     * Тест отображаемых имён редкостей.
     */
    @Test
    void testDisplayNames() {
        assertNotNull(CardRarity.COMMON.getDisplayName());
        assertNotNull(CardRarity.RARE.getDisplayName());
        assertNotNull(CardRarity.EPIC.getDisplayName());
        assertNotNull(CardRarity.LEGENDARY.getDisplayName());
        
        assertFalse(CardRarity.COMMON.getDisplayName().isEmpty());
        assertFalse(CardRarity.RARE.getDisplayName().isEmpty());
        assertFalse(CardRarity.EPIC.getDisplayName().isEmpty());
        assertFalse(CardRarity.LEGENDARY.getDisplayName().isEmpty());
        
        // Проверяем конкретные значения
        assertEquals("Обычная", CardRarity.COMMON.getDisplayName());
        assertEquals("Редкая", CardRarity.RARE.getDisplayName());
        assertEquals("Эпическая", CardRarity.EPIC.getDisplayName());
        assertEquals("Легендарная", CardRarity.LEGENDARY.getDisplayName());
    }
    
    /**
     * Тест цветов редкостей.
     * Проверяет, что у каждой редкости есть уникальный цвет.
     */
    @Test
    void testColors() {
        int commonColor = CardRarity.COMMON.getColor();
        int rareColor = CardRarity.RARE.getColor();
        int epicColor = CardRarity.EPIC.getColor();
        int legendaryColor = CardRarity.LEGENDARY.getColor();
        
        // Цвета должны быть разными
        assertNotEquals(commonColor, rareColor);
        assertNotEquals(rareColor, epicColor);
        assertNotEquals(epicColor, legendaryColor);
        assertNotEquals(commonColor, legendaryColor);
        
        // Проверяем, что это валидные ARGB цвета (формат 0xAARRGGBB)
        // Альфа-канал должен быть установлен (первые 8 бит)
        assertTrue(commonColor != 0);
        assertTrue(rareColor != 0);
        assertTrue(epicColor != 0);
        assertTrue(legendaryColor != 0);
        
        // Проверяем конкретные значения цветов
        assertEquals(0xFF808080, commonColor);      // Серый
        assertEquals(0xFF0080FF, rareColor);        // Синий
        assertEquals(0xFF8000FF, epicColor);        // Фиолетовый
        assertEquals(0xFFFFD700, legendaryColor);   // Золотой
    }
    
    /**
     * Тест того, что сумма шансов всех редкостей не превышает 1.0
     * (или близка к 1.0, если учитываются все возможные редкости).
     */
    @Test
    void testDropChanceSum() {
        float totalChance = CardRarity.COMMON.getDropChance() +
                           CardRarity.RARE.getDropChance() +
                           CardRarity.EPIC.getDropChance() +
                           CardRarity.LEGENDARY.getDropChance();
        
        // Сумма шансов должна быть близка к 1.0
        // Допускаем небольшую погрешность
        assertEquals(1.0f, totalChance, 0.01f);
    }
    
    /**
     * Тест значений шансов для каждой редкости.
     */
    @Test
    void testSpecificDropChances() {
        assertEquals(0.70f, CardRarity.COMMON.getDropChance(), 0.001f);
        assertEquals(0.20f, CardRarity.RARE.getDropChance(), 0.001f);
        assertEquals(0.08f, CardRarity.EPIC.getDropChance(), 0.001f);
        assertEquals(0.02f, CardRarity.LEGENDARY.getDropChance(), 0.001f);
    }
}

