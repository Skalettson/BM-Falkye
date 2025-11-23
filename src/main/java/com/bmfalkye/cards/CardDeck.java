package com.bmfalkye.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Представляет колоду карт для игры в Falkye
 * 
 * <p>Колода содержит список карт и стопку сброса. Карты перемешиваются при создании колоды.
 * Колода используется для раздачи карт игрокам в начале игры.</p>
 * 
 * <p>Особенности:</p>
 * <ul>
 *   <li>Автоматическое перемешивание карт при создании</li>
 *   <li>Поддержка стопки сброса для карт, которые были использованы</li>
 *   <li>Возможность создания колоды из списка карт или использования стандартной колоды</li>
 * </ul>
 * 
 * @author BM Falkye Team
 * @see Card
 * @see CardRegistry
 */
public class CardDeck {
    private final List<Card> cards;
    private final List<Card> discardPile;

    public CardDeck() {
        this.cards = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        // TODO: Загрузить карты из сохранения игрока или использовать стандартную колоду
        initializeDefaultDeck();
        shuffle();
    }

    public CardDeck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
        this.discardPile = new ArrayList<>();
        shuffle();
    }

    /**
     * Инициализирует колоду стандартным набором карт
     * 
     * <p>Добавляет базовые карты из реестра для тестирования.
     * В будущем карты будут загружаться из сохранения игрока.</p>
     */
    private void initializeDefaultDeck() {
        // Создаём базовую колоду для тестирования
        // В будущем это будет загружаться из сохранения игрока
        // Добавляем карты из реестра
        cards.add(CardRegistry.getCard("fire_dragon_ignisar"));
        cards.add(CardRegistry.getCard("fire_mage"));
        cards.add(CardRegistry.getCard("pyro_phoenix"));
        cards.add(CardRegistry.getCard("ice_dragon_glacis"));
        cards.add(CardRegistry.getCard("watcher_scholar"));
        cards.add(CardRegistry.getCard("void_walker"));
        cards.add(CardRegistry.getCard("lightning_dragon_fulgur"));
        cards.add(CardRegistry.getCard("nature_guardian"));
        cards.add(CardRegistry.getCard("ancient_tree"));
        cards.add(CardRegistry.getCard("ruin_echo"));
        cards.add(CardRegistry.getCard("corrupted_mage"));
        
        // Удаляем null карты
        cards.removeIf(card -> card == null);
        
        // НЕ добавляем дубликаты - правило: 1 карта одного вида на игрока
        // Если нужно больше карт, они должны быть добавлены из коллекции игрока
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    public Card getRandomCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(new java.util.Random().nextInt(cards.size()));
    }
    
    public boolean isEmpty() {
        return cards.isEmpty() && discardPile.isEmpty();
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            // Если колода пуста, перемешиваем сброс
            if (!discardPile.isEmpty()) {
                cards.addAll(discardPile);
                discardPile.clear();
                shuffle();
            } else {
                return null; // Нет карт
            }
        }
        return cards.remove(0);
    }

    public void addToDiscard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }
    
    /**
     * Возвращает карту в колоду (используется при возврате карт из сброса)
     */
    public void returnCardToDeck(Card card) {
        if (card != null) {
            // Проверяем, что карта с таким ID еще не добавлена
            boolean alreadyExists = cards.stream()
                .anyMatch(c -> c != null && c.getId().equals(card.getId()));
            if (!alreadyExists) {
                cards.add(card);
            }
        }
    }
    
    public void addCard(Card card) {
        if (card == null) {
            return;
        }
        // Проверяем, что карта с таким ID еще не добавлена (правило: 1 карта одного вида на игрока)
        boolean alreadyExists = cards.stream()
            .anyMatch(c -> c != null && c.getId().equals(card.getId()));
        if (!alreadyExists) {
            cards.add(card);
        }
    }

    public int size() {
        return cards.size();
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
    
    public void fillWithRandomCards(int count) {
        // Добавляем случайные карты из реестра (без дубликатов)
        List<Card> allCards = CardRegistry.getAllCards();
        if (allCards.isEmpty()) {
            return;
        }
        java.util.Random random = new java.util.Random();
        java.util.Set<String> addedCardIds = new java.util.HashSet<>();
        
        // Собираем уже добавленные ID
        for (Card card : cards) {
            if (card != null) {
                addedCardIds.add(card.getId());
            }
        }
        
        // Добавляем случайные карты, избегая дубликатов
        int attempts = 0;
        int maxAttempts = count * 10; // Ограничение попыток, чтобы избежать бесконечного цикла
        while (cards.size() < count && attempts < maxAttempts) {
            Card randomCard = allCards.get(random.nextInt(allCards.size()));
            if (randomCard != null && !addedCardIds.contains(randomCard.getId())) {
                cards.add(randomCard);
                addedCardIds.add(randomCard.getId());
            }
            attempts++;
        }
    }
}

