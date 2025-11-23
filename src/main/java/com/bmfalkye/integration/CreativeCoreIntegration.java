package com.bmfalkye.integration;

import com.bmfalkye.util.ModLogger;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * Расширенная интеграция с CreativeCore для продвинутых GUI компонентов
 * Создает улучшенные кнопки, панели, слайдеры и другие элементы интерфейса
 */
public class CreativeCoreIntegration {
    
    /**
     * Создает улучшенную кнопку с использованием CreativeCore
     */
    public static Button createAdvancedButton(int x, int y, int width, int height, 
                                            Component text, Button.OnPress onPress) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            // Fallback на стандартную кнопку
            return Button.builder(text, onPress)
                    .bounds(x, y, width, height)
                    .build();
        }
        
        try {
            // Используем CreativeCore виджеты через рефлексию
            Class<?> buttonClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiButton");
            Object button = buttonClass.getConstructor(String.class, int.class, int.class, int.class, int.class)
                    .newInstance(text.getString(), x, y, width, height);
            
            // Устанавливаем обработчик через рефлексию
            buttonClass.getMethod("setOnClick", Runnable.class).invoke(button, (Runnable) () -> onPress.onPress(null));
            
            return (Button) button;
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore button, using fallback: {}", e.getMessage());
            return Button.builder(text, onPress)
                    .bounds(x, y, width, height)
                    .build();
        }
    }
    
    /**
     * Создает слайдер с использованием CreativeCore
     */
    public static Object createSlider(int x, int y, int width, int height, 
                                     double min, double max, double value) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            return null;
        }
        
        try {
            Class<?> sliderClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiSlider");
            return sliderClass.getConstructor(String.class, int.class, int.class, int.class, int.class, 
                    double.class, double.class, double.class)
                    .newInstance("slider", x, y, width, height, min, max, value);
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore slider: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает вкладки с использованием CreativeCore
     */
    public static Object createTabs(int x, int y, String... tabNames) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            return null;
        }
        
        try {
            Class<?> tabsClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiTabs");
            return tabsClass.getConstructor(int.class, int.class, String[].class)
                    .newInstance(x, y, tabNames);
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore tabs: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает улучшенную панель с использованием CreativeCore
     */
    public static Object createPanel(int x, int y, int width, int height, int backgroundColor, int borderColor) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            return null;
        }
        
        try {
            Class<?> panelClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiPanel");
            return panelClass.getConstructor(int.class, int.class, int.class, int.class, int.class, int.class)
                    .newInstance(x, y, width, height, backgroundColor, borderColor);
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore panel: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает улучшенное текстовое поле с использованием CreativeCore
     */
    public static Object createTextField(int x, int y, int width, int height, String text) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            return null;
        }
        
        try {
            Class<?> textFieldClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiTextField");
            return textFieldClass.getConstructor(int.class, int.class, int.class, int.class, String.class)
                    .newInstance(x, y, width, height, text);
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore text field: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает улучшенную кнопку с анимацией и эффектами
     */
    public static Button createAnimatedButton(int x, int y, int width, int height, 
                                             Component text, Button.OnPress onPress) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            return createAdvancedButton(x, y, width, height, text, onPress);
        }
        
        try {
            // Используем CreativeCore для создания кнопки с анимацией
            Class<?> buttonClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiButton");
            Object button = buttonClass.getConstructor(String.class, int.class, int.class, int.class, int.class)
                    .newInstance(text.getString(), x, y, width, height);
            
            // Устанавливаем обработчик
            buttonClass.getMethod("setOnClick", Runnable.class).invoke(button, (Runnable) () -> onPress.onPress(null));
            
            // Включаем анимацию (если доступно)
            try {
                buttonClass.getMethod("setAnimated", boolean.class).invoke(button, true);
            } catch (Exception e) {
                // Анимация не поддерживается, продолжаем без неё
            }
            
            return (Button) button;
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore animated button, using fallback: {}", e.getMessage());
            return createAdvancedButton(x, y, width, height, text, onPress);
        }
    }
    
    /**
     * Создает улучшенную кнопку-переключатель (toggle button)
     */
    public static Button createToggleButton(int x, int y, int width, int height,
                                           Component text, boolean initialState, 
                                           java.util.function.Consumer<Boolean> onToggle) {
        if (!LibraryIntegration.isCreativeCoreLoaded()) {
            // Fallback на обычную кнопку
            return Button.builder(text, (btn) -> {
                // Простой переключатель
                onToggle.accept(!initialState);
            }).bounds(x, y, width, height).build();
        }
        
        try {
            Class<?> toggleClass = Class.forName("com.creativemd.creativecore.common.gui.controls.GuiToggleButton");
            Object toggle = toggleClass.getConstructor(String.class, int.class, int.class, int.class, int.class, boolean.class)
                    .newInstance(text.getString(), x, y, width, height, initialState);
            
            // Устанавливаем обработчик
            try {
                toggleClass.getMethod("setOnToggle", java.util.function.Consumer.class).invoke(toggle, onToggle);
            } catch (Exception e) {
                // Fallback на обычную кнопку
                return Button.builder(text, (btn) -> onToggle.accept(!initialState))
                        .bounds(x, y, width, height).build();
            }
            
            return (Button) toggle;
        } catch (Exception e) {
            ModLogger.warn("Failed to create CreativeCore toggle button, using fallback: {}", e.getMessage());
            return Button.builder(text, (btn) -> onToggle.accept(!initialState))
                    .bounds(x, y, width, height).build();
        }
    }
}

