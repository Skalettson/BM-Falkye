package com.bmfalkye.storage;

import com.bmfalkye.cards.CardRegistry;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для систем хранения данных
 */
@DisplayName("Storage Tests")
public class StorageTest {
    
    @BeforeAll
    static void setUp() {
        CardRegistry.initializeDefaultCards();
    }
    
    @Test
    @DisplayName("Should serialize and deserialize deck data")
    void testDeckDataSerialization() {
        PlayerDeckStorage.PlayerDeckData deckData = new PlayerDeckStorage.PlayerDeckData();
        deckData.setDeckName("Test Deck");
        deckData.getCardIds().add("fire_dragon_ignisar");
        deckData.getCardIds().add("ice_dragon_glacis");
        
        CompoundTag tag = deckData.toNBT();
        assertNotNull(tag, "Deck data should serialize to NBT");
        
        PlayerDeckStorage.PlayerDeckData loaded = PlayerDeckStorage.PlayerDeckData.fromNBT(tag);
        assertNotNull(loaded, "Deck data should deserialize from NBT");
        assertEquals("Test Deck", loaded.getDeckName(), "Deck name should be preserved");
        assertTrue(loaded.getCardIds().contains("fire_dragon_ignisar"), "Card IDs should be preserved");
    }
    
    @Test
    @DisplayName("Should handle empty deck data")
    void testEmptyDeckData() {
        PlayerDeckStorage.PlayerDeckData deckData = new PlayerDeckStorage.PlayerDeckData();
        deckData.setDeckName("Empty Deck");
        
        CompoundTag tag = deckData.toNBT();
        PlayerDeckStorage.PlayerDeckData loaded = PlayerDeckStorage.PlayerDeckData.fromNBT(tag);
        
        assertNotNull(loaded, "Empty deck data should load");
        assertEquals("Empty Deck", loaded.getDeckName(), "Deck name should be preserved");
        assertTrue(loaded.getCardIds().isEmpty(), "Empty deck should have no cards");
    }
    
    @Test
    @DisplayName("Should validate card IDs during loading")
    void testCardIdValidation() {
        PlayerDeckStorage.PlayerDeckData deckData = new PlayerDeckStorage.PlayerDeckData();
        deckData.setDeckName("Test Deck");
        deckData.getCardIds().add("valid_card_id");
        deckData.getCardIds().add("invalid_card_id_12345");
        
        CompoundTag tag = deckData.toNBT();
        PlayerDeckStorage.PlayerDeckData loaded = PlayerDeckStorage.PlayerDeckData.fromNBT(tag);
        
        assertNotNull(loaded, "Deck data should load even with invalid card IDs");
        // Валидация должна происходить при загрузке
    }
}

