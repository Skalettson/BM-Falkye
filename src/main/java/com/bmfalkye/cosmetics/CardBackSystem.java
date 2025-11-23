package com.bmfalkye.cosmetics;

import com.bmfalkye.sounds.ModSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Система рубашек карт (анимированные, с звуками)
 */
public class CardBackSystem {
    // Реестр рубашек карт
    private static final Map<String, CardBack> cardBacks = new HashMap<>();
    
    static {
        // Регистрируем стандартные рубашки
        registerCardBack("default", new CardBack(
            "default",
            "Стандартная",
            new ResourceLocation("bm_falkye", "textures/card_backs/default.png"),
            null, // Нет звука
            false // Не анимированная
        ));
        
        registerCardBack("legendary", new CardBack(
            "legendary",
            "Легендарная",
            new ResourceLocation("bm_falkye", "textures/card_backs/legendary.png"),
            null, // TODO: Добавить звук
            true // Анимированная
        ));
        
        registerCardBack("tournament", new CardBack(
            "tournament",
            "Турнирная",
            new ResourceLocation("bm_falkye", "textures/card_backs/tournament.png"),
            null,
            true
        ));
    }
    
    /**
     * Регистрирует рубашку карт
     */
    public static void registerCardBack(String id, CardBack cardBack) {
        cardBacks.put(id, cardBack);
    }
    
    /**
     * Получает рубашку карт
     */
    public static CardBack getCardBack(String id) {
        return cardBacks.getOrDefault(id, cardBacks.get("default"));
    }
    
    /**
     * Воспроизводит звук тасовки для рубашки
     */
    public static void playShuffleSound(ServerPlayer player, String cardBackId) {
        CardBack cardBack = getCardBack(cardBackId);
        if (cardBack != null && cardBack.shuffleSound != null) {
            player.level().playSound(null, player.blockPosition(), 
                cardBack.shuffleSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
    
    /**
     * Рубашка карт
     */
    public static class CardBack {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        public final SoundEvent shuffleSound;
        public final boolean animated;
        
        public CardBack(String id, String name, ResourceLocation texture, 
                       SoundEvent shuffleSound, boolean animated) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.shuffleSound = shuffleSound;
            this.animated = animated;
        }
    }
}

