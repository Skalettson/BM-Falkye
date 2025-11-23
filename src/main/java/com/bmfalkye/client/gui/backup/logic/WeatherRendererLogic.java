package com.bmfalkye.client.gui.backup.logic;

import com.bmfalkye.game.FalkyeGameSession;

/**
 * БЭКАП ЛОГИКИ WeatherRenderer
 * 
 * Этот класс содержит всю логику работы с погодой БЕЗ визуального оформления.
 */
public class WeatherRendererLogic {
    
    /**
     * Получает название погоды
     */
    public static String getWeatherName(FalkyeGameSession.WeatherType weather) {
        return switch (weather) {
            case FROST -> "Мороз";
            case FOG -> "Туман";
            case RAIN -> "Дождь";
            case NONE -> "";
        };
    }
    
    /**
     * Получает описание эффекта погоды
     */
    public static String getWeatherEffect(FalkyeGameSession.WeatherType weather) {
        return switch (weather) {
            case FROST -> "Ближний бой: сила = 1";
            case FOG -> "Дальний бой: сила = 1";
            case RAIN -> "Осада: сила = 1";
            case NONE -> "";
        };
    }
    
    /**
     * Получает цветовой код для погоды
     */
    public static String getWeatherColorCode(FalkyeGameSession.WeatherType weather) {
        return switch (weather) {
            case FROST -> "§b"; // Голубой
            case FOG -> "§7"; // Серый
            case RAIN -> "§9"; // Синий
            case NONE -> "";
        };
    }
    
    /**
     * Получает иконку для погоды
     */
    public static String getWeatherIcon(FalkyeGameSession.WeatherType weather) {
        return switch (weather) {
            case FROST -> "❄";
            case FOG -> "☁";
            case RAIN -> "🌧";
            case NONE -> "";
        };
    }
    
    /**
     * Вычисляет интенсивность пульсации для анимации
     */
    public static float calculatePulseIntensity(long animationTime) {
        return (float) (0.5f + 0.3f * Math.sin(animationTime / 1000.0f));
    }
}

