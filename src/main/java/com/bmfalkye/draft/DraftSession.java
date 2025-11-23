package com.bmfalkye.draft;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Сессия драфта для режима "Великий Турнир"
 * Игрок выбирает 30 карт из 90 предложенных (30 раз по 3 карты)
 */
public class DraftSession {
    private UUID playerUUID;
    private String playerName;
    private List<String> selectedCards = new ArrayList<>(); // Выбранные карты
    private List<CardChoice> choices = new ArrayList<>(); // Все предложенные выборы
    private int currentChoiceIndex = 0; // Текущий выбор (0-29)
    private boolean isCompleted = false;
    private boolean isActive = true;
    
    // Статистика арены
    private int wins = 0;
    private int losses = 0;
    private boolean arenaCompleted = false;
    
    // Лидер для драфта (выбирается случайно или игроком)
    private LeaderCard selectedLeader;
    
    public DraftSession(ServerPlayer player) {
        this.playerUUID = player.getUUID();
        this.playerName = player.getName().getString();
        generateChoices();
        // Выбираем случайного лидера
        List<LeaderCard> allLeaders = LeaderRegistry.getAllLeaders();
        if (!allLeaders.isEmpty()) {
            this.selectedLeader = allLeaders.get(new Random().nextInt(allLeaders.size()));
        }
    }
    
    /**
     * Пустой конструктор для загрузки из NBT
     */
    private DraftSession() {
        // Инициализация полей
        this.selectedCards = new ArrayList<>();
        this.choices = new ArrayList<>();
    }
    
    /**
     * Генерирует 30 выборов по 3 карты каждый
     */
    private void generateChoices() {
        Random random = new Random();
        List<Card> allCards = CardRegistry.getAllCards();
        
        for (int i = 0; i < 30; i++) {
            List<Card> choiceCards = new ArrayList<>();
            Set<String> usedCardIds = new HashSet<>();
            
            // Выбираем 3 случайные карты
            while (choiceCards.size() < 3) {
                Card randomCard = allCards.get(random.nextInt(allCards.size()));
                if (!usedCardIds.contains(randomCard.getId())) {
                    choiceCards.add(randomCard);
                    usedCardIds.add(randomCard.getId());
                }
            }
            
            choices.add(new CardChoice(i, choiceCards));
        }
    }
    
    /**
     * Выбирает карту из текущего выбора
     */
    public boolean selectCard(int choiceIndex) {
        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            return false;
        }
        
        CardChoice choice = choices.get(choiceIndex);
        if (choice.isSelected()) {
            return false; // Уже выбрано
        }
        
        // Выбираем первую карту из выбора (в GUI игрок выберет конкретную)
        // Пока что просто берём первую
        if (!choice.getCards().isEmpty()) {
            Card selectedCard = choice.getCards().get(0);
            selectedCards.add(selectedCard.getId());
            choice.setSelected(true);
            currentChoiceIndex++;
            
            // Проверяем, завершён ли драфт
            if (currentChoiceIndex >= 30) {
                isCompleted = true;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Выбирает конкретную карту из выбора
     */
    public boolean selectCardFromChoice(int choiceIndex, int cardIndex) {
        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            return false;
        }
        
        CardChoice choice = choices.get(choiceIndex);
        if (choice.isSelected()) {
            return false;
        }
        
        if (cardIndex < 0 || cardIndex >= choice.getCards().size()) {
            return false;
        }
        
        Card selectedCard = choice.getCards().get(cardIndex);
        selectedCards.add(selectedCard.getId());
        choice.setSelected(true);
        currentChoiceIndex++;
        
        if (currentChoiceIndex >= 30) {
            isCompleted = true;
        }
        
        return true;
    }
    
    /**
     * Создаёт колоду из выбранных карт
     */
    public CardDeck createDeck() {
        if (!isCompleted || selectedCards.size() != 30) {
            return null;
        }
        
        CardDeck deck = new CardDeck();
        for (String cardId : selectedCards) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                deck.addCard(card);
            }
        }
        
        return deck;
    }
    
    /**
     * Записывает победу в арене
     */
    public void recordWin() {
        wins++;
        if (wins >= 7) {
            arenaCompleted = true;
            isActive = false;
        }
    }
    
    /**
     * Записывает поражение в арене
     */
    public void recordLoss() {
        losses++;
        if (losses >= 3) {
            arenaCompleted = true;
            isActive = false;
        }
    }
    
    // Геттеры
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public List<String> getSelectedCards() { return new ArrayList<>(selectedCards); }
    public List<CardChoice> getChoices() { return new ArrayList<>(choices); }
    public CardChoice getCurrentChoice() {
        if (currentChoiceIndex < choices.size()) {
            return choices.get(currentChoiceIndex);
        }
        return null;
    }
    public int getCurrentChoiceIndex() { return currentChoiceIndex; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public boolean isArenaCompleted() { return arenaCompleted; }
    public LeaderCard getSelectedLeader() { return selectedLeader; }
    public void setSelectedLeader(LeaderCard leader) { this.selectedLeader = leader; }
    
    /**
     * Сохраняет сессию в NBT
     */
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
        tag.putUUID("playerUUID", playerUUID);
        tag.putString("playerName", playerName);
        
        net.minecraft.nbt.ListTag selectedCardsTag = new net.minecraft.nbt.ListTag();
        for (String cardId : selectedCards) {
            selectedCardsTag.add(net.minecraft.nbt.StringTag.valueOf(cardId));
        }
        tag.put("selectedCards", selectedCardsTag);
        
        tag.putInt("currentChoiceIndex", currentChoiceIndex);
        tag.putBoolean("isCompleted", isCompleted);
        tag.putBoolean("isActive", isActive);
        tag.putInt("wins", wins);
        tag.putInt("losses", losses);
        tag.putBoolean("arenaCompleted", arenaCompleted);
        
        if (selectedLeader != null) {
            tag.putString("selectedLeader", selectedLeader.getId());
        }
        
        // Сохраняем выборы
        net.minecraft.nbt.ListTag choicesTag = new net.minecraft.nbt.ListTag();
        for (CardChoice choice : choices) {
            net.minecraft.nbt.CompoundTag choiceTag = new net.minecraft.nbt.CompoundTag();
            choiceTag.putInt("index", choice.getIndex());
            choiceTag.putBoolean("selected", choice.isSelected());
            net.minecraft.nbt.ListTag cardsTag = new net.minecraft.nbt.ListTag();
            for (Card card : choice.getCards()) {
                cardsTag.add(net.minecraft.nbt.StringTag.valueOf(card.getId()));
            }
            choiceTag.put("cards", cardsTag);
            choicesTag.add(choiceTag);
        }
        tag.put("choices", choicesTag);
        
        return tag;
    }
    
    /**
     * Загружает сессию из NBT
     */
    public static DraftSession load(net.minecraft.nbt.CompoundTag tag) {
        UUID playerUUID = tag.getUUID("playerUUID");
        String playerName = tag.getString("playerName");
        
        // Создаём сессию с пустым конструктором
        DraftSession session = new DraftSession();
        session.playerUUID = playerUUID;
        session.playerName = playerName;
        
        // Загружаем выбранные карты
        net.minecraft.nbt.ListTag selectedCardsTag = tag.getList("selectedCards", 8); // TAG_String
        for (int i = 0; i < selectedCardsTag.size(); i++) {
            session.selectedCards.add(selectedCardsTag.getString(i));
        }
        
        session.currentChoiceIndex = tag.getInt("currentChoiceIndex");
        session.isCompleted = tag.getBoolean("isCompleted");
        session.isActive = tag.getBoolean("isActive");
        session.wins = tag.getInt("wins");
        session.losses = tag.getInt("losses");
        session.arenaCompleted = tag.getBoolean("arenaCompleted");
        
        if (tag.contains("selectedLeader")) {
            session.selectedLeader = com.bmfalkye.cards.LeaderRegistry.getLeader(tag.getString("selectedLeader"));
        }
        
        // Загружаем выборы
        net.minecraft.nbt.ListTag choicesTag = tag.getList("choices", 10); // TAG_Compound
        session.choices.clear();
        for (int i = 0; i < choicesTag.size(); i++) {
            net.minecraft.nbt.CompoundTag choiceTag = choicesTag.getCompound(i);
            int index = choiceTag.getInt("index");
            boolean selected = choiceTag.getBoolean("selected");
            net.minecraft.nbt.ListTag cardsTag = choiceTag.getList("cards", 8); // TAG_String
            List<Card> cards = new ArrayList<>();
            for (int j = 0; j < cardsTag.size(); j++) {
                Card card = CardRegistry.getCard(cardsTag.getString(j));
                if (card != null) {
                    cards.add(card);
                }
            }
            CardChoice choice = new CardChoice(index, cards);
            choice.setSelected(selected);
            session.choices.add(choice);
        }
        
        return session;
    }
    
    /**
     * Выбор из 3 карт
     */
    public static class CardChoice {
        private final int index;
        private final List<Card> cards;
        private boolean selected = false;
        
        public CardChoice(int index, List<Card> cards) {
            this.index = index;
            this.cards = new ArrayList<>(cards);
        }
        
        public int getIndex() { return index; }
        public List<Card> getCards() { return new ArrayList<>(cards); }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}

