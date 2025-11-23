package com.bmfalkye.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Кастомная кнопка коллекции карт со стилем мода
 */
public class StyledCardCollectionButton extends Button {
    public StyledCardCollectionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Используем кастомный рендеринг со стилем мода вместо стандартного
        if (this.visible) {
            GuiUtils.renderStyledButton(
                guiGraphics,
                net.minecraft.client.Minecraft.getInstance().font,
                this,
                mouseX,
                mouseY,
                false
            );
        }
    }
}

