package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.team.TeamGameSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * GUI для командного режима 2v2
 */
public class TeamGameScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 1200;
    private static final int BASE_GUI_HEIGHT = 800;
    private static final int MIN_GUI_WIDTH = 1000;
    private static final int MIN_GUI_HEIGHT = 700;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private TeamGameSession session;
    
    public TeamGameScreen(TeamGameSession session) {
        super(Component.literal("§6§lКОМАНДНЫЙ РЕЖИМ 2v2"));
        this.session = session;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT,
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Кнопка "Пас"
        Button passButton = createStyledButton(
            layout.getX(80), layout.getY(85), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§7Пас"),
            (btn) -> {
                // Отправляем пакет паса
                // TODO: Реализовать сетевой пакет
            }
        );
        this.addRenderableWidget(passButton);
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
        graphics.drawString(font, "§6§lКОМАНДНЫЙ РЕЖИМ 2v2", guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        // Рисуем поле (4 ряда)
        renderField(graphics, guiX, guiY);
        
        // Рисуем руки команд
        renderHands(graphics, guiX, guiY);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderField(GuiGraphics graphics, int guiX, int guiY) {
        // Рисуем 4 ряда для каждой команды
        // TODO: Реализовать визуализацию поля
    }
    
    private void renderHands(GuiGraphics graphics, int guiX, int guiY) {
        // Рисуем общие руки команд
        // TODO: Реализовать визуализацию рук
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
}

