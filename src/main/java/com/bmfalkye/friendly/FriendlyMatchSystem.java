package com.bmfalkye.friendly;

import com.bmfalkye.game.GameManager;
import com.bmfalkye.game.MatchConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система дружеских матчей с настройками
 */
public class FriendlyMatchSystem {
    private static final Map<UUID, FriendlyMatchRequest> pendingRequests = new HashMap<>();
    
    /**
     * Создаёт запрос на дружеский матч
     */
    public static boolean createFriendlyMatchRequest(ServerPlayer challenger, ServerPlayer target, 
                                                    FriendlyMatchSettings settings) {
        if (challenger.equals(target)) {
            challenger.sendSystemMessage(Component.literal("§cНельзя вызвать самого себя!"));
            return false;
        }
        
        // Проверяем, не занят ли игрок
        if (GameManager.hasActiveGame(challenger) || GameManager.hasActiveGame(target)) {
            challenger.sendSystemMessage(Component.literal("§cОдин из игроков уже в игре!"));
            return false;
        }
        
        FriendlyMatchRequest request = new FriendlyMatchRequest(challenger, target, settings);
        pendingRequests.put(target.getUUID(), request);
        
        // Уведомляем игроков
        challenger.sendSystemMessage(Component.literal(
            "§aЗапрос на дружеский матч отправлен игроку " + target.getName().getString()));
        target.sendSystemMessage(Component.literal(
            "§aИгрок " + challenger.getName().getString() + " приглашает вас на дружеский матч!"));
        target.sendSystemMessage(Component.literal("§7Используйте /falkye accept для принятия"));
        
        return true;
    }
    
    /**
     * Принимает запрос на дружеский матч
     */
    public static boolean acceptFriendlyMatch(ServerPlayer player) {
        FriendlyMatchRequest request = pendingRequests.remove(player.getUUID());
        if (request == null) {
            player.sendSystemMessage(Component.literal("§cНет активных запросов на дружеский матч!"));
            return false;
        }
        
        ServerPlayer challenger = request.getChallenger();
        if (challenger == null || !challenger.isAlive()) {
            player.sendSystemMessage(Component.literal("§cИгрок, отправивший запрос, недоступен!"));
            return false;
        }
        
        // Создаём матч с настройками
        FriendlyMatchSettings settings = request.getSettings();
        MatchConfig config = createMatchConfig(settings);
        
        // Создаём игровую сессию
        GameManager.startPlayerMatch(challenger, player, config);
        challenger.sendSystemMessage(Component.literal("§aИгрок принял ваш запрос! Начинаем матч!"));
        player.sendSystemMessage(Component.literal("§aДружеский матч начался!"));
        return true;
    }
    
    /**
     * Отклоняет запрос на дружеский матч
     */
    public static boolean denyFriendlyMatch(ServerPlayer player) {
        FriendlyMatchRequest request = pendingRequests.remove(player.getUUID());
        if (request == null) {
            return false;
        }
        
        ServerPlayer challenger = request.getChallenger();
        if (challenger != null && challenger.isAlive()) {
            challenger.sendSystemMessage(Component.literal(
                "§cИгрок " + player.getName().getString() + " отклонил ваш запрос на дружеский матч"));
        }
        
        player.sendSystemMessage(Component.literal("§7Вы отклонили запрос на дружеский матч"));
        return true;
    }
    
    /**
     * Создаёт конфигурацию матча из настроек дружеского матча
     */
    private static MatchConfig createMatchConfig(FriendlyMatchSettings settings) {
        MatchConfig config = new MatchConfig();
        config.setBetAmount(settings.getBetAmount());
        config.setDifficulty(settings.getDifficulty());
        config.setAllowLeader(settings.isAllowLeader());
        config.setAllowWeather(settings.isAllowWeather());
        config.setMaxRounds(settings.getMaxRounds());
        config.setTurnTimeLimit(settings.getTurnTimeLimit());
        return config;
    }
    
    /**
     * Настройки дружеского матча
     */
    public static class FriendlyMatchSettings {
        private int betAmount = 0;
        private MatchConfig.Difficulty difficulty = MatchConfig.Difficulty.NORMAL;
        private boolean allowLeader = true;
        private boolean allowWeather = true;
        private int maxRounds = 3;
        private boolean allowSpectators = false;
        private int turnTimeLimit = 90; // секунды
        
        public FriendlyMatchSettings() {}
        
        // Геттеры и сеттеры
        public int getBetAmount() { return betAmount; }
        public void setBetAmount(int betAmount) { this.betAmount = betAmount; }
        
        public MatchConfig.Difficulty getDifficulty() { return difficulty; }
        public void setDifficulty(MatchConfig.Difficulty difficulty) { this.difficulty = difficulty; }
        
        public boolean isAllowLeader() { return allowLeader; }
        public void setAllowLeader(boolean allowLeader) { this.allowLeader = allowLeader; }
        
        public boolean isAllowWeather() { return allowWeather; }
        public void setAllowWeather(boolean allowWeather) { this.allowWeather = allowWeather; }
        
        public int getMaxRounds() { return maxRounds; }
        public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
        
        public boolean isAllowSpectators() { return allowSpectators; }
        public void setAllowSpectators(boolean allowSpectators) { this.allowSpectators = allowSpectators; }
        
        public int getTurnTimeLimit() { return turnTimeLimit; }
        public void setTurnTimeLimit(int turnTimeLimit) { this.turnTimeLimit = turnTimeLimit; }
    }
    
    /**
     * Запрос на дружеский матч
     */
    private static class FriendlyMatchRequest {
        private final ServerPlayer challenger;
        private final ServerPlayer target;
        private final FriendlyMatchSettings settings;
        private final long timestamp;
        
        public FriendlyMatchRequest(ServerPlayer challenger, ServerPlayer target, FriendlyMatchSettings settings) {
            this.challenger = challenger;
            this.target = target;
            this.settings = settings;
            this.timestamp = System.currentTimeMillis();
        }
        
        public ServerPlayer getChallenger() { return challenger; }
        public ServerPlayer getTarget() { return target; }
        public FriendlyMatchSettings getSettings() { return settings; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000; // 1 минута
        }
    }
}

