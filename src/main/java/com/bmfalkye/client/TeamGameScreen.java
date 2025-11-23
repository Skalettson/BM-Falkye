package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.team.TeamGameSession;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

/**
 * GUI для командного режима 2v2
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class TeamGameScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 1200;
    private static final int BASE_GUI_HEIGHT = 800;
    private static final int MIN_GUI_WIDTH = 1000;
    private static final int MIN_GUI_HEIGHT = 700;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private TeamGameSession session;
    
    public TeamGameScreen(TeamGameSession session) {
        super(Component.translatable("screen.bm_falkye.team_game_title"));
        this.session = session;
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT,
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Пас"
        Button passButton = GuiUtils.createStyledButton(
            layout.getX(80), layout.getY(85), layout.getWidth(15), layout.getHeight(5),
            Component.translatable("button.bm_falkye.pass").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                // Отправляем пакет паса
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.PassPacket());
            }
        );
        this.addRenderableWidget(passButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        }
        
        this.renderBackground(graphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int GUI_WIDTH = layout.getGuiWidth();
        int GUI_HEIGHT = layout.getGuiHeight();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable("screen.bm_falkye.team_game_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Разделитель
        int dividerY = layout.getY(8);
        graphics.fill(guiX + layout.getWidth(5), dividerY, 
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFFA500);
        
        // Рисуем поле (4 ряда)
        renderField(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT);
        
        // Рисуем руки команд
        renderHands(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT);
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderField(GuiGraphics graphics, int guiX, int guiY, int width, int height) {
        if (session == null) return;
        
        // Рисуем 4 ряда для каждой команды (ближний, дальний, осада, лидер)
        int fieldAreaY = layout.getY(12);
        int fieldAreaHeight = layout.getGuiHeight() - fieldAreaY - layout.getHeight(25);
        int fieldAreaWidth = layout.getGuiWidth() - layout.getX(5) * 2;
        
        // Разделение на 2 команды (сверху и снизу)
        int team1Y = fieldAreaY;
        int team2Y = fieldAreaY + fieldAreaHeight / 2;
        int rowHeight = (fieldAreaHeight / 2 - 20) / 4; // 4 ряда + отступы
        
        // Команда 1 (сверху)
        renderTeamRows(graphics, guiX + layout.getX(5), team1Y, fieldAreaWidth, rowHeight, 1, session);
        
        // Разделитель между командами
        int dividerY = team2Y - 5;
        graphics.fill(guiX + layout.getX(5), dividerY, 
            guiX + layout.getX(5) + fieldAreaWidth, dividerY + 2, 0xFF8B7355);
        
        // Команда 2 (снизу)
        renderTeamRows(graphics, guiX + layout.getX(5), team2Y, fieldAreaWidth, rowHeight, 2, session);
    }
    
    private void renderTeamRows(GuiGraphics graphics, int x, int y, int width, int rowHeight, 
                                int teamNumber, TeamGameSession session) {
        // Рендерим 4 ряда: ближний, дальний, осада, лидер
        String[] rowNames = {"screen.bm_falkye.melee_row", "screen.bm_falkye.ranged_row", 
                            "screen.bm_falkye.siege_row", "screen.bm_falkye.leader_row"};
        
        for (int i = 0; i < 4; i++) {
            int rowY = y + i * (rowHeight + 5);
            
            // Название ряда
            MutableComponent rowName = Component.translatable(rowNames[i])
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(i == 3)); // Лидер выделен
            graphics.drawString(this.font, rowName, x + 5, rowY + 5, 0xFFFFFF, false);
            
            // Фон ряда
            GuiUtils.drawRoundedRect(graphics, x + 5, rowY + 15, width - 10, rowHeight - 15, 0x33000000);
            GuiUtils.drawRoundedBorder(graphics, x + 5, rowY + 15, width - 10, rowHeight - 15, 0xFF8B7355, 1);
            
            // TODO: Рендерить карты команды в этом ряду
            // Это требует доступа к данным сессии, которые нужно получить из TeamGameSession
            MutableComponent placeholder = Component.translatable("screen.bm_falkye.team_placeholder")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, placeholder, x + 15, rowY + 25, 0x888888, false);
        }
    }
    
    private void renderHands(GuiGraphics graphics, int guiX, int guiY, int width, int height) {
        if (session == null) return;
        
        // Рисуем общие руки команд внизу
        int handsY = layout.getBottomY(layout.getHeight(18), 2);
        int handsHeight = layout.getHeight(18);
        int handsWidth = layout.getGuiWidth() - layout.getX(5) * 2;
        int handsX = guiX + layout.getX(5);
        
        // Фон руки
        GuiUtils.drawLeatherElement(graphics, handsX, handsY, handsWidth, handsHeight);
        GuiUtils.drawMetalFrame(graphics, handsX, handsY, handsWidth, handsHeight, 2, false);
        
        // Заголовок руки
        MutableComponent handTitle = Component.translatable("screen.bm_falkye.team_hand")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        graphics.drawString(this.font, handTitle, handsX + 10, handsY + 5, 0xFFFFFF, false);
        
        // TODO: Рендерить карты в руке команды
        MutableComponent placeholder = Component.translatable("screen.bm_falkye.hand_placeholder")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        graphics.drawString(this.font, placeholder, handsX + 10, handsY + 20, 0x888888, false);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
