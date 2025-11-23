package com.bmfalkye.game;

import com.bmfalkye.cards.LeaderCard;

/**
 * Конфигурация матча перед началом игры
 */
public class MatchConfig {
    public enum Difficulty {
        EASY(0.6f),
        NORMAL(1.0f),
        HARD(1.5f),
        EXPERT(2.0f);
        
        private final float aiMultiplier;
        
        Difficulty(float multiplier) {
            this.aiMultiplier = multiplier;
        }
        
        public float getAIMultiplier() {
            return aiMultiplier;
        }
        
        public String getDisplayName() {
            return net.minecraft.network.chat.Component.translatable("difficulty.bm_falkye." + this.name().toLowerCase()).getString();
        }
    }
    
    private Difficulty difficulty = Difficulty.NORMAL;
    private int betAmount = 0; // Ставка (0 = без ставки)
    private LeaderCard selectedLeader = null; // null = использовать сохранённого лидера
    private String selectedDeckName = null; // null = использовать сохранённую колоду
    private boolean allowLeader = true; // Разрешить использование лидеров
    private boolean allowWeather = true; // Разрешить погодные эффекты
    private int maxRounds = 3; // Максимальное количество раундов
    private int turnTimeLimit = 90; // Лимит времени на ход в секундах
    private boolean isFriendlyMatch = false; // Дружеская игра (без ставки и наград)
    private boolean draftArena = false; // Матч арены драфта
    private com.bmfalkye.settings.GameModeSettings.GameMode gameMode = com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D; // Режим отображения игры
    
    public MatchConfig() {
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public int getBetAmount() {
        return betAmount;
    }
    
    public void setBetAmount(int betAmount) {
        this.betAmount = Math.max(0, betAmount); // Не может быть отрицательным
    }
    
    public LeaderCard getSelectedLeader() {
        return selectedLeader;
    }
    
    public void setSelectedLeader(LeaderCard leader) {
        this.selectedLeader = leader;
    }
    
    public String getSelectedDeckName() {
        return selectedDeckName;
    }
    
    public void setSelectedDeckName(String deckName) {
        this.selectedDeckName = deckName;
    }
    
    public boolean isAllowLeader() {
        return allowLeader;
    }
    
    public void setAllowLeader(boolean allowLeader) {
        this.allowLeader = allowLeader;
    }
    
    public boolean isAllowWeather() {
        return allowWeather;
    }
    
    public void setAllowWeather(boolean allowWeather) {
        this.allowWeather = allowWeather;
    }
    
    public int getMaxRounds() {
        return maxRounds;
    }
    
    public void setMaxRounds(int maxRounds) {
        this.maxRounds = Math.max(1, Math.min(5, maxRounds)); // От 1 до 5 раундов
    }
    
    public int getTurnTimeLimit() {
        return turnTimeLimit;
    }
    
    public void setTurnTimeLimit(int turnTimeLimit) {
        this.turnTimeLimit = Math.max(30, Math.min(300, turnTimeLimit)); // От 30 до 300 секунд
    }
    
    public boolean isFriendlyMatch() {
        return isFriendlyMatch;
    }
    
    public void setFriendlyMatch(boolean isFriendlyMatch) {
        this.isFriendlyMatch = isFriendlyMatch;
    }
    
    public boolean isDraftArena() {
        return draftArena;
    }
    
    public void setDraftArena(boolean draftArena) {
        this.draftArena = draftArena;
    }
    
    public com.bmfalkye.settings.GameModeSettings.GameMode getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(com.bmfalkye.settings.GameModeSettings.GameMode gameMode) {
        this.gameMode = gameMode != null ? gameMode : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
    }
}

