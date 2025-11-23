package com.bmfalkye.client.gui.backup.logic;

import com.bmfalkye.cards.CardBuff;
import java.util.List;

/**
 * БЭКАП ЛОГИКИ BuffRenderer
 * 
 * Этот класс содержит всю логику работы с баффами БЕЗ визуального оформления.
 */
public class BuffRendererLogic {
    
    /**
     * Вычисляет позиции иконок баффов на карте
     */
    public static BuffLayout calculateBuffLayout(List<CardBuff> buffs, int cardX, int cardY, 
                                                int cardWidth, int cardHeight) {
        if (buffs == null || buffs.isEmpty()) {
            return new BuffLayout(0, 0, 0, 0, 0);
        }
        
        int iconSize = Math.max(8, Math.min(12, cardWidth / 8));
        int startX = cardX + cardWidth - iconSize - 2;
        int startY = cardY + 2;
        int spacing = iconSize + 1;
        int maxIcons = Math.min(buffs.size(), 3);
        
        return new BuffLayout(startX, startY, iconSize, spacing, maxIcons);
    }
    
    /**
     * Получает цвет для типа баффа
     */
    public static int getBuffColor(CardBuff.BuffType type) {
        return switch (type) {
            case POWER_INCREASE -> 0x00FF00; // Зелёный
            case POWER_DECREASE -> 0xFF0000; // Красный
            case IMMUNITY -> 0xFFFF00; // Жёлтый
            case FROZEN -> 0x00FFFF; // Голубой
            case SHIELDED -> 0x0000FF; // Синий
            case DOOMED -> 0xFF00FF; // Пурпурный
        };
    }
    
    /**
     * Получает символ для типа баффа
     */
    public static String getBuffSymbol(CardBuff.BuffType type) {
        return switch (type) {
            case POWER_INCREASE -> "+";
            case POWER_DECREASE -> "-";
            case IMMUNITY -> "I";
            case FROZEN -> "F";
            case SHIELDED -> "S";
            case DOOMED -> "D";
        };
    }
    
    /**
     * Структура для хранения информации о расположении баффов
     */
    public static class BuffLayout {
        public final int startX;
        public final int startY;
        public final int iconSize;
        public final int spacing;
        public final int maxIcons;
        
        public BuffLayout(int startX, int startY, int iconSize, int spacing, int maxIcons) {
            this.startX = startX;
            this.startY = startY;
            this.iconSize = iconSize;
            this.spacing = spacing;
            this.maxIcons = maxIcons;
        }
    }
}

