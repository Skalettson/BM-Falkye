package com.bmfalkye.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Проверяет отсутствующие переводы в файлах локализации
 * 
 * <p>Сравнивает файлы локализации разных языков и находит ключи,
 * которые отсутствуют в некоторых языках.</p>
 * 
 * @author BM Falkye Team
 */
public class LocalizationChecker {
    
    /** Список поддерживаемых языков */
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("ru_ru", "en_us");
    
    /** Язык по умолчанию для сравнения */
    private static final String DEFAULT_LANGUAGE = "ru_ru";
    
    /**
     * Проверяет отсутствующие переводы
     * 
     * @param resourceManager менеджер ресурсов Minecraft
     * @return карта языков к списку отсутствующих ключей
     */
    public static Map<String, List<String>> checkMissingTranslations(ResourceManager resourceManager) {
        Map<String, List<String>> missingTranslations = new HashMap<>();
        
        try {
            // Загружаем файл локализации по умолчанию
            ResourceLocation defaultLangFile = new ResourceLocation("bm_falkye", "lang/" + DEFAULT_LANGUAGE + ".json");
            Optional<net.minecraft.server.packs.resources.Resource> defaultResource = resourceManager.getResource(defaultLangFile);
            
            if (defaultResource.isEmpty()) {
                ModLogger.warn("Default language file not found", "language", DEFAULT_LANGUAGE);
                return missingTranslations;
            }
            
            JsonObject defaultLang = loadJsonFromResource(defaultResource.get());
            Set<String> defaultKeys = defaultLang.keySet();
            
            // Проверяем каждый язык
            for (String language : SUPPORTED_LANGUAGES) {
                if (language.equals(DEFAULT_LANGUAGE)) {
                    continue; // Пропускаем язык по умолчанию
                }
                
                ResourceLocation langFile = new ResourceLocation("bm_falkye", "lang/" + language + ".json");
                Optional<net.minecraft.server.packs.resources.Resource> langResource = resourceManager.getResource(langFile);
                
                if (langResource.isEmpty()) {
                    ModLogger.warn("Language file not found", "language", language);
                    missingTranslations.put(language, new ArrayList<>(defaultKeys));
                    continue;
                }
                
                JsonObject langJson = loadJsonFromResource(langResource.get());
                Set<String> langKeys = langJson.keySet();
                
                // Находим отсутствующие ключи
                List<String> missing = new ArrayList<>();
                for (String key : defaultKeys) {
                    if (!langKeys.contains(key)) {
                        missing.add(key);
                    }
                }
                
                if (!missing.isEmpty()) {
                    missingTranslations.put(language, missing);
                    ModLogger.warn("Missing translations found", 
                        "language", language, 
                        "missingCount", missing.size(),
                        "missingKeys", String.join(", ", missing.subList(0, Math.min(10, missing.size()))));
                }
            }
            
        } catch (Exception e) {
            ModLogger.error("Error checking missing translations", e);
        }
        
        return missingTranslations;
    }
    
    /**
     * Загружает JSON из ресурса
     */
    private static JsonObject loadJsonFromResource(net.minecraft.server.packs.resources.Resource resource) throws Exception {
        try (InputStream stream = resource.open();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.getAsJsonObject();
        }
    }
    
    /**
     * Генерирует отчёт об отсутствующих переводах
     * 
     * @param missingTranslations карта языков к списку отсутствующих ключей
     * @return строковое представление отчёта
     */
    public static String generateReport(Map<String, List<String>> missingTranslations) {
        if (missingTranslations.isEmpty()) {
            return "✓ Все переводы присутствуют!";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Отсутствующие переводы:\n\n");
        
        for (Map.Entry<String, List<String>> entry : missingTranslations.entrySet()) {
            String language = entry.getKey();
            List<String> missing = entry.getValue();
            
            report.append(String.format("Язык: %s (%d отсутствующих ключей)\n", language, missing.size()));
            for (String key : missing) {
                report.append(String.format("  - %s\n", key));
            }
            report.append("\n");
        }
        
        return report.toString();
    }
}

