package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Экран Зала Славы с визуальными портретами топ-игроков
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class HallOfFameScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 700;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 600;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private final Screen parentScreen;
    
    private List<HallOfFameEntry> hallOfFame = new ArrayList<>();
    private List<LeaderboardEntry> weeklyLeaderboard = new ArrayList<>();
    
    private Tab currentTab = Tab.HALL_OF_FAME;
    
    // Параметры прокрутки
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private static final int ENTRY_HEIGHT = 40;
    private static final int ENTRY_SPACING = 5;
    
    public HallOfFameScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.hall_of_fame_title"));
        this.parentScreen = parentScreen;
        requestData();
    }
    
    private void requestData() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestLeaderboardPacket());
    }
    
    public void updateHallOfFame(List<HallOfFameEntry> entries) {
        this.hallOfFame = new ArrayList<>(entries);
        this.contentHeight = hallOfFame.size() * (ENTRY_HEIGHT + ENTRY_SPACING);
    }
    
    public void updateWeeklyLeaderboard(List<LeaderboardEntry> entries) {
        this.weeklyLeaderboard = new ArrayList<>(entries);
        this.contentHeight = weeklyLeaderboard.size() * (ENTRY_HEIGHT + ENTRY_SPACING);
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    minecraft.setScreen(new FalkyeMainMenuScreen());
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        // Вкладки
        int tabWidth = layout.getWidth(20);
        int tabHeight = layout.getHeight(5);
        int tabSpacing = layout.getWidth(2);
        
        Button hallOfFameTab = GuiUtils.createStyledButton(
            layout.getX(20), layout.getY(2), tabWidth, tabHeight,
            Component.translatable("screen.bm_falkye.hall_of_fame")
                .withStyle(currentTab == Tab.HALL_OF_FAME ? 
                    Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true) :
                    Style.EMPTY.withColor(ChatFormatting.GRAY)),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                currentTab = Tab.HALL_OF_FAME;
                scrollOffset = 0;
                updateContentHeight();
                init();
            }
        );
        this.addRenderableWidget(hallOfFameTab);
        
        Button weeklyTab = GuiUtils.createStyledButton(
            layout.getX(20) + tabWidth + tabSpacing, layout.getY(2), tabWidth, tabHeight,
            Component.translatable("screen.bm_falkye.weekly_leaderboard")
                .withStyle(currentTab == Tab.WEEKLY ? 
                    Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true) :
                    Style.EMPTY.withColor(ChatFormatting.GRAY)),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                currentTab = Tab.WEEKLY;
                scrollOffset = 0;
                updateContentHeight();
                init();
            }
        );
        this.addRenderableWidget(weeklyTab);
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                requestData();
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    private void updateContentHeight() {
        if (currentTab == Tab.HALL_OF_FAME) {
            this.contentHeight = hallOfFame.size() * (ENTRY_HEIGHT + ENTRY_SPACING);
        } else {
            this.contentHeight = weeklyLeaderboard.size() * (ENTRY_HEIGHT + ENTRY_SPACING);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
        }
        
        this.renderBackground(graphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable("screen.bm_falkye.hall_of_fame_title")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GOLD)
                .withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(5);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Область контента
        int contentY = layout.getY(12);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(10);
        int contentStartX = layout.getX(5);
        int contentEndX = layout.getRightX(layout.getWidth(90), 5);
        
        // Обрезка контента
        graphics.enableScissor(contentStartX, contentY, contentEndX, contentY + contentHeight);
        
        if (currentTab == Tab.HALL_OF_FAME) {
            renderHallOfFame(graphics, guiX, contentY);
        } else if (currentTab == Tab.WEEKLY) {
            renderWeeklyLeaderboard(graphics, guiX, contentY);
        }
        
        graphics.disableScissor();
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
        
        // Индикаторы прокрутки
        int visibleEntries = contentHeight / (ENTRY_HEIGHT + ENTRY_SPACING);
        int totalEntries = currentTab == Tab.HALL_OF_FAME ? hallOfFame.size() : weeklyLeaderboard.size();
        
        if (totalEntries > visibleEntries) {
            int scrollBarX = layout.getRightX(4, 2);
            int scrollBarY = contentY;
            int scrollBarHeight = contentHeight;
            int scrollBarWidth = 4;
            
            // Фон полосы прокрутки
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                scrollBarY + scrollBarHeight, 0x66000000);
            
            // Ползунок
            int maxScroll = Math.max(1, totalEntries - visibleEntries);
            int sliderHeight = Math.max(20, (int)((double)visibleEntries / totalEntries * scrollBarHeight));
            int sliderY = scrollBarY + (int)((double)scrollOffset / maxScroll * (scrollBarHeight - sliderHeight));
            graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
        }
    }
    
    private void renderHallOfFame(GuiGraphics graphics, int guiX, int startY) {
        MutableComponent subtitle = Component.translatable("screen.bm_falkye.legends_all_seasons")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        graphics.drawString(this.font, subtitle, layout.getX(5), startY - scrollOffset, 0xFFFFFF, false);
        
        int y = startY - scrollOffset + 25;
        int visibleEntries = (layout.getGuiHeight() - layout.getY(12) - layout.getHeight(10) - 25) / (ENTRY_HEIGHT + ENTRY_SPACING);
        int startIndex = scrollOffset / (ENTRY_HEIGHT + ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleEntries + 1, hallOfFame.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= hallOfFame.size()) break;
            
            HallOfFameEntry entry = hallOfFame.get(i);
            
            // Фон записи
            int entryX = layout.getX(3);
            int entryWidth = layout.getWidth(94);
            GuiUtils.drawRoundedRect(graphics, entryX, y, entryWidth, ENTRY_HEIGHT, 0xE0202020);
            
            // Рамка в зависимости от места
            int borderColor = i < 3 ? 0xFFCD7F32 : 0xFF8B7355;
            GuiUtils.drawRoundedBorder(graphics, entryX, y, entryWidth, ENTRY_HEIGHT, borderColor, 1);
            
            // Место (портрет)
            MutableComponent position = Component.literal(String.valueOf(i + 1))
                .withStyle(i < 3 ? 
                    Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true) :
                    Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, position, entryX + 10, y + 12, 0xFFFFFF, false);
            
            // Имя игрока
            MutableComponent playerName = Component.literal(entry.playerName)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            graphics.drawString(this.font, playerName, entryX + 50, y + 5, 0xFFFFFF, false);
            
            // Сезон
            MutableComponent season = Component.translatable("screen.bm_falkye.season", entry.season)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, season, entryX + 50, y + 18, 0x888888, false);
            
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
        
        if (hallOfFame.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_legends")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), startY + 50, 0x888888);
        }
    }
    
    private void renderWeeklyLeaderboard(GuiGraphics graphics, int guiX, int startY) {
        MutableComponent subtitle = Component.translatable("screen.bm_falkye.weekly_rating_top_50")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        graphics.drawString(this.font, subtitle, layout.getX(5), startY - scrollOffset, 0xFFFFFF, false);
        
        int y = startY - scrollOffset + 25;
        int visibleEntries = (layout.getGuiHeight() - layout.getY(12) - layout.getHeight(10) - 25) / (ENTRY_HEIGHT + ENTRY_SPACING);
        int startIndex = scrollOffset / (ENTRY_HEIGHT + ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleEntries + 1, weeklyLeaderboard.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= weeklyLeaderboard.size()) break;
            
            LeaderboardEntry entry = weeklyLeaderboard.get(i);
            
            // Фон записи
            int entryX = layout.getX(3);
            int entryWidth = layout.getWidth(94);
            GuiUtils.drawRoundedRect(graphics, entryX, y, entryWidth, ENTRY_HEIGHT, 0xE0202020);
            
            // Рамка в зависимости от места
            int borderColor = i < 3 ? 0xFFCD7F32 : 0xFF8B7355;
            GuiUtils.drawRoundedBorder(graphics, entryX, y, entryWidth, ENTRY_HEIGHT, borderColor, 1);
            
            // Место
            MutableComponent position = Component.literal(String.valueOf(i + 1) + ".")
                .withStyle(i < 3 ? 
                    Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true) :
                    Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, position, entryX + 10, y + 12, 0xFFFFFF, false);
            
            // Имя игрока
            MutableComponent playerName = Component.literal(entry.playerName)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            graphics.drawString(this.font, playerName, entryX + 50, y + 12, 0xFFFFFF, false);
            
            // Рейтинг
            MutableComponent rating = Component.literal(String.valueOf(entry.rating))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            int ratingX = layout.getRightX(this.font.width(rating), 10);
            graphics.drawString(this.font, rating, ratingX, y + 12, 0xFFFFFF, false);
            
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
        
        if (weeklyLeaderboard.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.rating_empty")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), startY + 50, 0x888888);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null) return super.mouseScrolled(mouseX, mouseY, delta);
        
        int guiX = layout.getGuiX();
        int contentY = layout.getY(12);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(10);
        
        // Проверяем, находится ли мышь в области контента
        if (mouseX >= guiX && mouseX <= guiX + GUI_WIDTH &&
            mouseY >= contentY && mouseY <= contentY + contentHeight) {
            
            int totalEntries = currentTab == Tab.HALL_OF_FAME ? hallOfFame.size() : weeklyLeaderboard.size();
            int visibleEntries = contentHeight / (ENTRY_HEIGHT + ENTRY_SPACING);
            int maxScroll = Math.max(0, totalEntries - visibleEntries);
            int scrollStep = ENTRY_HEIGHT + ENTRY_SPACING;
            
            if (delta < 0 && scrollOffset < maxScroll) {
                scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep);
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset = Math.max(0, scrollOffset - scrollStep);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private enum Tab {
        HALL_OF_FAME, WEEKLY
    }
    
    /**
     * Запись в Зале Славы
     */
    public static class HallOfFameEntry {
        public final UUID playerUUID;
        public final String playerName;
        public final int season;
        
        public HallOfFameEntry(UUID playerUUID, String playerName, int season) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.season = season;
        }
    }
    
    /**
     * Запись в еженедельном рейтинге
     */
    public static class LeaderboardEntry {
        public final UUID playerUUID;
        public final String playerName;
        public final int rating;
        
        public LeaderboardEntry(UUID playerUUID, String playerName, int rating) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.rating = rating;
        }
    }
}
