package com.bmfalkye.client.gui;

import com.bmfalkye.integration.CreativeCoreIntegration;
import com.bmfalkye.integration.LibraryIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Улучшенные утилиты для рисования GUI с градиентами, тенями, свечением и скруглёнными углами
 * ПЕРЕПИСАНО: Теперь использует скевоморфный дизайн
 * Интегрировано с CreativeCore для продвинутых компонентов
 */
public class GuiUtils {
    private static final int CORNER_RADIUS = 8;
    
    /**
     * Рисует прямоугольник со скруглёнными углами
     */
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Основной прямоугольник
        guiGraphics.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + height, color);
        guiGraphics.fill(x, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, color);
        
        // Скруглённые углы (рисуем маленькие прямоугольники)
        // Верхний левый
        drawCorner(guiGraphics, x, y, CORNER_RADIUS, color, 0);
        // Верхний правый
        drawCorner(guiGraphics, x + width - CORNER_RADIUS, y, CORNER_RADIUS, color, 1);
        // Нижний левый
        drawCorner(guiGraphics, x, y + height - CORNER_RADIUS, CORNER_RADIUS, color, 2);
        // Нижний правый
        drawCorner(guiGraphics, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, color, 3);
    }
    
    /**
     * Рисует прямоугольник с градиентом
     */
    public static void drawGradientRect(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                       int colorTop, int colorBottom) {
        for (int i = 0; i < height; i++) {
            float progress = (float) i / height;
            int color = blendColors(colorTop, colorBottom, progress);
            guiGraphics.fill(x, y + i, x + width, y + i + 1, color);
        }
    }
    
    /**
     * Рисует прямоугольник со скруглёнными углами и градиентом
     */
    public static void drawRoundedGradientRect(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                              int colorTop, int colorBottom) {
        // Рисуем градиентный фон
        drawGradientRect(guiGraphics, x + CORNER_RADIUS, y, width - CORNER_RADIUS * 2, height, colorTop, colorBottom);
        drawGradientRect(guiGraphics, x, y + CORNER_RADIUS, width, height - CORNER_RADIUS * 2, colorTop, colorBottom);
        
        // Скруглённые углы с градиентом
        drawGradientCorner(guiGraphics, x, y, CORNER_RADIUS, colorTop, colorBottom, 0);
        drawGradientCorner(guiGraphics, x + width - CORNER_RADIUS, y, CORNER_RADIUS, colorTop, colorBottom, 1);
        drawGradientCorner(guiGraphics, x, y + height - CORNER_RADIUS, CORNER_RADIUS, colorTop, colorBottom, 2);
        drawGradientCorner(guiGraphics, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, colorTop, colorBottom, 3);
    }
    
    /**
     * Рисует скруглённый угол с градиентом
     */
    private static void drawGradientCorner(GuiGraphics guiGraphics, int x, int y, int radius, 
                                         int colorTop, int colorBottom, int corner) {
        for (int i = 0; i < radius; i++) {
            float progress = (float) i / radius;
            int color = blendColors(colorTop, colorBottom, progress);
            int width = i + 1;
            
            switch (corner) {
                case 0: // Верхний левый
                    guiGraphics.fill(x, y + i, x + width, y + i + 1, color);
                    break;
                case 1: // Верхний правый
                    guiGraphics.fill(x + radius - width, y + i, x + radius, y + i + 1, color);
                    break;
                case 2: // Нижний левый
                    guiGraphics.fill(x, y + radius - i - 1, x + width, y + radius - i, color);
                    break;
                case 3: // Нижний правый
                    guiGraphics.fill(x + radius - width, y + radius - i - 1, x + radius, y + radius - i, color);
                    break;
            }
        }
    }
    
    /**
     * Смешивает два цвета
     */
    private static int blendColors(int color1, int color2, float t) {
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
    
    /**
     * Рисует тень вокруг элемента
     */
    public static void drawShadow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                 int shadowSize, int shadowColor) {
        // Верхняя тень
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (shadowColor >> 24) & 0xFF;
            alpha = (int) (alpha * (1.0f - (float) i / shadowSize));
            int color = (shadowColor & 0x00FFFFFF) | (alpha << 24);
            guiGraphics.fill(x + i, y - shadowSize + i, x + width - i, y - shadowSize + i + 1, color);
        }
        
        // Нижняя тень
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (shadowColor >> 24) & 0xFF;
            alpha = (int) (alpha * (1.0f - (float) i / shadowSize));
            int color = (shadowColor & 0x00FFFFFF) | (alpha << 24);
            guiGraphics.fill(x + i, y + height + i, x + width - i, y + height + i + 1, color);
        }
        
        // Левая тень
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (shadowColor >> 24) & 0xFF;
            alpha = (int) (alpha * (1.0f - (float) i / shadowSize));
            int color = (shadowColor & 0x00FFFFFF) | (alpha << 24);
            guiGraphics.fill(x - shadowSize + i, y + i, x - shadowSize + i + 1, y + height - i, color);
        }
        
        // Правая тень
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (shadowColor >> 24) & 0xFF;
            alpha = (int) (alpha * (1.0f - (float) i / shadowSize));
            int color = (shadowColor & 0x00FFFFFF) | (alpha << 24);
            guiGraphics.fill(x + width + i, y + i, x + width + i + 1, y + height - i, color);
        }
    }
    
    /**
     * Рисует свечение вокруг элемента
     */
    public static void drawGlow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                               int glowColor, float intensity) {
        // Внешнее свечение
        for (int i = 0; i < 5; i++) {
            int alpha = (int) (intensity * 255 * (1.0f - (float) i / 5));
            int color = (glowColor & 0x00FFFFFF) | (alpha << 24);
            
            // Верх
            guiGraphics.fill(x - i, y - i, x + width + i, y, color);
            // Низ
            guiGraphics.fill(x - i, y + height, x + width + i, y + height + i, color);
            // Лево
            guiGraphics.fill(x - i, y, x, y + height, color);
            // Право
            guiGraphics.fill(x + width, y, x + width + i, y + height, color);
        }
    }
    
    /**
     * Рисует скруглённый угол
     * @param corner 0=верхний левый, 1=верхний правый, 2=нижний левый, 3=нижний правый
     */
    private static void drawCorner(GuiGraphics guiGraphics, int x, int y, int radius, int color, int corner) {
        // Упрощённая версия - рисуем маленькие прямоугольники для имитации скругления
        for (int i = 0; i < radius; i++) {
            int offset = radius - i;
            int width = i + 1;
            int height = 1;
            
            switch (corner) {
                case 0: // Верхний левый
                    guiGraphics.fill(x, y + i, x + width, y + i + height, color);
                    break;
                case 1: // Верхний правый
                    guiGraphics.fill(x + radius - width, y + i, x + radius, y + i + height, color);
                    break;
                case 2: // Нижний левый
                    guiGraphics.fill(x, y + radius - i - 1, x + width, y + radius - i, color);
                    break;
                case 3: // Нижний правый
                    guiGraphics.fill(x + radius - width, y + radius - i - 1, x + radius, y + radius - i, color);
                    break;
            }
        }
    }
    
    /**
     * Рисует рамку со скруглёнными углами
     */
    public static void drawRoundedBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int thickness) {
        // Верхняя и нижняя линии
        guiGraphics.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + thickness, borderColor);
        guiGraphics.fill(x + CORNER_RADIUS, y + height - thickness, x + width - CORNER_RADIUS, y + height, borderColor);
        
        // Левая и правая линии
        guiGraphics.fill(x, y + CORNER_RADIUS, x + thickness, y + height - CORNER_RADIUS, borderColor);
        guiGraphics.fill(x + width - thickness, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, borderColor);
        
        // Углы (упрощённо)
        drawCornerBorder(guiGraphics, x, y, CORNER_RADIUS, borderColor, thickness, 0);
        drawCornerBorder(guiGraphics, x + width - CORNER_RADIUS, y, CORNER_RADIUS, borderColor, thickness, 1);
        drawCornerBorder(guiGraphics, x, y + height - CORNER_RADIUS, CORNER_RADIUS, borderColor, thickness, 2);
        drawCornerBorder(guiGraphics, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, borderColor, thickness, 3);
    }
    
    private static void drawCornerBorder(GuiGraphics guiGraphics, int x, int y, int radius, int color, int thickness, int corner) {
        for (int i = 0; i < radius; i++) {
            int offset = radius - i;
            int width = Math.min(i + 1, thickness);
            
            switch (corner) {
                case 0:
                    guiGraphics.fill(x, y + i, x + width, y + i + 1, color);
                    break;
                case 1:
                    guiGraphics.fill(x + radius - width, y + i, x + radius, y + i + 1, color);
                    break;
                case 2:
                    guiGraphics.fill(x, y + radius - i - 1, x + width, y + radius - i, color);
                    break;
                case 3:
                    guiGraphics.fill(x + radius - width, y + radius - i - 1, x + radius, y + radius - i, color);
                    break;
            }
        }
    }
    
    /**
     * Создаёт улучшенную кнопку в скевоморфном стиле
     * Использует CreativeCore если доступен, иначе fallback на скевоморфную кнопку
     */
    @SuppressWarnings("null")
    public static Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        // Пытаемся использовать CreativeCore если доступен
        if (LibraryIntegration.isCreativeCoreLoaded()) {
            Button creativeButton = CreativeCoreIntegration.createAdvancedButton(x, y, width, height, text, onPress);
            if (creativeButton != null) {
                return creativeButton;
            }
        }
        
        // Простая кнопка Minecraft
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
    
    /**
     * Рисует улучшенную кнопку с использованием текстуры mod_button.png
     * Текстура адаптивно масштабируется под размер кнопки, текст рендерится поверх
     */
    public static void renderStyledButton(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, Button button, int mouseX, int mouseY, boolean isSelected) {
        boolean hovered = button.isHovered();
        boolean pressed = button.isFocused();
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        // Используем текстуру кнопки, если доступна
        ResourceLocation buttonTexture = GameTextures.MOD_BUTTON;
        if (CardTextures.textureExists(buttonTexture)) {
            // Рендерим текстуру с правильным масштабированием
            // Текстура адаптивно масштабируется под размер кнопки
            int textureWidth = 256; // Размер текстуры mod_button.png в файле
            int textureHeight = 64;
            guiGraphics.blit(buttonTexture, x, y, 0, 0, width, height, textureWidth, textureHeight);
            
            // Добавляем эффект при наведении (полупрозрачное наложение)
            if (hovered) {
                guiGraphics.fill(x, y, x + width, y + height, 0x20FFFFFF);
            }
            
            // Добавляем эффект при нажатии или выборе
            if (pressed || isSelected) {
                guiGraphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
            }
        } else {
            // Fallback: простая кнопка
            int bgColor = pressed || isSelected ? 0xFF4A4A4A : (hovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            guiGraphics.fill(x, y, x + width, y + height, bgColor);
            
            int borderColor = hovered ? 0xFF8B8B8B : 0xFF6B6B6B;
            int borderThickness = 2;
            guiGraphics.fill(x, y, x + width, y + borderThickness, borderColor);
            guiGraphics.fill(x, y + height - borderThickness, x + width, y + height, borderColor);
            guiGraphics.fill(x, y, x + borderThickness, y + height, borderColor);
            guiGraphics.fill(x + width - borderThickness, y, x + width, y + height, borderColor);
        }
        
        // Текст рендерится ПОВЕРХ текстуры, центрированно по вертикали и горизонтали
        int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFF0F0F0 : 0xFFE0E0E0);
        // Правильное центрирование: вычисляем центр кнопки и позицию текста
        int textX = x + width / 2; // Центр по горизонтали
        // Центр по вертикали: убеждаемся, что текст всегда внутри кнопки
        int textY = y + Math.max(1, (height - font.lineHeight) / 2); // Минимум 1 пиксель от верха
        // Проверяем, что текст не выходит за нижнюю границу кнопки
        if (textY + font.lineHeight > y + height) {
            textY = y + height - font.lineHeight - 1; // Смещаем вверх, если не помещается
        }
        // Используем drawCenteredString - первый параметр это центр X, второй - верхняя позиция Y
        guiGraphics.drawCenteredString(font, button.getMessage(), textX, textY, textColor);
    }
    
    /**
     * Перегрузка метода без параметра isSelected (для обратной совместимости)
     */
    public static void renderStyledButton(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, Button button, int mouseX, int mouseY) {
        renderStyledButton(guiGraphics, font, button, mouseX, mouseY, false);
    }
    
    /**
     * Рендерит только фон кнопки (текстуру) без текста
     * Используется для кнопок с кастомным содержимым (например, с иконками)
     */
    public static void renderButtonBackground(GuiGraphics guiGraphics, Button button, boolean isSelected) {
        boolean hovered = button.isHovered();
        boolean pressed = button.isFocused();
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        // Используем текстуру кнопки, если доступна
        ResourceLocation buttonTexture = GameTextures.MOD_BUTTON;
        if (CardTextures.textureExists(buttonTexture)) {
            // Текстура адаптивно масштабируется под размер кнопки
            int textureWidth = 256;
            int textureHeight = 64;
            guiGraphics.blit(buttonTexture, x, y, 0, 0, width, height, textureWidth, textureHeight);
            
            // Добавляем эффект при наведении
            if (hovered) {
                guiGraphics.fill(x, y, x + width, y + height, 0x20FFFFFF);
            }
            
            // Добавляем эффект при нажатии или выборе
            if (pressed || isSelected) {
                guiGraphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
            }
        } else {
            // Fallback: простая кнопка
            int bgColor = pressed || isSelected ? 0xFF4A4A4A : (hovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            guiGraphics.fill(x, y, x + width, y + height, bgColor);
            int borderColor = hovered ? 0xFF8B8B8B : 0xFF6B6B6B;
            int borderThickness = 2;
            guiGraphics.fill(x, y, x + width, y + borderThickness, borderColor);
            guiGraphics.fill(x, y + height - borderThickness, x + width, y + height, borderColor);
            guiGraphics.fill(x, y, x + borderThickness, y + height, borderColor);
            guiGraphics.fill(x + width - borderThickness, y, x + width, y + height, borderColor);
        }
    }
    
    /**
     * Рисует карту с эффектом свечения
     */
    public static void drawCardGlow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                   int glowColor, float intensity) {
        drawGlow(guiGraphics, x, y, width, height, glowColor, intensity);
    }
    
    /**
     * Рисует прямоугольник с тенью и скруглёнными углами
     * ПЕРЕПИСАНО: Теперь использует деревянную панель в скевоморфном стиле
     */
    public static void drawRoundedRectWithShadow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                                int color, int shadowSize) {
        // Простая панель с тенью
        drawWoodenPanel(guiGraphics, x, y, width, height, true);
    }
    
    /**
     * Рисует простую панель (замена скевоморфного стиля)
     */
    public static void drawWoodenPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean dark) {
        // Простой тёмный фон
        int bgColor = dark ? 0xFF2A1F15 : 0xFF3A2F25;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Простая рамка
        int borderColor = 0xFF4A3A2A;
        guiGraphics.fill(x, y, x + width, y + 2, borderColor);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 2, y + height, borderColor);
        guiGraphics.fill(x + width - 2, y, x + width, y + height, borderColor);
    }
    
    /**
     * Рисует простую рамку (замена скевоморфного стиля)
     */
    public static void drawMetalFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                     int thickness, boolean gold) {
        // Простая рамка
        int frameColor = gold ? 0xFFFFD700 : 0xFF8B7355;
        guiGraphics.fill(x, y, x + width, y + thickness, frameColor);
        guiGraphics.fill(x, y + height - thickness, x + width, y + height, frameColor);
        guiGraphics.fill(x, y, x + thickness, y + height, frameColor);
        guiGraphics.fill(x + width - thickness, y, x + width, y + height, frameColor);
    }
    
    /**
     * Рисует простой элемент (замена скевоморфного стиля)
     */
    public static void drawLeatherElement(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Простой фон
        int bgColor = 0xFF3A2F25;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Простая рамка
        int borderColor = 0xFF4A3A2A;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
    
    /**
     * Рисует простой фон (замена скевоморфного стиля)
     */
    public static void drawWoodenTable(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Простой тёмный фон
        int bgColor = 0xFF2A1F15;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
    }
    
    /**
     * Рисует простую рамку (замена скевоморфного стиля)
     */
    public static void drawWoodenBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean dark) {
        // Простая рамка
        int borderColor = dark ? 0xFF3A2F25 : 0xFF4A3A2A;
        guiGraphics.fill(x, y, x + width, y + 2, borderColor);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 2, y + height, borderColor);
        guiGraphics.fill(x + width - 2, y, x + width, y + height, borderColor);
    }
}
