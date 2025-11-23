package com.bmfalkye.cards;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для класса {@link Card}.
 * 
 * <p>Проверяет базовую функциональность карт, включая:
 * <ul>
 *   <li>Создание карт с различными параметрами</li>
 *   <li>Получение свойств карт</li>
 *   <li>Типы карт</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
class CardTest {
    
    /**
     * Тест создания карты существа.
     */
    @Test
    void testCreatureCardCreation() {
        Card card = new Card("test_creature", "Тестовое существо", 
                            Card.CardType.CREATURE, 5, 
                            "Описание тестового существа", 
                            "Дом Пламени", 
                            CardRarity.COMMON, 10);
        
        assertEquals("test_creature", card.getId());
        assertEquals("Тестовое существо", card.getName());
        assertEquals(Card.CardType.CREATURE, card.getType());
        assertEquals(5, card.getPower());
        assertEquals("Описание тестового существа", card.getDescription());
        assertEquals("Дом Пламени", card.getFaction());
        assertEquals(CardRarity.COMMON, card.getRarity());
        assertEquals(10, card.getCost());
    }
    
    /**
     * Тест создания карты заклинания.
     */
    @Test
    void testSpellCardCreation() {
        Card card = new Card("test_spell", "Тестовое заклинание", 
                            Card.CardType.SPELL, 0, 
                            "Описание тестового заклинания", 
                            "Дозорные Руин", 
                            CardRarity.RARE, 20);
        
        assertEquals(Card.CardType.SPELL, card.getType());
        assertEquals(0, card.getPower()); // Заклинания обычно имеют силу 0
        assertEquals(CardRarity.RARE, card.getRarity());
    }
    
    /**
     * Тест создания карты с обратной совместимостью (без редкости и стоимости).
     */
    @Test
    void testCardCreationBackwardsCompatible() {
        Card card = new Card("test_card", "Тестовая карта", 
                            Card.CardType.CREATURE, 3, 
                            "Описание", 
                            "Дети Рощения");
        
        assertEquals("test_card", card.getId());
        assertEquals(CardRarity.COMMON, card.getRarity()); // Дефолтная редкость
        assertEquals(10, card.getCost()); // Дефолтная стоимость
    }
    
    /**
     * Тест всех типов карт.
     */
    @Test
    void testAllCardTypes() {
        Card creature = new Card("creature", "Существо", Card.CardType.CREATURE, 5, "Описание", "Фракция");
        Card spell = new Card("spell", "Заклинание", Card.CardType.SPELL, 0, "Описание", "Фракция");
        Card special = new Card("special", "Особое", Card.CardType.SPECIAL, 3, "Описание", "Фракция");
        
        assertEquals(Card.CardType.CREATURE, creature.getType());
        assertEquals(Card.CardType.SPELL, spell.getType());
        assertEquals(Card.CardType.SPECIAL, special.getType());
    }
    
    /**
     * Тест разных уровней редкости карт.
     */
    @Test
    void testCardRarities() {
        Card common = new Card("common", "Обычная", Card.CardType.CREATURE, 3, 
                              "Описание", "Фракция", CardRarity.COMMON, 10);
        Card rare = new Card("rare", "Редкая", Card.CardType.CREATURE, 5, 
                            "Описание", "Фракция", CardRarity.RARE, 20);
        Card epic = new Card("epic", "Эпическая", Card.CardType.CREATURE, 7, 
                            "Описание", "Фракция", CardRarity.EPIC, 50);
        Card legendary = new Card("legendary", "Легендарная", Card.CardType.CREATURE, 10, 
                                 "Описание", "Фракция", CardRarity.LEGENDARY, 100);
        
        assertEquals(CardRarity.COMMON, common.getRarity());
        assertEquals(CardRarity.RARE, rare.getRarity());
        assertEquals(CardRarity.EPIC, epic.getRarity());
        assertEquals(CardRarity.LEGENDARY, legendary.getRarity());
        
        // Проверяем стоимость в зависимости от редкости
        assertTrue(common.getCost() < rare.getCost());
        assertTrue(rare.getCost() < epic.getCost());
        assertTrue(epic.getCost() < legendary.getCost());
    }
    
    /**
     * Тест методов getDisplayName() и getDisplayDescription().
     * Проверяет, что методы возвращают компоненты (даже если локализация не настроена).
     */
    @Test
    void testDisplayMethods() {
        Card card = new Card("test_card", "Тестовая карта", Card.CardType.CREATURE, 5, 
                            "Тестовое описание", "Тестовая фракция");
        
        // Методы должны возвращать не-null значения
        assertNotNull(card.getDisplayName());
        assertNotNull(card.getDisplayDescription());
        
        // Проверяем, что методы не выбрасывают исключения
        assertDoesNotThrow(() -> card.getDisplayName().getString());
        assertDoesNotThrow(() -> card.getDisplayDescription().getString());
    }
    
    /**
     * Тест граничных значений силы карты.
     */
    @Test
    void testCardPowerBoundaries() {
        // Минимальная сила (0)
        Card zeroPower = new Card("zero", "Нулевая сила", Card.CardType.SPELL, 0, 
                                  "Описание", "Фракция");
        assertEquals(0, zeroPower.getPower());
        
        // Положительная сила
        Card positivePower = new Card("positive", "Положительная сила", Card.CardType.CREATURE, 10, 
                                     "Описание", "Фракция");
        assertEquals(10, positivePower.getPower());
        assertTrue(positivePower.getPower() > 0);
    }
    
    /**
     * Тест различных фракций карт.
     */
    @Test
    void testCardFactions() {
        Card fireHouse = new Card("fire", "Карта огня", Card.CardType.CREATURE, 5, 
                                 "Описание", "Дом Пламени");
        Card ruinWatchers = new Card("ruin", "Карта руин", Card.CardType.CREATURE, 5, 
                                    "Описание", "Дозорные Руин");
        Card natureChildren = new Card("nature", "Карта природы", Card.CardType.CREATURE, 5, 
                                      "Описание", "Дети Рощения");
        
        assertEquals("Дом Пламени", fireHouse.getFaction());
        assertEquals("Дозорные Руин", ruinWatchers.getFaction());
        assertEquals("Дети Рощения", natureChildren.getFaction());
    }
}

