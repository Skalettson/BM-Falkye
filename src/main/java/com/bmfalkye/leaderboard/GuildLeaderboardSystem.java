package com.bmfalkye.leaderboard;

import com.bmfalkye.rating.RatingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система гильдейского рейтинга
 * Рейтинг основан на сумме очков участников и контролируемых территориях
 */
public class GuildLeaderboardSystem {
    /**
     * Вычисляет рейтинг гильдии
     */
    public static int calculateGuildRating(ServerLevel level, List<ServerPlayer> members) {
        int totalRating = 0;
        
        // Суммируем рейтинги всех участников
        for (ServerPlayer member : members) {
            RatingSystem.PlayerRating rating = RatingSystem.getPlayerRating(member);
            totalRating += rating.getRating();
        }
        
        // TODO: Добавить бонус за контролируемые территории
        // (когда будет реализована Территориальная Война Гильдий)
        
        return totalRating;
    }
    
    /**
     * Получает рейтинг всех гильдий
     */
    public static List<GuildRatingEntry> getGuildRatings(ServerLevel level) {
        List<GuildRatingEntry> ratings = new ArrayList<>();
        
        // TODO: Интегрировать с существующей системой гильдий
        // Пока возвращаем пустой список
        
        return ratings;
    }
    
    /**
     * Запись рейтинга гильдии
     */
    public static class GuildRatingEntry {
        public final String guildId;
        public final String guildName;
        public final int rating;
        public final int memberCount;
        public final int controlledTerritories;
        
        public GuildRatingEntry(String guildId, String guildName, int rating, 
                               int memberCount, int controlledTerritories) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.rating = rating;
            this.memberCount = memberCount;
            this.controlledTerritories = controlledTerritories;
        }
    }
}

