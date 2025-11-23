package com.bmfalkye.integration;

import com.bmfalkye.util.ModLogger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Интеграция с Patchouli для внутриигровых книг и документации
 */
public class PatchouliIntegration {
    
    /**
     * Открывает книгу Patchouli
     * @return true если книга успешно открыта, false в противном случае
     */
    public static boolean openBook(ResourceLocation bookId) {
        if (!LibraryIntegration.isPatchouliLoaded()) {
            ModLogger.warn("Patchouli not loaded, cannot open book: {}", bookId);
            return false;
        }
        
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                ModLogger.warn("Player is null, cannot open Patchouli book");
                return false;
            }
            
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getField("get").get(null);
            apiClass.getMethod("openBookGUI", net.minecraft.world.entity.player.Player.class, ResourceLocation.class)
                    .invoke(api, mc.player, bookId);
            return true;
        } catch (Exception e) {
            ModLogger.error("Failed to open Patchouli book: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Создает книгу для обучения
     */
    public static void registerTutorialBook() {
        if (!LibraryIntegration.isPatchouliLoaded()) {
            return;
        }
        
        try {
            // Регистрация книги через Patchouli API
            ResourceLocation bookId = new ResourceLocation("bm_falkye", "tutorial");
            ModLogger.info("Registering Patchouli tutorial book: {}", bookId);
            
            // Здесь можно добавить регистрацию категорий и записей
        } catch (Exception e) {
            ModLogger.error("Failed to register Patchouli tutorial book: {}", e.getMessage());
        }
    }
    
    /**
     * Получает ItemStack книги
     */
    public static ItemStack getBookItem(ResourceLocation bookId) {
        if (!LibraryIntegration.isPatchouliLoaded()) {
            return ItemStack.EMPTY;
        }
        
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getField("get").get(null);
            return (ItemStack) apiClass.getMethod("getBookStack", ResourceLocation.class)
                    .invoke(api, bookId);
        } catch (Exception e) {
            ModLogger.error("Failed to get Patchouli book item: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}

