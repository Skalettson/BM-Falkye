package com.bmfalkye.api;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Всеобъемлющее API для модмейкеров
 * Позволяет создавать карты, способности и события через Fluent API
 */
public class FalkyeAPI {
    private static final List<CardRegistration> pendingCardRegistrations = new ArrayList<>();
    private static final List<LeaderRegistration> pendingLeaderRegistrations = new ArrayList<>();
    private static final List<GameEventRegistration> pendingEventRegistrations = new ArrayList<>();
    
    /**
     * Регистрирует карту через CardBuilder
     */
    public static void registerCard(IEventBus modBus, CardBuilder builder) {
        Card card = builder.build();
        pendingCardRegistrations.add(new CardRegistration(card, modBus));
        
        // Подписываемся на событие инициализации
        modBus.addListener((FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                CardRegistry.registerCard(card);
                com.bmfalkye.BMFalkye.LOGGER.info("Registered custom card: {}", card.getId());
            });
        });
    }
    
    /**
     * Регистрирует лидера через LeaderBuilder
     */
    public static void registerLeader(IEventBus modBus, LeaderBuilder builder) {
        LeaderCard leader = builder.build();
        pendingLeaderRegistrations.add(new LeaderRegistration(leader, modBus));
        
        modBus.addListener((FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                LeaderRegistry.registerLeader(leader);
                com.bmfalkye.BMFalkye.LOGGER.info("Registered custom leader: {}", leader.getId());
            });
        });
    }
    
    /**
     * Регистрирует игровое событие
     */
    public static void registerGameEvent(IEventBus modBus, GameEvent event) {
        pendingEventRegistrations.add(new GameEventRegistration(event, modBus));
        
        modBus.addListener((FMLCommonSetupEvent e) -> {
            e.enqueueWork(() -> {
                com.bmfalkye.game.GameEventSystem.registerEvent(event);
                com.bmfalkye.BMFalkye.LOGGER.info("Registered custom game event: {}", event.getId());
            });
        });
    }
    
    /**
     * Регистрация карты
     */
    private static class CardRegistration {
        final Card card;
        final IEventBus modBus;
        
        CardRegistration(Card card, IEventBus modBus) {
            this.card = card;
            this.modBus = modBus;
        }
    }
    
    /**
     * Регистрация лидера
     */
    private static class LeaderRegistration {
        final LeaderCard leader;
        final IEventBus modBus;
        
        LeaderRegistration(LeaderCard leader, IEventBus modBus) {
            this.leader = leader;
            this.modBus = modBus;
        }
    }
    
    /**
     * Регистрация события
     */
    private static class GameEventRegistration {
        final GameEvent event;
        final IEventBus modBus;
        
        GameEventRegistration(GameEvent event, IEventBus modBus) {
            this.event = event;
            this.modBus = modBus;
        }
    }
}
