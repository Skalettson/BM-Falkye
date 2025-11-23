package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardDeck;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Режим Арена - создание колоды из ограниченного выбора карт
 */
public class ArenaMode {
    private static final int CARDS_TO_CHOOSE = 30; // Всего карт для выбора
    private static final int CARDS_PER_PICK = 3; // Карт на выбор за раз
    private static final Random random = new Random();
    
    /**
     * Создаёт арену-драфт для игрока
     */
    public static ArenaDraft createArenaDraft(ServerPlayer player) {
        List<Card> allCards = CardRegistry.getAllCards();
        List<Card> availableCards = new ArrayList<>(allCards);
        
        // Создаём список выборов (по 3 карты)
        List<CardPick> picks = new ArrayList<>();
        
        for (int i = 0; i < CARDS_TO_CHOOSE / CARDS_PER_PICK; i++) {
            List<Card> pickOptions = new ArrayList<>();
            
            // Выбираем 3 случайные карты
            for (int j = 0; j < CARDS_PER_PICK && !availableCards.isEmpty(); j++) {
                Card selected = availableCards.remove(random.nextInt(availableCards.size()));
                pickOptions.add(selected);
            }
            
            picks.add(new CardPick(pickOptions));
        }
        
        return new ArenaDraft(player.getUUID(), picks);
    }
    
    /**
     * Создаёт колоду из выбранных карт арены
     */
    public static CardDeck createArenaDeck(ArenaDraft draft) {
        CardDeck deck = new CardDeck();
        
        for (CardPick pick : draft.getPicks()) {
            if (pick.getSelectedCard() != null) {
                deck.addCard(pick.getSelectedCard());
            }
        }
        
        return deck;
    }
    
    /**
     * Драфт арены
     */
    public static class ArenaDraft {
        private final UUID playerId;
        private final List<CardPick> picks;
        private int currentPick = 0;
        
        public ArenaDraft(UUID playerId, List<CardPick> picks) {
            this.playerId = playerId;
            this.picks = picks;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public List<CardPick> getPicks() {
            return picks;
        }
        
        public int getCurrentPick() {
            return currentPick;
        }
        
        public void nextPick() {
            currentPick++;
        }
        
        public boolean isComplete() {
            return currentPick >= picks.size();
        }
        
        public CardPick getCurrentCardPick() {
            if (currentPick < picks.size()) {
                return picks.get(currentPick);
            }
            return null;
        }
    }
    
    /**
     * Выбор карт
     */
    public static class CardPick {
        private final List<Card> options;
        private Card selectedCard = null;
        
        public CardPick(List<Card> options) {
            this.options = options;
        }
        
        public List<Card> getOptions() {
            return options;
        }
        
        public Card getSelectedCard() {
            return selectedCard;
        }
        
        public void selectCard(Card card) {
            if (options.contains(card)) {
                selectedCard = card;
            }
        }
    }
}

