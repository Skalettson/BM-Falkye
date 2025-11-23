package com.bmfalkye.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Рендерер лога действий игры Falkye.
 * 
 * <p>Отвечает за отображение лога действий игроков, включая:
 * <ul>
 *   <li>Разыгрывание карт</li>
 *   <li>Применение эффектов</li>
 *   <li>Изменения состояния игры</li>
 * </ul>
 * 
 * <p>Лог поддерживает автоматическую прокрутку и перенос строк для длинных сообщений.
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class ActionLogRenderer {
    
    /**
     * Рендерит лог действий.
     * 
     * @param guiGraphics графический контекст для рендеринга
     * @param font шрифт для отображения текста
     * @param actionLog список записей лога
     * @param logX координата X левого верхнего угла лога
     * @param logY координата Y левого верхнего угла лога
     * @param logWidth ширина области лога
     * @param logHeight высота области лога
     * @param logScrollOffset смещение для прокрутки (количество записей, скрытых сверху)
     */
    private static long animationTime = 0;
    
    public static void render(GuiGraphics guiGraphics, Font font, 
                             List<String> actionLog,
                             int logX, int logY, int logWidth, int logHeight,
                             int logScrollOffset) {
        updateAnimationTime();
        
        // Используем текстуру лога действий, если доступна
        if (CardTextures.textureExists(com.bmfalkye.client.gui.GameTextures.GAME_ACTIVITY_LOG)) {
            // Рендерим текстуру с правильным масштабированием
            guiGraphics.blit(com.bmfalkye.client.gui.GameTextures.GAME_ACTIVITY_LOG, logX, logY, 0, 0, logWidth, logHeight, logWidth, logHeight);
        } else {
            // Fallback: простой фон
            GuiUtils.drawLeatherElement(guiGraphics, logX, logY, logWidth, logHeight);
            float pulse = (float) (0.5f + 0.2f * Math.sin(animationTime / 2000.0f));
            boolean goldFrame = pulse > 0.6f;
            GuiUtils.drawMetalFrame(guiGraphics, logX, logY, logWidth, logHeight, 2, goldFrame);
            int glowColor = (0xFF8B7355 & 0x00FFFFFF) | ((int)(pulse * 30) << 24);
            guiGraphics.fill(logX + 2, logY + 2, logX + logWidth - 2, logY + 4, glowColor);
            guiGraphics.fill(logX + 2, logY + logHeight - 4, logX + logWidth - 2, logY + logHeight - 2, glowColor);
        }
        
        // Заголовок рендерится ПОВЕРХ текстуры (адаптивно позиционируется)
        // Позиционируем заголовок так, чтобы он был виден на текстуре
        int headerX = logX + Math.max(15, (int)(logWidth * 0.08)); // 8% от ширины лога
        int headerY = logY + Math.max(12, (int)(logHeight * 0.05)); // 5% от высоты лога
        // Рисуем текст с тенью для лучшей читаемости на текстуре
        guiGraphics.drawString(font, 
            Component.literal("§6§lЛог действий:"), 
            headerX + 1, headerY + 1, 0x000000, false); // Тень
        guiGraphics.drawString(font, 
            Component.literal("§6§lЛог действий:"), 
            headerX, headerY, 0xFFFFFF, false); // Основной текст
        
        // Разделитель с градиентом (позиционируется ниже заголовка)
        int dividerY = headerY + font.lineHeight + Math.max(5, (int)(logHeight * 0.02)); // 2% от высоты лога
        for (int i = 0; i < 2; i++) {
            int alpha = 100 - (i * 30);
            int dividerColor = (0xFF4A3A2A & 0x00FFFFFF) | (alpha << 24);
            guiGraphics.fill(logX + 10, dividerY + i, logX + logWidth - 10, dividerY + 1 + i, dividerColor);
        }
        
        // Рендерим записи лога с переносом строк (без сокращений) и автоматической прокруткой
        int y = dividerY + 5; // Отступ после разделителя
        int maxVisibleEntries = Math.min(6, (logHeight - 30) / 12); // Учитываем перенос строк
        
        int startIndex = Math.max(0, logScrollOffset);
        int endIndex = Math.min(startIndex + maxVisibleEntries, actionLog.size());
        
        // Включаем обрезку для лога
        guiGraphics.enableScissor(logX, logY, logX + logWidth, logY + logHeight);
        
        for (int i = startIndex; i < endIndex; i++) {
            if (y > logY + logHeight - 15) break;
            String entry = actionLog.get(i);
            
            // Разбиваем на строки с учетом ширины лога (БЕЗ сокращений)
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(
                Component.literal("§7• " + entry), logWidth - 20);
            
            // Рендерим все строки
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                if (y > logY + logHeight - 15) break;
                guiGraphics.drawString(font, line, logX + 5, y, 0xCCCCCC, false);
                y += font.lineHeight + 1; // Отступ между строками
            }
            y += 2; // Отступ между записями
        }
        
        // Отключаем обрезку
        guiGraphics.disableScissor();
    }
    
    /**
     * Обновляет время анимации
     */
    private static void updateAnimationTime() {
        animationTime = System.currentTimeMillis();
    }
    
    /**
     * Смешивает два цвета
     */
    private static int blendColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        int a = (int) (a1 + (a2 - a1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

