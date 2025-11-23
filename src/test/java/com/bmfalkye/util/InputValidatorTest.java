package com.bmfalkye.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для InputValidator
 */
@DisplayName("InputValidator Tests")
public class InputValidatorTest {
    
    @Test
    @DisplayName("Should validate bet amounts")
    void testBetValidation() {
        assertTrue(com.bmfalkye.util.InputValidator.isValidBet(100), "Valid bet should pass");
        assertTrue(com.bmfalkye.util.InputValidator.isValidBet(1), "Minimum bet should pass");
        assertTrue(com.bmfalkye.util.InputValidator.isValidBet(100000), "Maximum bet should pass");
        
        assertFalse(com.bmfalkye.util.InputValidator.isValidBet(0), "Zero bet should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidBet(-1), "Negative bet should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidBet(100001), "Bet over maximum should fail");
    }
    
    @Test
    @DisplayName("Should validate UUIDs")
    void testUUIDValidation() {
        UUID validUUID = UUID.randomUUID();
        assertTrue(com.bmfalkye.util.InputValidator.isValidPlayerUUID(validUUID), "Valid UUID should pass");
        
        assertFalse(com.bmfalkye.util.InputValidator.isValidPlayerUUID(null), "Null UUID should fail");
    }
    
    @Test
    @DisplayName("Should validate card IDs")
    void testCardIdValidation() {
        assertTrue(com.bmfalkye.util.InputValidator.isValidCardId("fire_dragon_ignisar"), "Valid card ID should pass");
        assertTrue(com.bmfalkye.util.InputValidator.isValidCardId("test_card_123"), "Alphanumeric card ID should pass");
        
        assertFalse(com.bmfalkye.util.InputValidator.isValidCardId(null), "Null card ID should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidCardId(""), "Empty card ID should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidCardId("   "), "Whitespace-only card ID should fail");
    }
    
    @Test
    @DisplayName("Should validate deck names")
    void testDeckNameValidation() {
        assertTrue(com.bmfalkye.util.InputValidator.isValidDeckName("My Deck"), "Valid deck name should pass");
        assertTrue(com.bmfalkye.util.InputValidator.isValidDeckName("Deck 123"), "Deck name with numbers should pass");
        
        assertFalse(com.bmfalkye.util.InputValidator.isValidDeckName(null), "Null deck name should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidDeckName(""), "Empty deck name should fail");
        assertFalse(com.bmfalkye.util.InputValidator.isValidDeckName("   "), "Whitespace-only deck name should fail");
    }
}

