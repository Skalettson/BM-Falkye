package com.bmfalkye.game;

import com.bmfalkye.cards.Card;
import net.minecraft.server.level.ServerPlayer;
import java.util.Random;
import java.util.UUID;

/**
 * Случайные события в матчах
 */
public class RandomEvent {
    public enum EventType {
        CARD_DRAW,          // Дополнительная карта
        POWER_BOOST,        // Усиление всех карт
        WEATHER_CHANGE,     // Изменение погоды
        CARD_REVEAL,        // Показ карты противника
        COIN_FLIP,          // Подбрасывание монеты
        NONE
    }
    
    private static final Random random = new Random();
    private static final double EVENT_CHANCE = 0.05; // 5% шанс события за ход
    
    /**
     * Проверяет, должно ли произойти случайное событие
     */
    public static boolean shouldTriggerEvent() {
        return random.nextDouble() < EVENT_CHANCE;
    }
    
    /**
     * Генерирует случайное событие
     */
    public static EventType generateRandomEvent() {
        if (!shouldTriggerEvent()) {
            return EventType.NONE;
        }
        
        EventType[] events = EventType.values();
        return events[random.nextInt(events.length - 1)]; // -1 чтобы исключить NONE
    }
    
    /**
     * Применяет событие к игровой сессии
     */
    public static void applyEvent(FalkyeGameSession session, ServerPlayer player, EventType event) {
        if (session == null || player == null || event == EventType.NONE) {
            return;
        }
        
        switch (event) {
            case NONE:
                // Ничего не делаем
                break;
            case COIN_FLIP:
                // Подбрасываем монету - случайный эффект
                if (random.nextBoolean()) {
                    // Положительный эффект - бафф для игрока
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aСлучайное событие: Монета выпала орлом! Вы получаете +2 к силе всех карт на 1 раунд!"));
                    
                    // Применяем временный бафф ко всем картам игрока на поле
                    applyTemporaryPowerBuff(session, player, 2, 1);
                } else {
                    // Отрицательный эффект - бафф для противника
                    ServerPlayer opponent = session.getOpponent(player);
                    if (opponent != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cСлучайное событие: Монета выпала решкой! Противник получает +2 к силе всех карт на 1 раунд!"));
                        opponent.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aСлучайное событие: Монета выпала решкой! Вы получаете +2 к силе всех карт на 1 раунд!"));
                        
                        // Применяем временный бафф ко всем картам противника на поле
                        applyTemporaryPowerBuff(session, opponent, 2, 1);
                    } else if (session.isPlayingWithVillager()) {
                        // Игра с villager - бафф для villager
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cСлучайное событие: Монета выпала решкой! Противник получает +2 к силе всех карт на 1 раунд!"));
                        
                        // Применяем бафф ко всем картам villager на поле (player = null для villager)
                        applyTemporaryPowerBuff(session, null, 2, 1);
                    }
                }
                break;
            case CARD_DRAW:
                // Игрок получает дополнительную карту из колоды
                drawExtraCard(session, player);
                break;
                
            case POWER_BOOST:
                // Все карты игрока получают +1 к силе
                java.util.List<Card> hand = session.getHand(player);
                for (Card card : hand) {
                    com.bmfalkye.cards.CardBuff buff = new com.bmfalkye.cards.CardBuff(
                        "random_boost_" + System.currentTimeMillis(),
                        com.bmfalkye.cards.CardBuff.BuffType.POWER_INCREASE,
                        1,
                        1, // Длится 1 раунд
                        player.getUUID()
                    );
                    com.bmfalkye.cards.BuffSystem.applyBuff(session, player, card, buff);
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aСлучайное событие: Все ваши карты получили +1 к силе!"));
                break;
                
            case WEATHER_CHANGE:
                // Случайная погода
                FalkyeGameSession.WeatherType[] weathers = FalkyeGameSession.WeatherType.values();
                FalkyeGameSession.WeatherType newWeather = weathers[random.nextInt(weathers.length - 1) + 1]; // Исключаем NONE
                session.setWeather(newWeather);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aСлучайное событие: Погода изменилась на " + newWeather.getDisplayName() + "!"));
                break;
                
            case CARD_REVEAL:
                // Показываем случайную карту противника
                ServerPlayer opponent = session.getOpponent(player);
                if (opponent != null) {
                    java.util.List<Card> opponentHand = session.getHand(opponent);
                    if (!opponentHand.isEmpty()) {
                        Card revealedCard = opponentHand.get(random.nextInt(opponentHand.size()));
                        // Добавляем карту в список показанных
                        java.util.List<String> revealed = session.getRevealedCards(player);
                        if (revealed == null) {
                            revealed = new java.util.ArrayList<>();
                        }
                        revealed.add(revealedCard.getId());
                        session.setRevealedCards(player, revealed);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aСлучайное событие: Вы видите карту противника: " + revealedCard.getName()));
                    }
                }
                break;
        }
    }
    
    /**
     * Применяет временный бафф силы ко всем картам игрока на поле
     */
    private static void applyTemporaryPowerBuff(FalkyeGameSession session, ServerPlayer targetPlayer, 
                                               int powerBonus, int durationRounds) {
        if (session == null) {
            return;
        }
        
        // Получаем все карты игрока на поле (все три ряда)
        java.util.List<Card> meleeCards = session.getMeleeRow(targetPlayer);
        java.util.List<Card> rangedCards = session.getRangedRow(targetPlayer);
        java.util.List<Card> siegeCards = session.getSiegeRow(targetPlayer);
        
        // Создаём уникальный ID баффа
        String buffId = "random_event_buff_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
        UUID sourcePlayerId = targetPlayer != null ? targetPlayer.getUUID() : 
            (session.isPlayingWithVillager() ? java.util.UUID.randomUUID() : null);
        
        // Применяем бафф ко всем картам в ближнем бою
        for (Card card : meleeCards) {
            com.bmfalkye.cards.CardBuff buff = new com.bmfalkye.cards.CardBuff(
                buffId + "_" + card.getId(),
                com.bmfalkye.cards.CardBuff.BuffType.POWER_INCREASE,
                powerBonus,
                durationRounds,
                sourcePlayerId
            );
            com.bmfalkye.cards.BuffSystem.applyBuff(session, targetPlayer, card, buff);
        }
        
        // Применяем бафф ко всем картам в дальнем бою
        for (Card card : rangedCards) {
            com.bmfalkye.cards.CardBuff buff = new com.bmfalkye.cards.CardBuff(
                buffId + "_" + card.getId(),
                com.bmfalkye.cards.CardBuff.BuffType.POWER_INCREASE,
                powerBonus,
                durationRounds,
                sourcePlayerId
            );
            com.bmfalkye.cards.BuffSystem.applyBuff(session, targetPlayer, card, buff);
        }
        
        // Применяем бафф ко всем картам в осаде
        for (Card card : siegeCards) {
            com.bmfalkye.cards.CardBuff buff = new com.bmfalkye.cards.CardBuff(
                buffId + "_" + card.getId(),
                com.bmfalkye.cards.CardBuff.BuffType.POWER_INCREASE,
                powerBonus,
                durationRounds,
                sourcePlayerId
            );
            com.bmfalkye.cards.BuffSystem.applyBuff(session, targetPlayer, card, buff);
        }
    }
    
    /**
     * Выдаёт игроку дополнительную карту из колоды
     */
    private static void drawExtraCard(FalkyeGameSession session, ServerPlayer player) {
        if (session == null || player == null) {
            return;
        }
        
        // Определяем, какая колода принадлежит игроку
        com.bmfalkye.cards.CardDeck playerDeck = null;
        if (player.equals(session.getPlayer1())) {
            playerDeck = session.getDeck1();
        } else if (session.getPlayer2() != null && player.equals(session.getPlayer2())) {
            playerDeck = session.getDeck2();
        } else {
            // Игрок не найден в сессии
            return;
        }
        
        if (playerDeck == null || playerDeck.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eСлучайное событие: Вы получили дополнительную карту! (но колода пуста)"));
            return;
        }
        
        // Берём карту из колоды
        Card drawnCard = playerDeck.drawCard();
        if (drawnCard != null) {
            // Добавляем карту в руку игрока
            java.util.List<Card> hand = session.getHand(player);
            hand.add(drawnCard);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aСлучайное событие: Вы получили дополнительную карту: §b" + drawnCard.getName() + "§a!"));
            
            // Уведомляем через лог действий
            com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                "§aСлучайное событие: Получена карта " + drawnCard.getName());
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eСлучайное событие: Вы получили дополнительную карту! (но колода пуста)"));
        }
    }
}

