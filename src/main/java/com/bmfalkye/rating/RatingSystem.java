package com.bmfalkye.rating;

import com.bmfalkye.storage.PlayerProgressStorage;
import com.bmfalkye.player.PlayerProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система рейтинга и рангов для соревновательной игры
 */
public class RatingSystem {
    private static final Map<UUID, PlayerRating> playerRatings = new HashMap<>();
    
    // Настройки рейтинга
    private static final int INITIAL_RATING = 1000;
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 5000;
    
    // Ранги
    public enum Rank {
        BRONZE("Бронза", 0, 1000, "§c"),
        SILVER("Серебро", 1000, 1500, "§7"),
        GOLD("Золото", 1500, 2000, "§e"),
        PLATINUM("Платина", 2000, 2500, "§b"),
        DIAMOND("Алмаз", 2500, 3000, "§d"),
        MASTER("Мастер", 3000, 3500, "§5"),
        GRANDMASTER("Грандмастер", 3500, 4000, "§6"),
        LEGEND("Легенда", 4000, MAX_RATING, "§c§l");
        
        private final String displayName;
        private final int minRating;
        private final int maxRating;
        private final String colorCode;
        
        Rank(String displayName, int minRating, int maxRating, String colorCode) {
            this.displayName = displayName;
            this.minRating = minRating;
            this.maxRating = maxRating;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getMinRating() {
            return minRating;
        }
        
        public int getMaxRating() {
            return maxRating;
        }
        
        public String getColorCode() {
            return colorCode;
        }
        
        public static Rank getRankForRating(int rating) {
            for (Rank rank : values()) {
                if (rating >= rank.minRating && rating < rank.maxRating) {
                    return rank;
                }
            }
            return LEGEND; // По умолчанию легенда для максимального рейтинга
        }
    }
    
    /**
     * Получает рейтинг игрока
     */
    public static PlayerRating getPlayerRating(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        return playerRatings.computeIfAbsent(playerUUID, k -> {
            // Загружаем из сохранения или создаём новый
            PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            
            // Пытаемся загрузить из прогресса
            int savedRating = progress.getStatistics().getOrDefault("rating", INITIAL_RATING);
            return new PlayerRating(savedRating);
        });
    }
    
    /**
     * Обновляет рейтинг после игры
     */
    public static void updateRating(ServerPlayer winner, ServerPlayer loser, int winnerRoundsWon, int loserRoundsWon) {
        PlayerRating winnerRating = getPlayerRating(winner);
        PlayerRating loserRating = getPlayerRating(loser);
        
        // Вычисляем изменение рейтинга (система Elo)
        int ratingChange = calculateRatingChange(winnerRating.getRating(), loserRating.getRating());
        
        // Обновляем рейтинги
        int oldWinnerRating = winnerRating.getRating();
        int oldLoserRating = loserRating.getRating();
        
        winnerRating.addRating(ratingChange);
        loserRating.addRating(-ratingChange);
        
        // Проверяем изменение ранга
        Rank oldWinnerRank = Rank.getRankForRating(oldWinnerRating);
        Rank newWinnerRank = Rank.getRankForRating(winnerRating.getRating());
        Rank oldLoserRank = Rank.getRankForRating(oldLoserRating);
        Rank newLoserRank = Rank.getRankForRating(loserRating.getRating());
        
        // Уведомляем о изменении ранга
        if (!oldWinnerRank.equals(newWinnerRank)) {
            winner.sendSystemMessage(Component.literal(
                "§a§lНОВЫЙ РАНГ! " + newWinnerRank.getColorCode() + newWinnerRank.getDisplayName()));
            com.bmfalkye.network.NetworkHandler.addActionLog(winner, 
                "§a§lНОВЫЙ РАНГ! " + newWinnerRank.getColorCode() + newWinnerRank.getDisplayName());
            
            // Проверяем достижение ранга Легенда
            if (newWinnerRank == Rank.LEGEND) {
                com.bmfalkye.leaderboard.LegendRankTracker.checkLegendRank(winner);
            }
        }
        
        if (!oldLoserRank.equals(newLoserRank)) {
            loser.sendSystemMessage(Component.literal(
                "§c§lИЗМЕНЕНИЕ РАНГА: " + newLoserRank.getColorCode() + newLoserRank.getDisplayName()));
            com.bmfalkye.network.NetworkHandler.addActionLog(loser, 
                "§c§lИЗМЕНЕНИЕ РАНГА: " + newLoserRank.getColorCode() + newLoserRank.getDisplayName());
        }
        
        // Уведомляем об изменении рейтинга
        winner.sendSystemMessage(Component.literal(
            "§aРейтинг: " + oldWinnerRating + " → " + winnerRating.getRating() + 
            " (" + (ratingChange > 0 ? "+" : "") + ratingChange + ")"));
        loser.sendSystemMessage(Component.literal(
            "§cРейтинг: " + oldLoserRating + " → " + loserRating.getRating() + 
            " (" + (-ratingChange) + ")"));
        
        // Сохраняем рейтинг в прогресс
        saveRating(winner, winnerRating);
        saveRating(loser, loserRating);
    }
    
    /**
     * Вычисляет изменение рейтинга по системе Elo
     */
    private static int calculateRatingChange(int winnerRating, int loserRating) {
        // K-фактор (максимальное изменение)
        int K = 32;
        
        // Ожидаемый результат
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (loserRating - winnerRating) / 400.0));
        
        // Фактический результат (1.0 для победы)
        double actualScore = 1.0;
        
        // Изменение рейтинга
        int ratingChange = (int)(K * (actualScore - expectedScore));
        
        // Ограничиваем изменение
        return Math.max(1, Math.min(50, ratingChange));
    }
    
    /**
     * Сохраняет рейтинг в прогресс игрока
     */
    private static void saveRating(ServerPlayer player, PlayerRating rating) {
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        progress.getStatistics().put("rating", rating.getRating());
        storage.setPlayerProgress(player, progress);
    }
    
    /**
     * Получает топ игроков по рейтингу
     */
    public static List<RatingEntry> getTopPlayers(int count) {
        List<RatingEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, PlayerRating> entry : playerRatings.entrySet()) {
            entries.add(new RatingEntry(entry.getKey(), entry.getValue().getRating()));
        }
        entries.sort((a, b) -> Integer.compare(b.rating, a.rating));
        return entries.subList(0, Math.min(count, entries.size()));
    }
    
    /**
     * Получает позицию игрока в рейтинге
     */
    public static int getPlayerPosition(ServerPlayer player) {
        List<RatingEntry> topPlayers = getTopPlayers(Integer.MAX_VALUE);
        UUID playerUUID = player.getUUID();
        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).playerUUID.equals(playerUUID)) {
                return i + 1;
            }
        }
        return -1; // Игрок не найден в рейтинге
    }
    
    /**
     * Класс рейтинга игрока
     */
    public static class PlayerRating {
        private int rating;
        private int gamesPlayed;
        private int gamesWon;
        private int gamesLost;
        private int winStreak;
        private int bestWinStreak;
        
        public PlayerRating(int initialRating) {
            this.rating = Math.max(MIN_RATING, Math.min(MAX_RATING, initialRating));
            this.gamesPlayed = 0;
            this.gamesWon = 0;
            this.gamesLost = 0;
            this.winStreak = 0;
            this.bestWinStreak = 0;
        }
        
        public void addRating(int change) {
            this.rating = Math.max(MIN_RATING, Math.min(MAX_RATING, this.rating + change));
        }
        
        public int getRating() {
            return rating;
        }
        
        public Rank getRank() {
            return Rank.getRankForRating(rating);
        }
        
        public void recordWin() {
            gamesPlayed++;
            gamesWon++;
            winStreak++;
            if (winStreak > bestWinStreak) {
                bestWinStreak = winStreak;
            }
        }
        
        public void recordLoss() {
            gamesPlayed++;
            gamesLost++;
            winStreak = 0;
        }
        
        public double getWinRate() {
            if (gamesPlayed == 0) return 0.0;
            return (double)gamesWon / gamesPlayed * 100.0;
        }
    }
    
    /**
     * Запись в рейтинге
     */
    public static class RatingEntry {
        public final UUID playerUUID;
        public final int rating;
        
        public RatingEntry(UUID playerUUID, int rating) {
            this.playerUUID = playerUUID;
            this.rating = rating;
        }
    }
}

