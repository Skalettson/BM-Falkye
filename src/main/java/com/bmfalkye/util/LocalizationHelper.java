package com.bmfalkye.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Вспомогательный класс для работы с локализацией
 * 
 * <p>Предоставляет методы для получения локализованных строк и компонентов.
 * Поддерживает несколько языков и автоматическое определение языка игрока.</p>
 * 
 * @author BM Falkye Team
 */
public class LocalizationHelper {
    
    /** Текущий язык по умолчанию */
    private static final String DEFAULT_LANGUAGE = "ru_RU";
    
    /** Текущий язык */
    private static String currentLanguage = DEFAULT_LANGUAGE;
    
    /**
     * Устанавливает язык для локализации
     * 
     * @param language код языка (например, "ru_RU", "en_US")
     */
    public static void setLanguage(String language) {
        if (language != null && !language.isEmpty()) {
            currentLanguage = language;
        }
    }
    
    /**
     * Получает текущий язык
     * 
     * @return код текущего языка
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Получает язык игрока
     * 
     * @param player игрок
     * @return код языка игрока или язык по умолчанию
     */
    public static String getPlayerLanguage(ServerPlayer player) {
        if (player == null) {
            return DEFAULT_LANGUAGE;
        }
        
        // Пытаемся получить язык из настроек игрока
        try {
            String langCode = player.getLanguage();
            if (langCode != null && !langCode.isEmpty()) {
                // Конвертируем код языка Minecraft в наш формат
                // Поддерживаемые языки: ru_ru, en_us
                if (langCode.startsWith("ru") || langCode.equals("ru_ru")) {
                    return "ru_ru";
                } else if (langCode.startsWith("en") || langCode.equals("en_us")) {
                    return "en_us";
                }
            }
        } catch (Exception e) {
            ModLogger.warn("Failed to get player language", "player", player.getName().getString());
        }
        
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * Добавляет поддержку нового языка
     * 
     * <p>Для добавления нового языка необходимо:
     * 1. Создать файл локализации в src/main/resources/assets/bm_falkye/lang/{язык}.json
     * 2. Добавить все ключи из ru_ru.json
     * 3. Перевести все значения на новый язык
     * 4. Добавить язык в SUPPORTED_LANGUAGES в LocalizationChecker
     * 
     * @param languageCode код языка (например, "de_de", "fr_fr")
     * @return true если язык успешно добавлен
     */
    public static boolean addLanguageSupport(String languageCode) {
        // Языки автоматически определяются из файлов локализации
        // Этот метод можно использовать для валидации или дополнительной настройки
        ModLogger.info("Language support check", "language", languageCode);
        return true;
    }
    
    /**
     * Получает локализованную строку
     * 
     * @param key ключ локализации
     * @param args аргументы для подстановки
     * @return локализованная строка
     */
    public static String getLocalizedString(String key, Object... args) {
        // Используем стандартную систему локализации Minecraft
        String translationKey = "bm_falkye." + key;
        
        // Пытаемся получить перевод через Minecraft
        try {
            // I18n - это статический класс, используем статические методы
            if (net.minecraft.client.resources.language.I18n.exists(translationKey)) {
                Object[] safeArgs = (args != null && args.length > 0) ? args : new Object[0];
                String translated = net.minecraft.client.resources.language.I18n.get(translationKey, safeArgs);
                return translated;
            }
        } catch (Exception e) {
            // На сервере I18n может быть недоступен
        }
        
        // Fallback: возвращаем ключ, если перевод не найден
        if (args != null && args.length > 0) {
            return key + " [" + String.join(", ", java.util.Arrays.stream(args)
                .map(Object::toString)
                .toArray(String[]::new)) + "]";
        }
        return key;
    }
    
    /**
     * Получает локализованный компонент
     * 
     * @param key ключ локализации
     * @param args аргументы для подстановки
     * @return локализованный компонент
     */
    public static Component getLocalizedComponent(String key, Object... args) {
        String translationKey = "bm_falkye." + key;
        
        if (args == null || args.length == 0) {
            return Component.translatable(translationKey);
        } else {
            Component[] componentArgs = new Component[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component) {
                    componentArgs[i] = (Component) args[i];
                } else {
                    String argStr = args[i] != null ? String.valueOf(args[i]) : "";
                    if (argStr != null) {
                        componentArgs[i] = Component.literal(argStr);
                    } else {
                        componentArgs[i] = Component.literal("");
                    }
                }
            }
            return Component.translatable(translationKey, (Object[]) componentArgs);
        }
    }
    
    /**
     * Отправляет локализованное сообщение игроку
     * 
     * @param player игрок
     * @param key ключ локализации
     * @param args аргументы для подстановки
     */
    public static void sendLocalizedMessage(ServerPlayer player, String key, Object... args) {
        if (player == null) {
            return;
        }
        
        Component message = getLocalizedComponent(key, args);
        if (message != null) {
            player.sendSystemMessage(message);
        }
    }
    
    /**
     * Получает локализованное сообщение об ошибке
     * 
     * @param errorKey ключ ошибки
     * @param args аргументы для подстановки
     * @return локализованное сообщение об ошибке
     */
    public static Component getLocalizedError(String errorKey, Object... args) {
        return getLocalizedComponent("error." + errorKey, args);
    }
    
    /**
     * Получает локализованное сообщение об успехе
     * 
     * @param successKey ключ успеха
     * @param args аргументы для подстановки
     * @return локализованное сообщение об успехе
     */
    public static Component getLocalizedSuccess(String successKey, Object... args) {
        return getLocalizedComponent("success." + successKey, args);
    }
    
    /**
     * Получает локализованное информационное сообщение
     * 
     * @param infoKey ключ информации
     * @param args аргументы для подстановки
     * @return локализованное информационное сообщение
     */
    public static Component getLocalizedInfo(String infoKey, Object... args) {
        return getLocalizedComponent("info." + infoKey, args);
    }
}

