package com.bmfalkye.client.settings;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.settings.GameModeSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран настроек игры
 */
public class GameSettingsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 400;
    private static final int MIN_GUI_WIDTH = 500;
    private static final int MIN_GUI_HEIGHT = 300;
    private static final double MAX_SCREEN_RATIO = 0.8;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    private final Screen parentScreen;
    
    public GameSettingsScreen(Screen parentScreen) {
        super(Component.literal("§6§lНАСТРОЙКИ ИГРЫ"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT,
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
        
        // Заголовок
        int titleY = guiY + 20;
        
        // Кнопка "Назад"
        Button backButton = Button.builder(
            Component.literal("§7Назад"),
            (btn) -> {
                if (parentScreen != null) {
                    minecraft.setScreen(parentScreen);
                } else {
                    minecraft.setScreen(null);
                }
            })
            .bounds(guiX + 50, guiY + GUI_HEIGHT - 50, 100, 30)
            .build();
        this.addRenderableWidget(backButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
        
        // Рисуем фон
        graphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xC0101010);
        graphics.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + GUI_HEIGHT - 1, 0xFF2C2C2C);
        
        // Заголовок
        graphics.drawString(font, "§6§lНАСТРОЙКИ ИГРЫ", guiX + 20, guiY + 20, 0xFFFFFF, false);
        
        // Информация о режимах
        graphics.drawString(font, "§eРежим игры выбирается в настройках матча", guiX + 50, guiY + 50, 0xFFFFFF, false);
        graphics.drawString(font, "§72D - Классический режим с картами", guiX + 70, guiY + 160, 0x888888, false);
        graphics.drawString(font, "§73D - Режим с 3D юнитами на поле", guiX + 70, guiY + 180, 0x888888, false);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}

