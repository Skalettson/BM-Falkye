package com.bmfalkye.client.gui;

import net.minecraft.nbt.CompoundTag;

/**
 * Настройки интерфейса для персонализации
 * 
 * <p>Позволяет игрокам настраивать различные аспекты интерфейса:
 * - Размер карт
 * - Размер шрифта
 * - Прозрачность элементов
 * - Цветовая схема
 * - Анимации
 * </p>
 * 
 * @author BM Falkye Team
 */
public class UISettings {
    
    // Размеры
    private float cardScale = 1.0f;
    private float buttonScale = 1.0f;
    private float fontSize = 1.0f;
    
    // Прозрачность
    private float backgroundOpacity = 0.9f;
    private float cardOpacity = 1.0f;
    
    // Цветовая схема
    private String colorScheme = "default"; // default, dark, light, custom
    
    // Анимации
    private boolean enableAnimations = true;
    private float animationSpeed = 1.0f;
    
    // Дополнительные настройки
    private boolean showCardTooltips = true;
    private boolean showActionLog = true;
    private int actionLogMaxLines = 10;
    private boolean compactMode = false;
    
    /**
     * Загружает настройки из NBT
     */
    public static UISettings load(CompoundTag tag) {
        UISettings settings = new UISettings();
        
        if (tag.contains("cardScale")) {
            settings.cardScale = tag.getFloat("cardScale");
        }
        if (tag.contains("buttonScale")) {
            settings.buttonScale = tag.getFloat("buttonScale");
        }
        if (tag.contains("fontSize")) {
            settings.fontSize = tag.getFloat("fontSize");
        }
        if (tag.contains("backgroundOpacity")) {
            settings.backgroundOpacity = tag.getFloat("backgroundOpacity");
        }
        if (tag.contains("cardOpacity")) {
            settings.cardOpacity = tag.getFloat("cardOpacity");
        }
        if (tag.contains("colorScheme")) {
            settings.colorScheme = tag.getString("colorScheme");
        }
        if (tag.contains("enableAnimations")) {
            settings.enableAnimations = tag.getBoolean("enableAnimations");
        }
        if (tag.contains("animationSpeed")) {
            settings.animationSpeed = tag.getFloat("animationSpeed");
        }
        if (tag.contains("showCardTooltips")) {
            settings.showCardTooltips = tag.getBoolean("showCardTooltips");
        }
        if (tag.contains("showActionLog")) {
            settings.showActionLog = tag.getBoolean("showActionLog");
        }
        if (tag.contains("actionLogMaxLines")) {
            settings.actionLogMaxLines = tag.getInt("actionLogMaxLines");
        }
        if (tag.contains("compactMode")) {
            settings.compactMode = tag.getBoolean("compactMode");
        }
        
        return settings;
    }
    
    /**
     * Сохраняет настройки в NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("cardScale", cardScale);
        tag.putFloat("buttonScale", buttonScale);
        tag.putFloat("fontSize", fontSize);
        tag.putFloat("backgroundOpacity", backgroundOpacity);
        tag.putFloat("cardOpacity", cardOpacity);
        tag.putString("colorScheme", colorScheme);
        tag.putBoolean("enableAnimations", enableAnimations);
        tag.putFloat("animationSpeed", animationSpeed);
        tag.putBoolean("showCardTooltips", showCardTooltips);
        tag.putBoolean("showActionLog", showActionLog);
        tag.putInt("actionLogMaxLines", actionLogMaxLines);
        tag.putBoolean("compactMode", compactMode);
        return tag;
    }
    
    // Геттеры и сеттеры
    public float getCardScale() {
        return cardScale;
    }
    
    public void setCardScale(float cardScale) {
        this.cardScale = Math.max(0.5f, Math.min(2.0f, cardScale));
    }
    
    public float getButtonScale() {
        return buttonScale;
    }
    
    public void setButtonScale(float buttonScale) {
        this.buttonScale = Math.max(0.5f, Math.min(2.0f, buttonScale));
    }
    
    public float getFontSize() {
        return fontSize;
    }
    
    public void setFontSize(float fontSize) {
        this.fontSize = Math.max(0.5f, Math.min(2.0f, fontSize));
    }
    
    public float getBackgroundOpacity() {
        return backgroundOpacity;
    }
    
    public void setBackgroundOpacity(float backgroundOpacity) {
        this.backgroundOpacity = Math.max(0.0f, Math.min(1.0f, backgroundOpacity));
    }
    
    public float getCardOpacity() {
        return cardOpacity;
    }
    
    public void setCardOpacity(float cardOpacity) {
        this.cardOpacity = Math.max(0.0f, Math.min(1.0f, cardOpacity));
    }
    
    public String getColorScheme() {
        return colorScheme;
    }
    
    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }
    
    public boolean isEnableAnimations() {
        return enableAnimations;
    }
    
    public void setEnableAnimations(boolean enableAnimations) {
        this.enableAnimations = enableAnimations;
    }
    
    public float getAnimationSpeed() {
        return animationSpeed;
    }
    
    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0.1f, Math.min(3.0f, animationSpeed));
    }
    
    public boolean isShowCardTooltips() {
        return showCardTooltips;
    }
    
    public void setShowCardTooltips(boolean showCardTooltips) {
        this.showCardTooltips = showCardTooltips;
    }
    
    public boolean isShowActionLog() {
        return showActionLog;
    }
    
    public void setShowActionLog(boolean showActionLog) {
        this.showActionLog = showActionLog;
    }
    
    public int getActionLogMaxLines() {
        return actionLogMaxLines;
    }
    
    public void setActionLogMaxLines(int actionLogMaxLines) {
        this.actionLogMaxLines = Math.max(1, Math.min(50, actionLogMaxLines));
    }
    
    public boolean isCompactMode() {
        return compactMode;
    }
    
    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }
    
    /**
     * Сбрасывает настройки к значениям по умолчанию
     */
    public void resetToDefaults() {
        cardScale = 1.0f;
        buttonScale = 1.0f;
        fontSize = 1.0f;
        backgroundOpacity = 0.9f;
        cardOpacity = 1.0f;
        colorScheme = "default";
        enableAnimations = true;
        animationSpeed = 1.0f;
        showCardTooltips = true;
        showActionLog = true;
        actionLogMaxLines = 10;
        compactMode = false;
    }
}

