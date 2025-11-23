package com.bmfalkye.cosmetics;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Система аватаров и рамок профиля
 */
public class AvatarSystem {
    // Реестр аватаров
    private static final Map<String, Avatar> avatars = new HashMap<>();
    
    // Реестр рамок профиля
    private static final Map<String, ProfileFrame> profileFrames = new HashMap<>();
    
    static {
        // Регистрируем стандартные аватары
        registerAvatar("default", new Avatar(
            "default",
            "Стандартный",
            new ResourceLocation("bm_falkye", "textures/avatars/default.png")
        ));
        
        // Регистрируем стандартные рамки
        registerProfileFrame("default", new ProfileFrame(
            "default",
            "Стандартная",
            new ResourceLocation("bm_falkye", "textures/profile_frames/default.png")
        ));
        
        registerProfileFrame("legend", new ProfileFrame(
            "legend",
            "Легенда",
            new ResourceLocation("bm_falkye", "textures/profile_frames/legend.png")
        ));
    }
    
    /**
     * Регистрирует аватар
     */
    public static void registerAvatar(String id, Avatar avatar) {
        avatars.put(id, avatar);
    }
    
    /**
     * Получает аватар
     */
    public static Avatar getAvatar(String id) {
        return avatars.getOrDefault(id, avatars.get("default"));
    }
    
    /**
     * Регистрирует рамку профиля
     */
    public static void registerProfileFrame(String id, ProfileFrame frame) {
        profileFrames.put(id, frame);
    }
    
    /**
     * Получает рамку профиля
     */
    public static ProfileFrame getProfileFrame(String id) {
        return profileFrames.getOrDefault(id, profileFrames.get("default"));
    }
    
    /**
     * Аватар
     */
    public static class Avatar {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        
        public Avatar(String id, String name, ResourceLocation texture) {
            this.id = id;
            this.name = name;
            this.texture = texture;
        }
    }
    
    /**
     * Рамка профиля
     */
    public static class ProfileFrame {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        
        public ProfileFrame(String id, String name, ResourceLocation texture) {
            this.id = id;
            this.name = name;
            this.texture = texture;
        }
    }
}

