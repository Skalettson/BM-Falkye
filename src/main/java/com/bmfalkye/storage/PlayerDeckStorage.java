package com.bmfalkye.storage;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Система сохранения колод игроков
 */
public class PlayerDeckStorage extends SavedData {
    private final Map<UUID, PlayerDeckData> playerDecks = new HashMap<>();

    public static PlayerDeckStorage load(CompoundTag tag) {
        PlayerDeckStorage storage = new PlayerDeckStorage();
        
        try {
            CompoundTag decksTag = tag.getCompound("decks");
            
            // Используем общий метод загрузки
            Map<UUID, Object> loadedDecks = DeckStorageHelper.loadDecksFromNBT(decksTag, true);
            
            // Преобразуем в нужный формат
            for (Map.Entry<UUID, Object> entry : loadedDecks.entrySet()) {
                if (entry.getValue() instanceof PlayerDeckData) {
                    storage.playerDecks.put(entry.getKey(), (PlayerDeckData) entry.getValue());
                }
            }
        } catch (Exception e) {
            return DeckStorageHelper.handleLoadError("PlayerDeckStorage", e, PlayerDeckStorage::new);
        }
        
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag decksTag = new CompoundTag();
        
        // Используем общий метод сохранения
        @SuppressWarnings("unchecked")
        Map<UUID, PlayerDeckData> typedMap = (Map<UUID, PlayerDeckData>) (Map<?, ?>) playerDecks;
        DeckStorageHelper.saveDecksToNBT(decksTag, typedMap, true);
        
        tag.put("decks", decksTag);
        return tag;
    }

    public PlayerDeckData getPlayerDeck(ServerPlayer player) {
        return playerDecks.computeIfAbsent(player.getUUID(), k -> new PlayerDeckData());
    }

    public void setPlayerDeck(ServerPlayer player, PlayerDeckData deckData) {
        playerDecks.put(player.getUUID(), deckData);
        setDirty();
    }

    public static PlayerDeckStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PlayerDeckStorage::load,
            PlayerDeckStorage::new,
            "bm_falkye_decks"
        );
    }

    /**
     * Данные колоды игрока
     */
    public static class PlayerDeckData {
        private List<String> cardIds = new ArrayList<>();
        private String leaderId;
        private String deckName = "Моя колода";

        public CardDeck createDeck(ServerPlayer player, ServerLevel level) {
            CardDeck deck = new CardDeck();
            deck = new CardDeck(new ArrayList<>()); // Создаём пустую колоду
            
            // ВСЕГДА загружаем карты из коллекции игрока, чтобы гарантировать синхронизацию
            com.bmfalkye.storage.PlayerCardCollection collection = 
                com.bmfalkye.storage.PlayerCardCollection.get(level);
            java.util.List<Card> collectionCards = collection.getCards(player);
            
            // Используем Set для отслеживания уже добавленных карт (по ID)
            java.util.Set<String> addedCardIds = new java.util.HashSet<>();
            
            // Если колода не пустая, проверяем, что все карты есть в коллекции
            if (!cardIds.isEmpty()) {
                for (String cardId : cardIds) {
                    // Пропускаем дубликаты
                    if (addedCardIds.contains(cardId)) {
                        continue;
                    }
                    
                    // Проверяем, что карта есть в коллекции игрока
                    boolean hasCard = collectionCards.stream()
                        .anyMatch(card -> card.getId().equals(cardId));
                    
                    if (hasCard) {
                        Card card = CardRegistry.getCard(cardId);
                        if (card != null) {
                            deck.addCard(card);
                            addedCardIds.add(cardId);
                        }
                    }
                    // Если карты нет в коллекции, просто пропускаем её
                }
            }
            
            // Если колода пустая или недостаточно карт, добавляем карты из коллекции (без дубликатов)
            if (deck.size() < 10) {
                for (Card card : collectionCards) {
                    // Проверяем, что карта еще не добавлена
                    if (!addedCardIds.contains(card.getId())) {
                        deck.addCard(card);
                        addedCardIds.add(card.getId());
                    }
                }
            }
            
            // Если коллекция тоже пустая, используем дефолтную колоду (только для тестирования)
            if (collectionCards.isEmpty() && deck.size() == 0) {
                deck = new CardDeck(); // Дефолтная колода
            }
            
            return deck;
        }
        
        // Обратная совместимость
        public CardDeck createDeck() {
            CardDeck deck = new CardDeck(new ArrayList<>()); // Создаём пустую колоду
            java.util.Set<String> addedCardIds = new java.util.HashSet<>();
            for (String cardId : cardIds) {
                // Пропускаем дубликаты
                if (addedCardIds.contains(cardId)) {
                    continue;
                }
                Card card = CardRegistry.getCard(cardId);
                if (card != null) {
                    deck.addCard(card);
                    addedCardIds.add(cardId);
                }
            }
            return deck;
        }

        public LeaderCard getLeader() {
            if (leaderId != null) {
                return LeaderRegistry.getLeader(leaderId);
            }
            return null;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag cardsTag = new ListTag();
            for (String cardId : cardIds) {
                cardsTag.add(StringTag.valueOf(cardId));
            }
            tag.put("cards", cardsTag);
            if (leaderId != null) {
                tag.putString("leader", leaderId);
            }
            tag.putString("name", deckName);
            return tag;
        }

        public static PlayerDeckData fromNBT(CompoundTag tag) {
            PlayerDeckData data = new PlayerDeckData();
            
            try {
                // Загружаем карты с валидацией
                ListTag cardsTag = tag.getList("cards", Tag.TAG_STRING);
                for (int i = 0; i < cardsTag.size(); i++) {
                    String cardId = cardsTag.getString(i);
                    if (com.bmfalkye.util.DataLoadValidator.isValidCardId(cardId)) {
                        data.cardIds.add(cardId);
                    } else {
                        com.bmfalkye.util.ModLogger.warn("Skipping invalid card ID during deck load", 
                            "cardId", cardId, "index", i);
                    }
                }
                
                // Ограничиваем количество карт в колоде
                if (data.cardIds.size() > 100) {
                    com.bmfalkye.util.ModLogger.warn("Deck has too many cards, truncating", 
                        "count", data.cardIds.size());
                    data.cardIds = new ArrayList<>(data.cardIds.subList(0, 100));
                }
                
                // Загружаем лидера с валидацией
                if (tag.contains("leader")) {
                    String leaderId = tag.getString("leader");
                    if (com.bmfalkye.util.DataLoadValidator.isValidLeaderId(leaderId)) {
                        data.leaderId = leaderId;
                    } else {
                        com.bmfalkye.util.ModLogger.warn("Invalid leader ID during deck load", "leaderId", leaderId);
                        data.leaderId = null;
                    }
                }
                
                // Загружаем имя колоды с валидацией
                if (tag.contains("name")) {
                    data.deckName = com.bmfalkye.util.DataLoadValidator.validateDeckName(tag.getString("name"));
                }
            } catch (Exception e) {
                com.bmfalkye.BMFalkye.LOGGER.error("Error loading PlayerDeckData, using defaults", e);
                com.bmfalkye.util.ModLogger.error("Error loading PlayerDeckData", "error", e.getMessage());
                // Возвращаем данные с дефолтными значениями
                return new PlayerDeckData();
            }
            
            return data;
        }

        // Геттеры и сеттеры
        public List<String> getCardIds() { return cardIds; }
        public void setCardIds(List<String> cardIds) { this.cardIds = cardIds; }
        public String getLeaderId() { return leaderId; }
        public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
        public String getDeckName() { return deckName; }
        public void setDeckName(String deckName) { this.deckName = deckName; }
    }
}

