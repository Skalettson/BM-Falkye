package com.bmfalkye.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * Система автоматической адаптации интерфейса
 * Автоматически рассчитывает позиции и размеры всех элементов
 * Улучшена для поддержки очень маленьких экранов
 */
public class AdaptiveLayout {
    private final Screen screen;
    private final int baseWidth;
    private final int baseHeight;
    private final double maxScreenRatio;
    private final int minWidth;
    private final int minHeight;
    
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;
    
    public AdaptiveLayout(Screen screen, int baseWidth, int baseHeight, 
                         double maxScreenRatio, int minWidth, int minHeight) {
        this.screen = screen;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.maxScreenRatio = maxScreenRatio;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        calculateLayout();
    }
    
    private void calculateLayout() {
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Используем оптимизатор для маленьких экранов
        boolean isSmallScreen = SmallScreenOptimizer.isSmallScreen(screen);
        
        // Максимальные размеры (для маленьких экранов используем больше места)
        double effectiveMaxRatio = isSmallScreen ? 0.95 : maxScreenRatio;
        int maxWidth = (int)(screenWidth * effectiveMaxRatio);
        int maxHeight = (int)(screenHeight * effectiveMaxRatio);
        
        // Адаптивные размеры с учётом маленьких экранов
        if (isSmallScreen) {
            // Для маленьких экранов используем оптимизатор
            guiWidth = SmallScreenOptimizer.getGuiWidth(screen, baseWidth, minWidth);
            guiHeight = SmallScreenOptimizer.getGuiHeight(screen, baseHeight, minHeight);
        } else {
            guiWidth = Math.max(minWidth, Math.min(baseWidth, maxWidth));
            guiHeight = Math.max(minHeight, Math.min(baseHeight, maxHeight));
        }
        
        // Центрирование
        guiX = (screenWidth - guiWidth) / 2;
        guiY = (screenHeight - guiHeight) / 2;
    }
    
    public int getGuiWidth() { return guiWidth; }
    public int getGuiHeight() { return guiHeight; }
    public int getGuiX() { return guiX; }
    public int getGuiY() { return guiY; }
    
    /**
     * Получает адаптивную координату X с отступом от левого края
     */
    public int getX(int offsetPercent) {
        return guiX + (int)(guiWidth * offsetPercent / 100.0);
    }
    
    /**
     * Получает адаптивную координату Y с отступом от верхнего края
     */
    public int getY(int offsetPercent) {
        return guiY + (int)(guiHeight * offsetPercent / 100.0);
    }
    
    /**
     * Получает адаптивную ширину в процентах от ширины окна
     */
    public int getWidth(int percent) {
        return (int)(guiWidth * percent / 100.0);
    }
    
    /**
     * Получает адаптивную высоту в процентах от высоты окна
     */
    public int getHeight(int percent) {
        return (int)(guiHeight * percent / 100.0);
    }
    
    /**
     * Получает адаптивный размер шрифта
     */
    public int getFontSize() {
        int baseFontSize = Math.max(9, Math.min(12, (int)(guiWidth / 50.0)));
        return SmallScreenOptimizer.getFontSize(screen, baseFontSize);
    }
    
    /**
     * Получает адаптивный отступ между элементами
     */
    public int getSpacing() {
        int baseSpacing = Math.max(4, Math.min(8, (int)(guiHeight / 50.0)));
        return SmallScreenOptimizer.getSpacing(screen, baseSpacing);
    }
    
    /**
     * Получает координату X с пиксельным отступом от левого края
     */
    public int getX(int offsetPercent, int pixelOffset) {
        return getX(offsetPercent) + pixelOffset;
    }
    
    /**
     * Получает координату Y с пиксельным отступом от верхнего края
     */
    public int getY(int offsetPercent, int pixelOffset) {
        return getY(offsetPercent) + pixelOffset;
    }
    
    /**
     * Получает координату X для центрирования элемента заданной ширины
     */
    public int getCenteredX(int elementWidth) {
        return guiX + (guiWidth - elementWidth) / 2;
    }
    
    /**
     * Получает координату Y для центрирования элемента заданной высоты
     */
    public int getCenteredY(int elementHeight) {
        return guiY + (guiHeight - elementHeight) / 2;
    }
    
    /**
     * Получает координату X для центрирования элемента заданной ширины с вертикальным отступом
     */
    public int getCenteredX(int elementWidth, int yOffsetPercent) {
        return getCenteredX(elementWidth);
    }
    
    /**
     * Получает координату X справа от элемента с отступом
     */
    public int getRightX(int elementWidth, int offsetPercent) {
        return guiX + guiWidth - getWidth(offsetPercent) - elementWidth;
    }
    
    /**
     * Получает координату Y снизу с отступом
     */
    public int getBottomY(int elementHeight, int offsetPercent) {
        return guiY + guiHeight - getHeight(offsetPercent) - elementHeight;
    }
    
    /**
     * Проверяет, нужно ли пересчитать layout (при изменении размера экрана)
     */
    public boolean needsRecalculation() {
        return screen.width != guiWidth + 2 * guiX || screen.height != guiHeight + 2 * guiY;
    }
    
    /**
     * Пересчитывает layout (вызывать при изменении размера экрана)
     */
    public void recalculate() {
        calculateLayout();
    }
}

