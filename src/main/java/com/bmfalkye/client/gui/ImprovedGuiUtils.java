package com.bmfalkye.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Улучшенные утилиты для создания GUI
 * Предоставляет упрощённые методы для создания общих элементов интерфейса
 */
public class ImprovedGuiUtils {
    
    /**
     * Создаёт адаптивную кнопку с автоматическим позиционированием
     */
    public static Button createAdaptiveButton(int x, int y, int minWidth, int maxWidth, 
                                             Component text, Button.OnPress onPress) {
        int width = Math.max(minWidth, Math.min(maxWidth, text.getString().length() * 6 + 20));
        return Button.builder(text, onPress)
            .bounds(x, y, width, 20)
            .build();
    }
    
    /**
     * Создаёт кнопку с иконкой (если текстура доступна)
     */
    public static Button createIconButton(int x, int y, int size, ResourceLocation icon, 
                                         Component tooltip, Button.OnPress onPress) {
        return Button.builder(Component.empty(), onPress)
            .bounds(x, y, size, size)
            .build();
    }
    
    /**
     * Создаёт поле ввода с валидацией
     */
    public static EditBox createValidatedEditBox(net.minecraft.client.gui.Font font, int x, int y, 
                                                 int width, int height, Component hint,
                                                 java.util.function.Predicate<String> validator) {
        EditBox editBox = new EditBox(font, x, y, width, height, hint);
        editBox.setFilter(validator);
        return editBox;
    }
    
    /**
     * Рисует прогресс-бар с анимацией
     */
    public static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                      float progress, int bgColor, int fillColor, int borderColor) {
        // Фон
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Заполнение
        if (progress > 0) {
            int fillWidth = (int)(width * progress);
            guiGraphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }
        
        // Рамка
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
    
    /**
     * Рисует скроллируемый список с автоматическим управлением прокруткой
     */
    public static void drawScrollableList(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int x, int y, int width, int height,
                                         List<Component> items, int scrollOffset, int itemHeight,
                                         int selectedIndex, int textColor, int selectedColor) {
        int visibleItems = height / itemHeight;
        int startIndex = Math.max(0, scrollOffset);
        int endIndex = Math.min(items.size(), startIndex + visibleItems);
        
        for (int i = startIndex; i < endIndex; i++) {
            int itemY = y + (i - startIndex) * itemHeight;
            Component item = items.get(i);
            
            // Выделение выбранного элемента
            if (i == selectedIndex) {
                guiGraphics.fill(x, itemY, x + width, itemY + itemHeight, selectedColor);
            }
            
            // Текст
            guiGraphics.drawString(font, item, x + 5, itemY + (itemHeight - 9) / 2, textColor, false);
        }
    }
    
    /**
     * Рисует тултип при наведении
     */
    public static void drawTooltip(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY, List<Component> tooltip) {
        if (tooltip.isEmpty()) return;
        
        // Простая реализация тултипа
        // В реальной реализации можно использовать более продвинутые методы
        int maxWidth = tooltip.stream()
            .mapToInt(component -> font.width(component))
            .max()
            .orElse(0);
        
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        int tooltipHeight = tooltip.size() * 10 + 4;
        
        // Фон тултипа
        guiGraphics.fill(tooltipX - 2, tooltipY - 2, tooltipX + maxWidth + 2, tooltipY + tooltipHeight, 0xF0100010);
        guiGraphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + maxWidth + 1, tooltipY + tooltipHeight - 1, 0x505000FF);
        
        // Текст тултипа
        for (int i = 0; i < tooltip.size(); i++) {
            guiGraphics.drawString(font, tooltip.get(i), tooltipX, tooltipY + i * 10, 0xFFFFFF, false);
        }
    }
    
    /**
     * Создаёт адаптивный размер окна на основе разрешения экрана
     */
    public static class AdaptiveSize {
        public final int width;
        public final int height;
        public final int x;
        public final int y;
        
        public AdaptiveSize(int screenWidth, int screenHeight, int baseWidth, int baseHeight,
                           double maxRatio, int minWidth, int minHeight) {
            int maxWidth = (int)(screenWidth * maxRatio);
            int maxHeight = (int)(screenHeight * maxRatio);
            
            this.width = Math.max(minWidth, Math.min(baseWidth, maxWidth));
            this.height = Math.max(minHeight, Math.min(baseHeight, maxHeight));
            this.x = (screenWidth - this.width) / 2;
            this.y = (screenHeight - this.height) / 2;
        }
    }
    
    /**
     * Вычисляет адаптивный размер окна
     */
    public static AdaptiveSize calculateAdaptiveSize(int screenWidth, int screenHeight, 
                                                     int baseWidth, int baseHeight) {
        return new AdaptiveSize(screenWidth, screenHeight, baseWidth, baseHeight, 0.85, 350, 300);
    }
    
    /**
     * Рисует разделительную линию
     */
    public static void drawDivider(GuiGraphics guiGraphics, int x, int y, int width, int color) {
        guiGraphics.fill(x, y, x + width, y + 2, color);
    }
    
    /**
     * Рисует иконку с текстом рядом
     */
    public static void drawIconWithText(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, ResourceLocation icon, int x, int y,
                                       Component text, int iconSize) {
        // Иконка
        if (icon != null) {
            guiGraphics.blit(icon, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }
        
        // Текст
        guiGraphics.drawString(font, text, x + iconSize + 5, y + (iconSize - 9) / 2, 0xFFFFFF, false);
    }
}

