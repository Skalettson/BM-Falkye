package com.bmfalkye.client.animations;

import com.bmfalkye.util.ObjectPool;

/**
 * Пул для переиспользования анимаций карт
 */
public class AnimationPool {
    private static final ObjectPool<CardAnimation> ANIMATION_POOL = 
        new ObjectPool<>(CardAnimation::new, 50);
    
    /**
     * Получает анимацию из пула и инициализирует её
     */
    public static CardAnimation acquire(float startX, float startY, float targetX, float targetY, 
                                       CardAnimation.AnimationType type, int duration) {
        CardAnimation anim = ANIMATION_POOL.acquire();
        anim.initialize(startX, startY, targetX, targetY, type, duration);
        return anim;
    }
    
    /**
     * Возвращает анимацию в пул
     */
    public static void release(CardAnimation anim) {
        if (anim != null) {
            anim.reset();
            ANIMATION_POOL.release(anim);
        }
    }
    
    /**
     * Очищает пул
     */
    public static void clear() {
        ANIMATION_POOL.clear();
    }
}

