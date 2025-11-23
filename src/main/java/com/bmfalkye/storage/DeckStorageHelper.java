package com.bmfalkye.storage;

import com.bmfalkye.util.DataLoadValidator;
import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Вспомогательный класс для устранения дублирующейся логики между PlayerDeckStorage и PlayerDeckManager
 * Содержит общие методы для работы с сохранением и загрузкой колод
 */
public class DeckStorageHelper {
    
    /**
     * Загружает колоды игрока из NBT тега
     * @param decksTag CompoundTag с данными колод
     * @param singleDeckMode Если true, загружает одну колоду на игрока, иначе список колод
     * @return Map с UUID игрока -> данные колод
     */
    public static Map<UUID, Object> loadDecksFromNBT(CompoundTag decksTag, boolean singleDeckMode) {
        Map<UUID, Object> result = new HashMap<>();
        
        for (String key : decksTag.getAllKeys()) {
            // Безопасно парсим UUID
            UUID playerUUID = DataLoadValidator.safeParseUUID(key);
            if (playerUUID == null) {
                ModLogger.warn("Skipping invalid UUID during deck load", "key", key);
                continue;
            }
            
            try {
                if (singleDeckMode) {
                    // Режим одной колоды на игрока (PlayerDeckStorage)
                    CompoundTag deckTag = decksTag.getCompound(key);
                    PlayerDeckStorage.PlayerDeckData deckData = PlayerDeckStorage.PlayerDeckData.fromNBT(deckTag);
                    result.put(playerUUID, deckData);
                } else {
                    // Режим нескольких колод на игрока (PlayerDeckManager)
                    CompoundTag playerDecksTag = decksTag.getCompound(key);
                    List<PlayerDeckStorage.PlayerDeckData> decks = loadPlayerDecksList(playerDecksTag);
                    result.put(playerUUID, decks);
                }
            } catch (Exception e) {
                java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectContext(e,
                    "uuid", playerUUID.toString(),
                    "key", key,
                    "singleDeckMode", singleDeckMode,
                    "operation", "loadDecksFromNBT");
                ModLogger.error("Error loading deck(s) for player", e, context);
                // Пропускаем этого игрока, продолжаем загрузку остальных
            }
        }
        
        return result;
    }
    
    /**
     * Загружает список колод игрока из NBT тега
     * @param playerDecksTag CompoundTag с данными колод игрока
     * @return Список колод
     */
    public static List<PlayerDeckStorage.PlayerDeckData> loadPlayerDecksList(CompoundTag playerDecksTag) {
        List<PlayerDeckStorage.PlayerDeckData> decks = new ArrayList<>();
        int MAX_DECKS = 10;
        
        // Валидируем количество колод
        int deckCount = Math.min(playerDecksTag.getInt("count"), MAX_DECKS);
        if (deckCount < 0) {
            deckCount = 0;
        }
        
        for (int i = 0; i < deckCount; i++) {
            try {
                CompoundTag deckTag = playerDecksTag.getCompound("deck_" + i);
                PlayerDeckStorage.PlayerDeckData deckData = PlayerDeckStorage.PlayerDeckData.fromNBT(deckTag);
                decks.add(deckData);
            } catch (Exception e) {
                java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectContext(e,
                    "index", i,
                    "operation", "loadPlayerDecksList");
                ModLogger.error("Error loading deck at index", e, context);
                // Пропускаем эту колоду, продолжаем загрузку остальных
            }
        }
        
        // Ограничиваем количество колод
        if (decks.size() > MAX_DECKS) {
            ModLogger.warn("Player has too many decks, truncating", 
                "count", decks.size());
            decks = new ArrayList<>(decks.subList(0, MAX_DECKS));
        }
        
        return decks;
    }
    
    /**
     * Сохраняет колоды игрока в NBT тег
     * @param decksTag CompoundTag для сохранения
     * @param playerDecksMap Map с UUID игрока -> данные колод
     * @param singleDeckMode Если true, сохраняет одну колоду на игрока, иначе список колод
     */
    public static void saveDecksToNBT(CompoundTag decksTag, Map<UUID, ?> playerDecksMap, boolean singleDeckMode) {
        for (Map.Entry<UUID, ?> entry : playerDecksMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            Object deckData = entry.getValue();
            
            if (singleDeckMode) {
                // Режим одной колоды на игрока (PlayerDeckStorage)
                if (deckData instanceof PlayerDeckStorage.PlayerDeckData) {
                    decksTag.put(playerUUID.toString(), ((PlayerDeckStorage.PlayerDeckData) deckData).toNBT());
                }
            } else {
                // Режим нескольких колод на игрока (PlayerDeckManager)
                if (deckData instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<PlayerDeckStorage.PlayerDeckData> decks = (List<PlayerDeckStorage.PlayerDeckData>) deckData;
                    CompoundTag playerDecksTag = new CompoundTag();
                    playerDecksTag.putInt("count", decks.size());
                    
                    for (int i = 0; i < decks.size(); i++) {
                        playerDecksTag.put("deck_" + i, decks.get(i).toNBT());
                    }
                    
                    decksTag.put(playerUUID.toString(), playerDecksTag);
                }
            }
        }
    }
    
    /**
     * Обрабатывает ошибку загрузки и возвращает пустое хранилище
     * @param storageName Имя хранилища для логирования
     * @param e Исключение
     * @return Пустое хранилище
     */
    public static <T extends SavedData> T handleLoadError(String storageName, Exception e, java.util.function.Supplier<T> emptySupplier) {
        java.util.Map<String, Object> context = com.bmfalkye.util.ErrorContextCollector.collectDataOperationContext(
            "load", storageName, null);
        com.bmfalkye.BMFalkye.LOGGER.error("Error loading " + storageName + ", using empty storage", e);
        ModLogger.error("Error loading " + storageName, e, context);
        return emptySupplier.get();
    }
    
    /**
     * Валидирует индекс колоды
     * @param index Индекс для проверки
     * @param deckCount Количество колод
     * @return true если индекс валиден
     */
    public static boolean isValidDeckIndex(int index, int deckCount) {
        return index >= 0 && index < deckCount;
    }
    
    /**
     * Получает или создаёт список колод для игрока
     * @param playerDecksMap Map с колодами игроков
     * @param playerUUID UUID игрока
     * @return Список колод игрока
     */
    @SuppressWarnings("unchecked")
    public static List<PlayerDeckStorage.PlayerDeckData> getOrCreatePlayerDecksList(
            Map<UUID, List<PlayerDeckStorage.PlayerDeckData>> playerDecksMap, UUID playerUUID) {
        return playerDecksMap.computeIfAbsent(playerUUID, k -> new ArrayList<>());
    }
}

