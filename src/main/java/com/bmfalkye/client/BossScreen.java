package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран выбора боссов
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class BossScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 750;
    private static final int BASE_GUI_HEIGHT = 550;
    private static final int MIN_GUI_WIDTH = 650;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private Screen parentScreen;
    private int scrollOffset = 0;
    private com.bmfalkye.network.BossPackets.SendBossesPacket bossesData = null;
    
    private static final int BOSS_ENTRY_HEIGHT = 90;
    private static final int BOSS_ENTRY_SPACING = 10;
    
    public BossScreen(Screen parent) {
        super(Component.translatable("screen.bm_falkye.bosses_title"));
        this.parentScreen = parent;
    }
    
    public void updateBossesList(com.bmfalkye.network.BossPackets.SendBossesPacket data) {
        this.bossesData = data;
        this.scrollOffset = 0;
        this.init();
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getBottomY(layout.getHeight(5), 2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                this.onClose();
            }
        );
        this.addRenderableWidget(backButton);
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                requestBossesList();
            }
        );
        this.addRenderableWidget(refreshButton);
        
        // Запрашиваем список боссов при открытии
        if (bossesData == null) {
            requestBossesList();
        }
    }
    
    private void requestBossesList() {
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.BossPackets.RequestBossesPacket());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int GUI_WIDTH = layout.getGuiWidth();
        int GUI_HEIGHT = layout.getGuiHeight();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable("screen.bm_falkye.bosses_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Разделитель
        int dividerY = layout.getY(8);
        guiGraphics.fill(guiX + layout.getWidth(5), dividerY, 
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFF0000);
        
        // Область списка боссов
        int listY = layout.getY(11);
        int listHeight = GUI_HEIGHT - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = GUI_WIDTH - listX * 2;
        
        // Обрезка списка боссов
        guiGraphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        
        if (bossesData == null || bossesData.getBossIds() == null || bossesData.getBossIds().isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_bosses_available")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), listY + listHeight / 2, 0xAAAAAA);
        } else {
            renderBossesList(guiGraphics, listX, listY, listWidth, listHeight, mouseX, mouseY);
        }
        
        guiGraphics.disableScissor();
        
        // Индикатор прокрутки
        if (bossesData != null && bossesData.getBossIds() != null && !bossesData.getBossIds().isEmpty()) {
            int visibleCount = listHeight / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
            int totalCount = bossesData.getBossIds().size();
            
            if (totalCount > visibleCount) {
                int scrollBarX = layout.getRightX(4, 2);
                int scrollBarY = listY;
                int scrollBarHeight = listHeight;
                int scrollBarWidth = 4;
                
                // Фон полосы прокрутки
                guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                    scrollBarY + scrollBarHeight, 0x66000000);
                
                // Ползунок
                int maxScroll = Math.max(1, totalCount - visibleCount);
                int sliderHeight = Math.max(20, (int)((double)visibleCount / totalCount * scrollBarHeight));
                int scrollStep = BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING;
                int currentScrollIndex = scrollOffset / scrollStep;
                int sliderY = scrollBarY + (int)((double)currentScrollIndex / maxScroll * (scrollBarHeight - sliderHeight));
                guiGraphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                    sliderY + sliderHeight, 0xFF8B7355);
            }
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderBossesList(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                  int mouseX, int mouseY) {
        int visibleCount = height / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
        int startIndex = scrollOffset / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, bossesData.getBossIds().size());
        
        int bossY = y - (scrollOffset % (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= bossesData.getBossIds().size()) break;
            
            String bossId = bossesData.getBossIds().get(i);
            String bossName = i < bossesData.getBossNames().size() ? 
                bossesData.getBossNames().get(i) : 
                Component.translatable("screen.bm_falkye.unknown_boss").getString();
            String description = i < bossesData.getBossDescriptions().size() ? 
                bossesData.getBossDescriptions().get(i) : "";
            int difficulty = i < bossesData.getBossDifficulties().size() ? 
                bossesData.getBossDifficulties().get(i) : 0;
            boolean unlocked = i < bossesData.getBossUnlocked().size() ? 
                bossesData.getBossUnlocked().get(i) : false;
            
            boolean isHovered = mouseX >= x && mouseX <= x + width && 
                               mouseY >= bossY && mouseY <= bossY + BOSS_ENTRY_HEIGHT;
            
            // Фон записи босса
            int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
            if (!unlocked) {
                bgColor = isHovered ? 0xAA888888 : 0x66444444;
            }
            GuiUtils.drawLeatherElement(guiGraphics, x, bossY, width, BOSS_ENTRY_HEIGHT);
            // Оверлей
            guiGraphics.fill(x + 2, bossY + 2, x + width - 2, bossY + BOSS_ENTRY_HEIGHT - 2, bgColor);
            GuiUtils.drawMetalFrame(guiGraphics, x, bossY, width, BOSS_ENTRY_HEIGHT, 2, false);
            
            int textX = x + 10;
            int textY = bossY + 10;
            int lineHeight = 15;
            
            // Название босса
            MutableComponent name = Component.literal(bossName)
                .withStyle(Style.EMPTY
                    .withColor(unlocked ? ChatFormatting.RED : ChatFormatting.GRAY)
                    .withBold(true));
            guiGraphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
            textY += lineHeight + 2;
            
            // Описание
            if (!description.isEmpty()) {
                String desc = description;
                int maxDescWidth = width - 140;
                if (this.font.width(desc) > maxDescWidth) {
                    desc = this.font.plainSubstrByWidth(desc, maxDescWidth - 5) + "...";
                }
                MutableComponent descText = Component.literal(desc)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                guiGraphics.drawString(this.font, descText, textX, textY, 0xFFFFFF, false);
                textY += lineHeight;
            }
            
            // Сложность
            MutableComponent difficultyText = Component.translatable("screen.bm_falkye.difficulty", 
                difficulty, 10)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            guiGraphics.drawString(this.font, difficultyText, textX, textY, 0xFFFFFF, false);
            
            // Кнопка "Вызвать"
            int buttonX = x + width - 120;
            int buttonY = bossY + 10;
            int buttonWidth = 110;
            int buttonHeight = 30;
            
            boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                                   mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            
            int buttonColor = unlocked ? (buttonHovered ? 0xAAFF0000 : 0xAA880000) : 
                              (buttonHovered ? 0xAA444444 : 0xAA222222);
            GuiUtils.drawRoundedRect(guiGraphics, buttonX, buttonY, buttonWidth, buttonHeight, buttonColor);
            GuiUtils.drawRoundedBorder(guiGraphics, buttonX, buttonY, buttonWidth, buttonHeight, 
                unlocked ? 0xFFFF0000 : 0xFF555555, 1);
            
            MutableComponent buttonText = Component.translatable(unlocked ? 
                "screen.bm_falkye.challenge" : "screen.bm_falkye.locked")
                .withStyle(Style.EMPTY
                    .withColor(unlocked ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            int buttonTextX = buttonX + (buttonWidth - this.font.width(buttonText)) / 2;
            guiGraphics.drawString(this.font, buttonText, buttonTextX, buttonY + 10, 0xFFFFFF, false);
            
            bossY += BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && bossesData != null && bossesData.getBossIds() != null && 
            !bossesData.getBossIds().isEmpty()) {
            
            int listY = layout.getY(11);
            int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
            int listX = layout.getX(3);
            int listWidth = layout.getGuiWidth() - listX * 2;
            
            int visibleCount = listHeight / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
            int startIndex = scrollOffset / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, bossesData.getBossIds().size());
            
            int bossY = listY - (scrollOffset % (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= bossesData.getBossIds().size()) break;
                
                String bossId = bossesData.getBossIds().get(i);
                boolean unlocked = i < bossesData.getBossUnlocked().size() ? 
                    bossesData.getBossUnlocked().get(i) : false;
                
                int buttonX = listX + listWidth - 120;
                int buttonY = bossY + 10;
                int buttonWidth = 110;
                int buttonHeight = 30;
                
                if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    
                    if (unlocked) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                            new com.bmfalkye.network.BossPackets.ChallengeBossPacket(bossId));
                    } else {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        if (minecraft != null && minecraft.player != null) {
                            MutableComponent message = Component.translatable("screen.bm_falkye.boss_locked")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                            minecraft.player.sendSystemMessage(message);
                        }
                    }
                    return true;
                }
                
                bossY += BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || bossesData == null || bossesData.getBossIds() == null || 
            bossesData.getBossIds().isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int listY = layout.getY(11);
        int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = layout.getGuiWidth() - listX * 2;
        
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleCount = listHeight / (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
            int totalCount = bossesData.getBossIds().size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING);
            int scrollStep = BOSS_ENTRY_HEIGHT + BOSS_ENTRY_SPACING;
            
            if (delta < 0 && scrollOffset < maxScroll) {
                scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep);
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset = Math.max(0, scrollOffset - scrollStep);
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        if (parentScreen != null && minecraft != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
