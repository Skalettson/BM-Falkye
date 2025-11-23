package com.bmfalkye.client.gui;

import com.bmfalkye.game.LocationEffect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Рендерер для отображения эффектов окружения
 */
public class LocationEffectRenderer {
    
    /**
     * Рендерит информацию об эффекте локации
     */
    public static void renderLocationEffect(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                           LocationEffect.LocationType location) {
        if (location == LocationEffect.LocationType.NONE || location == LocationEffect.LocationType.PLAINS) {
            return;
        }
        
        // ПЕРЕПИСАНО: Рисуем фон в скевоморфном стиле (кожаная панель)
        com.bmfalkye.client.gui.GuiUtils.drawLeatherElement(guiGraphics, x, y, width, height);
        // Металлическая рамка
        com.bmfalkye.client.gui.GuiUtils.drawMetalFrame(guiGraphics, x, y, width, height, 2, false);
        
        // Рисуем название локации
        String locationName = getLocationName(location);
        guiGraphics.drawString(font, Component.literal("§bЛокация: " + locationName), 
            x + 5, y + 5, 0xFFFFFF);
        
        // Рисуем описание эффекта
        String effectDesc = getLocationEffectDescription(location);
        guiGraphics.drawString(font, Component.literal("§7" + effectDesc), 
            x + 5, y + 18, 0xCCCCCC);
    }
    
    private static String getLocationName(LocationEffect.LocationType location) {
        return switch (location) {
            case DESERT -> "Пустыня";
            case SNOW -> "Снег";
            case FOREST -> "Лес";
            case OCEAN -> "Океан";
            case MOUNTAIN -> "Горы";
            case NETHER -> "Незер";
            case END -> "Энд";
            default -> "Неизвестно";
        };
    }
    
    private static String getLocationEffectDescription(LocationEffect.LocationType location) {
        return switch (location) {
            case DESERT -> "Огненные карты +2";
            case SNOW -> "Ледяные карты +2";
            case FOREST -> "Природные карты +1";
            case OCEAN -> "Водные карты +2";
            case MOUNTAIN -> "Карты осады +1";
            case NETHER -> "Демонические карты +3";
            case END -> "Мистические карты +3";
            default -> "";
        };
    }
}

