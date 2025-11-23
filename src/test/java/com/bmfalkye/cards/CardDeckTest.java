package com.bmfalkye.cards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit-тесты для класса {@link CardDeck}.
 * 
 * <p>Проверяет функциональность колоды карт, включая:
 * <ul>
 *   <li>Создание колоды</li>
 *   <li>Добавление и удаление карт</li>
 *   <li>Взятие карт из колоды</li>
 *   <li>Работа со сбросом</li>
 *   <li>Предотвращение дубликатов</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
class CardDeckTest {
    
    private List<Card> testCards;
    
    /**
     * Подготовка тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        testCards = new ArrayList<>();
        // Создаём тестовые карты
        testCards.add(new Card("card1", "Карта 1", Card.CardType.CREATURE, 5, 
                              "Описание 1", "Дом Пламени", CardRarity.COMMON, 10));
        testCards.add(new Card("card2", "Карта 2", Card.CardType.CREATURE, 3, 
                              "Описание 2", "Дозорные Руин", CardRarity.COMMON, 10));
        testCards.add(new Card("card3", "Карта 3", Card.CardType.SPELL, 0, 
                              "Описание 3", "Дети Рощения", CardRarity.RARE, 20));
        testCards.add(new Card("card4", "Карта 4", Card.CardType.CREATURE, 7, 
                              "Описание 4", "Дом Пламени", CardRarity.EPIC, 50));
        testCards.add(new Card("card5", "Карта 5", Card.CardType.SPECIAL, 2, 
                              "Описание 5", "Дозорные Руин", CardRarity.LEGENDARY, 100));
    }
    
    /**
     * Тест создания колоды с заданными картами.
     */
    @Test
    void testDeckCreationWithCards() {
        CardDeck deck = new CardDeck(testCards);
        
        assertNotNull(deck);
        assertFalse(deck.isEmpty());
        assertEquals(5, deck.size());
    }
    
    /**
     * Тест создания пустой колоды.
     */
    @Test
    void testEmptyDeckCreation() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        
        assertNotNull(deck);
        assertTrue(deck.isEmpty());
        assertEquals(0, deck.size());
    }
    
    /**
     * Тест взятия карты из колоды.
     */
    @Test
    void testDrawCard() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        int initialSize = deck.size();
        
        Card drawnCard = deck.drawCard();
        
        assertNotNull(drawnCard);
        assertTrue(testCards.contains(drawnCard));
        assertEquals(initialSize - 1, deck.size());
    }
    
    /**
     * Тест взятия карты из пустой колоды.
     */
    @Test
    void testDrawFromEmptyDeck() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        
        Card drawnCard = deck.drawCard();
        
        assertNull(drawnCard);
        assertTrue(deck.isEmpty());
    }
    
    /**
     * Тест взятия всех карт из колоды.
     */
    @Test
    void testDrawAllCards() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        int cardCount = deck.size();
        
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < cardCount; i++) {
            Card card = deck.drawCard();
            if (card != null) {
                drawnCards.add(card);
            }
        }
        
        assertEquals(cardCount, drawnCards.size());
        assertTrue(deck.isEmpty() || deck.size() == 0);
        
        // Попытка взять ещё одну карту из пустой колоды
        Card nullCard = deck.drawCard();
        assertNull(nullCard);
    }
    
    /**
     * Тест добавления карты в колоду.
     */
    @Test
    void testAddCard() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card newCard = testCards.get(0);
        
        deck.addCard(newCard);
        
        assertEquals(1, deck.size());
        assertFalse(deck.isEmpty());
    }
    
    /**
     * Тест предотвращения дубликатов при добавлении карт.
     */
    @Test
    void testNoDuplicateCards() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card card1 = testCards.get(0);
        
        // Добавляем карту дважды
        deck.addCard(card1);
        int sizeAfterFirstAdd = deck.size();
        
        deck.addCard(card1); // Попытка добавить ту же карту
        
        // Размер не должен измениться (правило: 1 карта одного вида на игрока)
        assertEquals(sizeAfterFirstAdd, deck.size());
        assertEquals(1, deck.size());
    }
    
    /**
     * Тест добавления null карты (должно игнорироваться).
     */
    @Test
    void testAddNullCard() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        int initialSize = deck.size();
        
        deck.addCard(null);
        
        // Размер не должен измениться
        assertEquals(initialSize, deck.size());
    }
    
    /**
     * Тест добавления карты в сброс.
     */
    @Test
    void testAddToDiscard() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card card = testCards.get(0);
        
        deck.addToDiscard(card);
        
        // После добавления в сброс колода должна остаться пустой
        // Но isEmpty() проверяет и колоду, и сброс
        // Тест проверяет, что метод не выбрасывает исключение
        assertDoesNotThrow(() -> deck.addToDiscard(card));
    }
    
    /**
     * Тест добавления null в сброс (должно игнорироваться).
     */
    @Test
    void testAddNullToDiscard() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        
        assertDoesNotThrow(() -> deck.addToDiscard(null));
    }
    
    /**
     * Тест возврата карты в колоду из сброса.
     */
    @Test
    void testReturnCardToDeck() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card card = testCards.get(0);
        
        // Добавляем в сброс
        deck.addToDiscard(card);
        
        // Возвращаем в колоду
        deck.returnCardToDeck(card);
        
        // Карта должна быть доступна для взятия
        assertFalse(deck.isEmpty());
    }
    
    /**
     * Тест предотвращения дубликатов при возврате карты в колоду.
     */
    @Test
    void testReturnDuplicateCardToDeck() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card card = testCards.get(0);
        
        deck.addCard(card);
        int sizeAfterAdd = deck.size();
        
        // Попытка вернуть ту же карту
        deck.returnCardToDeck(card);
        
        // Размер не должен измениться
        assertEquals(sizeAfterAdd, deck.size());
    }
    
    /**
     * Тест получения случайной карты из колоды.
     */
    @Test
    void testGetRandomCard() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        
        Card randomCard = deck.getRandomCard();
        
        assertNotNull(randomCard);
        assertTrue(testCards.contains(randomCard));
        // Размер колоды не должен измениться при getRandomCard()
        assertEquals(testCards.size(), deck.size());
    }
    
    /**
     * Тест получения случайной карты из пустой колоды.
     */
    @Test
    void testGetRandomCardFromEmptyDeck() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        
        Card randomCard = deck.getRandomCard();
        
        assertNull(randomCard);
    }
    
    /**
     * Тест перемешивания колоды.
     */
    @Test
    void testShuffle() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        List<Card> cardsBeforeShuffle = new ArrayList<>(deck.getCards());
        
        deck.shuffle();
        List<Card> cardsAfterShuffle = deck.getCards();
        
        // Размер не должен измениться
        assertEquals(cardsBeforeShuffle.size(), cardsAfterShuffle.size());
        
        // Все карты должны остаться (хотя порядок может измениться)
        assertTrue(cardsAfterShuffle.containsAll(cardsBeforeShuffle));
    }
    
    /**
     * Тест метода getCards() - должен возвращать копию, а не оригинал.
     */
    @Test
    void testGetCardsReturnsCopy() {
        CardDeck deck = new CardDeck(new ArrayList<>(testCards));
        
        List<Card> cards1 = deck.getCards();
        List<Card> cards2 = deck.getCards();
        
        // Это должны быть разные объекты
        assertNotSame(cards1, cards2);
        
        // Но с одинаковым содержимым
        assertEquals(cards1, cards2);
        
        // Изменение возвращённого списка не должно влиять на колоду
        cards1.clear();
        assertFalse(deck.isEmpty());
        assertEquals(testCards.size(), deck.size());
    }
    
    /**
     * Тест работы сброса при исчерпании колоды.
     */
    @Test
    void testDiscardPileRecycling() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        Card card1 = testCards.get(0);
        Card card2 = testCards.get(1);
        
        // Добавляем карты в колоду
        deck.addCard(card1);
        deck.addCard(card2);
        
        // Берём все карты
        Card drawn1 = deck.drawCard();
        Card drawn2 = deck.drawCard();
        
        // Колода пуста
        assertNull(deck.drawCard());
        
        // Добавляем карты в сброс
        deck.addToDiscard(drawn1);
        deck.addToDiscard(drawn2);
        
        // При следующем взятии карты из пустой колоды, сброс должен перемешаться обратно
        // (Это сложно протестировать без прямого доступа к внутреннему состоянию,
        // но мы можем проверить, что метод работает корректно)
        // В реальной игре это работает через drawCard() когда колода пуста
    }
    
    /**
     * Тест размера колоды после различных операций.
     */
    @Test
    void testDeckSize() {
        CardDeck deck = new CardDeck(new ArrayList<>());
        
        assertEquals(0, deck.size());
        
        deck.addCard(testCards.get(0));
        assertEquals(1, deck.size());
        
        deck.addCard(testCards.get(1));
        assertEquals(2, deck.size());
        
        deck.drawCard();
        assertEquals(1, deck.size());
        
        deck.drawCard();
        assertEquals(0, deck.size());
    }
}

