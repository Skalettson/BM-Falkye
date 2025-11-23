package com.bmfalkye.analytics;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Система сбора аналитики игры
 */
public class AnalyticsSystem {
    /**
     * Записывает аналитику завершённой игры
     */
    public static void recordGameAnalytics(FalkyeGameSession session, ServerPlayer winner, ServerPlayer loser) {
        if (winner.level() instanceof ServerLevel serverLevel) {
            AnalyticsStorage storage = AnalyticsStorage.get(serverLevel);
            
            // Получаем использованные карты
            List<String> winnerCards = session.getUsedCards(winner);
            List<String> loserCards = session.getUsedCards(loser);
            
            // Записываем Win Rate карт
            for (String cardId : winnerCards) {
                storage.recordCardResult(cardId, true);
            }
            for (String cardId : loserCards) {
                storage.recordCardResult(cardId, false);
            }
            
            // Записываем комбинации карт (топ-5 карт победителя)
            List<String> topWinnerCards = winnerCards.size() > 5 ? 
                winnerCards.subList(0, 5) : winnerCards;
            for (int i = 0; i < topWinnerCards.size(); i++) {
                for (int j = i + 1; j < topWinnerCards.size(); j++) {
                    storage.recordCombinationResult(topWinnerCards.get(i), topWinnerCards.get(j), true);
                }
            }
            
            // Записываем матч между фракциями
            LeaderCard winnerLeader = session.getLeader(winner);
            LeaderCard loserLeader = session.getLeader(loser);
            if (winnerLeader != null && loserLeader != null) {
                String winnerFaction = winnerLeader.getFaction() != null ? 
                    winnerLeader.getFaction() : "UNKNOWN";
                String loserFaction = loserLeader.getFaction() != null ? 
                    loserLeader.getFaction() : "UNKNOWN";
                storage.recordFactionMatchup(winnerFaction, loserFaction, true);
            }
        }
    }
    
    /**
     * Обновляет среднее время хода игрока
     */
    public static void updateTurnTime(ServerPlayer player, long turnTimeMs) {
        if (player.level() instanceof ServerLevel serverLevel) {
            AnalyticsStorage storage = AnalyticsStorage.get(serverLevel);
            double turnTimeSeconds = turnTimeMs / 1000.0;
            storage.updateAverageTurnTime(player.getUUID().toString(), turnTimeSeconds);
        }
    }
    
    /**
     * Получает Win Rate карты
     */
    public static double getCardWinRate(ServerLevel level, String cardId) {
        AnalyticsStorage storage = AnalyticsStorage.get(level);
        return storage.getCardWinRate(cardId);
    }
    
    /**
     * Получает топ карт по Win Rate
     */
    public static List<AnalyticsStorage.CardWinRateEntry> getTopCardsByWinRate(ServerLevel level, int count) {
        AnalyticsStorage storage = AnalyticsStorage.get(level);
        return storage.getTopCardsByWinRate(count);
    }
}

