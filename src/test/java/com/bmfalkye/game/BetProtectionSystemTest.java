package com.bmfalkye.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для BetProtectionSystem
 */
@DisplayName("BetProtectionSystem Tests")
public class BetProtectionSystemTest {
    
    @BeforeEach
    void setUp() {
        // Очищаем состояние перед каждым тестом
        BetProtectionSystem.cleanupExpiredLocks();
    }
    
    @Test
    @DisplayName("Should validate bet amount")
    void testValidateBetAmount() {
        assertTrue(BetProtectionSystem.isValidBetAmount(100), "Valid bet amount should pass");
        assertTrue(BetProtectionSystem.isValidBetAmount(1), "Minimum bet amount should pass");
        assertTrue(BetProtectionSystem.isValidBetAmount(100000), "Maximum bet amount should pass");
        
        assertFalse(BetProtectionSystem.isValidBetAmount(0), "Zero bet should fail");
        assertFalse(BetProtectionSystem.isValidBetAmount(-1), "Negative bet should fail");
        assertFalse(BetProtectionSystem.isValidBetAmount(100001), "Bet over maximum should fail");
    }
    
    @Test
    @DisplayName("Should cleanup expired locks")
    void testCleanupExpiredLocks() {
        assertDoesNotThrow(() -> {
            BetProtectionSystem.cleanupExpiredLocks();
        }, "Cleanup should not throw exception");
    }
    
    @Test
    @DisplayName("Should get min and max bet amounts")
    void testGetBetAmounts() {
        int minBet = BetProtectionSystem.getMinBetAmount();
        int maxBet = BetProtectionSystem.getMaxBetAmount();
        
        assertTrue(minBet > 0, "Min bet should be positive");
        assertTrue(maxBet > minBet, "Max bet should be greater than min bet");
    }
}

