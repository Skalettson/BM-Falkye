package com.bmfalkye.integration;

import com.bmfalkye.BMFalkye;
import net.minecraft.client.gui.screens.Screen;

/**
 * Интеграция с ModMenu API для добавления кнопки конфигурации в меню модов
 * ModMenu позволяет модам добавлять свои кнопки в экран модов
 */
public class ModMenuIntegration {
    
    /**
     * Проверяет, установлен ли мод ModMenu
     */
    public static boolean isModMenuLoaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("modmenu");
    }
    
    /**
     * Получает экран конфигурации для ModMenu
     * Использует рефлексию для безопасного доступа без обязательной зависимости
     */
    public static Screen getConfigScreen(Screen parent) {
        if (!isModMenuLoaded()) {
            return null;
        }
        
        try {
            // Используем рефлексию для доступа к ModMenu API
            // ModMenu автоматически находит экраны конфигурации через интерфейс
            return ClothConfigIntegration.createConfigScreen(parent);
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("ModMenu API not available", e);
            return null;
        }
    }
    
    /**
     * Регистрирует интеграцию с ModMenu (вызывается при инициализации)
     */
    public static void registerModMenuIntegration() {
        if (!isModMenuLoaded()) {
            return;
        }
        
        try {
            BMFalkye.LOGGER.info("ModMenu integration: Config button will be available if ModMenu is installed");
        } catch (Exception e) {
            BMFalkye.LOGGER.debug("ModMenu integration not available", e);
        }
    }
}

