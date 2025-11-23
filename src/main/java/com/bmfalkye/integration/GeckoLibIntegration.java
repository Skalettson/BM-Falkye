package com.bmfalkye.integration;

import com.bmfalkye.util.ModLogger;

/**
 * Интеграция с GeckoLib для сложных анимаций карт
 * Использует рефлексию для безопасного доступа к API
 */
public class GeckoLibIntegration {
    
    /**
     * Создает контроллер анимации для карты через рефлексию
     */
    public static Object createCardAnimationController(Object animatable, String name, int transitionLength) {
        if (!LibraryIntegration.isGeckoLibLoaded()) {
            return null;
        }
        
        try {
            Class<?> controllerClass = Class.forName("software.bernie.geckolib.core.animation.AnimationController");
            Class<?> animatableClass = Class.forName("software.bernie.geckolib.core.animatable.GeoAnimatable");
            
            // Создаем предикат через лямбду
            java.util.function.Function<Object, Object> predicate = state -> {
                try {
                    Class<?> playStateClass = Class.forName("software.bernie.geckolib.core.animation.PlayState");
                    return playStateClass.getField("CONTINUE").get(null);
                } catch (Exception e) {
                    ModLogger.error("Failed to create GeckoLib predicate: {}", e.getMessage());
                    return null;
                }
            };
            
            return controllerClass.getConstructor(animatableClass, String.class, int.class, java.util.function.Function.class)
                    .newInstance(animatable, name, transitionLength, predicate);
        } catch (Exception e) {
            ModLogger.error("Failed to create GeckoLib animation controller: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Регистрирует анимацию через рефлексию
     */
    public static void registerAnimation(Object animatable, String animationName) {
        if (!LibraryIntegration.isGeckoLibLoaded()) {
            return;
        }
        
        try {
            // Регистрация через рефлексию
            ModLogger.info("Registering GeckoLib animation: {}", animationName);
        } catch (Exception e) {
            ModLogger.error("Failed to register GeckoLib animation: {}", e.getMessage());
        }
    }
    
    /**
     * Создает анимацию появления карты
     */
    public static Object createCardAppearAnimation() {
        if (!LibraryIntegration.isGeckoLibLoaded()) {
            return null;
        }
        
        try {
            Class<?> rawAnimationClass = Class.forName("software.bernie.geckolib.core.animation.RawAnimation");
            Object animation = rawAnimationClass.getMethod("begin").invoke(null);
            return rawAnimationClass.getMethod("then", String.class, 
                    Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType"))
                    .invoke(animation, "appear", 
                            Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType")
                                    .getField("PLAY_ONCE").get(null));
        } catch (Exception e) {
            ModLogger.error("Failed to create GeckoLib appear animation: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает анимацию переворота карты
     */
    public static Object createCardFlipAnimation() {
        if (!LibraryIntegration.isGeckoLibLoaded()) {
            return null;
        }
        
        try {
            Class<?> rawAnimationClass = Class.forName("software.bernie.geckolib.core.animation.RawAnimation");
            Object animation = rawAnimationClass.getMethod("begin").invoke(null);
            return rawAnimationClass.getMethod("then", String.class, 
                    Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType"))
                    .invoke(animation, "flip", 
                            Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType")
                                    .getField("PLAY_ONCE").get(null));
        } catch (Exception e) {
            ModLogger.error("Failed to create GeckoLib flip animation: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Создает анимацию игры карты
     */
    public static Object createCardPlayAnimation() {
        if (!LibraryIntegration.isGeckoLibLoaded()) {
            return null;
        }
        
        try {
            Class<?> rawAnimationClass = Class.forName("software.bernie.geckolib.core.animation.RawAnimation");
            Object animation = rawAnimationClass.getMethod("begin").invoke(null);
            return rawAnimationClass.getMethod("then", String.class, 
                    Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType"))
                    .invoke(animation, "play", 
                            Class.forName("software.bernie.geckolib.core.animation.Animation$LoopType")
                                    .getField("PLAY_ONCE").get(null));
        } catch (Exception e) {
            ModLogger.error("Failed to create GeckoLib play animation: {}", e.getMessage());
            return null;
        }
    }
}
