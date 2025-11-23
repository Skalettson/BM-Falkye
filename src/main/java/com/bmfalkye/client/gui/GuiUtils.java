package com.bmfalkye.client.gui;

import com.bmfalkye.integration.CreativeCoreIntegration;
import com.bmfalkye.integration.LibraryIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Современная система GUI утилит для Minecraft Forge 1.20.1
 * Полностью переписана с использованием актуального API
 * Обеспечивает корректное отображение текстур и полную адаптивность
 * Дата: 23 ноября 2025
 */
public class GuiUtils {
    private static final int CORNER_RADIUS = 8;
    private static final float TEXTURE_SCALE = 1.0f / 256.0f; // Масштаб текстур
    
    /**
     * Рисует прямоугольник со скруглёнными углами (современный подход)
     */
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Центральный прямоугольник
        guiGraphics.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + height, color);
        guiGraphics.fill(x, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, color);
        
        // Скруглённые углы (антиалиасинг)
        drawRoundedCorner(guiGraphics, x, y, CORNER_RADIUS, color, 0); // Верхний левый
        drawRoundedCorner(guiGraphics, x + width - CORNER_RADIUS, y, CORNER_RADIUS, color, 1); // Верхний правый
        drawRoundedCorner(guiGraphics, x, y + height - CORNER_RADIUS, CORNER_RADIUS, color, 2); // Нижний левый
        drawRoundedCorner(guiGraphics, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, color, 3); // Нижний правый
    }
    
    /**
     * Рисует скруглённый угол с улучшенным качеством
     */
    private static void drawRoundedCorner(GuiGraphics guiGraphics, int x, int y, int radius, int color, int corner) {
        // Улучшенная реализация скруглённых углов
        for (int i = 0; i < radius; i++) {
            int width = (int) Math.sqrt(radius * radius - (radius - i - 1) * (radius - i - 1));
            
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
     * Рисует прямоугольник с градиентом (улучшенная версия)
     */
    public static void drawGradientRect(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                       int colorTop, int colorBottom) {
        // Используем оптимизированный метод Minecraft для градиентов
        guiGraphics.fillGradient(x, y, x + width, y + height, colorTop, colorBottom);
    }
    
    /**
     * Рисует прямоугольник со скруглёнными углами и градиентом
     */
    public static void drawRoundedGradientRect(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                              int colorTop, int colorBottom) {
        // Центральный градиент
        guiGraphics.fillGradient(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + height, colorTop, colorBottom);
        guiGraphics.fillGradient(x, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, colorTop, colorBottom);
        
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
            int width = (int) Math.sqrt(radius * radius - (radius - i - 1) * (radius - i - 1));
            
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
     * Смешивает два цвета (улучшенная версия)
     */
    private static int blendColors(int color1, int color2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t)); // Ограничиваем от 0 до 1
        
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
     * Рисует тень вокруг элемента (улучшенная версия)
     */
    public static void drawShadow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                 int shadowSize, int shadowColor) {
        // Верхняя тень
        for (int i = 0; i < shadowSize; i++) {
            float alpha = 1.0f - (float) i / shadowSize;
            int alphaValue = (int) (((shadowColor >> 24) & 0xFF) * alpha);
            int color = ((alphaValue << 24) | (shadowColor & 0x00FFFFFF));
            guiGraphics.fill(x - i, y - shadowSize + i, x + width + i, y - shadowSize + i + 1, color);
        }
        
        // Нижняя тень
        for (int i = 0; i < shadowSize; i++) {
            float alpha = 1.0f - (float) i / shadowSize;
            int alphaValue = (int) (((shadowColor >> 24) & 0xFF) * alpha);
            int color = ((alphaValue << 24) | (shadowColor & 0x00FFFFFF));
            guiGraphics.fill(x - i, y + height + i, x + width + i, y + height + i + 1, color);
        }
        
        // Левая тень
        for (int i = 0; i < shadowSize; i++) {
            float alpha = 1.0f - (float) i / shadowSize;
            int alphaValue = (int) (((shadowColor >> 24) & 0xFF) * alpha);
            int color = ((alphaValue << 24) | (shadowColor & 0x00FFFFFF));
            guiGraphics.fill(x - shadowSize + i, y - i, x - shadowSize + i + 1, y + height + i, color);
        }
        
        // Правая тень
        for (int i = 0; i < shadowSize; i++) {
            float alpha = 1.0f - (float) i / shadowSize;
            int alphaValue = (int) (((shadowColor >> 24) & 0xFF) * alpha);
            int color = ((alphaValue << 24) | (shadowColor & 0x00FFFFFF));
            guiGraphics.fill(x + width + i, y - i, x + width + i + 1, y + height + i, color);
        }
    }
    
    /**
     * Рисует свечение вокруг элемента (улучшенная версия)
     */
    public static void drawGlow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                               int glowColor, float intensity) {
        intensity = Math.max(0.0f, Math.min(1.0f, intensity)); // Ограничиваем от 0 до 1
        
        // Многослойное свечение
        for (int layer = 0; layer < 5; layer++) {
            float alpha = intensity * (1.0f - (float) layer / 5.0f);
            int alphaValue = (int) (255 * alpha);
            int color = ((alphaValue << 24) | (glowColor & 0x00FFFFFF));
            int offset = layer;
            
            // Верх
            guiGraphics.fill(x - offset, y - offset, x + width + offset, y, color);
            // Низ
            guiGraphics.fill(x - offset, y + height, x + width + offset, y + height + offset, color);
            // Лево
            guiGraphics.fill(x - offset, y, x, y + height, color);
            // Право
            guiGraphics.fill(x + width, y, x + width + offset, y + height, color);
        }
    }
    
    /**
     * Рисует рамку со скруглёнными углами
     */
    public static void drawRoundedBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                        int borderColor, int thickness) {
        // Верхняя и нижняя линии
        guiGraphics.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + thickness, borderColor);
        guiGraphics.fill(x + CORNER_RADIUS, y + height - thickness, x + width - CORNER_RADIUS, y + height, borderColor);
        
        // Левая и правая линии
        guiGraphics.fill(x, y + CORNER_RADIUS, x + thickness, y + height - CORNER_RADIUS, borderColor);
        guiGraphics.fill(x + width - thickness, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, borderColor);
        
        // Углы
        drawCornerBorder(guiGraphics, x, y, CORNER_RADIUS, borderColor, thickness, 0);
        drawCornerBorder(guiGraphics, x + width - CORNER_RADIUS, y, CORNER_RADIUS, borderColor, thickness, 1);
        drawCornerBorder(guiGraphics, x, y + height - CORNER_RADIUS, CORNER_RADIUS, borderColor, thickness, 2);
        drawCornerBorder(guiGraphics, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, borderColor, thickness, 3);
    }
    
    private static void drawCornerBorder(GuiGraphics guiGraphics, int x, int y, int radius, int color, int thickness, int corner) {
        for (int i = 0; i < radius; i++) {
            int width = (int) Math.sqrt(radius * radius - (radius - i - 1) * (radius - i - 1));
            int drawWidth = Math.min(width, thickness);
            
            switch (corner) {
                case 0:
                    guiGraphics.fill(x, y + i, x + drawWidth, y + i + 1, color);
                    break;
                case 1:
                    guiGraphics.fill(x + radius - drawWidth, y + i, x + radius, y + i + 1, color);
                    break;
                case 2:
                    guiGraphics.fill(x, y + radius - i - 1, x + drawWidth, y + radius - i, color);
                    break;
                case 3:
                    guiGraphics.fill(x + radius - drawWidth, y + radius - i - 1, x + radius, y + radius - i, color);
                    break;
            }
        }
    }
    
    /**
     * Создаёт стилизованную кнопку с современным API
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
        
        // Стандартная кнопка Minecraft Forge 1.20.1
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
    
    /**
     * Рендерит стилизованную кнопку с правильной обработкой текстур
     * Полностью переписана для корректного отображения текстур и текста
     */
    public static void renderStyledButton(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, 
                                         Button button, int mouseX, int mouseY, boolean isSelected) {
        if (button == null) return;
        
        boolean hovered = button.isHovered();
        boolean pressed = button.isFocused() || button.isHovered() && 
                         net.minecraft.client.Minecraft.getInstance().mouseHandler.isLeftPressed();
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        // Используем текстуру кнопки, если доступна
        ResourceLocation buttonTexture = GameTextures.MOD_BUTTON;
        if (CardTextures.textureExists(buttonTexture)) {
            // Рендерим текстуру с правильным масштабированием
            // Используем правильный размер текстуры из файла
            int textureWidth = 256;
            int textureHeight = 64;
            
            // Используем blit для правильного отображения текстуры
            // Параметры: x, y, uOffset, vOffset, width, height, textureWidth, textureHeight
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
            // Fallback: красивая кнопка с градиентом
            int bgColor = pressed || isSelected ? 0xFF4A4A4A : (hovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            drawRoundedGradientRect(guiGraphics, x, y, width, height, 
                bgColor, blendColors(bgColor, 0xFF000000, 0.3f));
            
            // Рамка
            int borderColor = hovered ? 0xFF8B8B8B : 0xFF6B6B6B;
            drawRoundedBorder(guiGraphics, x, y, width, height, borderColor, 2);
        }
        
        // Рендерим текст поверх текстуры с правильным центрированием
        if (button.getMessage() != null && !button.getMessage().getString().isEmpty()) {
            int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFF0F0F0 : 0xFFE0E0E0);
            
            // Правильное центрирование текста
            int textX = x + width / 2;
            int textY = y + (height - font.lineHeight) / 2;
            
            // Ограничиваем позицию текста, чтобы он не выходил за границы кнопки
            textY = Math.max(y + 1, Math.min(textY, y + height - font.lineHeight - 1));
            
            // Рендерим текст с тенью для лучшей читаемости
            Component message = button.getMessage();
            guiGraphics.drawString(font, message, textX - font.width(message) / 2 + 1, textY + 1, 0x000000, false);
            guiGraphics.drawCenteredString(font, message, textX, textY, textColor);
        }
    }
    
    /**
     * Перегрузка метода без параметра isSelected
     */
    public static void renderStyledButton(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, 
                                         Button button, int mouseX, int mouseY) {
        renderStyledButton(guiGraphics, font, button, mouseX, mouseY, false);
    }
    
    /**
     * Рендерит только фон кнопки без текста
     */
    public static void renderButtonBackground(GuiGraphics guiGraphics, Button button, boolean isSelected) {
        if (button == null) return;
        
        boolean hovered = button.isHovered();
        boolean pressed = button.isFocused();
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        ResourceLocation buttonTexture = GameTextures.MOD_BUTTON;
        if (CardTextures.textureExists(buttonTexture)) {
            int textureWidth = 256;
            int textureHeight = 64;
            guiGraphics.blit(buttonTexture, x, y, 0, 0, width, height, textureWidth, textureHeight);
            
            if (hovered) {
                guiGraphics.fill(x, y, x + width, y + height, 0x20FFFFFF);
            }
            
            if (pressed || isSelected) {
                guiGraphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
            }
        } else {
            int bgColor = pressed || isSelected ? 0xFF4A4A4A : (hovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            drawRoundedGradientRect(guiGraphics, x, y, width, height, 
                bgColor, blendColors(bgColor, 0xFF000000, 0.3f));
            
            int borderColor = hovered ? 0xFF8B8B8B : 0xFF6B6B6B;
            drawRoundedBorder(guiGraphics, x, y, width, height, borderColor, 2);
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
     * Рисует деревянную панель (современная версия)
     */
    public static void drawWoodenPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean dark) {
        // Красивый фон с градиентом
        int bgColor = dark ? 0xFF2A1F15 : 0xFF3A2F25;
        int bgColorDark = blendColors(bgColor, 0xFF000000, 0.2f);
        drawRoundedGradientRect(guiGraphics, x, y, width, height, bgColor, bgColorDark);
        
        // Текстурированная рамка
        int borderColor = 0xFF4A3A2A;
        drawRoundedBorder(guiGraphics, x, y, width, height, borderColor, 2);
        
        // Добавляем тень для объёма
        drawShadow(guiGraphics, x, y, width, height, 3, 0x40000000);
    }
    
    /**
     * Рисует металлическую рамку (современная версия)
     */
    public static void drawMetalFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                     int thickness, boolean gold) {
        int frameColor = gold ? 0xFFFFD700 : 0xFF8B7355;
        int frameColorLight = gold ? 0xFFFFFFAA : 0xFFAA9966;
        
        // Градиентная рамка
        drawRoundedBorder(guiGraphics, x, y, width, height, frameColor, thickness);
        
        // Световая полоса для эффекта металла
        if (thickness >= 2) {
            int lightColor = gold ? 0x66FFD700 : 0x668B7355;
            drawRoundedBorder(guiGraphics, x + 1, y + 1, width - 2, height - 2, lightColor, 1);
        }
    }
    
    /**
     * Рисует кожаный элемент (современная версия)
     */
    public static void drawLeatherElement(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int bgColor = 0xFF3A2F25;
        int bgColorDark = 0xFF2A1F15;
        drawRoundedGradientRect(guiGraphics, x, y, width, height, bgColor, bgColorDark);
        
        int borderColor = 0xFF4A3A2A;
        drawRoundedBorder(guiGraphics, x, y, width, height, borderColor, 1);
    }
    
    /**
     * Рисует деревянный стол (современная версия)
     */
    public static void drawWoodenTable(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int bgColor = 0xFF2A1F15;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        drawShadow(guiGraphics, x, y, width, height, 4, 0x60000000);
    }
    
    /**
     * Рисует деревянную рамку (современная версия)
     */
    public static void drawWoodenBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean dark) {
        int borderColor = dark ? 0xFF3A2F25 : 0xFF4A3A2A;
        drawRoundedBorder(guiGraphics, x, y, width, height, borderColor, 2);
    }
    
    /**
     * Рисует прямоугольник с тенью и скруглёнными углами
     */
    public static void drawRoundedRectWithShadow(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                                int color, int shadowSize) {
        drawShadow(guiGraphics, x, y, width, height, shadowSize, 0x60000000);
        drawWoodenPanel(guiGraphics, x, y, width, height, true);
    }
    
    /**
     * Рендерит текстуру с правильным масштабированием и проверкой существования
     */
    public static void renderTextureSafely(GuiGraphics guiGraphics, ResourceLocation texture, 
                                          int x, int y, int width, int height,
                                          int textureWidth, int textureHeight) {
        if (texture == null || !CardTextures.textureExists(texture)) {
            // Fallback: рисуем простой прямоугольник
            guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);
            return;
        }
        
        try {
            guiGraphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
        } catch (Exception e) {
            // Если произошла ошибка при рендеринге, рисуем fallback
            com.bmfalkye.util.ModLogger.warn("Error rendering texture", 
                "texture", texture.toString(),
                "error", e.getMessage());
            guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);
        }
    }
}
