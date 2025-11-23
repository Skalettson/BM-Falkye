package com.bmfalkye.client.gui.backup.logic;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import java.util.List;

/**
 * БЭКАП ЛОГИКИ CardRenderer
 * 
 * Этот класс содержит всю логику рендеринга карт БЕЗ визуального оформления.
 * Используется для сохранения функциональности при переписывании графики.
 */
public class CardRendererLogic {
    
    /**
     * Вычисляет позицию текста названия карты с переносом строк
     */
    public static NameLayout calculateNameLayout(String name, int maxWidth, int fontLineHeight) {
        // Логика разбиения названия на строки
        // Возвращает позиции и количество строк
        return new NameLayout(name, maxWidth, fontLineHeight);
    }
    
    /**
     * Вычисляет эффективную силу карты с учётом баффов/дебаффов
     */
    public static int calculateEffectivePower(Card card, Integer basePower, List<com.bmfalkye.cards.CardBuff> buffs) {
        int power = basePower != null ? basePower : card.getPower();
        
        if (buffs != null) {
            for (com.bmfalkye.cards.CardBuff buff : buffs) {
                switch (buff.getType()) {
                    case POWER_INCREASE:
                        power += buff.getPowerModifier();
                        break;
                    case POWER_DECREASE:
                        power -= buff.getPowerModifier();
                        break;
                    case IMMUNITY:
                    case FROZEN:
                    case SHIELDED:
                    case DOOMED:
                        // Эти типы не влияют на силу напрямую
                        break;
                }
            }
        }
        
        return Math.max(0, power);
    }
    
    /**
     * Определяет цвет для отображения силы (зелёный/красный/белый)
     */
    public static String getPowerColorCode(int effectivePower, int basePower) {
        if (effectivePower > basePower) {
            return "§a"; // Зелёный для увеличения
        } else if (effectivePower < basePower) {
            return "§c"; // Красный для уменьшения
        }
        return "§f"; // Белый для базовой
    }
    
    /**
     * Получает иконку типа карты
     * Использует card.getType() напрямую, так как CardType - это внутренний enum класса Card
     */
    public static String getTypeIcon(Card card) {
        return switch (card.getType()) {
            case CREATURE -> "§a⚔";
            case SPELL -> "§b✨";
            case SPECIAL -> "§d★";
        };
    }
    
    /**
     * Вычисляет интенсивность свечения для карты
     */
    public static float calculateGlowIntensity(boolean selected, boolean hovered, CardRarity rarity, long animationTime) {
        float intensity = selected ? 0.6f : (hovered ? 0.4f : 0.0f);
        
        if (rarity == CardRarity.LEGENDARY) {
            float pulse = (float) (0.5f + 0.3f * Math.sin(animationTime / 200.0));
            intensity += pulse * 0.2f;
        }
        
        return intensity;
    }
    
    /**
     * Структура для хранения информации о расположении названия
     */
    public static class NameLayout {
        public final List<String> lines;
        public final int totalHeight;
        public final int startY;
        
        public NameLayout(String name, int maxWidth, int fontLineHeight) {
            // Упрощённая логика разбиения на строки
            this.lines = java.util.Arrays.asList(name.split(" "));
            this.totalHeight = lines.size() * (fontLineHeight + 1);
            this.startY = 0;
        }
    }
}

