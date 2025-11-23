package com.bmfalkye.game;

import com.bmfalkye.api.GameEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система обработки игровых событий
 */
public class GameEventSystem {
    private static final List<GameEvent> registeredEvents = new ArrayList<>();
    private static final Random random = new Random();
    
    /**
     * Регистрирует игровое событие
     */
    public static void registerEvent(GameEvent event) {
        registeredEvents.add(event);
    }
    
    /**
     * Проверяет и запускает случайные события в игре
     */
    public static void checkAndTriggerEvents(FalkyeGameSession session, ServerPlayer player) {
        for (GameEvent event : registeredEvents) {
            if (event.canTrigger(session) && random.nextDouble() < event.getProbability()) {
                event.execute(session, player);
            }
        }
    }
    
    /**
     * Получает все зарегистрированные события
     */
    public static List<GameEvent> getRegisteredEvents() {
        return new ArrayList<>(registeredEvents);
    }
}

