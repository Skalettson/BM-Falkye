package com.bmfalkye.leaderboard;

import com.bmfalkye.rating.RatingSystem;
import com.bmfalkye.storage.PlayerProgressStorage;
import com.bmfalkye.player.PlayerProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Система еженедельного рейтинга
 */
public class WeeklyLeaderboardSystem {
    private static final int TOP_PLAYERS_COUNT = 50;
    
    /**
     * Обновляет еженедельный рейтинг игрока
     */
    public static void updateWeeklyRating(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            LeaderboardStorage storage = LeaderboardStorage.get(serverLevel);
            RatingSystem.PlayerRating rating = RatingSystem.getPlayerRating(player);
            
            storage.updateWeeklyEntry(
                player.getUUID(),
                player.getName().getString(),
                rating.getRating()
            );
            
            // Проверяем, попал ли игрок в топ-1
            int position = storage.getWeeklyPosition(player.getUUID());
            if (position == 1) {
                // Проверяем, есть ли уже титул
                PlayerProgressStorage progressStorage = PlayerProgressStorage.get(serverLevel);
                PlayerProgress progress = progressStorage.getPlayerProgress(player);
                
                if (!progress.getStatistics().containsKey("weekly_champion")) {
                    player.sendSystemMessage(Component.literal(
                        "§6§l══════ ЗАВОЕВАТЕЛЬ НЕДЕЛИ! ══════"));
                    player.sendSystemMessage(Component.literal(
                        "§eВы заняли первое место в еженедельном рейтинге!"));
                    
                    // Сохраняем титул
                    progress.getStatistics().put("weekly_champion", 1);
                    progressStorage.setPlayerProgress(player, progress);
                }
            }
        }
    }
    
    /**
     * Получает еженедельный рейтинг (топ-50)
     */
    public static List<LeaderboardStorage.LeaderboardEntry> getWeeklyLeaderboard(ServerLevel level) {
        LeaderboardStorage storage = LeaderboardStorage.get(level);
        return storage.getWeeklyLeaderboard();
    }
    
    /**
     * Получает позицию игрока в еженедельном рейтинге
     */
    public static int getPlayerWeeklyPosition(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            LeaderboardStorage storage = LeaderboardStorage.get(serverLevel);
            return storage.getWeeklyPosition(player.getUUID());
        }
        return -1;
    }
    
    /**
     * Проверяет и выдаёт титул "Завоеватель Недели" для топ-1
     */
    public static void checkWeeklyChampion(ServerLevel level) {
        LeaderboardStorage storage = LeaderboardStorage.get(level);
        List<LeaderboardStorage.LeaderboardEntry> leaderboard = storage.getWeeklyLeaderboard();
        
        if (!leaderboard.isEmpty()) {
            LeaderboardStorage.LeaderboardEntry champion = leaderboard.get(0);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(champion.playerUUID);
            
            if (player != null) {
                PlayerProgressStorage progressStorage = PlayerProgressStorage.get(level);
                PlayerProgress progress = progressStorage.getPlayerProgress(player);
                
                // Проверяем, есть ли уже титул
                if (!progress.getStatistics().containsKey("weekly_champion")) {
                    player.sendSystemMessage(Component.literal(
                        "§6§l══════ ЗАВОЕВАТЕЛЬ НЕДЕЛИ! ══════"));
                    player.sendSystemMessage(Component.literal(
                        "§eВы заняли первое место в еженедельном рейтинге!"));
                    
                    // Сохраняем титул
                    progress.getStatistics().put("weekly_champion", 1);
                    progressStorage.setPlayerProgress(player, progress);
                }
            }
        }
    }
}

