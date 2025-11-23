package com.bmfalkye.client.gui;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.CardRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Утилитный класс для работы с текстурами карт
 * Проверяет наличие текстур и предоставляет доступ к ним
 */
public class CardTextures {
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, Boolean> TEXTURE_EXISTS_CACHE = new HashMap<>();
    
    // Текстуры карт по редкости
    public static final ResourceLocation CARD_COMMON = new ResourceLocation(BMFalkye.MOD_ID, "textures/card/card_common.png");
    public static final ResourceLocation CARD_RARE = new ResourceLocation(BMFalkye.MOD_ID, "textures/card/card_rare.png");
    public static final ResourceLocation CARD_EPIC = new ResourceLocation(BMFalkye.MOD_ID, "textures/card/card_epic.png");
    public static final ResourceLocation CARD_LEGENDARY = new ResourceLocation(BMFalkye.MOD_ID, "textures/card/card_legendary.png");
    
    // GUI текстуры
    public static final ResourceLocation CARD_BACK = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/card_back.png");
    public static final ResourceLocation CARD_FRAME = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/card_frame.png");
    public static final ResourceLocation FALKYE_BOARD = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/falkye_board.png");
    
    // Иконки фракций
    public static final ResourceLocation FACTION_FIRE_HOUSE = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/faction_fire_house.png");
    public static final ResourceLocation FACTION_WATCHERS = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/faction_watchers.png");
    public static final ResourceLocation FACTION_NATURE_CHILDREN = new ResourceLocation(BMFalkye.MOD_ID, "textures/gui/faction_nature_children.png");
    
    // Fallback текстура (используется, если основная текстура не найдена)
    private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    
    /**
     * Проверяет, существует ли текстура
     * С обработкой ошибок и логированием
     */
    public static boolean textureExists(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        
        String key = texture.toString();
        if (TEXTURE_EXISTS_CACHE.containsKey(key)) {
            return TEXTURE_EXISTS_CACHE.get(key);
        }
        
        boolean exists = false;
        try {
            if (Minecraft.getInstance() != null && 
                Minecraft.getInstance().getResourceManager() != null) {
                Optional<Resource> resource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(texture);
                exists = resource.isPresent();
            }
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение
            com.bmfalkye.util.ModLogger.warn("Error checking texture existence", 
                "texture", texture.toString(),
                "error", e.getMessage());
            exists = false;
        }
        
        TEXTURE_EXISTS_CACHE.put(key, exists);
        return exists;
    }
    
    /**
     * Получает текстуру с fallback, если основная не найдена
     * @param texture Основная текстура
     * @return Основная текстура, если она существует, иначе fallback
     */
    public static ResourceLocation getTextureWithFallback(ResourceLocation texture) {
        if (texture == null) {
            return FALLBACK_TEXTURE;
        }
        
        if (textureExists(texture)) {
            return texture;
        }
        
        // Логируем отсутствие текстуры
        com.bmfalkye.util.ModLogger.warn("Texture not found, using fallback", 
            "texture", texture.toString(),
            "fallback", FALLBACK_TEXTURE.toString());
        
        return FALLBACK_TEXTURE;
    }
    
    /**
     * Получает текстуру карты по редкости
     */
    public static ResourceLocation getCardTextureByRarity(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> CARD_COMMON;
            case RARE -> CARD_RARE;
            case EPIC -> CARD_EPIC;
            case LEGENDARY -> CARD_LEGENDARY;
        };
    }
    
    /**
     * Проверяет, существует ли текстура карты для данной редкости
     */
    public static boolean hasCardTexture(CardRarity rarity) {
        return textureExists(getCardTextureByRarity(rarity));
    }
    
    /**
     * Проверяет, существует ли текстура рубашки карты
     */
    public static boolean hasCardBackTexture() {
        return textureExists(CARD_BACK);
    }
    
    /**
     * Проверяет, существует ли текстура рамки карты
     */
    public static boolean hasCardFrameTexture() {
        return textureExists(CARD_FRAME);
    }
    
    /**
     * Проверяет, существует ли текстура фона игрового поля
     */
    public static boolean hasFalkyeBoardTexture() {
        return textureExists(FALKYE_BOARD);
    }
    
    /**
     * Получает текстуру иконки фракции
     */
    public static ResourceLocation getFactionIcon(String faction) {
        return switch (faction) {
            case "Дом Пламени" -> FACTION_FIRE_HOUSE;
            case "Дозорные Руин" -> FACTION_WATCHERS;
            case "Дети Рощения" -> FACTION_NATURE_CHILDREN;
            default -> null;
        };
    }
    
    /**
     * Проверяет, существует ли иконка фракции
     */
    public static boolean hasFactionIcon(String faction) {
        ResourceLocation icon = getFactionIcon(faction);
        return icon != null && textureExists(icon);
    }
    
    /**
     * Очищает кэш текстур (полезно при перезагрузке ресурсов)
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
        TEXTURE_EXISTS_CACHE.clear();
    }
}

