package com.bmfalkye.quests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Хранилище прогресса квестов игроков
 */
public class QuestStorage extends SavedData {
    // UUID игрока -> Map<ID квеста, прогресс>
    private final Map<UUID, Map<String, QuestProgress>> playerQuests = new HashMap<>();
    
    public static QuestStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            QuestStorage::load,
            QuestStorage::new,
            "bm_falkye_quests"
        );
    }
    
    public static QuestStorage load(CompoundTag tag) {
        QuestStorage storage = new QuestStorage();
        
        try {
            CompoundTag playersTag = tag.getCompound("players");
            
            for (String key : playersTag.getAllKeys()) {
                UUID playerUUID = com.bmfalkye.util.DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    com.bmfalkye.util.ModLogger.warn("Skipping invalid UUID during quest load", "key", key);
                    continue;
                }
                
                try {
                    CompoundTag playerQuestsTag = playersTag.getCompound(key);
                    Map<String, QuestProgress> quests = new HashMap<>();
                    
                    for (String questId : playerQuestsTag.getAllKeys()) {
                        CompoundTag questTag = playerQuestsTag.getCompound(questId);
                        QuestProgress progress = QuestProgress.load(questTag);
                        if (progress != null) {
                            quests.put(questId, progress);
                        }
                    }
                    
                    if (!quests.isEmpty()) {
                        storage.playerQuests.put(playerUUID, quests);
                    }
                } catch (Exception e) {
                    com.bmfalkye.util.ModLogger.error("Error loading quests for player", 
                        "uuid", playerUUID.toString(), "error", e.getMessage());
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading QuestStorage, using empty storage", e);
            com.bmfalkye.util.ModLogger.error("Error loading QuestStorage", "error", e.getMessage());
            return new QuestStorage();
        }
        
        return storage;
    }
    
    @Override
    public CompoundTag save(@org.jetbrains.annotations.NotNull CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        
        for (Map.Entry<UUID, Map<String, QuestProgress>> playerEntry : playerQuests.entrySet()) {
            CompoundTag playerQuestsTag = new CompoundTag();
            
            for (Map.Entry<String, QuestProgress> questEntry : playerEntry.getValue().entrySet()) {
                CompoundTag questTag = questEntry.getValue().save(new CompoundTag());
                playerQuestsTag.put(questEntry.getKey(), questTag);
            }
            
            if (!playerQuestsTag.isEmpty()) {
                playersTag.put(playerEntry.getKey().toString(), playerQuestsTag);
            }
        }
        
        tag.put("players", playersTag);
        return tag;
    }
    
    /**
     * Начать квест для игрока
     */
    public void startQuest(ServerPlayer player, String questId) {
        Map<String, QuestProgress> quests = playerQuests.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        
        QuestProgress progress = new QuestProgress(questId, true, 0);
        quests.put(questId, progress);
        setDirty();
    }
    
    /**
     * Завершить квест
     */
    public void completeQuest(ServerPlayer player, String questId) {
        Map<String, QuestProgress> quests = playerQuests.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        
        QuestProgress progress = quests.get(questId);
        if (progress != null) {
            progress.setCompleted(true);
            progress.setActive(false);
            setDirty();
        }
    }
    
    /**
     * Получить прогресс квеста
     */
    public QuestProgress getQuestProgress(ServerPlayer player, String questId) {
        Map<String, QuestProgress> quests = playerQuests.get(player.getUUID());
        if (quests == null) {
            return null;
        }
        return quests.get(questId);
    }
    
    /**
     * Установить прогресс квеста
     */
    public void setQuestProgress(ServerPlayer player, String questId, QuestProgress progress) {
        Map<String, QuestProgress> quests = playerQuests.computeIfAbsent(
            player.getUUID(), k -> new HashMap<>());
        quests.put(questId, progress);
        setDirty();
    }
    
    /**
     * Проверить, завершён ли квест
     */
    public boolean isQuestCompleted(ServerPlayer player, String questId) {
        QuestProgress progress = getQuestProgress(player, questId);
        return progress != null && progress.isCompleted();
    }
    
    /**
     * Получить все активные квесты игрока
     */
    public List<String> getActiveQuests(ServerPlayer player) {
        Map<String, QuestProgress> quests = playerQuests.get(player.getUUID());
        if (quests == null) {
            return new ArrayList<>();
        }
        
        List<String> active = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().isActive() && !entry.getValue().isCompleted()) {
                active.add(entry.getKey());
            }
        }
        return active;
    }
    
    /**
     * Получить все завершённые квесты игрока
     */
    public List<String> getCompletedQuests(ServerPlayer player) {
        Map<String, QuestProgress> quests = playerQuests.get(player.getUUID());
        if (quests == null) {
            return new ArrayList<>();
        }
        
        List<String> completed = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().isCompleted()) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }
    
    /**
     * Прогресс квеста игрока
     */
    public static class QuestProgress {
        private String questId;
        private boolean active;
        private boolean completed;
        private int currentStep;
        private Map<String, Object> stepData; // Дополнительные данные для шагов
        
        public QuestProgress(String questId, boolean active, int currentStep) {
            this.questId = questId;
            this.active = active;
            this.completed = false;
            this.currentStep = currentStep;
            this.stepData = new HashMap<>();
        }
        
        public static QuestProgress load(CompoundTag tag) {
            try {
                String questId = tag.getString("questId");
                boolean active = tag.getBoolean("active");
                boolean completed = tag.getBoolean("completed");
                int currentStep = tag.getInt("currentStep");
                
                QuestProgress progress = new QuestProgress(questId, active, currentStep);
                progress.setCompleted(completed);
                
                // Загружаем дополнительные данные шагов
                if (tag.contains("stepData")) {
                    CompoundTag stepDataTag = tag.getCompound("stepData");
                    for (String key : stepDataTag.getAllKeys()) {
                        progress.stepData.put(key, stepDataTag.get(key));
                    }
                }
                
                return progress;
            } catch (Exception e) {
                com.bmfalkye.util.ModLogger.error("Error loading QuestProgress", "error", e.getMessage());
                return null;
            }
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putString("questId", questId);
            tag.putBoolean("active", active);
            tag.putBoolean("completed", completed);
            tag.putInt("currentStep", currentStep);
            
            // Сохраняем дополнительные данные шагов
            if (!stepData.isEmpty()) {
                CompoundTag stepDataTag = new CompoundTag();
                for (Map.Entry<String, Object> entry : stepData.entrySet()) {
                    // Упрощённое сохранение (только строки и числа)
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        stepDataTag.putString(entry.getKey(), (String) value);
                    } else if (value instanceof Integer) {
                        stepDataTag.putInt(entry.getKey(), (Integer) value);
                    } else if (value instanceof Long) {
                        stepDataTag.putLong(entry.getKey(), (Long) value);
                    }
                }
                tag.put("stepData", stepDataTag);
            }
            
            return tag;
        }
        
        public String getQuestId() { return questId; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public int getCurrentStep() { return currentStep; }
        public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
        public Map<String, Object> getStepData() { return stepData; }
    }
}

