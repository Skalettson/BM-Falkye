package com.bmfalkye.quests;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * Базовый класс для квестов
 */
public abstract class Quest {
    protected final String id;
    protected final String name;
    protected final String description;
    protected final QuestType type;
    protected final QuestRarity rarity;
    protected final List<QuestStep> steps;
    protected final QuestReward reward;
    
    public Quest(String id, String name, String description, QuestType type, 
                 QuestRarity rarity, List<QuestStep> steps, QuestReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.rarity = rarity;
        this.steps = steps;
        this.reward = reward;
    }
    
    /**
     * Проверяет, может ли игрок начать этот квест
     */
    public abstract boolean canStart(ServerPlayer player);
    
    /**
     * Начинает квест для игрока
     */
    public abstract void startQuest(ServerPlayer player);
    
    /**
     * Проверяет прогресс квеста
     */
    public abstract void checkProgress(ServerPlayer player);
    
    /**
     * Завершает квест
     */
    public abstract void completeQuest(ServerPlayer player);
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestType getType() { return type; }
    public QuestRarity getRarity() { return rarity; }
    public List<QuestStep> getSteps() { return steps; }
    public QuestReward getReward() { return reward; }
    
    /**
     * Типы квестов
     */
    public enum QuestType {
        LEGENDARY,  // Легендарные квесты с уникальными наградами
        STORY,      // Сюжетные квесты
        DAILY,      // Ежедневные квесты
        ACHIEVEMENT // Квесты-достижения
    }
    
    /**
     * Редкость квеста
     */
    public enum QuestRarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY
    }
    
    /**
     * Шаг квеста
     */
    public static class QuestStep {
        private final String id;
        private final String description;
        private final QuestStepType type;
        private final Object target; // Зависит от типа шага
        
        public QuestStep(String id, String description, QuestStepType type, Object target) {
            this.id = id;
            this.description = description;
            this.type = type;
            this.target = target;
        }
        
        public String getId() { return id; }
        public String getDescription() { return description; }
        public QuestStepType getType() { return type; }
        public Object getTarget() { return target; }
        
        /**
         * Типы шагов квеста
         */
        public enum QuestStepType {
            FIND_ITEM,      // Найти предмет (target = ItemStack или String itemId)
            REACH_LOCATION, // Достичь локации (target = BlockPos или координаты)
            WIN_DUEL,       // Выиграть дуэль (target = String npcId или null для любого)
            COLLECT_CARDS,  // Собрать карты (target = List<String> cardIds)
            USE_ITEM,       // Использовать предмет (target = String itemId)
            WAIT_TIME,      // Подождать время (target = Long timestamp или условие)
            CUSTOM          // Кастомное условие
        }
    }
    
    /**
     * Награда за квест
     */
    public static class QuestReward {
        private final List<String> cardIds; // Карты в награде
        private final int coins;
        private final int experience;
        private final String achievementId; // Достижение
        private final List<String> itemIds; // Предметы
        
        public QuestReward(List<String> cardIds, int coins, int experience, 
                          String achievementId, List<String> itemIds) {
            this.cardIds = cardIds != null ? cardIds : new java.util.ArrayList<>();
            this.coins = coins;
            this.experience = experience;
            this.achievementId = achievementId;
            this.itemIds = itemIds != null ? itemIds : new java.util.ArrayList<>();
        }
        
        public List<String> getCardIds() { return cardIds; }
        public int getCoins() { return coins; }
        public int getExperience() { return experience; }
        public String getAchievementId() { return achievementId; }
        public List<String> getItemIds() { return itemIds; }
        
        /**
         * Выдаёт награду игроку
         */
        public void giveReward(ServerPlayer player) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Выдаём карты
                com.bmfalkye.storage.PlayerCardCollection collection = 
                    com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                for (String cardId : cardIds) {
                    collection.addCard(player, cardId);
                }
                
                // Выдаём монеты
                if (coins > 0) {
                    com.bmfalkye.storage.PlayerCurrency currency = 
                        com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                    currency.addCoins(player, coins);
                }
                
                // Выдаём опыт
                if (experience > 0) {
                    com.bmfalkye.storage.PlayerProgressStorage progressStorage = 
                        com.bmfalkye.storage.PlayerProgressStorage.get(serverLevel);
                    com.bmfalkye.player.PlayerProgress progress = progressStorage.getPlayerProgress(player);
                    progress.addExperience(experience);
                    progressStorage.setPlayerProgress(player, progress);
                }
                
                // Выдаём достижение
                if (achievementId != null && !achievementId.isEmpty()) {
                    com.bmfalkye.storage.PlayerProgressStorage progressStorage = 
                        com.bmfalkye.storage.PlayerProgressStorage.get(serverLevel);
                    com.bmfalkye.player.PlayerProgress progress = progressStorage.getPlayerProgress(player);
                    progress.unlockAchievement(achievementId);
                    com.bmfalkye.achievements.AchievementSystem.checkAchievements(player, progress);
                    progressStorage.setPlayerProgress(player, progress);
                }
                
                // Выдаём предметы
                for (String itemId : itemIds) {
                    // TODO: Создать предметы и выдать их
                }
            }
        }
    }
}

