package com.bmfalkye.events;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.client.gui.CardTextures;
import com.bmfalkye.util.CacheCleanupManager;
import com.bmfalkye.util.ModLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик перезагрузки ресурсов
 * Очищает кэши текстур при перезагрузке ресурсов
 */
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID, value = Dist.CLIENT)
public class ResourceReloadHandler {
    
    @SubscribeEvent
    public static void onResourceReload(AddReloadListenerEvent event) {
        // Очищаем кэш текстур при перезагрузке ресурсов
        try {
            CardTextures.clearCache();
            ModLogger.logGameEvent("Texture cache cleared on resource reload");
        } catch (Exception e) {
            ModLogger.warn("Error clearing texture cache on resource reload", 
                "error", e.getMessage());
        }
    }
}

