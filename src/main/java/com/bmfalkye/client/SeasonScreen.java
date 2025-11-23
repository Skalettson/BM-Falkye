package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

/**
 * Красивый экран сезона с визуализацией прогресса
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class SeasonScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    
    private int seasonNumber = 1;
    private int seasonLevel = 1;
    private int seasonXP = 0;
    private int xpForNextLevel = 100;
    private int daysRemaining = 30;
    
    private boolean dataLoaded = false;
    private final Screen parentScreen;
    
    public SeasonScreen() {
        this(null);
    }
    
    public SeasonScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.season_title"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestSeasonPacket());
        }
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getBottomY(layout.getHeight(5), 2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    this.onClose();
                }
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
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestSeasonPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    public void updateSeason(FriendlyByteBuf data) {
        seasonNumber = data.readInt();
        seasonLevel = data.readInt();
        seasonXP = data.readInt();
        xpForNextLevel = data.readInt();
        daysRemaining = data.readInt();
        dataLoaded = true;
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
        MutableComponent title = Component.translatable("screen.bm_falkye.season_number", seasonNumber)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFF4CAF50);
        
        int contentY = layout.getY(11);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(10);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_season_data")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), contentY + contentHeight / 2, 0xFFFFFF);
        } else {
            renderSeasonContent(guiGraphics, guiX, contentY, GUI_WIDTH, contentHeight);
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderSeasonContent(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int contentX = layout.getX(5);
        int currentY = y;
        int lineHeight = 16;
        int sectionSpacing = 20;
        
        // Осталось дней
        MutableComponent daysRemainingText = Component.translatable("screen.bm_falkye.days_remaining", daysRemaining)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, daysRemainingText, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + sectionSpacing;
        
        // Уровень сезона
        MutableComponent levelText = Component.translatable("screen.bm_falkye.season_level", 
            seasonLevel, 30)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
        guiGraphics.drawString(this.font, levelText, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 10;
        
        // Прогресс-бар уровня
        int progressBarWidth = width - contentX * 2;
        int progressBarHeight = 22;
        int progressBarX = contentX;
        int progressBarY = currentY;
        
        // Фон прогресс-бара
        GuiUtils.drawRoundedRect(guiGraphics, progressBarX, progressBarY, 
            progressBarWidth, progressBarHeight, 0xFF2A2A2A);
        
        // Заполнение прогресс-бара
        float progress = seasonLevel >= 30 ? 1.0f : 
            (xpForNextLevel > 0 ? (float) seasonXP / xpForNextLevel : 0f);
        int filledWidth = (int)(progress * progressBarWidth);
        if (filledWidth > 0) {
            GuiUtils.drawRoundedRect(guiGraphics, progressBarX, progressBarY, 
                filledWidth, progressBarHeight, 0xFF4CAF50);
        }
        
        // Рамка прогресс-бара
        GuiUtils.drawRoundedBorder(guiGraphics, progressBarX, progressBarY, 
            progressBarWidth, progressBarHeight, 0xFF555555, 1);
        
        // Текст прогресса
        MutableComponent progressText;
        if (seasonLevel >= 30) {
            progressText = Component.translatable("screen.bm_falkye.max_level")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
        } else {
            progressText = Component.translatable("screen.bm_falkye.progress_xp", 
                seasonXP, xpForNextLevel)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        }
        int textWidth = this.font.width(progressText);
        int textX = progressBarX + (progressBarWidth - textWidth) / 2;
        int textY = progressBarY + (progressBarHeight - 9) / 2;
        guiGraphics.drawString(this.font, progressText, textX, textY, 0xFFFFFF, false);
        
        currentY += progressBarHeight + sectionSpacing;
        
        // Визуализация уровней
        MutableComponent levelsTitle = Component.translatable("screen.bm_falkye.levels_progress")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, levelsTitle, contentX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 10;
        
        int levelsAreaY = currentY;
        int levelsAreaHeight = height - (currentY - y) - 30;
        
        // Обрезка уровней
        guiGraphics.enableScissor(contentX, levelsAreaY, 
            x + width - layout.getWidth(5), levelsAreaY + levelsAreaHeight);
        
        int levelsPerRow = 10;
        int levelCardSize = Math.min((width - contentX * 2 - layout.getSpacing() * (levelsPerRow - 1)) / levelsPerRow, 
            (int)(levelsAreaHeight / 3));
        int levelSpacing = layout.getSpacing();
        int levelsStartX = contentX;
        
        for (int level = 1; level <= 30; level++) {
            int row = (level - 1) / levelsPerRow;
            int col = (level - 1) % levelsPerRow;
            int levelX = levelsStartX + col * (levelCardSize + levelSpacing);
            int levelY = levelsAreaY + row * (levelCardSize + levelSpacing);
            
            // Проверяем, виден ли уровень
            if (levelY + levelCardSize > levelsAreaY + levelsAreaHeight) {
                break;
            }
            
            // Цвет в зависимости от статуса уровня
            int color;
            int borderColor;
            if (level < seasonLevel) {
                color = 0xAA4CAF50; // Пройден
                borderColor = 0xFF66BB6A;
            } else if (level == seasonLevel) {
                color = 0xAAFFA500; // Текущий
                borderColor = 0xFFFFB74D;
            } else {
                color = 0xAA404040; // Недоступен
                borderColor = 0xFF505050;
            }
            
            // Карточка уровня
            GuiUtils.drawLeatherElement(guiGraphics, levelX, levelY, levelCardSize, levelCardSize);
            // Цветной оверлей
            guiGraphics.fill(levelX + 2, levelY + 2, levelX + levelCardSize - 2, 
                levelY + levelCardSize - 2, color);
            GuiUtils.drawMetalFrame(guiGraphics, levelX, levelY, levelCardSize, levelCardSize, 1, false);
            
            // Номер уровня
            String levelTextStr = String.valueOf(level);
            int levelTextX = levelX + (levelCardSize - this.font.width(levelTextStr)) / 2;
            int levelTextY = levelY + (levelCardSize - 9) / 2;
            ChatFormatting levelColor = level < seasonLevel ? ChatFormatting.WHITE : 
                                       (level == seasonLevel ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            guiGraphics.drawString(this.font, levelTextStr, levelTextX, levelTextY, 
                levelColor.getColor() != null ? levelColor.getColor() : 0xFFFFFF, false);
        }
        
        guiGraphics.disableScissor();
        
        // Легенда
        int legendY = levelsAreaY + levelsAreaHeight + 10;
        MutableComponent legend = Component.empty()
            .append(Component.literal("■ ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
            .append(Component.translatable("screen.bm_falkye.completed").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  "))
            .append(Component.literal("■ ").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
            .append(Component.translatable("screen.bm_falkye.current").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  "))
            .append(Component.literal("■ ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
            .append(Component.translatable("screen.bm_falkye.unavailable").withStyle(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, legend, contentX, legendY, 0xFFFFFF, false);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
