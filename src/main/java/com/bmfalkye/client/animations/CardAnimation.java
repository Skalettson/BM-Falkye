package com.bmfalkye.client.animations;

import com.bmfalkye.integration.GeckoLibIntegration;
import com.bmfalkye.integration.LibraryIntegration;
import com.bmfalkye.util.ModLogger;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

/**
 * Улучшенная система анимаций карт с расширенными easing функциями и цепочками анимаций
 * Интегрировано с GeckoLib для сложных 3D анимаций
 */
public class CardAnimation {
    // GeckoLib анимация (если доступна)
    private Object geckoAnimation = null;
    private float startX, startY;  // Начальные позиции
    private float x, y;            // Текущие позиции
    private float targetX, targetY;
    private float startScale = 1.0f;  // Начальный масштаб
    private float scale = 1.0f;
    private float targetScale = 1.0f;
    private float rotation = 0.0f;
    private float alpha = 1.0f;
    private int duration = 0;
    private int elapsed = 0;
    private AnimationType type;
    private EasingType easingType = EasingType.EASE_OUT_CUBIC;
    
    public enum AnimationType {
        DEAL,      // Раздача карт
        PLAY,      // Разыгрывание карты
        FLIP,      // Переворот карты
        FADE_IN,   // Появление
        FADE_OUT,  // Исчезновение
        BOUNCE,    // Подпрыгивание
        SLIDE,     // Скольжение
        ROTATE     // Вращение
    }
    
    public enum EasingType {
        LINEAR,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_ELASTIC,
        EASE_OUT_ELASTIC,
        EASE_IN_BOUNCE,
        EASE_OUT_BOUNCE,
        EASE_IN_OUT_BOUNCE
    }
    
    /**
     * Конструктор по умолчанию для пула объектов
     */
    public CardAnimation() {
        // Инициализация будет выполнена через initialize()
    }
    
    public CardAnimation(float startX, float startY, float targetX, float targetY, 
                        AnimationType type, int duration) {
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.type = type;
        this.duration = duration;
        
        // Пытаемся использовать GeckoLib для сложных анимаций
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                switch (type) {
                    case FLIP:
                        geckoAnimation = GeckoLibIntegration.createCardFlipAnimation();
                        break;
                    case PLAY:
                        geckoAnimation = GeckoLibIntegration.createCardPlayAnimation();
                        break;
                    case FADE_IN:
                        geckoAnimation = GeckoLibIntegration.createCardAppearAnimation();
                        break;
                }
            } catch (Exception e) {
                ModLogger.warn("Failed to create GeckoLib animation, using fallback: {}", e.getMessage());
            }
        }
        
        // Настройки в зависимости от типа анимации
        switch (type) {
            case DEAL:
                this.startScale = 0.0f;
                this.scale = 0.0f;
                this.targetScale = 1.0f;
                this.easingType = EasingType.EASE_OUT_BOUNCE;
                break;
            case PLAY:
                this.startScale = 1.0f;
                this.scale = 1.0f;
                this.targetScale = 1.2f;
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case FLIP:
                this.rotation = 0.0f;
                this.easingType = EasingType.EASE_IN_OUT_CUBIC;
                break;
            case FADE_IN:
                this.alpha = 0.0f;
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case FADE_OUT:
                this.alpha = 1.0f;
                this.easingType = EasingType.EASE_IN_CUBIC;
                break;
            case BOUNCE:
                this.startScale = 1.0f;
                this.scale = 1.0f;
                this.targetScale = 1.0f;
                this.easingType = EasingType.EASE_OUT_BOUNCE;
                break;
            case SLIDE:
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case ROTATE:
                this.easingType = EasingType.EASE_IN_OUT_CUBIC;
                break;
        }
    }
    
    public CardAnimation(float startX, float startY, float targetX, float targetY, 
                        AnimationType type, int duration, EasingType easingType) {
        this(startX, startY, targetX, targetY, type, duration);
        this.easingType = easingType;
    }
    
    public void update() {
        if (elapsed >= duration) {
            return;
        }
        
        float progress = (float) elapsed / duration;
        float eased = applyEasing(progress, easingType);
        
        // Интерполяция позиции (используем начальные значения для правильной интерполяции)
        switch (type) {
            case DEAL:
            case PLAY:
            case SLIDE:
                x = lerp(startX, targetX, eased);
                y = lerp(startY, targetY, eased);
                scale = lerp(startScale, targetScale, eased);
                break;
            case FLIP:
            case ROTATE:
                rotation = progress * 360.0f;
                if (progress > 0.5f) {
                    alpha = 1.0f - (progress - 0.5f) * 2.0f;
                } else {
                    alpha = progress * 2.0f;
                }
                break;
            case FADE_IN:
                // Плавное появление с движением сверху вниз и увеличением
                x = lerp(startX, targetX, eased);
                y = lerp(startY, targetY, eased);
                scale = lerp(0.5f, 1.0f, eased); // Начинаем с 50% размера
                alpha = eased; // Плавное появление
                break;
            case FADE_OUT:
                alpha = 1.0f - eased;
                break;
            case BOUNCE:
                float bounce = (float) Math.sin(progress * Math.PI * 4) * (1.0f - progress);
                y = lerp(startY, targetY, eased) - bounce * 20.0f;
                break;
        }
        
        elapsed++;
    }
    
    public boolean isFinished() {
        return elapsed >= duration;
    }
    
    /**
     * Сбрасывает анимацию для переиспользования в пуле
     */
    public void reset() {
        this.startX = 0;
        this.startY = 0;
        this.x = 0;
        this.y = 0;
        this.targetX = 0;
        this.targetY = 0;
        this.startScale = 1.0f;
        this.scale = 1.0f;
        this.targetScale = 1.0f;
        this.rotation = 0.0f;
        this.alpha = 1.0f;
        this.duration = 0;
        this.elapsed = 0;
        this.type = null;
        this.easingType = EasingType.EASE_OUT_CUBIC;
        this.geckoAnimation = null;
    }
    
    /**
     * Инициализирует анимацию для переиспользования
     */
    public void initialize(float startX, float startY, float targetX, float targetY, 
                          AnimationType type, int duration) {
        reset();
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.type = type;
        this.duration = duration;
        
        // Настройки в зависимости от типа анимации
        switch (type) {
            case DEAL:
                this.startScale = 0.0f;
                this.scale = 0.0f;
                this.targetScale = 1.0f;
                this.easingType = EasingType.EASE_OUT_BOUNCE;
                break;
            case PLAY:
                this.startScale = 1.0f;
                this.scale = 1.0f;
                this.targetScale = 1.2f;
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case FLIP:
                this.rotation = 0.0f;
                this.easingType = EasingType.EASE_IN_OUT_CUBIC;
                break;
            case FADE_IN:
                this.alpha = 0.0f;
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case FADE_OUT:
                this.alpha = 1.0f;
                this.easingType = EasingType.EASE_IN_CUBIC;
                break;
            case BOUNCE:
                this.startScale = 1.0f;
                this.scale = 1.0f;
                this.targetScale = 1.0f;
                this.easingType = EasingType.EASE_OUT_BOUNCE;
                break;
            case SLIDE:
                this.easingType = EasingType.EASE_OUT_CUBIC;
                break;
            case ROTATE:
                this.easingType = EasingType.EASE_IN_OUT_CUBIC;
                break;
        }
        
        // Пытаемся использовать GeckoLib для сложных анимаций
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                switch (type) {
                    case FLIP:
                        geckoAnimation = GeckoLibIntegration.createCardFlipAnimation();
                        break;
                    case PLAY:
                        geckoAnimation = GeckoLibIntegration.createCardPlayAnimation();
                        break;
                    case FADE_IN:
                        geckoAnimation = GeckoLibIntegration.createCardAppearAnimation();
                        break;
                }
            } catch (Exception e) {
                ModLogger.warn("Failed to create GeckoLib animation, using fallback: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Применяет easing функцию к прогрессу
     */
    private float applyEasing(float t, EasingType type) {
        switch (type) {
            case LINEAR:
                return t;
            case EASE_IN_QUAD:
                return t * t;
            case EASE_OUT_QUAD:
                return 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT_QUAD:
                return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case EASE_IN_CUBIC:
                return t * t * t;
            case EASE_OUT_CUBIC:
                return 1 - (float) Math.pow(1 - t, 3);
            case EASE_IN_OUT_CUBIC:
                return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
            case EASE_IN_ELASTIC:
                if (t == 0) return 0;
                if (t == 1) return 1;
                return (float) (-Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * (2 * Math.PI) / 3));
            case EASE_OUT_ELASTIC:
                if (t == 0) return 0;
                if (t == 1) return 1;
                return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI) / 3) + 1);
            case EASE_IN_BOUNCE:
                return 1 - easeOutBounce(1 - t);
            case EASE_OUT_BOUNCE:
                return easeOutBounce(t);
            case EASE_IN_OUT_BOUNCE:
                return t < 0.5f ? (1 - easeOutBounce(1 - 2 * t)) / 2 : (1 + easeOutBounce(2 * t - 1)) / 2;
            default:
                return t;
        }
    }
    
    private float easeOutBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        
        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5f / d1) * t + 0.75f;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        } else {
            return n1 * (t -= 2.625f / d1) * t + 0.984375f;
        }
    }
    
    /**
     * Рендерит карту с применением трансформаций анимации
     * Этот метод не используется напрямую - трансформации применяются в renderRow
     */
    public void render(GuiGraphics guiGraphics, int cardX, int cardY, int cardWidth, int cardHeight) {
        // Трансформации применяются в renderRow через getX(), getY(), getScale(), getAlpha()
    }
    
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    // Геттеры
    public float getX() { return x; }
    public float getY() { return y; }
    public float getScale() { return scale; }
    public float getRotation() { return rotation; }
    public float getAlpha() { return alpha; }
    public AnimationType getType() { return type; }
    public EasingType getEasingType() { return easingType; }
    
    /**
     * Цепочка анимаций для последовательного выполнения
     */
    public static class AnimationChain {
        private final List<CardAnimation> animations = new ArrayList<>();
        private int currentIndex = 0;
        private boolean loop = false;
        
        public void addAnimation(CardAnimation animation) {
            animations.add(animation);
        }
        
        public void update() {
            if (currentIndex < animations.size()) {
                CardAnimation current = animations.get(currentIndex);
                current.update();
                
                if (current.isFinished()) {
                    currentIndex++;
                    if (loop && currentIndex >= animations.size()) {
                        currentIndex = 0;
                        // Сброс всех анимаций для повтора
                        for (CardAnimation anim : animations) {
                            anim.elapsed = 0;
                        }
                    }
                }
            }
        }
        
        public boolean isFinished() {
            return !loop && currentIndex >= animations.size();
        }
        
        public void setLoop(boolean loop) {
            this.loop = loop;
        }
        
        public CardAnimation getCurrentAnimation() {
            if (currentIndex < animations.size()) {
                return animations.get(currentIndex);
            }
            return null;
        }
    }
}
