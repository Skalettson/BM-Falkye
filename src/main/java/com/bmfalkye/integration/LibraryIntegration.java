package com.bmfalkye.integration;

import com.bmfalkye.util.ModLogger;

/**
 * Центральный класс для интеграции с внешними библиотеками
 */
public class LibraryIntegration {
    private static boolean geckoLibLoaded = false;
    private static boolean patchouliLoaded = false;
    private static boolean creativeCoreLoaded = false;
    private static boolean citadelLoaded = false;
    
    /**
     * Инициализирует все интеграции
     */
    public static void init() {
        ModLogger.info("=== Initializing Library Integrations ===");
        
        checkGeckoLib();
        checkPatchouli();
        checkCreativeCore();
        checkCitadel();
        
        ModLogger.info("GeckoLib: {}", geckoLibLoaded ? "LOADED" : "NOT FOUND");
        ModLogger.info("Patchouli: {}", patchouliLoaded ? "LOADED" : "NOT FOUND");
        ModLogger.info("CreativeCore: {}", creativeCoreLoaded ? "LOADED" : "NOT FOUND");
        ModLogger.info("Citadel: {}", citadelLoaded ? "LOADED" : "NOT FOUND");
    }
    
    private static void checkGeckoLib() {
        try {
            Class.forName("software.bernie.geckolib.GeckoLib");
            geckoLibLoaded = true;
            ModLogger.info("GeckoLib integration enabled");
        } catch (ClassNotFoundException e) {
            ModLogger.warn("GeckoLib not found - animations will use fallback");
        }
    }
    
    private static void checkPatchouli() {
        try {
            Class.forName("vazkii.patchouli.api.PatchouliAPI");
            patchouliLoaded = true;
            ModLogger.info("Patchouli integration enabled");
        } catch (ClassNotFoundException e) {
            ModLogger.warn("Patchouli not found - documentation will use fallback");
        }
    }
    
    private static void checkCreativeCore() {
        try {
            Class.forName("com.creativemd.creativecore.common.gui.GuiLayer");
            creativeCoreLoaded = true;
            ModLogger.info("CreativeCore integration enabled");
        } catch (ClassNotFoundException e) {
            ModLogger.warn("CreativeCore not found - GUI will use fallback");
        }
    }
    
    private static void checkCitadel() {
        try {
            Class.forName("org.antlr.v4.runtime.atn.ATN");
            citadelLoaded = true;
            ModLogger.info("Citadel integration enabled");
        } catch (ClassNotFoundException e) {
            ModLogger.warn("Citadel not found - entity animations will use fallback");
        }
    }
    
    public static boolean isGeckoLibLoaded() {
        return geckoLibLoaded;
    }
    
    public static boolean isPatchouliLoaded() {
        return patchouliLoaded;
    }
    
    public static boolean isCreativeCoreLoaded() {
        return creativeCoreLoaded;
    }
    
    public static boolean isCitadelLoaded() {
        return citadelLoaded;
    }
}

