package com.bmfalkye.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для LocalizationHelper
 */
@DisplayName("LocalizationHelper Tests")
public class LocalizationHelperTest {
    
    @BeforeEach
    void setUp() {
        com.bmfalkye.util.LocalizationHelper.setLanguage("ru_ru");
    }
    
    @Test
    @DisplayName("Should set and get language")
    void testSetAndGetLanguage() {
        com.bmfalkye.util.LocalizationHelper.setLanguage("en_us");
        assertEquals("en_us", com.bmfalkye.util.LocalizationHelper.getCurrentLanguage(), "Language should be set correctly");
    }
    
    @Test
    @DisplayName("Should return default language for null input")
    void testNullLanguageInput() {
        com.bmfalkye.util.LocalizationHelper.setLanguage(null);
        // Должен остаться предыдущий язык или вернуться к дефолтному
        assertNotNull(com.bmfalkye.util.LocalizationHelper.getCurrentLanguage(), "Language should not be null");
    }
    
    @Test
    @DisplayName("Should return default language for empty input")
    void testEmptyLanguageInput() {
        String previousLanguage = com.bmfalkye.util.LocalizationHelper.getCurrentLanguage();
        com.bmfalkye.util.LocalizationHelper.setLanguage("");
        assertEquals(previousLanguage, com.bmfalkye.util.LocalizationHelper.getCurrentLanguage(), "Language should not change for empty input");
    }
    
    @Test
    @DisplayName("Should get localized string")
    void testGetLocalizedString() {
        String result = com.bmfalkye.util.LocalizationHelper.getLocalizedString("test.key", "arg1", "arg2");
        assertNotNull(result, "Localized string should not be null");
    }
    
    @Test
    @DisplayName("Should get localized component")
    void testGetLocalizedComponent() {
        net.minecraft.network.chat.Component component = com.bmfalkye.util.LocalizationHelper.getLocalizedComponent("test.key");
        assertNotNull(component, "Localized component should not be null");
    }
    
    @Test
    @DisplayName("Should add language support")
    void testAddLanguageSupport() {
        boolean result = com.bmfalkye.util.LocalizationHelper.addLanguageSupport("de_de");
        assertTrue(result, "Language support should be added");
    }
}

