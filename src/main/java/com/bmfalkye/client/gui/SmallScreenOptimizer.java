package com.bmfalkye.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * Оптимизатор для очень маленьких экранов
 * Улучшает масштабирование и отображение элементов на экранах с низким разрешением
 */
public class SmallScreenOptimizer {
    
    // Пороги для определения размера экрана
    private static final int VERY_SMALL_WIDTH = 640;
    private static final int VERY_SMALL_HEIGHT = 480;
    private static final int SMALL_WIDTH = 800;
    private static final int SMALL_HEIGHT = 600;
    
    // Минимальные размеры для элементов
    private static final int MIN_CARD_WIDTH = 40;
    private static final int MIN_CARD_HEIGHT = 60;
    private static final int MIN_BUTTON_WIDTH = 50;
    private static final int MIN_BUTTON_HEIGHT = 16;
    private static final int MIN_FONT_SIZE = 8;
    
    /**
     * Определяет размер экрана
     */
    public enum ScreenSize {
        VERY_SMALL,  // <= 640x480
        SMALL,       // <= 800x600
        NORMAL       // > 800x600
    }
    
    /**
     * Определяет размер экрана
     */
    public static ScreenSize getScreenSize(Screen screen) {
        if (screen == null) {
            return ScreenSize.NORMAL;
        }
        
        int width = screen.width;
        int height = screen.height;
        
        if (width <= VERY_SMALL_WIDTH || height <= VERY_SMALL_HEIGHT) {
            return ScreenSize.VERY_SMALL;
        } else if (width <= SMALL_WIDTH || height <= SMALL_HEIGHT) {
            return ScreenSize.SMALL;
        } else {
            return ScreenSize.NORMAL;
        }
    }
    
    /**
     * Проверяет, является ли экран очень маленьким
     */
    public static boolean isVerySmallScreen(Screen screen) {
        return getScreenSize(screen) == ScreenSize.VERY_SMALL;
    }
    
    /**
     * Проверяет, является ли экран маленьким
     */
    public static boolean isSmallScreen(Screen screen) {
        ScreenSize size = getScreenSize(screen);
        return size == ScreenSize.VERY_SMALL || size == ScreenSize.SMALL;
    }
    
    /**
     * Получает адаптивную ширину карты
     */
    public static int getCardWidth(Screen screen, int baseWidth) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(MIN_CARD_WIDTH, (int)(baseWidth * 0.6));
            case SMALL:
                return Math.max(MIN_CARD_WIDTH, (int)(baseWidth * 0.75));
            default:
                return baseWidth;
        }
    }
    
    /**
     * Получает адаптивную высоту карты
     */
    public static int getCardHeight(Screen screen, int baseHeight) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(MIN_CARD_HEIGHT, (int)(baseHeight * 0.6));
            case SMALL:
                return Math.max(MIN_CARD_HEIGHT, (int)(baseHeight * 0.75));
            default:
                return baseHeight;
        }
    }
    
    /**
     * Получает адаптивную ширину кнопки
     */
    public static int getButtonWidth(Screen screen, int baseWidth) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(MIN_BUTTON_WIDTH, (int)(baseWidth * 0.7));
            case SMALL:
                return Math.max(MIN_BUTTON_WIDTH, (int)(baseWidth * 0.85));
            default:
                return baseWidth;
        }
    }
    
    /**
     * Получает адаптивную высоту кнопки
     */
    public static int getButtonHeight(Screen screen, int baseHeight) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(MIN_BUTTON_HEIGHT, (int)(baseHeight * 0.8));
            case SMALL:
                return Math.max(MIN_BUTTON_HEIGHT, (int)(baseHeight * 0.9));
            default:
                return baseHeight;
        }
    }
    
    /**
     * Получает адаптивный размер шрифта
     */
    public static int getFontSize(Screen screen, int baseFontSize) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(MIN_FONT_SIZE, (int)(baseFontSize * 0.75));
            case SMALL:
                return Math.max(MIN_FONT_SIZE, (int)(baseFontSize * 0.85));
            default:
                return baseFontSize;
        }
    }
    
    /**
     * Получает адаптивный отступ между элементами
     */
    public static int getSpacing(Screen screen, int baseSpacing) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(2, (int)(baseSpacing * 0.5));
            case SMALL:
                return Math.max(3, (int)(baseSpacing * 0.75));
            default:
                return baseSpacing;
        }
    }
    
    /**
     * Получает адаптивную ширину GUI
     */
    public static int getGuiWidth(Screen screen, int baseWidth, int minWidth) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                // Для очень маленьких экранов используем почти весь экран
                return Math.max(minWidth, Math.min(baseWidth, (int)(screen.width * 0.95)));
            case SMALL:
                return Math.max(minWidth, Math.min(baseWidth, (int)(screen.width * 0.90)));
            default:
                return baseWidth;
        }
    }
    
    /**
     * Получает адаптивную высоту GUI
     */
    public static int getGuiHeight(Screen screen, int baseHeight, int minHeight) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                // Для очень маленьких экранов используем почти весь экран
                return Math.max(minHeight, Math.min(baseHeight, (int)(screen.height * 0.95)));
            case SMALL:
                return Math.max(minHeight, Math.min(baseHeight, (int)(screen.height * 0.90)));
            default:
                return baseHeight;
        }
    }
    
    /**
     * Получает максимальное количество карт в руке для отображения
     */
    public static int getMaxCardsInHand(Screen screen, int baseMax) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(5, (int)(baseMax * 0.6));
            case SMALL:
                return Math.max(7, (int)(baseMax * 0.8));
            default:
                return baseMax;
        }
    }
    
    /**
     * Получает масштаб для отображения элементов
     */
    public static double getScale(Screen screen) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return 0.6;
            case SMALL:
                return 0.75;
            default:
                return 1.0;
        }
    }
    
    /**
     * Проверяет, нужно ли использовать компактный режим
     */
    public static boolean shouldUseCompactMode(Screen screen) {
        return isSmallScreen(screen);
    }
    
    /**
     * Получает адаптивную ширину лога действий
     */
    public static int getLogWidth(Screen screen, int baseWidth) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                // На очень маленьких экранах лог может быть скрыт или минимизирован
                return Math.max(120, (int)(baseWidth * 0.6));
            case SMALL:
                return Math.max(140, (int)(baseWidth * 0.8));
            default:
                return baseWidth;
        }
    }
    
    /**
     * Получает адаптивную высоту лога действий
     */
    public static int getLogHeight(Screen screen, int baseHeight) {
        ScreenSize size = getScreenSize(screen);
        
        switch (size) {
            case VERY_SMALL:
                return Math.max(100, (int)(baseHeight * 0.5));
            case SMALL:
                return Math.max(150, (int)(baseHeight * 0.75));
            default:
                return baseHeight;
        }
    }
}

