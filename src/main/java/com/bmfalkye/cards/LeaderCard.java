package com.bmfalkye.cards;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Карта лидера - особая карта с уникальной способностью
 */
public class LeaderCard {
    private final String id;
    private final String name;
    private final String faction;
    private final String description;
    private final LeaderAbility ability;
    private boolean used = false;

    public LeaderCard(String id, String name, String faction, String description, LeaderAbility ability) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.description = description;
        this.ability = ability;
    }

    public void use(FalkyeGameSession session, ServerPlayer player) {
        if (used) return;
        
        ability.execute(session, player);
        used = true;
    }

    public boolean isUsed() {
        return used;
    }

    public void reset() {
        used = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFaction() { return faction; }
    public String getDescription() { return description; }
    public String getAbility() { return description; } // Возвращаем описание как способность

    @FunctionalInterface
    public interface LeaderAbility {
        void execute(FalkyeGameSession session, ServerPlayer player);
    }
}

