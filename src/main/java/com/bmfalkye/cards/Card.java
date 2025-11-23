package com.bmfalkye.cards;

import net.minecraft.network.chat.Component;

/**
 * Представляет карту в игре Falkye.
 * 
 * <p>Каждая карта имеет уникальный идентификатор, имя, тип (существо, заклинание или особое),
 * силу атаки, описание, фракцию, редкость и стоимость крафта.
 * 
 * <p>Типы карт:
 * <ul>
 *   <li>{@link CardType#CREATURE CREATURE} - существа, которые размещаются на поле в одном из трёх рядов</li>
 *   <li>{@link CardType#SPELL SPELL} - заклинания, которые применяют эффект и уходят в сброс</li>
 *   <li>{@link CardType#SPECIAL SPECIAL} - особые карты с уникальными способностями</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class Card {
    private final String id;
    private final String name;
    private final CardType type;
    private final int power;
    private final String description;
    private final String faction; // Фракция карты (например, "Дом Пламени", "Дозорные Руин", "Дети Рощения")
    private final CardRarity rarity;
    private final int cost; // Стоимость крафта

    public Card(String id, String name, CardType type, int power, String description, String faction, CardRarity rarity, int cost) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.power = power;
        this.description = description;
        this.faction = faction;
        this.rarity = rarity;
        this.cost = cost;
    }
    
    // Конструктор для обратной совместимости
    public Card(String id, String name, CardType type, int power, String description, String faction) {
        this(id, name, type, power, description, faction, CardRarity.COMMON, 10);
    }

    /**
     * Получает уникальный идентификатор карты.
     * 
     * @return строковый идентификатор карты
     */
    public String getId() {
        return id;
    }

    /**
     * Получает имя карты.
     * 
     * @return имя карты
     */
    public String getName() {
        return name;
    }

    /**
     * Получает тип карты (существо, заклинание или особое).
     * 
     * @return тип карты
     */
    public CardType getType() {
        return type;
    }

    /**
     * Получает силу карты (очки атаки/защиты).
     * 
     * @return сила карты
     */
    public int getPower() {
        return power;
    }

    /**
     * Получает описание карты.
     * 
     * @return описание карты
     */
    public String getDescription() {
        return description;
    }

    /**
     * Получает фракцию карты (например, "Дом Пламени", "Дозорные Руин", "Дети Рощения").
     * 
     * @return название фракции
     */
    public String getFaction() {
        return faction;
    }
    
    /**
     * Получает редкость карты.
     * 
     * @return редкость карты
     */
    public CardRarity getRarity() {
        return rarity;
    }
    
    /**
     * Получает стоимость крафта карты.
     * 
     * @return стоимость в игровой валюте
     */
    public int getCost() {
        return cost;
    }

    /**
     * Получает локализованное имя карты для отображения в GUI.
     * 
     * @return компонент с переведённым именем
     */
    public Component getDisplayName() {
        return Component.translatable("card.bm_falkye." + id + ".name");
    }

    /**
     * Получает локализованное описание карты для отображения в GUI.
     * 
     * @return компонент с переведённым описанием
     */
    public Component getDisplayDescription() {
        return Component.translatable("card.bm_falkye." + id + ".description");
    }

    /**
     * Типы карт в игре Falkye.
     * 
     * <ul>
     *   <li>{@link #CREATURE} - существа, размещаемые на поле</li>
     *   <li>{@link #SPELL} - заклинания с мгновенными эффектами</li>
     *   <li>{@link #SPECIAL} - особые карты с уникальными способностями</li>
     * </ul>
     */
    public enum CardType {
        /** Существо - карта, которая размещается на поле в одном из трёх рядов (ближний бой, дальний бой, осада) */
        CREATURE,
        
        /** Заклинание - карта способности, которая применяет эффект и уходит в сброс */
        SPELL,
        
        /** Специальная карта - особые карты способностей с уникальными эффектами */
        SPECIAL
    }
}

