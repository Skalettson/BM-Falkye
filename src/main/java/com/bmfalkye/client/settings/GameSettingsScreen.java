package com.bmfalkye.client.settings;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Экран настроек игры
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class GameSettingsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 700;
    private static final int BASE_GUI_HEIGHT = 500;
    private static final int MIN_GUI_WIDTH = 600;
    private static final int MIN_GUI_HEIGHT = 400;
    private static final double MAX_SCREEN_RATIO = 0.85;
    
    private AdaptiveLayout layout;
    private final Screen parentScreen;
    
    public GameSettingsScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.game_settings_title"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getCenteredX(layout.getWidth(20)), layout.getBottomY(layout.getHeight(6), 2), 
            layout.getWidth(20), layout.getHeight(6),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    minecraft.setScreen(null);
                }
            }
        );
        this.addRenderableWidget(backButton);
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
        MutableComponent title = Component.translatable("screen.bm_falkye.game_settings_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(5);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Разделитель
        int dividerY = layout.getY(10);
        graphics.fill(guiX + layout.getWidth(5), dividerY, 
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFFA500);
        
        // Контент
        int contentY = layout.getY(13);
        int contentX = layout.getX(5);
        int lineHeight = 18;
        int currentY = contentY;
        
        // Информация о режимах
        MutableComponent modesInfo = Component.translatable("screen.bm_falkye.game_mode_selection_info")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        graphics.drawString(this.font, modesInfo, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight * 2;
        
        // Разделитель
        graphics.fill(contentX, currentY, guiX + GUI_WIDTH - layout.getWidth(5), currentY + 1, 0xFF8B7355);
        currentY += 15;
        
        // Описание режимов
        MutableComponent mode2DTitle = Component.translatable("screen.bm_falkye.mode_2d_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        graphics.drawString(this.font, mode2DTitle, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        MutableComponent mode2DDesc = Component.translatable("screen.bm_falkye.mode_2d_description")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        List<net.minecraft.util.FormattedCharSequence> mode2DLines = this.font.split(mode2DDesc, GUI_WIDTH - contentX * 2);
        for (net.minecraft.util.FormattedCharSequence line : mode2DLines) {
            graphics.drawString(this.font, line, contentX, currentY, 0xFFFFFF, false);
            currentY += lineHeight;
        }
        currentY += 10;
        
        MutableComponent mode3DTitle = Component.translatable("screen.bm_falkye.mode_3d_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        graphics.drawString(this.font, mode3DTitle, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        MutableComponent mode3DDesc = Component.translatable("screen.bm_falkye.mode_3d_description")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        List<net.minecraft.util.FormattedCharSequence> mode3DLines = this.font.split(mode3DDesc, GUI_WIDTH - contentX * 2);
        for (net.minecraft.util.FormattedCharSequence line : mode3DLines) {
            graphics.drawString(this.font, line, contentX, currentY, 0xFFFFFF, false);
            currentY += lineHeight;
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
