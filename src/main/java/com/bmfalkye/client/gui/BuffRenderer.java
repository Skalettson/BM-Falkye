package com.bmfalkye.client.gui;

import com.bmfalkye.cards.CardBuff;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import java.util.List;

/**
 * Рендерер для отображения баффов/дебаффов на картах
 */
public class BuffRenderer {
    
    /**
     * Рендерит иконки баффов на карте
     */
    public static void renderBuffs(GuiGraphics guiGraphics, Font font, int cardX, int cardY, int cardWidth, int cardHeight, 
                                   List<CardBuff> buffs) {
        if (buffs == null || buffs.isEmpty()) {
            return;
        }
        
        int iconSize = Math.max(8, Math.min(12, cardWidth / 8));
        int startX = cardX + cardWidth - iconSize - 2;
        int startY = cardY + 2;
        int spacing = iconSize + 1;
        
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) { // Максимум 3 иконки
            CardBuff buff = buffs.get(i);
            int iconY = startY + i * spacing;
            
            // ПЕРЕПИСАНО: Рисуем иконку баффа в скевоморфном стиле (металлическая пластина)
            int color = getBuffColor(buff.getType());
            
            // Простая основа иконки
            int iconBgColor = 0xFF3A3A3A;
            guiGraphics.fill(startX, iconY, startX + iconSize, iconY + iconSize, iconBgColor);
            
            // Цветной оверлей для типа баффа
            int overlayColor = (0x80 << 24) | color;
            guiGraphics.fill(startX + 1, iconY + 1, startX + iconSize - 1, iconY + iconSize - 1, overlayColor);
            
            // Рисуем символ баффа
            String symbol = getBuffSymbol(buff.getType());
            guiGraphics.drawString(font, symbol, startX + iconSize/2 - 2, iconY + iconSize/2 - 4, 0xFFFFFF);
        }
    }
    
    /**
     * Получает цвет для типа баффа
     */
    private static int getBuffColor(CardBuff.BuffType type) {
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
    private static String getBuffSymbol(CardBuff.BuffType type) {
        return switch (type) {
            case POWER_INCREASE -> "+";
            case POWER_DECREASE -> "-";
            case IMMUNITY -> "I";
            case FROZEN -> "F";
            case SHIELDED -> "S";
            case DOOMED -> "D";
        };
    }
}

