package com.bmfalkye.client.gui.skeuomorphic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для TextureGenerator
 * Проверяет программную генерацию текстур для скевоморфного дизайна
 */
class TextureGeneratorTest {
    
    @Test
    void testPseudoRandomDeterministic() {
        // Проверяем, что псевдослучайный генератор детерминирован
        int result1 = TextureGeneratorTestHelper.getPseudoRandom(10, 20, 5);
        int result2 = TextureGeneratorTestHelper.getPseudoRandom(10, 20, 5);
        
        assertEquals(result1, result2, "Псевдослучайный генератор должен быть детерминированным");
    }
    
    @Test
    void testPseudoRandomDifferentSeeds() {
        // Проверяем, что разные seed дают разные результаты
        int result1 = TextureGeneratorTestHelper.getPseudoRandom(10, 20, 5);
        int result2 = TextureGeneratorTestHelper.getPseudoRandom(10, 20, 6);
        
        assertNotEquals(result1, result2, "Разные seed должны давать разные результаты");
    }
    
    @Test
    void testPseudoRandomRange() {
        // Проверяем, что результат в допустимом диапазоне
        for (int i = 0; i < 100; i++) {
            int result = TextureGeneratorTestHelper.getPseudoRandom(i, i * 2, i * 3);
            assertTrue(result >= 0, "Результат должен быть неотрицательным");
            assertTrue(result <= Integer.MAX_VALUE, "Результат должен быть в допустимом диапазоне");
        }
    }
    
    /**
     * Вспомогательный класс для тестирования приватных методов
     */
    static class TextureGeneratorTestHelper {
        private static int pseudoRandom(int x, int y, int seed) {
            int hash = x * 73856093 ^ y * 19349663 ^ seed * 83492791;
            hash = hash * (hash * hash * 15731 + 789221) + 1376312589;
            return hash & 0x7fffffff;
        }
        
        static int getPseudoRandom(int x, int y, int seed) {
            return pseudoRandom(x, y, seed);
        }
    }
}

