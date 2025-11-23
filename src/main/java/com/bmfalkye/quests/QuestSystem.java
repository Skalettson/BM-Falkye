package com.bmfalkye.quests;

import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система управления квестами
 */
public class QuestSystem {
    private static final Map<String, Quest> registeredQuests = new HashMap<>();
    
    /**
     * Регистрирует квест
     */
    public static void registerQuest(Quest quest) {
        registeredQuests.put(quest.getId(), quest);
    }
    
    /**
     * Получить квест по ID
     */
    public static Quest getQuest(String questId) {
        return registeredQuests.get(questId);
    }
    
    /**
     * Получить все зарегистрированные квесты
     */
    public static List<Quest> getAllQuests() {
        return new ArrayList<>(registeredQuests.values());
    }
    
    /**
     * Получить все легендарные квесты
     */
    public static List<LegendaryQuest> getLegendaryQuests() {
        List<LegendaryQuest> legendary = new ArrayList<>();
        for (Quest quest : registeredQuests.values()) {
            if (quest instanceof LegendaryQuest) {
                legendary.add((LegendaryQuest) quest);
            }
        }
        return legendary;
    }
    
    /**
     * Проверяет, может ли игрок начать квест
     */
    public static boolean canStartQuest(ServerPlayer player, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            return false;
        }
        return quest.canStart(player);
    }
    
    /**
     * Начинает квест для игрока
     */
    public static void startQuest(ServerPlayer player, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            return;
        }
        
        if (quest.canStart(player)) {
            quest.startQuest(player);
        }
    }
    
    /**
     * Проверяет прогресс всех активных квестов игрока
     */
    public static void checkPlayerQuests(ServerPlayer player) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            List<String> activeQuests = storage.getActiveQuests(player);
            
            for (String questId : activeQuests) {
                Quest quest = getQuest(questId);
                if (quest != null) {
                    quest.checkProgress(player);
                }
            }
        }
    }
    
    /**
     * Помечает шаг квеста как выполненный (для специальных случаев, например, победа в дуэли)
     */
    public static void completeQuestStep(ServerPlayer player, String questId, String stepId) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            com.bmfalkye.quests.QuestStorage.QuestProgress progress = storage.getQuestProgress(player, questId);
            
            if (progress != null && progress.isActive()) {
                Quest quest = getQuest(questId);
                if (quest != null) {
                    // Находим индекс шага
                    List<Quest.QuestStep> steps = quest.getSteps();
                    for (int i = 0; i < steps.size(); i++) {
                        if (steps.get(i).getId().equals(stepId)) {
                            if (i > progress.getCurrentStep()) {
                                progress.setCurrentStep(i);
                                storage.setQuestProgress(player, questId, progress);
                                
                                // Проверяем, не завершён ли квест
                                if (i == steps.size() - 1) {
                                    quest.completeQuest(player);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}

