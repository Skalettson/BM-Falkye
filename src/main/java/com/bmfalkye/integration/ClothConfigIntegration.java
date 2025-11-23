package com.bmfalkye.integration;

import com.bmfalkye.BMFalkye;
import net.minecraft.client.gui.screens.Screen;

/**
 * Интеграция с Cloth Config API для создания красивых конфигурационных экранов
 * Cloth Config предоставляет готовые компоненты для создания профессиональных GUI
 */
public class ClothConfigIntegration {
    
    /**
     * Проверяет, установлен ли мод Cloth Config
     */
    public static boolean isClothConfigLoaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("cloth_config");
    }
    
    /**
     * Создаёт конфигурационный экран с использованием Cloth Config API
     * Использует рефлексию для безопасного доступа без обязательной зависимости
     */
    public static Screen createConfigScreen(Screen parent) {
        if (!isClothConfigLoaded()) {
            return null;
        }
        
        try {
            // Используем рефлексию для доступа к Cloth Config API
            Class<?> clothConfigClass = Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            Object configBuilder = clothConfigClass.getMethod("create", Screen.class, String.class, String.class)
                .invoke(null, parent, "bm_falkye", "config");
            
            // Настройка конфигурации через API
            // Это пример - можно расширить для реальных настроек мода
            
            return (Screen) configBuilder.getClass().getMethod("build").invoke(configBuilder);
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("Cloth Config API not available", e);
            return null;
        }
    }
    
    /**
     * Регистрирует конфигурационный экран (вызывается при инициализации)
     */
    public static void registerConfigScreen() {
        if (!isClothConfigLoaded()) {
            return;
        }
        
        try {
            BMFalkye.LOGGER.info("Cloth Config integration: Config screen will be available if Cloth Config is installed");
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("Cloth Config integration not available", e);
        }
    }
}

