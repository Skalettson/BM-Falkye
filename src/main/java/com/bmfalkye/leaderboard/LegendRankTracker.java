package com.bmfalkye.leaderboard;

import com.bmfalkye.rating.RatingSystem;
import com.bmfalkye.season.SeasonSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Отслеживание ранга Легенда с сезонными отметками
 */
public class LegendRankTracker {
    /**
     * Проверяет, достиг ли игрок ранга Легенда, и добавляет в Зал Славы
     */
    public static void checkLegendRank(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            RatingSystem.PlayerRating rating = RatingSystem.getPlayerRating(player);
            
            if (rating.getRank() == RatingSystem.Rank.LEGEND) {
                // Получаем текущий сезон
                int currentSeason = SeasonSystem.getCurrentSeason() != null ? 
                    SeasonSystem.getCurrentSeason().getNumber() : 1;
                
                // Добавляем в Зал Славы
                LeaderboardStorage storage = LeaderboardStorage.get(serverLevel);
                storage.addToHallOfFame(
                    player.getUUID(),
                    player.getName().getString(),
                    currentSeason
                );
            }
        }
    }
}

