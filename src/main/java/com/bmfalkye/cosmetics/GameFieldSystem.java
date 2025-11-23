package com.bmfalkye.cosmetics;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Система тематических игровых полей
 */
public class GameFieldSystem {
    // Реестр игровых полей
    private static final Map<String, GameField> gameFields = new HashMap<>();
    
    static {
        // Регистрируем стандартные поля
        registerGameField("default", new GameField(
            "default",
            "Стандартное",
            new ResourceLocation("bm_falkye", "textures/game_fields/default.png")
        ));
        
        registerGameField("sky_palace", new GameField(
            "sky_palace",
            "Небесный Дворец",
            new ResourceLocation("bm_falkye", "textures/game_fields/sky_palace.png")
        ));
        
        registerGameField("lava_caves", new GameField(
            "lava_caves",
            "Лавовые Пещеры",
            new ResourceLocation("bm_falkye", "textures/game_fields/lava_caves.png")
        ));
        
        registerGameField("mushroom_forest", new GameField(
            "mushroom_forest",
            "Грибной Лес",
            new ResourceLocation("bm_falkye", "textures/game_fields/mushroom_forest.png")
        ));
    }
    
    /**
     * Регистрирует игровое поле
     */
    public static void registerGameField(String id, GameField field) {
        gameFields.put(id, field);
    }
    
    /**
     * Получает игровое поле
     */
    public static GameField getGameField(String id) {
        return gameFields.getOrDefault(id, gameFields.get("default"));
    }
    
    /**
     * Игровое поле
     */
    public static class GameField {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        
        public GameField(String id, String name, ResourceLocation texture) {
            this.id = id;
            this.name = name;
            this.texture = texture;
        }
    }
}

