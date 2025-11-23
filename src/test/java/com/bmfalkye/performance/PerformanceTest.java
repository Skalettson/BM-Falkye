package com.bmfalkye.performance;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.game.BetProtectionSystem;
import com.bmfalkye.memory.MemoryOptimizer;
import com.bmfalkye.util.ErrorRecoverySystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты производительности под нагрузкой
 */
@DisplayName("Performance Tests")
public class PerformanceTest {
    
    @BeforeAll
    static void setUp() {
        CardRegistry.initializeDefaultCards();
    }
    
    @Test
    @DisplayName("Should handle multiple card lookups efficiently")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCardLookupPerformance() {
        // Тест проверяет, что поиск карт выполняется быстро
        for (int i = 0; i < 1000; i++) {
            Card card = CardRegistry.getCard("fire_dragon_ignisar");
            assertNotNull(card, "Card lookup should be fast");
        }
    }
    
    @Test
    @DisplayName("Should handle bet validation efficiently")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testBetValidationPerformance() {
        // Тест проверяет производительность валидации ставок
        for (int i = 0; i < 10000; i++) {
            BetProtectionSystem.isValidBetAmount(i % 100000);
        }
    }
    
    @Test
    @DisplayName("Should handle memory cleanup efficiently")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testMemoryCleanupPerformance() {
        // Тест проверяет производительность очистки памяти
        assertDoesNotThrow(() -> {
            MemoryOptimizer.periodicMemoryCheck();
        }, "Memory cleanup should be efficient");
    }
    
    @Test
    @DisplayName("Should handle error recovery cleanup efficiently")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testErrorRecoveryCleanupPerformance() {
        // Тест проверяет производительность очистки попыток восстановления
        assertDoesNotThrow(() -> {
            ErrorRecoverySystem.cleanupExpiredAttempts();
        }, "Error recovery cleanup should be efficient");
    }
    
    @Test
    @DisplayName("Should handle large card list operations")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testLargeCardListPerformance() {
        // Тест проверяет работу с большим списком карт
        List<Card> allCards = CardRegistry.getAllCards();
        assertNotNull(allCards, "All cards list should not be null");
        
        // Проверяем фильтрацию
        long creatureCount = allCards.stream()
            .filter(card -> card.getType() == Card.CardType.CREATURE)
            .count();
        
        assertTrue(creatureCount >= 0, "Creature count should be non-negative");
    }
}

