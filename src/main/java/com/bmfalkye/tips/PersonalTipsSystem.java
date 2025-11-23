package com.bmfalkye.tips;

import com.bmfalkye.analytics.AnalyticsStorage;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система персональных подсказок для игроков
 */
public class PersonalTipsSystem {
    /**
     * Генерирует персональные подсказки для игрока на основе его статистики
     */
    public static List<String> generatePersonalTips(ServerPlayer player) {
        List<String> tips = new ArrayList<>();
        
        if (player.level() instanceof ServerLevel serverLevel) {
            PlayerProgressStorage progressStorage = PlayerProgressStorage.get(serverLevel);
            PlayerProgress progress = progressStorage.getPlayerProgress(player);
            AnalyticsStorage analytics = AnalyticsStorage.get(serverLevel);
            
            // Анализируем статистику проигрышей против фракций
            Map<String, Integer> lossesByFaction = analyzeLossesByFaction(player, progress, analytics);
            if (!lossesByFaction.isEmpty()) {
                String weakestFaction = lossesByFaction.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                if (weakestFaction != null) {
                    tips.add("Вы часто проигрываете против " + getFactionDisplayName(weakestFaction) + 
                        ". Попробуйте добавить в колоду карты с защитой от этой фракции.");
                }
            }
            
            // Анализируем Win Rate карт игрока
            Map<String, Double> cardWinRates = analyzeCardWinRates(player, progress, analytics);
            if (!cardWinRates.isEmpty()) {
                // Находим карты с низким Win Rate
                List<String> weakCards = new ArrayList<>();
                for (Map.Entry<String, Double> entry : cardWinRates.entrySet()) {
                    if (entry.getValue() < 40.0 && progress.getCardsPlayed().getOrDefault(entry.getKey(), 0) >= 5) {
                        weakCards.add(entry.getKey());
                    }
                }
                
                if (!weakCards.isEmpty() && weakCards.size() <= 3) {
                    tips.add("Карты с низким Win Rate в вашей колоде: " + 
                        String.join(", ", weakCards.stream()
                            .map(cardId -> {
                                Card card = CardRegistry.getCard(cardId);
                                return card != null ? card.getName() : cardId;
                            })
                            .toList()));
                }
            }
            
            // Анализируем среднее время хода
            String playerId = player.getUUID().toString();
            java.util.Map<String, Double> avgTurnTimes = analytics.getAverageTurnTime();
            Double avgTurnTime = avgTurnTimes != null ? avgTurnTimes.get(playerId) : null;
            if (avgTurnTime != null && avgTurnTime > 60.0) {
                tips.add("Ваше среднее время хода: " + String.format("%.1f", avgTurnTime) + 
                    " секунд. Попробуйте принимать решения быстрее.");
            }
        }
        
        return tips;
    }
    
    /**
     * Анализирует проигрыши по фракциям
     */
    private static Map<String, Integer> analyzeLossesByFaction(ServerPlayer player, 
                                                               PlayerProgress progress,
                                                               AnalyticsStorage analytics) {
        Map<String, Integer> lossesByFaction = new HashMap<>();
        
        // Анализируем статистику матчей между фракциями
        // TODO: Реализовать более детальный анализ на основе истории игр
        
        return lossesByFaction;
    }
    
    /**
     * Анализирует Win Rate карт игрока
     */
    private static Map<String, Double> analyzeCardWinRates(ServerPlayer player,
                                                           PlayerProgress progress,
                                                           AnalyticsStorage analytics) {
        Map<String, Double> cardWinRates = new HashMap<>();
        
        Map<String, Integer> cardsWon = progress.getCardsWon();
        Map<String, Integer> cardsPlayed = progress.getCardsPlayed();
        
        for (String cardId : cardsPlayed.keySet()) {
            int wins = cardsWon.getOrDefault(cardId, 0);
            int played = cardsPlayed.get(cardId);
            if (played > 0) {
                cardWinRates.put(cardId, (double)wins / played * 100.0);
            }
        }
        
        return cardWinRates;
    }
    
    /**
     * Получает отображаемое имя фракции
     */
    private static String getFactionDisplayName(String factionName) {
        // Простое преобразование имени фракции
        return factionName.replace("_", " ");
    }
    
    /**
     * Отправляет персональные подсказки игроку
     */
    public static void sendPersonalTips(ServerPlayer player) {
        List<String> tips = generatePersonalTips(player);
        
        if (!tips.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6§l══════ ПЕРСОНАЛЬНЫЕ ПОДСКАЗКИ ══════"));
            for (String tip : tips) {
                player.sendSystemMessage(Component.literal("§e• " + tip));
            }
        }
    }
}

