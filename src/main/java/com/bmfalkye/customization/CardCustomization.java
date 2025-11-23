package com.bmfalkye.customization;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система кастомизации карт (скины, эффекты)
 */
public class CardCustomization {
    // UUID игрока -> Map<ID карты, Кастомизация>
    private static final Map<UUID, Map<String, CardSkin>> playerCardSkins = new HashMap<>();
    
    /**
     * Применяет скин к карте
     */
    public static void applySkin(UUID playerId, String cardId, CardSkin skin) {
        playerCardSkins.computeIfAbsent(playerId, k -> new HashMap<>()).put(cardId, skin);
    }
    
    /**
     * Получает скин карты
     */
    public static CardSkin getSkin(UUID playerId, String cardId) {
        Map<String, CardSkin> skins = playerCardSkins.get(playerId);
        return skins != null ? skins.get(cardId) : null;
    }
    
    /**
     * Скин карты
     */
    public static class CardSkin {
        private final String skinId;
        private final String texturePath;
        private final String effectType;
        
        public CardSkin(String skinId, String texturePath, String effectType) {
            this.skinId = skinId;
            this.texturePath = texturePath;
            this.effectType = effectType;
        }
        
        public String getSkinId() { return skinId; }
        public String getTexturePath() { return texturePath; }
        public String getEffectType() { return effectType; }
    }
}

