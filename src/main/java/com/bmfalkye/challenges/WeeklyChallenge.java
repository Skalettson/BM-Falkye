package com.bmfalkye.challenges;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Еженедельные испытания
 */
public class WeeklyChallenge {
    public enum ChallengeType {
        WIN_GAMES,          // Выиграть N игр
        PLAY_CARDS,         // Разыграть N карт
        WIN_ROUNDS,         // Выиграть N раундов
        COLLECT_CARDS,      // Собрать N карт
        USE_LEADER,         // Использовать способность лидера N раз
        WIN_TOURNAMENT,     // Выиграть турнир
        REACH_RANK          // Достичь ранга
    }
    
    private final String id;
    private final String name;
    private final String description;
    private final ChallengeType type;
    private final int target;
    private final Map<UUID, Integer> playerProgress = new HashMap<>();
    private final long startTime;
    private final long endTime;
    
    public WeeklyChallenge(String id, String name, String description, 
                          ChallengeType type, int target, long durationMs) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + durationMs;
    }
    
    /**
     * Обновляет прогресс игрока
     */
    public void updateProgress(ServerPlayer player, int amount) {
        UUID playerId = player.getUUID();
        int current = playerProgress.getOrDefault(playerId, 0);
        playerProgress.put(playerId, Math.min(current + amount, target));
    }
    
    /**
     * Проверяет, выполнено ли испытание
     */
    public boolean isCompleted(ServerPlayer player) {
        return playerProgress.getOrDefault(player.getUUID(), 0) >= target;
    }
    
    /**
     * Проверяет, истёк ли срок испытания
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }
    
    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ChallengeType getType() { return type; }
    public int getTarget() { return target; }
    public int getProgress(ServerPlayer player) { 
        return playerProgress.getOrDefault(player.getUUID(), 0); 
    }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
}

