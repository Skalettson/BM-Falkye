package com.bmfalkye.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Менеджер колод игрока (до 10 слотов)
 */
public class PlayerDeckManager extends SavedData {
    private static final int MAX_DECKS = 10;
    private final Map<UUID, List<PlayerDeckStorage.PlayerDeckData>> playerDecks = new HashMap<>();
    
    public static PlayerDeckManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerDeckManager::load,
            PlayerDeckManager::new,
            "bm_falkye_deck_manager"
        );
    }
    
    public static PlayerDeckManager load(CompoundTag tag) {
        PlayerDeckManager manager = new PlayerDeckManager();
        
        try {
            CompoundTag decksTag = tag.getCompound("decks");
            
            // Используем общий метод загрузки
            Map<UUID, Object> loadedDecks = DeckStorageHelper.loadDecksFromNBT(decksTag, false);
            
            // Преобразуем в нужный формат
            for (Map.Entry<UUID, Object> entry : loadedDecks.entrySet()) {
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<PlayerDeckStorage.PlayerDeckData> decks = 
                        (List<PlayerDeckStorage.PlayerDeckData>) entry.getValue();
                    manager.playerDecks.put(entry.getKey(), decks);
                }
            }
        } catch (Exception e) {
            return DeckStorageHelper.handleLoadError("PlayerDeckManager", e, PlayerDeckManager::new);
        }
        
        return manager;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag decksTag = new CompoundTag();
        
        // Используем общий метод сохранения
        @SuppressWarnings("unchecked")
        Map<UUID, List<PlayerDeckStorage.PlayerDeckData>> typedMap = 
            (Map<UUID, List<PlayerDeckStorage.PlayerDeckData>>) (Map<?, ?>) playerDecks;
        DeckStorageHelper.saveDecksToNBT(decksTag, typedMap, false);
        
        tag.put("decks", decksTag);
        return tag;
    }
    
    public List<PlayerDeckStorage.PlayerDeckData> getPlayerDecks(ServerPlayer player) {
        return DeckStorageHelper.getOrCreatePlayerDecksList(playerDecks, player.getUUID());
    }
    
    public boolean addDeck(ServerPlayer player, PlayerDeckStorage.PlayerDeckData deck) {
        List<PlayerDeckStorage.PlayerDeckData> decks = getPlayerDecks(player);
        if (decks.size() >= MAX_DECKS) {
            return false;
        }
        decks.add(deck);
        setDirty();
        return true;
    }
    
    public boolean removeDeck(ServerPlayer player, int index) {
        List<PlayerDeckStorage.PlayerDeckData> decks = getPlayerDecks(player);
        if (!DeckStorageHelper.isValidDeckIndex(index, decks.size())) {
            return false;
        }
        decks.remove(index);
        setDirty();
        return true;
    }
    
    public PlayerDeckStorage.PlayerDeckData getDeck(ServerPlayer player, int index) {
        List<PlayerDeckStorage.PlayerDeckData> decks = getPlayerDecks(player);
        if (!DeckStorageHelper.isValidDeckIndex(index, decks.size())) {
            return null;
        }
        return decks.get(index);
    }
    
    public int getDeckCount(ServerPlayer player) {
        return getPlayerDecks(player).size();
    }
}

