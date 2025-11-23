package com.bmfalkye.cards;

import java.util.UUID;

/**
 * Бафф/дебафф для карты
 */
public class CardBuff {
    private final String id;
    private final BuffType type;
    private final int powerModifier;
    private final int duration; // В раундах, -1 = постоянный
    private final UUID sourcePlayer; // Игрок, который применил бафф
    private int remainingRounds;
    
    public enum BuffType {
        POWER_INCREASE,    // Увеличение силы
        POWER_DECREASE,    // Уменьшение силы
        IMMUNITY,          // Иммунитет к эффектам
        FROZEN,            // Заморожен (не может быть использован)
        SHIELDED,          // Защищён (следующий урон игнорируется)
        DOOMED             // Обречён (будет уничтожен в конце раунда)
    }
    
    public CardBuff(String id, BuffType type, int powerModifier, int duration, UUID sourcePlayer) {
        this.id = id;
        this.type = type;
        this.powerModifier = powerModifier;
        this.duration = duration;
        this.sourcePlayer = sourcePlayer;
        this.remainingRounds = duration;
    }
    
    /**
     * Уменьшает оставшееся время действия баффа
     */
    public void decreaseDuration() {
        if (duration > 0) {
            remainingRounds--;
        }
    }
    
    /**
     * Проверяет, истёк ли бафф
     */
    public boolean isExpired() {
        return duration > 0 && remainingRounds <= 0;
    }
    
    /**
     * Применяет модификатор силы
     */
    public int applyPowerModifier(int basePower) {
        if (type == BuffType.POWER_INCREASE || type == BuffType.POWER_DECREASE) {
            return basePower + powerModifier;
        }
        return basePower;
    }
    
    // Геттеры
    public String getId() { return id; }
    public BuffType getType() { return type; }
    public int getPowerModifier() { return powerModifier; }
    public int getDuration() { return duration; }
    public UUID getSourcePlayer() { return sourcePlayer; }
    public int getRemainingRounds() { return remainingRounds; }
}

