package com.bmfalkye.client.effects;

import com.bmfalkye.cards.Card;
import com.bmfalkye.client.animations.CardAnimation;
import com.bmfalkye.game.FalkyeGameSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Менеджер анимаций сброса карт на поле
 * Управляет анимациями сброса, эффектами частиц и постоянными анимациями карт на поле
 */
public class CardPlayAnimationManager {
    
    /**
     * Представляет активную анимацию сброса карты
     */
    public static class CardDropAnimation {
        public final Card card;
        public final float startX;
        public final float startY;
        public final float targetX;
        public final float targetY;
        public final CardAnimation animation;
        public final long startTime;
        public final FalkyeGameSession.CardRow row;
        public boolean effectsPlayed = false;
        
        public CardDropAnimation(Card card, float startX, float startY, 
                                 float targetX, float targetY, FalkyeGameSession.CardRow row) {
            this.card = card;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.row = row;
            this.startTime = System.currentTimeMillis();
            
            // Создаем анимацию сброса с эффектом "броска"
            this.animation = new CardAnimation(
                startX, startY, targetX, targetY,
                CardAnimation.AnimationType.PLAY,
                50, // Длительность анимации (увеличено для более плавного эффекта)
                CardAnimation.EasingType.EASE_OUT_BOUNCE
            );
        }
    }
    
    /**
     * Представляет анимацию карты способности (движение в центр и исчезновение)
     */
    public static class AbilityCardAnimation {
        public final Card card;
        public final float startX;
        public final float startY;
        public final float centerX;
        public final float centerY;
        public final CardAnimation moveAnimation;
        public final CardAnimation fadeAnimation;
        public final long startTime;
        public boolean moveComplete = false;
        public boolean fadeComplete = false;
        
        public AbilityCardAnimation(Card card, float startX, float startY, 
                                  float centerX, float centerY) {
            this.card = card;
            this.startX = startX;
            this.startY = startY;
            this.centerX = centerX;
            this.centerY = centerY;
            this.startTime = System.currentTimeMillis();
            
            // Анимация движения в центр
            this.moveAnimation = new CardAnimation(
                startX, startY, centerX, centerY,
                CardAnimation.AnimationType.SLIDE,
                40, // Увеличено для более плавного движения
                CardAnimation.EasingType.EASE_OUT_CUBIC
            );
            
            // Анимация исчезновения (начнется после движения)
            this.fadeAnimation = new CardAnimation(
                centerX, centerY, centerX, centerY,
                CardAnimation.AnimationType.FADE_OUT,
                30, // Увеличено для более плавного исчезновения
                CardAnimation.EasingType.EASE_IN_CUBIC
            );
        }
    }
    
    /**
     * Представляет постоянную анимацию карты на поле
     */
    public static class FieldCardAnimation {
        public final Card card;
        public final float baseX;
        public final float baseY;
        public final long startTime;
        public float currentOffsetY = 0.0f;
        public float currentScale = 1.0f;
        public float currentRotation = 0.0f;
        public final AnimationStyle style;
        
        public enum AnimationStyle {
            FLOATING,    // Плавающая анимация
            PULSING,     // Пульсирующая анимация
            GLOWING,     // Свечение
            ROTATING,    // Вращение
            COMBINED     // Комбинированная
        }
        
        public FieldCardAnimation(Card card, float baseX, float baseY, AnimationStyle style) {
            this.card = card;
            this.baseX = baseX;
            this.baseY = baseY;
            this.startTime = System.currentTimeMillis();
            this.style = style;
        }
        
        public void update() {
            long currentTime = System.currentTimeMillis();
            float time = (currentTime - startTime) / 1000.0f;
            
            switch (style) {
                case FLOATING:
                    // Плавное плавание вверх-вниз (замедлено)
                    currentOffsetY = (float) Math.sin(time * 1.0f) * 3.0f;
                    break;
                case PULSING:
                    // Пульсация размера (замедлено)
                    currentScale = 1.0f + (float) Math.sin(time * 1.5f) * 0.05f;
                    break;
                case GLOWING:
                    // Пульсация для эффекта свечения (замедлено)
                    currentScale = 1.0f + (float) Math.sin(time * 1.25f) * 0.03f;
                    break;
                case ROTATING:
                    // Медленное вращение (замедлено)
                    currentRotation = time * 2.5f;
                    break;
                case COMBINED:
                    // Комбинация плавания и пульсации (замедлено)
                    currentOffsetY = (float) Math.sin(time * 1.0f) * 2.0f;
                    currentScale = 1.0f + (float) Math.sin(time * 1.5f) * 0.03f;
                    break;
            }
        }
        
        public float getX() {
            return baseX;
        }
        
        public float getY() {
            return baseY + currentOffsetY;
        }
        
        public float getScale() {
            return currentScale;
        }
        
        public float getRotation() {
            return currentRotation;
        }
    }
    
    // Активные анимации сброса
    private final Map<String, CardDropAnimation> dropAnimations = new HashMap<>();
    
    // Активные анимации карт способности
    private final Map<String, AbilityCardAnimation> abilityAnimations = new HashMap<>();
    
    // Постоянные анимации карт на поле
    private final Map<String, FieldCardAnimation> fieldAnimations = new HashMap<>();
    
    /**
     * Создает анимацию сброса карты на поле
     */
    public void createDropAnimation(Card card, float handX, float handY, 
                                    float fieldX, float fieldY, FalkyeGameSession.CardRow row) {
        // Используем позицию на поле как часть ключа для более точного поиска
        String key = card.getId() + "_drop_" + (int)fieldX + "_" + (int)fieldY + "_" + System.currentTimeMillis();
        CardDropAnimation anim = new CardDropAnimation(card, handX, handY, fieldX, fieldY, row);
        dropAnimations.put(key, anim);
    }
    
    /**
     * Получает все активные анимации сброса
     */
    public Map<String, CardDropAnimation> getAllDropAnimations() {
        return new HashMap<>(dropAnimations);
    }
    
    /**
     * Создает анимацию карты способности (движение в центр и исчезновение)
     */
    public void createAbilityAnimation(Card card, float handX, float handY, 
                                      float centerX, float centerY) {
        String key = card.getId() + "_ability_" + System.currentTimeMillis();
        AbilityCardAnimation anim = new AbilityCardAnimation(card, handX, handY, centerX, centerY);
        abilityAnimations.put(key, anim);
    }
    
    /**
     * Добавляет постоянную анимацию для карты на поле
     */
    public void addFieldAnimation(Card card, float x, float y, FieldCardAnimation.AnimationStyle style) {
        String key = card.getId() + "_field_" + x + "_" + y;
        if (!fieldAnimations.containsKey(key)) {
            FieldCardAnimation anim = new FieldCardAnimation(card, x, y, style);
            fieldAnimations.put(key, anim);
        }
    }
    
    /**
     * Удаляет постоянную анимацию карты с поля
     */
    public void removeFieldAnimation(Card card, float x, float y) {
        String key = card.getId() + "_field_" + x + "_" + y;
        fieldAnimations.remove(key);
    }
    
    /**
     * Обновляет все анимации
     */
    public void update() {
        // Обновляем анимации сброса
        Iterator<Map.Entry<String, CardDropAnimation>> dropIter = dropAnimations.entrySet().iterator();
        while (dropIter.hasNext()) {
            Map.Entry<String, CardDropAnimation> entry = dropIter.next();
            CardDropAnimation anim = entry.getValue();
            anim.animation.update();
            
            if (anim.animation.isFinished()) {
                dropIter.remove();
            }
        }
        
        // Обновляем анимации карт способности
        Iterator<Map.Entry<String, AbilityCardAnimation>> abilityIter = abilityAnimations.entrySet().iterator();
        while (abilityIter.hasNext()) {
            Map.Entry<String, AbilityCardAnimation> entry = abilityIter.next();
            AbilityCardAnimation anim = entry.getValue();
            
            if (!anim.moveComplete) {
                anim.moveAnimation.update();
                if (anim.moveAnimation.isFinished()) {
                    anim.moveComplete = true;
                }
            } else if (!anim.fadeComplete) {
                anim.fadeAnimation.update();
                if (anim.fadeAnimation.isFinished()) {
                    anim.fadeComplete = true;
                }
            }
            
            if (anim.moveComplete && anim.fadeComplete) {
                abilityIter.remove();
            }
        }
        
        // ОПТИМИЗАЦИЯ: Обновляем только первые 10 анимаций на поле (для производительности)
        int updateCount = 0;
        int maxFieldAnimations = 10;
        for (FieldCardAnimation anim : fieldAnimations.values()) {
            if (updateCount >= maxFieldAnimations) break;
            anim.update();
            updateCount++;
        }
    }
    
    /**
     * Получает анимацию сброса для карты по позиции
     */
    public CardDropAnimation getDropAnimation(String cardId, float targetX, float targetY) {
        // Ищем анимацию, которая движется к нужной позиции
        for (CardDropAnimation anim : dropAnimations.values()) {
            if (anim.card.getId().equals(cardId) && 
                Math.abs(anim.targetX - targetX) < 5 && 
                Math.abs(anim.targetY - targetY) < 5) {
                return anim;
            }
        }
        // Если не нашли по позиции, ищем любую активную анимацию для этой карты
        for (CardDropAnimation anim : dropAnimations.values()) {
            if (anim.card.getId().equals(cardId) && !anim.animation.isFinished()) {
                return anim;
            }
        }
        return null;
    }
    
    /**
     * Получает анимацию способности для карты
     */
    public AbilityCardAnimation getAbilityAnimation(String cardId) {
        for (AbilityCardAnimation anim : abilityAnimations.values()) {
            if (anim.card.getId().equals(cardId)) {
                return anim;
            }
        }
        return null;
    }
    
    /**
     * Получает постоянную анимацию карты на поле
     */
    public FieldCardAnimation getFieldAnimation(Card card, float x, float y) {
        String key = card.getId() + "_field_" + x + "_" + y;
        return fieldAnimations.get(key);
    }
    
    /**
     * Получает все активные анимации карт способности
     */
    public Map<String, AbilityCardAnimation> getAbilityAnimations() {
        return new HashMap<>(abilityAnimations);
    }
    
    /**
     * Очищает все анимации
     */
    public void clear() {
        dropAnimations.clear();
        abilityAnimations.clear();
        fieldAnimations.clear();
    }
    
    /**
     * Определяет стиль анимации для карты на основе её типа и фракции
     */
    public static FieldCardAnimation.AnimationStyle getAnimationStyleForCard(Card card) {
        // Для карт со способностями используем комбинированную анимацию
        if (card.getType() == Card.CardType.SPECIAL) {
            return FieldCardAnimation.AnimationStyle.COMBINED;
        }
        
        // Для разных фракций разные стили
        String faction = card.getFaction();
        if (faction.contains("Пламени") || faction.contains("Fire")) {
            return FieldCardAnimation.AnimationStyle.PULSING;
        } else if (faction.contains("Руин") || faction.contains("Watchers")) {
            return FieldCardAnimation.AnimationStyle.GLOWING;
        } else if (faction.contains("Рощения") || faction.contains("Nature")) {
            return FieldCardAnimation.AnimationStyle.FLOATING;
        }
        
        // По умолчанию плавающая анимация
        return FieldCardAnimation.AnimationStyle.FLOATING;
    }
}

