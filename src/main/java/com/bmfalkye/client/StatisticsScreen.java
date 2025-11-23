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

import java.util.HashMap;
import java.util.Map;

/**
 * Красивый и информативный экран статистики игрока
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class StatisticsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 550;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private int scrollOffset = 0;
    
    // Данные статистики
    private int totalGames = 0;
    private int wins = 0;
    private int losses = 0;
    private double winRate = 0.0;
    private int roundsWon = 0;
    private int roundsLost = 0;
    private double roundWinRate = 0.0;
    private int rating = 1000;
    private com.bmfalkye.rating.RatingSystem.Rank rank = com.bmfalkye.rating.RatingSystem.Rank.BRONZE;
    private int level = 1;
    private String mostPlayedCard = null;
    private Map<String, Integer> factionWins = new HashMap<>();
    
    private boolean dataLoaded = false;
    private final Screen parentScreen;
    
    public StatisticsScreen() {
        this(null);
    }
    
    public StatisticsScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.statistics_title"));
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
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestStatisticsPacket());
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
            layout.getRightX(layout.getWidth(15), 2), layout.getBottomY(layout.getHeight(5), 2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestStatisticsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    public void updateStatistics(FriendlyByteBuf data) {
        totalGames = data.readInt();
        wins = data.readInt();
        losses = data.readInt();
        winRate = data.readDouble();
        roundsWon = data.readInt();
        roundsLost = data.readInt();
        roundWinRate = data.readDouble();
        rating = data.readInt();
        rank = com.bmfalkye.rating.RatingSystem.Rank.values()[data.readInt()];
        level = data.readInt();
        mostPlayedCard = data.readUtf();
        if (mostPlayedCard.isEmpty()) {
            mostPlayedCard = null;
        }
        
        int factionCount = data.readInt();
        factionWins.clear();
        for (int i = 0; i < factionCount; i++) {
            factionWins.put(data.readUtf(), data.readInt());
        }
        
        dataLoaded = true;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
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
        MutableComponent title = Component.translatable("screen.bm_falkye.player_statistics")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFF4A90E2);
        
        int contentY = layout.getY(11);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(10);
        int contentX = layout.getX(3);
        int contentWidth = GUI_WIDTH - contentX * 2;
        
        // Обрезка контента
        guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_statistics")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), contentY + contentHeight / 2, 0xFFFFFF);
        } else {
            renderStatisticsContent(guiGraphics, contentX, contentY, contentWidth, contentHeight);
        }
        
        guiGraphics.disableScissor();
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderStatisticsContent(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int leftX = x;
        int rightX = x + width / 2 + 10;
        int currentY = y;
        int lineHeight = 16;
        int sectionSpacing = 20;
        
        // Левая колонка - Общая статистика
        MutableComponent sectionTitle = Component.translatable("screen.bm_falkye.general_statistics")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, sectionTitle, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 5;
        
        // Всего игр
        MutableComponent totalGamesText = Component.translatable("screen.bm_falkye.total_games", totalGames)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, totalGamesText, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Побед
        MutableComponent winsText = Component.translatable("screen.bm_falkye.wins", wins)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
        guiGraphics.drawString(this.font, winsText, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Поражений
        MutableComponent lossesText = Component.translatable("screen.bm_falkye.losses", losses)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
        guiGraphics.drawString(this.font, lossesText, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Процент побед
        MutableComponent winRateText = Component.translatable("screen.bm_falkye.win_rate", 
            String.format("%.1f", winRate))
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        guiGraphics.drawString(this.font, winRateText, leftX, currentY, 0xFFFFFF, false);
        currentY += sectionSpacing;
        
        // Раунды
        MutableComponent roundsTitle = Component.translatable("screen.bm_falkye.rounds")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, roundsTitle, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 5;
        
        // Выиграно раундов
        MutableComponent roundsWonText = Component.translatable("screen.bm_falkye.rounds_won", roundsWon)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
        guiGraphics.drawString(this.font, roundsWonText, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Проиграно раундов
        MutableComponent roundsLostText = Component.translatable("screen.bm_falkye.rounds_lost", roundsLost)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
        guiGraphics.drawString(this.font, roundsLostText, leftX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Процент побед в раундах
        MutableComponent roundWinRateText = Component.translatable("screen.bm_falkye.round_win_rate", 
            String.format("%.1f", roundWinRate))
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        guiGraphics.drawString(this.font, roundWinRateText, leftX, currentY, 0xFFFFFF, false);
        
        // Правая колонка - Рейтинг и фракции
        currentY = y;
        
        // Рейтинг и ранг
        MutableComponent ratingTitle = Component.translatable("screen.bm_falkye.rating")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, ratingTitle, rightX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 5;
        
        // Рейтинг
        MutableComponent ratingText = Component.translatable("screen.bm_falkye.rating_value", rating)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, ratingText, rightX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Ранг
        String rankColorCode = rank.getColorCode();
        ChatFormatting rankColor = ChatFormatting.getByName(rankColorCode.replace("§", ""));
        if (rankColor == null) {
            rankColor = ChatFormatting.GOLD;
        }
        MutableComponent rankText = Component.translatable("screen.bm_falkye.rank", rank.getDisplayName())
            .withStyle(Style.EMPTY.withColor(rankColor));
        guiGraphics.drawString(this.font, rankText, rightX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Уровень
        MutableComponent levelText = Component.translatable("screen.bm_falkye.level", level)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, levelText, rightX, currentY, 0xFFFFFF, false);
        currentY += sectionSpacing;
        
        // Победы по фракциям
        if (!factionWins.isEmpty()) {
            MutableComponent factionTitle = Component.translatable("screen.bm_falkye.faction_wins")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
            guiGraphics.drawString(this.font, factionTitle, rightX, currentY, 0xFFFFFF, false);
            currentY += lineHeight + 5;
            
            for (Map.Entry<String, Integer> entry : factionWins.entrySet()) {
                MutableComponent factionText = Component.translatable("screen.bm_falkye.faction_wins_count", 
                    entry.getKey(), entry.getValue())
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                guiGraphics.drawString(this.font, factionText, rightX, currentY, 0xFFFFFF, false);
                currentY += lineHeight;
            }
            currentY += 5;
        }
        
        // Самая используемая карта
        if (mostPlayedCard != null && !mostPlayedCard.isEmpty()) {
            MutableComponent cardTitle = Component.translatable("screen.bm_falkye.most_played_card")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
            guiGraphics.drawString(this.font, cardTitle, rightX, currentY, 0xFFFFFF, false);
            currentY += lineHeight + 5;
            
            MutableComponent cardText = Component.literal(mostPlayedCard)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, cardText, rightX, currentY, 0xFFFFFF, false);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
