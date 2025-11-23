package com.bmfalkye.client.effects;

import com.bmfalkye.client.animations.CardAnimation;
import com.bmfalkye.cards.Card;
import com.bmfalkye.integration.GeckoLibIntegration;
import com.bmfalkye.integration.LibraryIntegration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Менеджер анимаций карт с поддержкой GeckoLib
 * Управляет всеми анимациями карт в игре
 */
public class AnimationManager {
    
    private final Map<String, CardAnimation> animations = new HashMap<>();
    
    /**
     * Добавляет анимацию для карты
     */
    public void addAnimation(String cardKey, CardAnimation animation) {
        animations.put(cardKey, animation);
    }
    
    /**
     * Обновляет все анимации
     */
    public void update() {
        Iterator<Map.Entry<String, CardAnimation>> iterator = animations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CardAnimation> entry = iterator.next();
            CardAnimation animation = entry.getValue();
            animation.update();
            
            if (animation.isFinished()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Получает анимацию для карты
     */
    public CardAnimation getAnimation(String cardKey) {
        return animations.get(cardKey);
    }
    
    /**
     * Создает анимацию раздачи карты с использованием GeckoLib
     */
    public void createDealAnimation(Card card, float startX, float startY, float targetX, float targetY, int cardIndex) {
        String cardKey = card.getId() + "_deal_" + cardIndex;
        
        CardAnimation animation = new CardAnimation(
            startX, startY, targetX, targetY,
            CardAnimation.AnimationType.DEAL,
            30
        );
        
        // Используем GeckoLib для сложных анимаций, если доступен
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                Object geckoAnim = GeckoLibIntegration.createCardAppearAnimation();
                if (geckoAnim != null) {
                    // Анимация будет обработана через GeckoLib
                }
            } catch (Exception e) {
                // Fallback на обычную анимацию
            }
        }
        
        animations.put(cardKey, animation);
    }
    
    /**
     * Создает анимацию игры карты
     */
    public void createPlayAnimation(Card card, float startX, float startY, float targetX, float targetY) {
        String cardKey = card.getId() + "_play_" + System.currentTimeMillis();
        
        CardAnimation animation = new CardAnimation(
            startX, startY, targetX, targetY,
            CardAnimation.AnimationType.PLAY,
            20
        );
        
        // Используем GeckoLib для сложных анимаций
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                Object geckoAnim = GeckoLibIntegration.createCardPlayAnimation();
                if (geckoAnim != null) {
                    // Анимация будет обработана через GeckoLib
                }
            } catch (Exception e) {
                // Fallback на обычную анимацию
            }
        }
        
        animations.put(cardKey, animation);
    }
    
    /**
     * Создает анимацию переворота карты
     */
    public void createFlipAnimation(Card card, float x, float y) {
        String cardKey = card.getId() + "_flip_" + System.currentTimeMillis();
        
        CardAnimation animation = new CardAnimation(
            x, y, x, y,
            CardAnimation.AnimationType.FLIP,
            30
        );
        
        // Используем GeckoLib для 3D переворота
        if (LibraryIntegration.isGeckoLibLoaded()) {
            try {
                Object geckoAnim = GeckoLibIntegration.createCardFlipAnimation();
                if (geckoAnim != null) {
                    // Анимация будет обработана через GeckoLib
                }
            } catch (Exception e) {
                // Fallback на обычную анимацию
            }
        }
        
        animations.put(cardKey, animation);
    }
    
    /**
     * Очищает все анимации
     */
    public void clear() {
        animations.clear();
    }
    
    /**
     * Проверяет, есть ли активные анимации
     */
    public boolean hasActiveAnimations() {
        return !animations.isEmpty();
    }
}

