package com.bmfalkye.storage;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Коллекция карт игрока (все карты, которые у него есть)
 */
public class PlayerCardCollection extends SavedData {
    private final Map<UUID, Set<String>> playerCollections = new HashMap<>();
    
    public static PlayerCardCollection get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerCardCollection::load,
            PlayerCardCollection::new,
            "bm_falkye_card_collections"
        );
    }
    
    public static PlayerCardCollection load(CompoundTag tag) {
        PlayerCardCollection collection = new PlayerCardCollection();
        
        try {
            CompoundTag collectionsTag = tag.getCompound("collections");
            
            for (String key : collectionsTag.getAllKeys()) {
                // Безопасно парсим UUID
                UUID playerUUID = com.bmfalkye.util.DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid UUID during card collection load", "key", key);
                    continue;
                }
                
                try {
                    ListTag cardsTag = collectionsTag.getList(key, 8); // 8 = String tag type
                    Set<String> cardIds = new HashSet<>();
                    
                    for (int i = 0; i < cardsTag.size(); i++) {
                        String cardId = cardsTag.getString(i);
                        // Валидируем ID карты перед добавлением
                        if (com.bmfalkye.util.DataLoadValidator.isValidCardId(cardId)) {
                            cardIds.add(cardId); // Set автоматически удаляет дубликаты
                        } else {
                            com.bmfalkye.util.ModLogger.warn("Skipping invalid card ID during collection load", 
                                "cardId", cardId, "playerUUID", playerUUID.toString(), "index", i);
                        }
                    }
                    
                    // Очищаем дубликаты (на всякий случай, хотя Set уже это делает)
                    collection.playerCollections.put(playerUUID, cardIds);
                } catch (Exception e) {
                    com.bmfalkye.util.ModLogger.error("Error loading card collection for player", 
                        "uuid", playerUUID.toString(), "error", e.getMessage());
                    // Пропускаем этого игрока, продолжаем загрузку остальных
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading PlayerCardCollection, using empty collection", e);
            com.bmfalkye.util.ModLogger.error("Error loading PlayerCardCollection", "error", e.getMessage());
            // Возвращаем пустую коллекцию
            return new PlayerCardCollection();
        }
        
        return collection;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag collectionsTag = new CompoundTag();
        
        for (Map.Entry<UUID, Set<String>> entry : playerCollections.entrySet()) {
            ListTag cardsTag = new ListTag();
            for (String cardId : entry.getValue()) {
                cardsTag.add(StringTag.valueOf(cardId));
            }
            collectionsTag.put(entry.getKey().toString(), cardsTag);
        }
        
        tag.put("collections", collectionsTag);
        return tag;
    }
    
    /**
     * Получить коллекцию карт игрока
     * Set автоматически предотвращает дубликаты
     */
    public Set<String> getPlayerCollection(ServerPlayer player) {
        return playerCollections.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
    }
    
    /**
     * Добавить карту в коллекцию
     */
    public boolean addCard(ServerPlayer player, String cardId) {
        Set<String> collection = getPlayerCollection(player);
        boolean added = collection.add(cardId);
        if (added) {
            setDirty();
        }
        return added;
    }
    
    /**
     * Удалить карту из коллекции
     */
    public boolean removeCard(ServerPlayer player, String cardId) {
        Set<String> collection = getPlayerCollection(player);
        boolean removed = collection.remove(cardId);
        if (removed) {
            setDirty();
        }
        return removed;
    }
    
    /**
     * Проверить, есть ли карта в коллекции
     */
    public boolean hasCard(ServerPlayer player, String cardId) {
        return getPlayerCollection(player).contains(cardId);
    }
    
    /**
     * Получить количество карт в коллекции
     */
    public int getCardCount(ServerPlayer player) {
        return getPlayerCollection(player).size();
    }
    
    /**
     * Получить все карты игрока как список Card объектов
     */
    public List<Card> getCards(ServerPlayer player) {
        return getPlayerCollection(player).stream()
            .map(CardRegistry::getCard)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Получить карты по редкости
     */
    public List<Card> getCardsByRarity(ServerPlayer player, CardRarity rarity) {
        return getCards(player).stream()
            .filter(card -> card.getRarity() == rarity)
            .collect(Collectors.toList());
    }
    
    /**
     * Удалить случайные карты из коллекции (при проигрыше)
     * @param player Игрок, у которого забирают карты
     * @param count Количество карт для удаления
     * @return Список ID удаленных карт
     */
    public List<String> removeRandomCards(ServerPlayer player, int count) {
        Set<String> collection = getPlayerCollection(player);
        if (collection.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> cardIds = new ArrayList<>(collection);
        List<String> removed = new ArrayList<>();
        Random random = new Random();
        
        // Взвешенный выбор карт по редкости
        // Очень редкие карты имеют очень малый шанс быть выбранными
        for (int i = 0; i < count && !cardIds.isEmpty(); i++) {
            String selectedCardId = selectRandomCardWithRarityWeight(cardIds, random);
            if (selectedCardId != null) {
                removed.add(selectedCardId);
                cardIds.remove(selectedCardId);
                collection.remove(selectedCardId);
            }
        }
        
        if (!removed.isEmpty()) {
            setDirty();
        }
        
        return removed;
    }
    
    /**
     * Выбрать случайную карту с учетом веса редкости
     * Очень редкие карты имеют очень малый шанс быть выбранными
     */
    private String selectRandomCardWithRarityWeight(List<String> cardIds, Random random) {
        if (cardIds.isEmpty()) {
            return null;
        }
        
        // Создаем взвешенный список
        List<String> weightedList = new ArrayList<>();
        
        for (String cardId : cardIds) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                int weight = getRarityWeight(card.getRarity());
                // Добавляем карту несколько раз в зависимости от веса
                for (int i = 0; i < weight; i++) {
                    weightedList.add(cardId);
                }
            } else {
                // Если карта не найдена, добавляем с минимальным весом
                weightedList.add(cardId);
            }
        }
        
        if (weightedList.isEmpty()) {
            return cardIds.get(random.nextInt(cardIds.size()));
        }
        
        return weightedList.get(random.nextInt(weightedList.size()));
    }
    
    /**
     * Получить вес редкости (чем выше редкость, тем меньше вес)
     */
    private int getRarityWeight(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> 100;      // Обычные - самый высокий шанс
            case RARE -> 20;          // Редкие
            case EPIC -> 5;           // Эпические
            case LEGENDARY -> 1;      // Легендарные - очень малый шанс
        };
    }
    
    /**
     * Добавить карты в коллекцию (при победе)
     */
    public void addCards(ServerPlayer player, List<String> cardIds) {
        Set<String> collection = getPlayerCollection(player);
        boolean changed = false;
        for (String cardId : cardIds) {
            if (collection.add(cardId)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }
    
    /**
     * Проверить, является ли это первым входом игрока
     */
    public boolean isFirstJoin(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        // Проверяем, есть ли запись для игрока
        if (!playerCollections.containsKey(playerUUID)) {
            return true;
        }
        // Проверяем, пуста ли коллекция (после очистки дубликатов)
        Set<String> collection = getPlayerCollection(player);
        return collection.isEmpty();
    }
    
    /**
     * Очистить дубликаты из коллекции игрока (если они есть)
     * @return true, если были найдены и удалены дубликаты
     */
    public boolean removeDuplicates(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        if (!playerCollections.containsKey(playerUUID)) {
            return false;
        }
        
        Set<String> oldCollection = playerCollections.get(playerUUID);
        Set<String> newCollection = new HashSet<>(oldCollection);
        
        // Если размер изменился, значит были дубликаты
        if (oldCollection.size() != newCollection.size()) {
            playerCollections.put(playerUUID, newCollection);
            setDirty();
            return true;
        }
        
        return false;
    }
}

