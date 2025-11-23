package com.bmfalkye.cards;

/**
 * Редкость карты
 */
public enum CardRarity {
    COMMON(0.70f, "Обычная", 0xFF808080),      // 70% - серый
    RARE(0.20f, "Редкая", 0xFF0080FF),         // 20% - синий
    EPIC(0.08f, "Эпическая", 0xFF8000FF),     // 8% - фиолетовый
    LEGENDARY(0.02f, "Легендарная", 0xFFFFD700); // 2% - золотой

    private final float dropChance;
    private final String displayName;
    private final int color;

    CardRarity(float dropChance, String displayName, int color) {
        this.dropChance = dropChance;
        this.displayName = displayName;
        this.color = color;
    }

    public float getDropChance() {
        return dropChance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}

