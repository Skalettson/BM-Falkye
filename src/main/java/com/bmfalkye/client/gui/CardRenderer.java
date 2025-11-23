package com.bmfalkye.client.gui;

import com.bmfalkye.cards.Card;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * Рендерер карт
 * Делегирует рендеринг SimpleCardRenderer для обеспечения обратной совместимости
 */
public class CardRenderer {
    
    /**
     * Обновляет время анимации (не требуется для SimpleCardRenderer)
     */
    public static void updateAnimationTime() {
        // Не требуется для простого рендеринга
    }
    
    /**
     * Рендерит карту в простом пиксельном стиле
     * Делегирует рендеринг SimpleCardRenderer
     */
    public static void renderCard(GuiGraphics guiGraphics, Font font, Card card, 
                                 int x, int y, int width, int height,
                                 int mouseX, int mouseY, boolean selected, boolean showTooltip, Integer effectivePower) {
        SimpleCardRenderer.renderCard(guiGraphics, font, card, x, y, width, height,
                                      mouseX, mouseY, selected, showTooltip, effectivePower);
    }
    
    /**
     * Перегрузка метода для обратной совместимости (использует базовую силу)
     */
    public static void renderCard(GuiGraphics guiGraphics, Font font, Card card, 
                                 int x, int y, int width, int height,
                                 int mouseX, int mouseY, boolean selected, boolean showTooltip) {
        renderCard(guiGraphics, font, card, x, y, width, height, mouseX, mouseY, selected, showTooltip, null);
    }
}
