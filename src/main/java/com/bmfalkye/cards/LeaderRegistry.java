package com.bmfalkye.cards;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр лидеров для каждой фракции
 */
public class LeaderRegistry {
    private static final Map<String, LeaderCard> LEADERS = new HashMap<>();

    public static void initializeLeaders() {
        // Лидер Дома Пламени - Архитектор Реальности
        registerLeader(new LeaderCard(
            "leader_fire_architect",
            "Архитектор Реальности",
            "Дом Пламени",
            "Уничтожает самую сильную карту противника",
            (session, player) -> {
                ServerPlayer opponent = session.getOpponent(player);
                Card strongest = null;
                
                if (opponent == null && session.isPlayingWithVillager()) {
                    // Для villager ищем самую сильную карту
                    strongest = session.findStrongestCard(null);
                } else if (opponent != null) {
                    strongest = session.findStrongestCard(opponent);
                }
                
                if (strongest != null) {
                    if (opponent == null && session.isPlayingWithVillager()) {
                        session.removeCardFromField(null, strongest);
                    } else if (opponent != null) {
                        session.removeCardFromField(opponent, strongest);
                    }
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cАрхитектор Реальности уничтожил карту противника: " + strongest.getName()));
                    }
                } else if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7У противника нет карт на поле"));
                }
            }
        ));

        // Лидер Дозорных Руин - Картограф Непознанного
        registerLeader(new LeaderCard(
            "leader_watcher_cartographer",
            "Картограф Непознанного",
            "Дозорные Руин",
            "Возвращает случайную карту из сброса в руку",
            (session, player) -> {
                // Получаем сброс игрока
                java.util.List<Card> graveyard = session.getGraveyard(player);
                if (!graveyard.isEmpty()) {
                    // Возвращаем случайную карту
                    Card card = graveyard.get(new java.util.Random().nextInt(graveyard.size()));
                    graveyard.remove(card);
                    session.getHand(player).add(card);
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§bКартограф Непознанного вернул карту: " + card.getName()));
                    }
                } else if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7В сбросе нет карт"));
                }
            }
        ));

        // Лидер Детей Рощения - Дирижёр Мировой Души
        registerLeader(new LeaderCard(
            "leader_nature_conductor",
            "Дирижёр Мировой Души",
            "Дети Рощения",
            "Усиливает все карты на поле на 2",
            (session, player) -> {
                // Усиливаем все карты игрока
                java.util.List<Card> melee = session.getMeleeRow(player);
                java.util.List<Card> ranged = session.getRangedRow(player);
                java.util.List<Card> siege = session.getSiegeRow(player);
                
                for (Card card : melee) {
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, 2, player);
                }
                for (Card card : ranged) {
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, 2, player);
                }
                for (Card card : siege) {
                    // ВАЖНО: Передаем игрока, чтобы модификатор применялся только к его картам
                    session.addPowerModifier(card, 2, player);
                }
                
                session.recalculateRoundScore();
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aДирижёр Мировой Души усилил все ваши карты на 2!"));
                }
            }
        ));
    }

    public static void registerLeader(LeaderCard leader) {
        LEADERS.put(leader.getId(), leader);
    }
    
    /**
     * Получает лидера по ID
     * 
     * @param leaderId ID лидера
     * @return лидер или null если не найден
     */
    public static LeaderCard getLeader(String leaderId) {
        return LEADERS.get(leaderId);
    }
    
    /**
     * Получает список всех зарегистрированных лидеров
     * 
     * @return список лидеров
     */
    public static java.util.List<LeaderCard> getAllLeaders() {
        return new java.util.ArrayList<>(LEADERS.values());
    }

    /**
     * Получает лидера для указанной фракции
     * 
     * @param faction название фракции
     * @return лидер фракции или null если не найден
     */
    public static LeaderCard getLeaderForFaction(String faction) {
        // Возвращает лидера для фракции
        for (LeaderCard leader : LEADERS.values()) {
            if (leader.getFaction().equals(faction)) {
                return leader;
            }
        }
        return null;
    }
}

