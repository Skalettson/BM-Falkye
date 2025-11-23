package com.bmfalkye.statistics;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система статистики и аналитики для игроков
 */
public class StatisticsSystem {
    
    /**
     * Получает полную статистику игрока
     */
    public static PlayerStatistics getPlayerStatistics(ServerPlayer player) {
        PlayerProgressStorage storage = PlayerProgressStorage.get(
            (net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        int totalGames = progress.getTotalGamesPlayed();
        int wins = progress.getTotalGamesWon();
        int losses = progress.getTotalGamesLost();
        double winRate = totalGames > 0 ? (wins * 100.0 / totalGames) : 0.0;
        
        // Статистика по картам
        Map<String, Integer> cardsPlayed = progress.getStatistics();
        String mostPlayedCard = findMostPlayedCard(cardsPlayed);
        
        // Статистика по фракциям
        Map<String, Integer> factionWins = getFactionStatistics(progress);
        
        // Статистика по раундам
        int totalRoundsWon = progress.getStatistics().getOrDefault("total_rounds_won", 0);
        int totalRoundsLost = progress.getStatistics().getOrDefault("total_rounds_lost", 0);
        double roundWinRate = (totalRoundsWon + totalRoundsLost) > 0 ? 
            (totalRoundsWon * 100.0 / (totalRoundsWon + totalRoundsLost)) : 0.0;
        
        // Рейтинг
        int rating = progress.getStatistics().getOrDefault("rating", 1000);
        com.bmfalkye.rating.RatingSystem.Rank rank = 
            com.bmfalkye.rating.RatingSystem.Rank.getRankForRating(rating);
        
        return new PlayerStatistics(
            totalGames, wins, losses, winRate,
            mostPlayedCard, factionWins,
            totalRoundsWon, totalRoundsLost, roundWinRate,
            rating, rank, progress.getLevel()
        );
    }
    
    /**
     * Находит самую часто используемую карту
     */
    private static String findMostPlayedCard(Map<String, Integer> cardsPlayed) {
        return cardsPlayed.entrySet().stream()
            .filter(e -> e.getKey().startsWith("card_"))
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(e -> e.getKey().substring(5)) // Убираем префикс "card_"
            .orElse("Нет данных");
    }
    
    /**
     * Получает статистику по фракциям
     */
    private static Map<String, Integer> getFactionStatistics(PlayerProgress progress) {
        Map<String, Integer> factionWins = new HashMap<>();
        Map<String, Integer> stats = progress.getStatistics();
        
        factionWins.put("Дом Пламени", stats.getOrDefault("faction_fire_wins", 0));
        factionWins.put("Дозорные Руин", stats.getOrDefault("faction_watcher_wins", 0));
        factionWins.put("Дети Рощения", stats.getOrDefault("faction_nature_wins", 0));
        factionWins.put("Нейтральная", stats.getOrDefault("faction_neutral_wins", 0));
        
        return factionWins;
    }
    
    /**
     * Отправляет статистику игроку
     */
    public static void sendStatistics(ServerPlayer player) {
        PlayerStatistics stats = getPlayerStatistics(player);
        
        player.sendSystemMessage(Component.literal("§6§l=== ВАША СТАТИСТИКА ==="));
        player.sendSystemMessage(Component.literal("§7Всего игр: §f" + stats.getTotalGames()));
        player.sendSystemMessage(Component.literal("§aПобед: §f" + stats.getWins()));
        player.sendSystemMessage(Component.literal("§cПоражений: §f" + stats.getLosses()));
        player.sendSystemMessage(Component.literal("§eПроцент побед: §f" + 
            String.format("%.1f", stats.getWinRate()) + "%"));
        
        player.sendSystemMessage(Component.literal("§6--- Рейтинг ---"));
        player.sendSystemMessage(Component.literal("§7Рейтинг: §f" + stats.getRating()));
        player.sendSystemMessage(Component.literal("§7Ранг: " + stats.getRank().getColorCode() + 
            stats.getRank().getDisplayName()));
        
        player.sendSystemMessage(Component.literal("§6--- Раунды ---"));
        player.sendSystemMessage(Component.literal("§aВыиграно раундов: §f" + stats.getRoundsWon()));
        player.sendSystemMessage(Component.literal("§cПроиграно раундов: §f" + stats.getRoundsLost()));
        player.sendSystemMessage(Component.literal("§eПроцент побед в раундах: §f" + 
            String.format("%.1f", stats.getRoundWinRate()) + "%"));
        
        player.sendSystemMessage(Component.literal("§6--- Фракции ---"));
        for (Map.Entry<String, Integer> entry : stats.getFactionWins().entrySet()) {
            player.sendSystemMessage(Component.literal(
                "§7" + entry.getKey() + ": §a" + entry.getValue() + " побед"));
        }
        
        if (!stats.getMostPlayedCard().equals("Нет данных")) {
            player.sendSystemMessage(Component.literal("§6--- Самая используемая карта ---"));
            player.sendSystemMessage(Component.literal("§7" + stats.getMostPlayedCard()));
        }
    }
    
    /**
     * Обновляет статистику после игры
     */
    public static void updateGameStatistics(FalkyeGameSession session, ServerPlayer winner, ServerPlayer loser) {
        PlayerProgressStorage storage = PlayerProgressStorage.get(
            (net.minecraft.server.level.ServerLevel) winner.level());
        
        // Обновляем статистику победителя
        PlayerProgress winnerProgress = storage.getPlayerProgress(winner);
        int roundsWon = session.getRoundsWon(winner);
        int roundsLost = session.getRoundsWon(loser);
        winnerProgress.getStatistics().put("total_rounds_won", 
            winnerProgress.getStatistics().getOrDefault("total_rounds_won", 0) + roundsWon);
        winnerProgress.getStatistics().put("total_rounds_lost", 
            winnerProgress.getStatistics().getOrDefault("total_rounds_lost", 0) + roundsLost);
        storage.setPlayerProgress(winner, winnerProgress);
        
        // Обновляем статистику проигравшего
        if (loser != null) {
            PlayerProgressStorage loserStorage = PlayerProgressStorage.get(
                (net.minecraft.server.level.ServerLevel) loser.level());
            PlayerProgress loserProgress = loserStorage.getPlayerProgress(loser);
            loserProgress.getStatistics().put("total_rounds_won", 
                loserProgress.getStatistics().getOrDefault("total_rounds_won", 0) + roundsLost);
            loserProgress.getStatistics().put("total_rounds_lost", 
                loserProgress.getStatistics().getOrDefault("total_rounds_lost", 0) + roundsWon);
            loserStorage.setPlayerProgress(loser, loserProgress);
        }
    }
    
    /**
     * Класс статистики игрока
     */
    public static class PlayerStatistics {
        private final int totalGames;
        private final int wins;
        private final int losses;
        private final double winRate;
        private final String mostPlayedCard;
        private final Map<String, Integer> factionWins;
        private final int roundsWon;
        private final int roundsLost;
        private final double roundWinRate;
        private final int rating;
        private final com.bmfalkye.rating.RatingSystem.Rank rank;
        private final int level;
        
        public PlayerStatistics(int totalGames, int wins, int losses, double winRate,
                               String mostPlayedCard, Map<String, Integer> factionWins,
                               int roundsWon, int roundsLost, double roundWinRate,
                               int rating, com.bmfalkye.rating.RatingSystem.Rank rank, int level) {
            this.totalGames = totalGames;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
            this.mostPlayedCard = mostPlayedCard;
            this.factionWins = factionWins;
            this.roundsWon = roundsWon;
            this.roundsLost = roundsLost;
            this.roundWinRate = roundWinRate;
            this.rating = rating;
            this.rank = rank;
            this.level = level;
        }
        
        public int getTotalGames() { return totalGames; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public double getWinRate() { return winRate; }
        public String getMostPlayedCard() { return mostPlayedCard; }
        public Map<String, Integer> getFactionWins() { return factionWins; }
        public int getRoundsWon() { return roundsWon; }
        public int getRoundsLost() { return roundsLost; }
        public double getRoundWinRate() { return roundWinRate; }
        public int getRating() { return rating; }
        public com.bmfalkye.rating.RatingSystem.Rank getRank() { return rank; }
        public int getLevel() { return level; }
    }
}

