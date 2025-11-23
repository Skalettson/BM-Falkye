package com.bmfalkye.client.gui;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Улучшенный рендерер погодных эффектов с визуальными эффектами и анимациями
 * 
 * <p>Отображает текущую погоду и её влияние на силу карт в различных рядах:
 * <ul>
 *   <li>Мороз - снижает силу карт ближнего боя до 1</li>
 *   <li>Туман - снижает силу карт дальнего боя до 1</li>
 *   <li>Дождь - снижает силу карт осады до 1</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class WeatherRenderer {
    
    private static long animationTime = 0;
    
    /**
     * Рендерит информацию о текущей погоде и её эффектах.
     * 
     * @param guiGraphics графический контекст для рендеринга
     * @param font шрифт для отображения текста
     * @param session игровая сессия, из которой берётся информация о погоде
     * @param weatherX координата X левого верхнего угла панели погоды
     * @param weatherY координата Y левого верхнего угла панели погоды
     * @param weatherWidth ширина панели погоды
     * @param weatherHeight высота панели погоды
     */
    public static void render(GuiGraphics guiGraphics, Font font,
                             FalkyeGameSession.WeatherType weather,
                             int weatherX, int weatherY, int weatherWidth, int weatherHeight) {
        if (weather == FalkyeGameSession.WeatherType.NONE) {
            return;
        }
        
        updateAnimationTime();
        
        // Определяем цвета в зависимости от типа погоды
        int backgroundColor = switch (weather) {
            case FROST -> 0xE0102030; // Синеватый
            case FOG -> 0xE0202020; // Серый
            case RAIN -> 0xE0102030; // Синеватый
            default -> 0xE0101010;
        };
        
        int borderColor = switch (weather) {
            case FROST -> 0xFF44AAFF; // Голубой
            case FOG -> 0xFF888888; // Серый
            case RAIN -> 0xFF4488FF; // Синий
            default -> 0xFF8B7355;
        };
        
        // Анимированная рамка с пульсацией
        float pulse = (float) (0.5f + 0.3f * Math.sin(animationTime / 1000.0f));
        int animatedBorderColor = blendColor(borderColor, 0xFFFFFFFF, pulse * 0.2f);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле (кожаная панель для погодного эффекта)
        GuiUtils.drawLeatherElement(guiGraphics, weatherX, weatherY, weatherWidth, weatherHeight);
        
        // Металлическая рамка (золотая для особых эффектов)
        boolean isGold = weather == FalkyeGameSession.WeatherType.FROST;
        GuiUtils.drawMetalFrame(guiGraphics, weatherX, weatherY, weatherWidth, weatherHeight, 3, isGold);
        
        // Внутреннее свечение
        drawWeatherGlow(guiGraphics, weatherX, weatherY, weatherWidth, weatherHeight, weather, pulse);
        
        // Заголовок с эффектом
        String weatherText = switch (weather) {
            case FROST -> "§b❄ Мороз";
            case FOG -> "§7☁ Туман";
            case RAIN -> "§9🌧 Дождь";
            default -> "";
        };
        guiGraphics.drawString(font, 
            Component.literal(weatherText), 
            weatherX + 10, weatherY + 10, 0xFFFFFF, false);
        
        // Эффект
        String effect = switch (weather) {
            case FROST -> "§cБлижний бой: сила = 1";
            case FOG -> "§cДальний бой: сила = 1";
            case RAIN -> "§cОсада: сила = 1";
            default -> "";
        };
        guiGraphics.drawString(font, 
            Component.literal(effect), 
            weatherX + 10, weatherY + 25, 0xFFFFFF, false);
    }
    
    /**
     * Обновляет время анимации
     */
    private static void updateAnimationTime() {
        animationTime = System.currentTimeMillis();
    }
    
    /**
     * Рисует свечение для погодного эффекта
     */
    private static void drawWeatherGlow(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                       FalkyeGameSession.WeatherType weather, float pulse) {
        int glowColor = switch (weather) {
            case FROST -> 0xFF44AAFF;
            case FOG -> 0xFF888888;
            case RAIN -> 0xFF4488FF;
            default -> 0xFFFFFFFF;
        };
        
        int alpha = (int) (pulse * 50);
        int color = (glowColor & 0x00FFFFFF) | (alpha << 24);
        
        // Верх и низ
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 4, color);
        guiGraphics.fill(x + 2, y + height - 4, x + width - 2, y + height - 2, color);
        
        // Лево и право
        guiGraphics.fill(x + 2, y + 2, x + 4, y + height - 2, color);
        guiGraphics.fill(x + width - 4, y + 2, x + width - 2, y + height - 2, color);
    }
    
    /**
     * Смешивает два цвета
     */
    private static int blendColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        int a = (int) (a1 + (a2 - a1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

