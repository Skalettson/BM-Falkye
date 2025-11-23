package com.bmfalkye.api;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;

/**
 * Fluent API для создания карт
 */
public class CardBuilder {
    private String id;
    private String name;
    private Card.CardType type = Card.CardType.CREATURE;
    private int power = 1;
    private String description = "";
    private String faction = "Нейтральная";
    private CardRarity rarity = CardRarity.COMMON;
    private int cost = 10;
    private Ability ability = null;
    
    private CardBuilder() {}
    
    /**
     * Создаёт новый CardBuilder
     */
    public static CardBuilder create() {
        return new CardBuilder();
    }
    
    /**
     * Устанавливает ID карты
     */
    public CardBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Устанавливает имя карты
     */
    public CardBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Устанавливает тип карты
     */
    public CardBuilder type(Card.CardType type) {
        this.type = type;
        return this;
    }
    
    /**
     * Устанавливает силу карты
     */
    public CardBuilder power(int power) {
        this.power = power;
        return this;
    }
    
    /**
     * Устанавливает описание карты
     */
    public CardBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Устанавливает фракцию карты
     */
    public CardBuilder faction(String faction) {
        this.faction = faction;
        return this;
    }
    
    /**
     * Устанавливает редкость карты
     */
    public CardBuilder rarity(CardRarity rarity) {
        this.rarity = rarity;
        return this;
    }
    
    /**
     * Устанавливает стоимость карты
     */
    public CardBuilder cost(int cost) {
        this.cost = cost;
        return this;
    }
    
    /**
     * Устанавливает способность карты
     */
    public CardBuilder ability(Ability ability) {
        this.ability = ability;
        return this;
    }
    
    /**
     * Создаёт карту
     */
    public Card build() {
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Card ID is required");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Card name is required");
        }
        
        Card card = new Card(id, name, type, power, description, faction, rarity, cost);
        
        // Если есть способность, регистрируем её
        if (ability != null) {
            com.bmfalkye.api.AbilityRegistry.registerCardAbility(id, ability);
        }
        
        return card;
    }
}

