package com.bmfalkye.quests;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Легендарный квест - особый тип квеста с уникальными условиями и наградами
 */
public class LegendaryQuest extends Quest {
    private final Set<String> requiredItems; // Предметы-ключи, необходимые для начала
    private final BlockPos questLocation; // Локация, где происходит квест
    private final QuestCondition startCondition; // Условие начала квеста
    private final String specialRules; // Специальные правила для дуэли (если есть)
    
    public LegendaryQuest(String id, String name, String description, 
                         List<QuestStep> steps, QuestReward reward,
                         Set<String> requiredItems, BlockPos questLocation,
                         QuestCondition startCondition, String specialRules) {
        super(id, name, description, QuestType.LEGENDARY, QuestRarity.LEGENDARY, steps, reward);
        this.requiredItems = requiredItems;
        this.questLocation = questLocation;
        this.startCondition = startCondition;
        this.specialRules = specialRules;
    }
    
    @Override
    public boolean canStart(ServerPlayer player) {
        // Проверяем, не завершён ли уже квест
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            if (storage.isQuestCompleted(player, id)) {
                return false;
            }
        }
        
        // Проверяем наличие предметов-ключей
        if (requiredItems != null && !requiredItems.isEmpty()) {
            for (String itemId : requiredItems) {
                if (!hasQuestItem(player, itemId)) {
                    return false;
                }
            }
        }
        
        // Проверяем условие начала
        if (startCondition != null) {
            return startCondition.check(player);
        }
        
        return true;
    }
    
    @Override
    public void startQuest(ServerPlayer player) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            storage.startQuest(player, id);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§lЛегендарный квест начат: §f" + name));
        }
    }
    
    @Override
    public void checkProgress(ServerPlayer player) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            com.bmfalkye.quests.QuestStorage.QuestProgress progress = storage.getQuestProgress(player, id);
            
            if (progress == null || !progress.isActive()) {
                return;
            }
            
            // Проверяем каждый шаг
            for (int i = 0; i < steps.size(); i++) {
                QuestStep step = steps.get(i);
                if (i <= progress.getCurrentStep()) {
                    // Шаг уже выполнен
                    continue;
                }
                
                if (checkStep(player, step)) {
                    // Шаг выполнен, переходим к следующему
                    progress.setCurrentStep(i);
                    storage.setQuestProgress(player, id, progress);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aШаг квеста выполнен: §f" + step.getDescription()));
                    
                    // Если это последний шаг, завершаем квест
                    if (i == steps.size() - 1) {
                        completeQuest(player);
                    }
                }
            }
        }
    }
    
    @Override
    public void completeQuest(ServerPlayer player) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.quests.QuestStorage storage = 
                com.bmfalkye.quests.QuestStorage.get(serverLevel);
            storage.completeQuest(player, id);
            
            // Выдаём награду
            reward.giveReward(player);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§lЛегендарный квест завершён: §f" + name));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aНаграда получена!"));
        }
    }
    
    /**
     * Проверяет выполнение шага квеста
     */
    private boolean checkStep(ServerPlayer player, QuestStep step) {
        switch (step.getType()) {
            case FIND_ITEM:
                String itemId = (String) step.getTarget();
                return hasQuestItem(player, itemId);
                
            case REACH_LOCATION:
                if (step.getTarget() instanceof BlockPos targetPos) {
                    BlockPos playerPos = player.blockPosition();
                    return playerPos.distSqr(targetPos) <= 25; // В радиусе 5 блоков
                }
                return false;
                
            case WIN_DUEL:
                // Проверяется через GameEndHandler при победе
                return false; // Вручную помечается как выполненный
                
            case COLLECT_CARDS:
                @SuppressWarnings("unchecked")
                List<String> requiredCards = (List<String>) step.getTarget();
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                    for (String cardId : requiredCards) {
                        if (!collection.hasCard(player, cardId)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
                
            case WAIT_TIME:
                // Проверяется через специальную логику
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Проверяет, есть ли у игрока предмет-ключ
     */
    private boolean hasQuestItem(ServerPlayer player, String itemId) {
        // Проверяем инвентарь игрока
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.hasTag()) {
                String stackItemId = stack.getTag().getString("QuestItemId");
                if (itemId.equals(stackItemId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Set<String> getRequiredItems() { return requiredItems; }
    public BlockPos getQuestLocation() { return questLocation; }
    public QuestCondition getStartCondition() { return startCondition; }
    public String getSpecialRules() { return specialRules; }
    
    /**
     * Условие начала квеста
     */
    @FunctionalInterface
    public interface QuestCondition {
        boolean check(ServerPlayer player);
    }
}

