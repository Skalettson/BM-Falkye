package com.bmfalkye.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * Современная система автоматической адаптации интерфейса
 * Полностью переписана для максимальной адаптивности и корректного отображения
 * Автоматически рассчитывает позиции и размеры всех элементов под любое разрешение
 * Дата: 23 ноября 2025
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
    
    // Кэш для оптимизации
    private int cachedScreenWidth = -1;
    private int cachedScreenHeight = -1;
    
    public AdaptiveLayout(Screen screen, int baseWidth, int baseHeight, 
                         double maxScreenRatio, int minWidth, int minHeight) {
        if (screen == null) {
            throw new IllegalArgumentException("Screen cannot be null");
        }
        
        this.screen = screen;
        this.baseWidth = Math.max(100, baseWidth); // Минимум 100 пикселей
        this.baseHeight = Math.max(100, baseHeight);
        this.maxScreenRatio = Math.max(0.1, Math.min(1.0, maxScreenRatio)); // Ограничиваем от 0.1 до 1.0
        this.minWidth = Math.max(100, minWidth);
        this.minHeight = Math.max(100, minHeight);
        calculateLayout();
    }
    
    /**
     * Пересчитывает layout с учётом текущего размера экрана
     */
    private void calculateLayout() {
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Проверяем кэш
        if (screenWidth == cachedScreenWidth && screenHeight == cachedScreenHeight) {
            return; // Размер экрана не изменился, не пересчитываем
        }
        
        cachedScreenWidth = screenWidth;
        cachedScreenHeight = screenHeight;
        
        // Определяем размер экрана
        boolean isSmallScreen = SmallScreenOptimizer.isSmallScreen(screen);
        boolean isVerySmallScreen = SmallScreenOptimizer.isVerySmallScreen(screen);
        
        // Максимальные размеры с учётом размера экрана
        double effectiveMaxRatio = isVerySmallScreen ? 0.98 : (isSmallScreen ? 0.95 : maxScreenRatio);
        int maxWidth = (int)(screenWidth * effectiveMaxRatio);
        int maxHeight = (int)(screenHeight * effectiveMaxRatio);
        
        // Адаптивные размеры
        if (isVerySmallScreen || isSmallScreen) {
            // Для маленьких экранов используем оптимизатор
            guiWidth = SmallScreenOptimizer.getGuiWidth(screen, baseWidth, minWidth);
            guiHeight = SmallScreenOptimizer.getGuiHeight(screen, baseHeight, minHeight);
        } else {
            // Для больших экранов используем адаптивные размеры
            guiWidth = Math.max(minWidth, Math.min(baseWidth, maxWidth));
            guiHeight = Math.max(minHeight, Math.min(baseHeight, maxHeight));
        }
        
        // Убеждаемся, что GUI не выходит за границы экрана
        guiWidth = Math.max(minWidth, Math.min(guiWidth, screenWidth - 20));
        guiHeight = Math.max(minHeight, Math.min(guiHeight, screenHeight - 20));
        
        // Центрирование
        guiX = (screenWidth - guiWidth) / 2;
        guiY = (screenHeight - guiHeight) / 2;
        
        // Убеждаемся, что GUI не выходит за границы экрана
        guiX = Math.max(10, Math.min(guiX, screenWidth - guiWidth - 10));
        guiY = Math.max(10, Math.min(guiY, screenHeight - guiHeight - 10));
    }
    
    /**
     * Получает ширину GUI
     */
    public int getGuiWidth() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiWidth;
    }
    
    /**
     * Получает высоту GUI
     */
    public int getGuiHeight() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiHeight;
    }
    
    /**
     * Получает координату X GUI
     */
    public int getGuiX() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiX;
    }
    
    /**
     * Получает координату Y GUI
     */
    public int getGuiY() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiY;
    }
    
    /**
     * Получает адаптивную координату X с отступом от левого края GUI (в процентах)
     */
    public int getX(int offsetPercent) {
        if (needsRecalculation()) {
            calculateLayout();
        }
        offsetPercent = Math.max(0, Math.min(100, offsetPercent)); // Ограничиваем от 0 до 100
        return guiX + (int)(guiWidth * offsetPercent / 100.0);
    }
    
    /**
     * Получает адаптивную координату Y с отступом от верхнего края GUI (в процентах)
     */
    public int getY(int offsetPercent) {
        if (needsRecalculation()) {
            calculateLayout();
        }
        offsetPercent = Math.max(0, Math.min(100, offsetPercent)); // Ограничиваем от 0 до 100
        return guiY + (int)(guiHeight * offsetPercent / 100.0);
    }
    
    /**
     * Получает адаптивную ширину в процентах от ширины GUI
     */
    public int getWidth(int percent) {
        if (needsRecalculation()) {
            calculateLayout();
        }
        percent = Math.max(1, Math.min(100, percent)); // Ограничиваем от 1 до 100
        int width = (int)(guiWidth * percent / 100.0);
        return Math.max(1, width); // Минимум 1 пиксель
    }
    
    /**
     * Получает адаптивную высоту в процентах от высоты GUI
     */
    public int getHeight(int percent) {
        if (needsRecalculation()) {
            calculateLayout();
        }
        percent = Math.max(1, Math.min(100, percent)); // Ограничиваем от 1 до 100
        int height = (int)(guiHeight * percent / 100.0);
        return Math.max(1, height); // Минимум 1 пиксель
    }
    
    /**
     * Получает адаптивный размер шрифта
     */
    public int getFontSize() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        int baseFontSize = Math.max(9, Math.min(12, (int)(guiWidth / 50.0)));
        return SmallScreenOptimizer.getFontSize(screen, baseFontSize);
    }
    
    /**
     * Получает адаптивный отступ между элементами
     */
    public int getSpacing() {
        if (needsRecalculation()) {
            calculateLayout();
        }
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
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiX + (guiWidth - elementWidth) / 2;
    }
    
    /**
     * Получает координату Y для центрирования элемента заданной высоты
     */
    public int getCenteredY(int elementHeight) {
        if (needsRecalculation()) {
            calculateLayout();
        }
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
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiX + guiWidth - getWidth(offsetPercent) - elementWidth;
    }
    
    /**
     * Получает координату Y снизу с отступом
     */
    public int getBottomY(int elementHeight, int offsetPercent) {
        if (needsRecalculation()) {
            calculateLayout();
        }
        return guiY + guiHeight - getHeight(offsetPercent) - elementHeight;
    }
    
    /**
     * Проверяет, нужно ли пересчитать layout (при изменении размера экрана)
     */
    public boolean needsRecalculation() {
        return screen.width != cachedScreenWidth || screen.height != cachedScreenHeight;
    }
    
    /**
     * Пересчитывает layout (вызывать при изменении размера экрана)
     */
    public void recalculate() {
        cachedScreenWidth = -1;
        cachedScreenHeight = -1;
        calculateLayout();
    }
    
    /**
     * Получает масштабный фактор для адаптации элементов
     */
    public float getScaleFactor() {
        if (needsRecalculation()) {
            calculateLayout();
        }
        float widthScale = (float) guiWidth / baseWidth;
        float heightScale = (float) guiHeight / baseHeight;
        return Math.min(widthScale, heightScale); // Используем меньший масштаб для сохранения пропорций
    }
    
    /**
     * Получает адаптивную ширину элемента с учётом масштаба
     */
    public int getScaledWidth(int baseElementWidth) {
        return (int)(baseElementWidth * getScaleFactor());
    }
    
    /**
     * Получает адаптивную высоту элемента с учётом масштаба
     */
    public int getScaledHeight(int baseElementHeight) {
        return (int)(baseElementHeight * getScaleFactor());
    }
}
