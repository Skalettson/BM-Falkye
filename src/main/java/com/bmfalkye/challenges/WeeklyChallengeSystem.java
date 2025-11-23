package com.bmfalkye.challenges;

import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система еженедельных испытаний
 */
public class WeeklyChallengeSystem {
    private static final List<WeeklyChallenge> activeChallenges = new ArrayList<>();
    private static final Map<UUID, Set<String>> completedChallenges = new ConcurrentHashMap<>();
    
    /**
     * Создаёт новые еженедельные испытания
     */
    public static void generateWeeklyChallenges() {
        activeChallenges.clear();
        
        // Создаём 5 случайных испытаний
        long weekDuration = 7 * 24 * 60 * 60 * 1000L; // 7 дней
        
        activeChallenges.add(new WeeklyChallenge(
            "weekly_win_10", "Победитель недели", 
            "Выиграйте 10 игр", WeeklyChallenge.ChallengeType.WIN_GAMES, 10, weekDuration));
        
        activeChallenges.add(new WeeklyChallenge(
            "weekly_play_50", "Активный игрок", 
            "Разыграйте 50 карт", WeeklyChallenge.ChallengeType.PLAY_CARDS, 50, weekDuration));
        
        activeChallenges.add(new WeeklyChallenge(
            "weekly_collect_5", "Коллекционер недели", 
            "Соберите 5 новых карт", WeeklyChallenge.ChallengeType.COLLECT_CARDS, 5, weekDuration));
        
        activeChallenges.add(new WeeklyChallenge(
            "weekly_leader_20", "Мастер лидеров", 
            "Используйте способности лидеров 20 раз", WeeklyChallenge.ChallengeType.USE_LEADER, 20, weekDuration));
        
        activeChallenges.add(new WeeklyChallenge(
            "weekly_rank_up", "Восхождение", 
            "Повысьте свой ранг", WeeklyChallenge.ChallengeType.REACH_RANK, 1, weekDuration));
    }
    
    /**
     * Получает активные испытания
     */
    public static List<WeeklyChallenge> getActiveChallenges() {
        // Удаляем истёкшие
        activeChallenges.removeIf(WeeklyChallenge::isExpired);
        return new ArrayList<>(activeChallenges);
    }
    
    /**
     * Обновляет прогресс испытания
     */
    public static void updateChallengeProgress(ServerPlayer player, WeeklyChallenge.ChallengeType type, int amount) {
        for (WeeklyChallenge challenge : activeChallenges) {
            if (challenge.getType() == type && !challenge.isCompleted(player)) {
                challenge.updateProgress(player, amount);
                
                // Проверяем, выполнено ли
                if (challenge.isCompleted(player)) {
                    giveChallengeReward(player, challenge);
                    completedChallenges.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet())
                        .add(challenge.getId());
                }
            }
        }
    }
    
    /**
     * Выдаёт награду за выполнение испытания
     */
    private static void giveChallengeReward(ServerPlayer player, WeeklyChallenge challenge) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Награды за еженедельные испытания
            int coins = 500;
            int xp = 1000;
            
            com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
            currency.addCoins(player, coins);
            
            com.bmfalkye.storage.PlayerProgressStorage storage = PlayerProgressStorage.get(serverLevel);
            com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(player);
            progress.addExperience(xp);
            storage.setPlayerProgress(player, progress);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aЕженедельное испытание выполнено: " + challenge.getName()));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eНаграда: " + coins + " монет, " + xp + " опыта"));
        }
    }
}

