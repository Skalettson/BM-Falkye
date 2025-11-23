package com.bmfalkye.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.*;

/**
 * Система прогресса игрока (уровни, опыт, достижения)
 */
public class PlayerProgress {
    private int level = 1;
    private int experience = 0;
    private int coins = 800; // Начальные монеты
    private final Set<String> unlockedCards = new HashSet<>();
    private final Set<String> achievements = new HashSet<>();
    private final Map<String, Integer> statistics = new HashMap<>();
    private int totalGamesPlayed = 0;
    private int totalGamesWon = 0;
    private int totalGamesLost = 0;
    
    // Статистика по картам
    private final Map<String, Integer> cardsPlayed = new HashMap<>();
    private final Map<String, Integer> cardsWon = new HashMap<>();
    
    public static PlayerProgress load(CompoundTag tag) {
        PlayerProgress progress = new PlayerProgress();
        
        try {
            // Валидируем и загружаем уровень
            progress.level = com.bmfalkye.util.DataLoadValidator.validateLevel(tag.getInt("level"));
            
            // Валидируем и загружаем опыт
            progress.experience = com.bmfalkye.util.DataLoadValidator.validateExperience(tag.getInt("experience"));
            
            // Загружаем монеты (если нет, используем начальное значение)
            progress.coins = tag.contains("coins") ? 
                com.bmfalkye.util.DataLoadValidator.validateCoins(tag.getInt("coins")) : 800;
            
            // Загружаем разблокированные карты с валидацией
            ListTag unlockedCardsList = tag.getList("unlockedCards", 8);
            for (int i = 0; i < unlockedCardsList.size(); i++) {
                String cardId = unlockedCardsList.getString(i);
                if (com.bmfalkye.util.DataLoadValidator.isValidCardId(cardId)) {
                    progress.unlockedCards.add(cardId);
                } else {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid card ID during progress load", 
                        "cardId", cardId, "index", i);
                }
            }
            
            // Загружаем достижения с валидацией
            ListTag achievementsList = tag.getList("achievements", 8); // 8 = String tag type
            for (int i = 0; i < achievementsList.size(); i++) {
                String achievementId = achievementsList.getString(i);
                if (com.bmfalkye.util.DataLoadValidator.isValidAchievementId(achievementId)) {
                    progress.achievements.add(achievementId);
                } else {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid achievement ID during progress load", 
                        "achievementId", achievementId, "index", i);
                }
            }
            
            // Загружаем статистику с валидацией
            CompoundTag statsTag = tag.getCompound("statistics");
            for (String key : statsTag.getAllKeys()) {
                int value = com.bmfalkye.util.DataLoadValidator.validateStatistic(statsTag.getInt(key));
                progress.statistics.put(key, value);
            }
            
            // Валидируем статистику игр
            progress.totalGamesPlayed = Math.max(0, tag.getInt("totalGamesPlayed"));
            progress.totalGamesWon = Math.max(0, tag.getInt("totalGamesWon"));
            progress.totalGamesLost = Math.max(0, tag.getInt("totalGamesLost"));
            
            // Проверяем логическую согласованность
            if (progress.totalGamesWon + progress.totalGamesLost > progress.totalGamesPlayed) {
                com.bmfalkye.util.ModLogger.warn("Inconsistent game statistics during load, fixing", 
                    "played", progress.totalGamesPlayed, 
                    "won", progress.totalGamesWon, 
                    "lost", progress.totalGamesLost);
                progress.totalGamesPlayed = progress.totalGamesWon + progress.totalGamesLost;
            }
            
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading PlayerProgress, using defaults", e);
            com.bmfalkye.util.ModLogger.error("Error loading PlayerProgress", "error", e.getMessage());
            // Возвращаем прогресс с дефолтными значениями
            return new PlayerProgress();
        }
        
        return progress;
    }
    
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("experience", experience);
        tag.putInt("coins", coins);
        
        // Сохраняем разблокированные карты
        ListTag unlockedList = new ListTag();
        for (String cardId : unlockedCards) {
            unlockedList.add(StringTag.valueOf(cardId));
        }
        tag.put("unlockedCards", unlockedList);
        
        // Сохраняем достижения
        ListTag achievementsList = new ListTag();
        for (String achievementId : achievements) {
            achievementsList.add(StringTag.valueOf(achievementId));
        }
        tag.put("achievements", achievementsList);
        
        // Сохраняем статистику
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
            statsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("statistics", statsTag);
        
        tag.putInt("totalGamesPlayed", totalGamesPlayed);
        tag.putInt("totalGamesWon", totalGamesWon);
        tag.putInt("totalGamesLost", totalGamesLost);
        
        return tag;
    }
    
    public void addExperience(int amount) {
        experience += amount;
        checkLevelUp();
    }
    
    /**
     * Добавляет опыт с возможностью выдачи наград за повышение уровня
     * @param player игрок (может быть null)
     * @param amount количество опыта
     */
    public void addExperience(net.minecraft.server.level.ServerPlayer player, int amount) {
        experience += amount;
        checkLevelUp(player);
    }
    
    /**
     * Устанавливает уровень игрока (для административных команд)
     */
    public void setLevel(int newLevel) {
        if (newLevel < 1) newLevel = 1;
        if (newLevel > 50) newLevel = 50;
        
        // Вычисляем опыт для нового уровня
        int expForNewLevel = getExperienceForLevel(newLevel);
        int expForCurrentLevel = getExperienceForLevel(level);
        
        // Устанавливаем опыт пропорционально уровню
        experience = expForCurrentLevel + (expForNewLevel - expForCurrentLevel);
        level = newLevel;
        
        // Проверяем, не превысили ли мы опыт для следующего уровня
        if (experience >= getExperienceForLevel(level + 1)) {
            experience = getExperienceForLevel(level + 1) - 1;
        }
    }
    
    private void checkLevelUp() {
        checkLevelUp(null); // Вызываем без игрока (награды не выдадутся)
    }
    
    /**
     * Проверяет повышение уровня с возможностью выдачи наград
     * @param player игрок (может быть null, тогда награды не выдаются)
     */
    public void checkLevelUp(net.minecraft.server.level.ServerPlayer player) {
        int expNeeded = getExperienceForLevel(level + 1);
        if (experience >= expNeeded && level < 50) {
            level++;
            experience -= expNeeded;
            
            // Выдаём награду за уровень, если игрок указан
            if (player != null) {
                com.bmfalkye.progress.LevelRewardSystem.giveLevelUpReward(player, level);
                
                // Вызываем событие повышения уровня через API
                // TODO: Реализовать через GameEventSystem при необходимости
            }
            
            checkLevelUp(player); // Рекурсивно проверяем, не перескочили ли уровни
        }
    }
    
    public static int getExperienceForLevel(int targetLevel) {
        // Формула: 100 * level^1.5
        return (int)(100 * Math.pow(targetLevel, 1.5));
    }
    
    public void unlockCard(String cardId) {
        unlockedCards.add(cardId);
    }
    
    public boolean hasCard(String cardId) {
        return unlockedCards.contains(cardId);
    }
    
    public void unlockAchievement(String achievementId) {
        achievements.add(achievementId);
    }
    
    public boolean hasAchievement(String achievementId) {
        return achievements.contains(achievementId);
    }
    
    public void recordGameWin() {
        recordGameWin(null);
    }
    
    public void recordGameWin(net.minecraft.server.level.ServerPlayer player) {
        totalGamesPlayed++;
        totalGamesWon++;
        addExperience(player, 50);
    }
    
    public void recordGameLoss() {
        recordGameLoss(null);
    }
    
    public void recordGameLoss(net.minecraft.server.level.ServerPlayer player) {
        totalGamesPlayed++;
        totalGamesLost++;
        addExperience(player, 20);
    }
    
    // Методы для работы с монетами
    public int getCoins() { return coins; }
    
    public void setCoins(int amount) {
        this.coins = Math.max(0, amount);
    }
    
    public void addCoins(int amount) {
        this.coins = Math.max(0, this.coins + amount);
    }
    
    public boolean removeCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }
    
    public boolean hasEnoughCoins(int amount) {
        return this.coins >= amount;
    }
    
    // Геттеры
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public Set<String> getUnlockedCards() { return unlockedCards; }
    public Set<String> getAchievements() { return achievements; }
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public int getTotalGamesWon() { return totalGamesWon; }
    public int getTotalGamesLost() { return totalGamesLost; }
    public Map<String, Integer> getStatistics() { return statistics; }
    public Map<String, Integer> getCardsWon() { return cardsWon; }
    public Map<String, Integer> getCardsPlayed() { return cardsPlayed; }
}

