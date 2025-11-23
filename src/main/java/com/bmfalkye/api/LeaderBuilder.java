package com.bmfalkye.api;

import com.bmfalkye.cards.LeaderCard;

/**
 * Fluent API для создания лидеров
 */
public class LeaderBuilder {
    private String id;
    private String name;
    private String faction;
    private String description = "";
    private LeaderCard.LeaderAbility ability;
    
    private LeaderBuilder() {}
    
    /**
     * Создаёт новый LeaderBuilder
     */
    public static LeaderBuilder create() {
        return new LeaderBuilder();
    }
    
    /**
     * Устанавливает ID лидера
     */
    public LeaderBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Устанавливает имя лидера
     */
    public LeaderBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Устанавливает фракцию лидера
     */
    public LeaderBuilder faction(String faction) {
        this.faction = faction;
        return this;
    }
    
    /**
     * Устанавливает описание лидера
     */
    public LeaderBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Устанавливает способность лидера
     */
    public LeaderBuilder ability(LeaderCard.LeaderAbility ability) {
        this.ability = ability;
        return this;
    }
    
    /**
     * Создаёт лидера
     */
    public LeaderCard build() {
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Leader ID is required");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Leader name is required");
        }
        if (faction == null || faction.isEmpty()) {
            throw new IllegalStateException("Leader faction is required");
        }
        if (ability == null) {
            throw new IllegalStateException("Leader ability is required");
        }
        
        return new LeaderCard(id, name, faction, description, ability);
    }
}

