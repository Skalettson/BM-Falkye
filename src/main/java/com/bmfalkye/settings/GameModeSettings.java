package com.bmfalkye.settings;

import net.minecraft.nbt.CompoundTag;

/**
 * Настройки режима игры (2D/3D)
 */
public class GameModeSettings {
    public enum GameMode {
        MODE_2D("2D"),
        MODE_3D("3D");
        
        private final String displayName;
        
        GameMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private static GameMode currentMode = GameMode.MODE_2D;
    
    /**
     * Получает текущий режим игры
     */
    public static GameMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Устанавливает режим игры
     */
    public static void setMode(GameMode mode) {
        currentMode = mode;
        saveSettings();
    }
    
    /**
     * Проверяет, включён ли 3D режим
     */
    public static boolean is3DMode() {
        return currentMode == GameMode.MODE_3D;
    }
    
    /**
     * Сохраняет настройки
     */
    private static void saveSettings() {
        // TODO: Сохранить в файл настроек клиента
    }
    
    /**
     * Загружает настройки
     */
    public static void loadSettings(CompoundTag tag) {
        if (tag.contains("gameMode")) {
            String modeStr = tag.getString("gameMode");
            try {
                currentMode = GameMode.valueOf(modeStr);
            } catch (IllegalArgumentException e) {
                currentMode = GameMode.MODE_2D;
            }
        }
    }
    
    /**
     * Сохраняет настройки в NBT
     */
    public static CompoundTag saveSettings(CompoundTag tag) {
        tag.putString("gameMode", currentMode.name());
        return tag;
    }
}

