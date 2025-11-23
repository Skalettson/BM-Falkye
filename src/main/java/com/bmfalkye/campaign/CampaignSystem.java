package com.bmfalkye.campaign;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Система кампании/сюжетного режима
 */
public class CampaignSystem {
    // UUID игрока -> Прогресс кампании
    private static final Map<UUID, CampaignProgress> playerProgress = new HashMap<>();
    
    /**
     * Получает прогресс кампании игрока
     */
    public static CampaignProgress getProgress(ServerPlayer player) {
        return playerProgress.computeIfAbsent(player.getUUID(), k -> new CampaignProgress());
    }
    
    /**
     * Разблокирует следующую главу
     */
    public static void unlockNextChapter(ServerPlayer player) {
        CampaignProgress progress = getProgress(player);
        progress.unlockNextChapter();
    }
    
    /**
     * Завершает миссию
     */
    public static void completeMission(ServerPlayer player, String missionId) {
        CampaignProgress progress = getProgress(player);
        progress.completeMission(missionId);
    }
    
    /**
     * Прогресс кампании
     */
    public static class CampaignProgress {
        private int currentChapter = 1;
        private final Set<String> completedMissions = new HashSet<>();
        private final Set<Integer> unlockedChapters = new HashSet<>();
        
        public CampaignProgress() {
            unlockedChapters.add(1); // Первая глава доступна сразу
        }
        
        public int getCurrentChapter() {
            return currentChapter;
        }
        
        public void unlockNextChapter() {
            currentChapter++;
            unlockedChapters.add(currentChapter);
        }
        
        public void completeMission(String missionId) {
            completedMissions.add(missionId);
        }
        
        public boolean isMissionCompleted(String missionId) {
            return completedMissions.contains(missionId);
        }
        
        public boolean isChapterUnlocked(int chapter) {
            return unlockedChapters.contains(chapter);
        }
        
        public Set<String> getCompletedMissions() {
            return new HashSet<>(completedMissions);
        }
    }
}

