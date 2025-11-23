package com.bmfalkye.client.gui;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Простой рендерер карт в пиксельном стиле
 * Улучшенная детализация старого стиля
 */
public class SimpleCardRenderer {
    
    /**
     * Рендерит карту в простом пиксельном стиле с улучшенной детализацией
     */
    public static void renderCard(GuiGraphics guiGraphics, Font font, Card card, 
                                 int x, int y, int width, int height,
                                 int mouseX, int mouseY, boolean selected, boolean showTooltip) {
        renderCard(guiGraphics, font, card, x, y, width, height, mouseX, mouseY, selected, showTooltip, null);
    }
    
    /**
     * Рендерит карту с использованием текстур в стиле ККИ
     * Текстура используется как фон/рамка, все элементы рендерятся поверх
     */
    public static void renderCard(GuiGraphics guiGraphics, Font font, Card card, 
                                 int x, int y, int width, int height,
                                 int mouseX, int mouseY, boolean selected, boolean showTooltip, Integer effectivePower) {
        if (card == null) return;
        
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        // Используем текстуру карты по редкости как фон/рамку
        ResourceLocation cardTexture = GameTextures.getCardTextureByRarity(card.getRarity());
        if (CardTextures.textureExists(cardTexture)) {
            // Рендерим текстуру карты (фон и рамка)
            guiGraphics.blit(cardTexture, x, y, 0, 0, width, height, width, height);
        } else {
            // Fallback: простой фон
            int rarityColor = card.getRarity().getColor();
            guiGraphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
            guiGraphics.fill(x, y, x + width, y + 2, rarityColor);
            guiGraphics.fill(x, y + height - 2, x + width, y + height, rarityColor);
            guiGraphics.fill(x, y, x + 2, y + height, rarityColor);
            guiGraphics.fill(x + width - 2, y, x + width, y + height, rarityColor);
        }
        
        // Адаптивные размеры для элементов карты
        float scale = Math.min(1.0f, Math.max(0.5f, (float)width / 64.0f));
        
        // 1. СИЛА - внутри ромбика текстуры (белый/серый текст)
        // Ромбик обычно находится примерно на 10-15% высоты карты от верха
        int power = effectivePower != null ? effectivePower : card.getPower();
        int powerY = y + (int)(height * 0.12f); // Примерно 12% от высоты карты - внутри ромбика
        // Используем белый цвет для силы
        guiGraphics.drawCenteredString(font, 
            Component.literal("§f" + power), 
            x + width / 2, powerY, 0xFFFFFF);
        
        // 2. НАЗВАНИЕ КАРТЫ - ниже силы (оранжевый текст)
        // Название должно быть внутри рамки текстуры и переноситься на несколько строк
        // Немного заходить на рамку допустимо
        String name = card.getName();
        int nameStartY = powerY + font.lineHeight + (int)(8 * scale); // Отступ от силы
        // Используем 90% ширины карты, чтобы название могло немного заходить на рамку
        int nameMaxWidth = (int)(width * 0.90f);
        // Вычисляем доступную высоту для названия (от начала до иконки типа)
        int iconAreaStartY = y + (int)(height * 0.65f); // Иконка типа примерно на 65% высоты
        int availableHeight = iconAreaStartY - nameStartY - (int)(4 * scale);
        
        // Разбиваем название на строки с переносом
        java.util.List<net.minecraft.util.FormattedCharSequence> nameLines = font.split(
            Component.literal("§6" + name), nameMaxWidth);
        
        // Рендерим название построчно, центрируя каждую строку
        int nameY = nameStartY;
        int lineSpacing = 2; // Отступ между строками
        int maxLines = Math.min(nameLines.size(), availableHeight / (font.lineHeight + lineSpacing));
        
        for (int i = 0; i < maxLines && i < nameLines.size(); i++) {
            net.minecraft.util.FormattedCharSequence line = nameLines.get(i);
            int lineWidth = font.width(line);
            // Центрируем строку по горизонтали
            int lineX = x + (width - lineWidth) / 2;
            // Рисуем черную тень для лучшей читаемости
            // Создаем черную версию текста для тени (без цветовых кодов)
            String lineText = line.toString();
            // Убираем все цветовые коды из текста для тени
            String shadowText = lineText.replaceAll("§[0-9a-fk-or]", "");
            net.minecraft.util.FormattedCharSequence shadowLine = Component.literal(shadowText).getVisualOrderText();
            guiGraphics.drawString(font, shadowLine, lineX + 1, nameY + 1, 0x000000, false);
            // Рисуем основной текст с цветом из форматирования (§6 - оранжевый)
            // Используем -1 как цвет, чтобы использовался цвет из FormattedCharSequence
            guiGraphics.drawString(font, line, lineX, nameY, -1, false);
            nameY += font.lineHeight + lineSpacing;
            
            // Проверяем, не выходим ли за пределы доступной области
            if (nameY + font.lineHeight > iconAreaStartY) {
                break;
            }
        }
        
        // 3. ИКОНКА ТИПА - в центре карты (зеленые скрещенные мечи)
        // Размещаем иконку примерно на 65% высоты карты
        String typeIcon = "§a⚔"; // Зеленые скрещенные мечи
        int iconY = y + (int)(height * 0.65f);
        guiGraphics.drawCenteredString(font, 
            Component.literal(typeIcon), 
            x + width / 2, iconY, 0xFFFFFF);
        
        // РЕДКОСТЬ НЕ РЕНДЕРИМ - она видна по текстуре карты
        
        // Эффекты при наведении и выборе
        if (isHovered) {
            guiGraphics.fill(x, y, x + width, y + height, 0x15FFFFFF);
        }
        if (selected) {
            guiGraphics.fill(x, y, x + width, y + height, 0x30FFFFFF);
            // Рамка выбора
            guiGraphics.fill(x - 2, y - 2, x + width + 2, y, 0xFFFFFFFF);
            guiGraphics.fill(x - 2, y + height, x + width + 2, y + height + 2, 0xFFFFFFFF);
            guiGraphics.fill(x - 2, y - 2, x, y + height + 2, 0xFFFFFFFF);
            guiGraphics.fill(x + width, y - 2, x + width + 2, y + height + 2, 0xFFFFFFFF);
        }
        
        // Рендерим tooltip при наведении, если требуется
        if (showTooltip && isHovered && mouseX >= 0 && mouseY >= 0) {
            renderCardTooltip(guiGraphics, font, card, mouseX, mouseY, effectivePower);
        }
    }
    
    /**
     * Получает текст редкости для отображения
     */
    private static String getRarityDisplayText(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> "ОБЫЧНАЯ";
            case RARE -> "Редкая";
            case EPIC -> "ЭПИЧЕСКАЯ";
            case LEGENDARY -> "Легендарный";
        };
    }
    
    /**
     * Получает цветовой код для редкости
     */
    private static String getRarityColorCode(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7"; // Серый
            case RARE -> "§b"; // Голубой
            case EPIC -> "§5"; // Фиолетовый
            case LEGENDARY -> "§6"; // Золотой
        };
    }
    
    /**
     * Рендерит tooltip для карты
     */
    private static void renderCardTooltip(GuiGraphics guiGraphics, Font font, Card card, 
                                         int mouseX, int mouseY, Integer effectivePower) {
        // Разбиваем описание на строки для корректного отображения
        int maxTooltipWidth = 200;
        java.util.List<net.minecraft.util.FormattedCharSequence> descriptionLines = font.split(
            Component.literal("§7" + card.getDescription()), maxTooltipWidth);
        
        // Собираем tooltip: название, описание, сила, тип, редкость
        java.util.List<net.minecraft.util.FormattedCharSequence> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.literal("§6" + card.getName()).getVisualOrderText());
        tooltip.addAll(descriptionLines);
        
        // Сила (используем эффективную, если указана)
        int power = effectivePower != null ? effectivePower : card.getPower();
        tooltip.add(Component.literal("§eСила: §f" + power).getVisualOrderText());
        
        // Тип карты
        String typeText = switch (card.getType()) {
            case CREATURE -> "§aСущество";
            case SPELL -> "§bЗаклинание";
            case SPECIAL -> "§dОсобая";
        };
        tooltip.add(Component.literal(typeText).getVisualOrderText());
        
        // Редкость
        tooltip.add(Component.literal("§7Редкость: " + card.getRarity().getDisplayName()).getVisualOrderText());
        
        guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }
}

