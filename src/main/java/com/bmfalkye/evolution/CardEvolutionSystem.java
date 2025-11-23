package com.bmfalkye.evolution;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.storage.CardEvolutionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * Система управления эволюцией карт.
 * 
 * <p>Обрабатывает начисление опыта картам за победы и управление улучшениями.
 */
public class CardEvolutionSystem {
    private static final int BASE_EXPERIENCE_PER_WIN = 50;
    private static final int BASE_EXPERIENCE_PER_LOSS = 20;
    
    /**
     * Начислить опыт картам игрока за победу в матче
     */
    public static void awardExperienceForWin(ServerPlayer player, FalkyeGameSession session) {
        if (player == null || session == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        CardEvolutionStorage storage = CardEvolutionStorage.get(serverLevel);
        List<String> usedCards = session.getUsedCards(player);
        
        if (usedCards.isEmpty()) {
            return;
        }
        
        // Начисляем опыт каждой использованной карте
        int experiencePerCard = BASE_EXPERIENCE_PER_WIN / usedCards.size();
        if (experiencePerCard < 1) {
            experiencePerCard = 1; // Минимум 1 опыт за карту
        }
        
        boolean anyLevelUp = false;
        for (String cardId : usedCards) {
            boolean leveledUp = storage.addExperience(player, cardId, experiencePerCard);
            if (leveledUp) {
                anyLevelUp = true;
                int newLevel = storage.getCardLevel(player, cardId);
                player.sendSystemMessage(Component.literal("§6Карта " + cardId + " достигла уровня " + newLevel + "!"));
            }
        }
        
        if (anyLevelUp) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.card_leveled_up"));
        }
    }
    
    /**
     * Начислить опыт картам игрока за поражение в матче
     */
    public static void awardExperienceForLoss(ServerPlayer player, FalkyeGameSession session) {
        if (player == null || session == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        CardEvolutionStorage storage = CardEvolutionStorage.get(serverLevel);
        List<String> usedCards = session.getUsedCards(player);
        
        if (usedCards.isEmpty()) {
            return;
        }
        
        // Начисляем меньше опыта за поражение
        int experiencePerCard = BASE_EXPERIENCE_PER_LOSS / usedCards.size();
        if (experiencePerCard < 1) {
            experiencePerCard = 1; // Минимум 1 опыт за карту
        }
        
        for (String cardId : usedCards) {
            storage.addExperience(player, cardId, experiencePerCard);
        }
    }
    
    /**
     * Открыть ветку улучшения для карты (используя Пыль Душ)
     */
    public static boolean unlockBranchWithSoulDust(ServerPlayer player, String cardId, String branchId, int soulDustCost) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        // Проверяем наличие Пыли Душ
        com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
        if (!currency.hasEnoughSoulDust(player, soulDustCost)) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.not_enough_soul_dust", soulDustCost));
            return false;
        }
        
        // Проверяем, есть ли древо эволюции для карты
        if (!CardEvolutionTree.hasEvolutionTree(cardId)) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.no_evolution_tree", cardId));
            return false;
        }
        
        // Проверяем уровень карты
        CardEvolutionStorage storage = CardEvolutionStorage.get(serverLevel);
        int cardLevel = storage.getCardLevel(player, cardId);
        CardEvolutionTree.EvolutionBranch branch = CardEvolutionTree.getEvolutionTree(cardId).getBranch(branchId);
        
        if (branch == null) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.branch_not_found", branchId));
            return false;
        }
        
        // Проверяем, доступна ли ветка на текущем уровне
        List<CardEvolutionTree.EvolutionBranch> availableBranches = CardEvolutionTree.getAvailableBranches(cardId, cardLevel);
        if (!availableBranches.contains(branch)) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.branch_not_available", branchId));
            return false;
        }
        
        // Проверяем, не открыта ли уже ветка
        Set<String> unlockedBranches = storage.getUnlockedBranches(player, cardId);
        if (unlockedBranches.contains(branchId)) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.branch_already_unlocked", branchId));
            return false;
        }
        
        // Списываем Пыль Душ и открываем ветку
        currency.removeSoulDust(player, soulDustCost);
        storage.unlockBranch(player, cardId, branchId);
        
        player.sendSystemMessage(Component.translatable("message.bm_falkye.branch_unlocked", branch.getName()));
        return true;
    }
    
    /**
     * Получить стоимость открытия ветки в Пыли Душ
     */
    public static int getBranchUnlockCost(String cardId, String branchId) {
        // Базовая стоимость: 100 Пыли Душ
        // Можно сделать более сложную формулу в зависимости от уровня карты и ветки
        return 100;
    }
}

