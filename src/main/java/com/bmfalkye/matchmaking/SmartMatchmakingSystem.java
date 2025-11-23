package com.bmfalkye.matchmaking;

import com.bmfalkye.analytics.AnalyticsStorage;
import com.bmfalkye.rating.RatingSystem;
import com.bmfalkye.storage.PlayerCardCollection;
import com.bmfalkye.storage.PlayerProgressStorage;
import com.bmfalkye.player.PlayerProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Интеллектуальная система матчмейкинга
 * Подбирает оппонентов на основе рейтинга, коллекции и стратегий
 */
public class SmartMatchmakingSystem {
    private static final int RATING_TOLERANCE = 200; // Допустимая разница в рейтинге
    private static final double COLLECTION_SIMILARITY_WEIGHT = 0.3; // Вес схожести коллекции
    private static final double RATING_WEIGHT = 0.5; // Вес рейтинга
    private static final double STRATEGY_WEIGHT = 0.2; // Вес стратегии
    
    /**
     * Находит лучшего оппонента для игрока
     */
    public static ServerPlayer findBestOpponent(ServerPlayer player, List<ServerPlayer> availablePlayers) {
        if (availablePlayers.isEmpty()) {
            return null;
        }
        
        if (player.level() instanceof ServerLevel serverLevel) {
            RatingSystem.PlayerRating playerRating = RatingSystem.getPlayerRating(player);
            int playerRatingValue = playerRating.getRating();
            
            // Получаем коллекцию игрока
            PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
            Set<String> playerCards = collection.getCards(player).stream()
                .map(card -> card.getId())
                .collect(java.util.stream.Collectors.toSet());
            
            List<MatchScore> scores = new ArrayList<>();
            
            for (ServerPlayer opponent : availablePlayers) {
                if (opponent.getUUID().equals(player.getUUID())) {
                    continue; // Пропускаем самого себя
                }
                
                // Вычисляем оценку матча
                double score = calculateMatchScore(
                    player, playerRatingValue, playerCards,
                    opponent, serverLevel
                );
                
                scores.add(new MatchScore(opponent, score));
            }
            
            // Сортируем по оценке (лучшие матчи первыми)
            scores.sort((a, b) -> Double.compare(b.score, a.score));
            
            // Возвращаем лучшего оппонента
            return scores.isEmpty() ? null : scores.get(0).player;
        }
        
        return null;
    }
    
    /**
     * Вычисляет оценку матча между двумя игроками
     */
    private static double calculateMatchScore(ServerPlayer player1, int rating1, Set<String> cards1,
                                             ServerPlayer player2, ServerLevel level) {
        RatingSystem.PlayerRating rating2 = RatingSystem.getPlayerRating(player2);
        int rating2Value = rating2.getRating();
        
        // Оценка по рейтингу (чем ближе, тем лучше)
        double ratingScore = 1.0 - Math.min(1.0, Math.abs(rating1 - rating2Value) / (double)RATING_TOLERANCE);
        
        // Оценка по коллекции
        PlayerCardCollection collection = PlayerCardCollection.get(level);
        Set<String> cards2 = collection.getCards(player2).stream()
            .map(card -> card.getId())
            .collect(java.util.stream.Collectors.toSet());
        double collectionScore = calculateCollectionSimilarity(cards1, cards2);
        
        // Оценка по стратегии (на основе выигрышных стратегий)
        double strategyScore = calculateStrategySimilarity(player1, player2, level);
        
        // Взвешенная сумма
        return ratingScore * RATING_WEIGHT + 
               collectionScore * COLLECTION_SIMILARITY_WEIGHT + 
               strategyScore * STRATEGY_WEIGHT;
    }
    
    /**
     * Вычисляет схожесть коллекций
     */
    private static double calculateCollectionSimilarity(Set<String> cards1, Set<String> cards2) {
        if (cards1.isEmpty() && cards2.isEmpty()) {
            return 1.0;
        }
        if (cards1.isEmpty() || cards2.isEmpty()) {
            return 0.0;
        }
        
        // Jaccard similarity
        Set<String> intersection = new HashSet<>(cards1);
        intersection.retainAll(cards2);
        
        Set<String> union = new HashSet<>(cards1);
        union.addAll(cards2);
        
        return union.isEmpty() ? 0.0 : (double)intersection.size() / union.size();
    }
    
    /**
     * Вычисляет схожесть стратегий (на основе выигрышных карт)
     */
    private static double calculateStrategySimilarity(ServerPlayer player1, ServerPlayer player2, ServerLevel level) {
        PlayerProgressStorage progressStorage = PlayerProgressStorage.get(level);
        
        // Получаем топ выигрышные карты каждого игрока
        PlayerProgress progress1 = progressStorage.getPlayerProgress(player1);
        PlayerProgress progress2 = progressStorage.getPlayerProgress(player2);
        
        // Используем статистику карт из прогресса
        Map<String, Integer> cardsWon1 = progress1.getCardsWon();
        Map<String, Integer> cardsWon2 = progress2.getCardsWon();
        
        // Находим общие выигрышные карты
        Set<String> topCards1 = getTopCards(cardsWon1, 10);
        Set<String> topCards2 = getTopCards(cardsWon2, 10);
        
        if (topCards1.isEmpty() && topCards2.isEmpty()) {
            return 0.5; // Нейтральная оценка для новых игроков
        }
        
        Set<String> intersection = new HashSet<>(topCards1);
        intersection.retainAll(topCards2);
        
        Set<String> union = new HashSet<>(topCards1);
        union.addAll(topCards2);
        
        return union.isEmpty() ? 0.0 : (double)intersection.size() / union.size();
    }
    
    /**
     * Получает топ карт по количеству побед
     */
    private static Set<String> getTopCards(Map<String, Integer> cardsWon, int count) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(cardsWon.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        Set<String> topCards = new HashSet<>();
        for (int i = 0; i < Math.min(count, sorted.size()); i++) {
            topCards.add(sorted.get(i).getKey());
        }
        return topCards;
    }
    
    /**
     * Оценка матча
     */
    private static class MatchScore {
        final ServerPlayer player;
        final double score;
        
        MatchScore(ServerPlayer player, double score) {
            this.player = player;
            this.score = score;
        }
    }
}

