package com.bmfalkye.api;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * Система для создания новых типов случайных событий
 */
public abstract class GameEvent {
    private final String id;
    private final String name;
    private final String description;
    private final double probability; // Вероятность события (0.0 - 1.0)
    
    public GameEvent(String id, String name, String description, double probability) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.probability = Math.max(0.0, Math.min(1.0, probability));
    }
    
    /**
     * Вызывается при срабатывании события
     */
    public abstract void execute(FalkyeGameSession session, ServerPlayer player);
    
    /**
     * Проверяет, может ли событие произойти в данной сессии
     */
    public boolean canTrigger(FalkyeGameSession session) {
        return true; // Переопределить для кастомной логики
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getProbability() { return probability; }
}

