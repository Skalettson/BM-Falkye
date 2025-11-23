package com.bmfalkye.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Система уведомлений о случайных событиях
 */
public class RandomEventNotification {
    private static final List<Notification> activeNotifications = new ArrayList<>();
    private static final long NOTIFICATION_DURATION = 3000; // 3 секунды
    
    /**
     * Добавляет уведомление
     */
    public static void addNotification(String message) {
        activeNotifications.add(new Notification(message, System.currentTimeMillis()));
    }
    
    /**
     * Рендерит все активные уведомления
     */
    public static void render(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight) {
        long currentTime = System.currentTimeMillis();
        
        // Удаляем истёкшие уведомления
        activeNotifications.removeIf(n -> currentTime - n.timestamp > NOTIFICATION_DURATION);
        
        // Рендерим уведомления сверху вниз
        int startY = 20;
        int spacing = 25;
        
        for (int i = 0; i < activeNotifications.size(); i++) {
            Notification notification = activeNotifications.get(i);
            int y = startY + i * spacing;
            
            // Вычисляем прозрачность (затухание в конце)
            long elapsed = currentTime - notification.timestamp;
            float alpha = 1.0f;
            if (elapsed > NOTIFICATION_DURATION - 500) {
                alpha = 1.0f - ((elapsed - (NOTIFICATION_DURATION - 500)) / 500.0f);
            }
            
            // ПЕРЕПИСАНО: Рисуем фон уведомления в скевоморфном стиле
            int textWidth = font.width(notification.message);
            int padding = 10;
            int x = (screenWidth - textWidth) / 2 - padding;
            int height = 20;
            int width = textWidth + padding * 2;
            
            // Простая тень уведомления
            guiGraphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x80000000);
            
            // Кожаная основа с прозрачностью
            int alphaInt = (int)(alpha * 255);
            int leatherAlpha = (alphaInt << 24) | 0x4A3728;
            guiGraphics.fill(x, y, x + width, y + height, leatherAlpha);
            
            // Металлическая рамка
            com.bmfalkye.client.gui.GuiUtils.drawMetalFrame(
                guiGraphics, x, y, width, height, 1, false);
            
            // Текст
            int textColor = (int)(0xFFFFFF * alpha) | 0xFF000000;
            guiGraphics.drawString(font, Component.literal(notification.message), 
                x + padding, y + 6, textColor);
        }
    }
    
    private static class Notification {
        final String message;
        final long timestamp;
        
        Notification(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}

