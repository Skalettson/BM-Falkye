package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Экран просмотра реплея игры
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class ReplayViewerScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 650;
    private static final int MIN_GUI_WIDTH = 800;
    private static final int MIN_GUI_HEIGHT = 550;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private int scrollOffset = 0;
    
    private String replayId;
    private String player1Name;
    private String player2Name;
    private String winnerName;
    private int roundsWon1;
    private int roundsWon2;
    private long timestamp;
    private int duration;
    
    private static class ReplayMove {
        String playerName;
        String action;
        String cardId;
        int round;
        long timestamp;
    }
    
    private List<ReplayMove> moves = new ArrayList<>();
    private final Screen parentScreen;
    
    private static final int MOVE_ENTRY_HEIGHT = 28;
    private static final int MOVE_ENTRY_SPACING = 5;
    
    public ReplayViewerScreen(FriendlyByteBuf data) {
        this(data, null);
    }
    
    public ReplayViewerScreen(FriendlyByteBuf data, Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.replay_viewer_title"));
        this.parentScreen = parentScreen;
        
        // Читаем данные реплея
        replayId = data.readUtf();
        player1Name = data.readUtf();
        player2Name = data.readUtf();
        winnerName = data.readUtf();
        roundsWon1 = data.readInt();
        roundsWon2 = data.readInt();
        timestamp = data.readLong();
        duration = data.readInt();
        
        int moveCount = data.readInt();
        for (int i = 0; i < moveCount; i++) {
            ReplayMove move = new ReplayMove();
            move.playerName = data.readUtf();
            move.action = data.readUtf();
            move.cardId = data.readUtf();
            move.round = data.readInt();
            move.timestamp = data.readLong();
            moves.add(move);
        }
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
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    minecraft.setScreen(new ReplayScreen(null));
                }
            }
        );
        this.addRenderableWidget(backButton);
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
        MutableComponent title = Component.translatable("screen.bm_falkye.replay_viewer_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFF808080);
        
        // Информация о реплее
        int infoY = layout.getY(11);
        int infoX = layout.getX(5);
        int lineHeight = 16;
        
        // Игроки
        MutableComponent player1 = Component.translatable("screen.bm_falkye.player_1", player1Name)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        guiGraphics.drawString(this.font, player1, infoX, infoY, 0xFFFFFF, false);
        infoY += lineHeight;
        
        MutableComponent player2 = Component.translatable("screen.bm_falkye.player_2", player2Name)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        guiGraphics.drawString(this.font, player2, infoX, infoY, 0xFFFFFF, false);
        infoY += lineHeight;
        
        // Счёт
        MutableComponent score = Component.translatable("screen.bm_falkye.score", 
            roundsWon1, roundsWon2)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, score, infoX, infoY, 0xFFFFFF, false);
        infoY += lineHeight;
        
        // Победитель
        MutableComponent winner = Component.translatable("screen.bm_falkye.winner", winnerName)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
        guiGraphics.drawString(this.font, winner, infoX, infoY, 0xFFFFFF, false);
        infoY += lineHeight + 5;
        
        // Правая колонка - дополнительная информация
        int rightX = layout.getX(55);
        int rightY = layout.getY(11);
        
        // Дата
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStr = sdf.format(new Date(timestamp));
        MutableComponent date = Component.translatable("screen.bm_falkye.date", dateStr)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, date, rightX, rightY, 0xFFFFFF, false);
        rightY += lineHeight;
        
        // Длительность
        if (duration > 0) {
            MutableComponent durationText = Component.translatable("screen.bm_falkye.duration_seconds", duration)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, durationText, rightX, rightY, 0xFFFFFF, false);
            rightY += lineHeight;
        }
        
        // Ходов
        MutableComponent movesCount = Component.translatable("screen.bm_falkye.moves_count", moves.size())
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        guiGraphics.drawString(this.font, movesCount, rightX, rightY, 0xFFFFFF, false);
        
        // Разделитель перед списком ходов
        int movesStartY = layout.getY(22);
        guiGraphics.fill(guiX + layout.getWidth(5), movesStartY, 
            guiX + GUI_WIDTH - layout.getWidth(5), movesStartY + 2, 0xFF8B7355);
        
        // Заголовок списка ходов
        int movesTitleY = movesStartY + 10;
        MutableComponent movesTitle = Component.translatable("screen.bm_falkye.game_moves")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, movesTitle, layout.getX(5), movesTitleY, 0xFFFFFF, false);
        
        // Область списка ходов
        int movesListY = movesTitleY + 20;
        int movesListHeight = GUI_HEIGHT - movesListY - layout.getHeight(10);
        int movesListX = layout.getX(3);
        int movesListWidth = GUI_WIDTH - movesListX * 2;
        
        // Обрезка списка ходов
        guiGraphics.enableScissor(movesListX, movesListY, 
            movesListX + movesListWidth, movesListY + movesListHeight);
        
        if (moves.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_moves_saved")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), movesListY + movesListHeight / 2, 0x888888);
        } else {
            renderMovesList(guiGraphics, movesListX, movesListY, movesListWidth, movesListHeight);
        }
        
        guiGraphics.disableScissor();
        
        // Индикатор прокрутки
        if (!moves.isEmpty()) {
            int visibleCount = movesListHeight / (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING);
            int totalCount = moves.size();
            
            if (totalCount > visibleCount) {
                int scrollBarX = layout.getRightX(4, 2);
                int scrollBarY = movesListY;
                int scrollBarHeight = movesListHeight;
                int scrollBarWidth = 4;
                
                // Фон полосы прокрутки
                guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                    scrollBarY + scrollBarHeight, 0x66000000);
                
                // Ползунок
                int maxScroll = Math.max(1, totalCount - visibleCount);
                int sliderHeight = Math.max(20, (int)((double)visibleCount / totalCount * scrollBarHeight));
                int scrollStep = MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING;
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
    
    private void renderMovesList(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int visibleCount = height / (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING);
        int startIndex = scrollOffset / (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, moves.size());
        
        int moveY = y - (scrollOffset % (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= moves.size()) break;
            
            ReplayMove move = moves.get(i);
            int moveWidth = width - 10;
            
            // Фон записи хода
            GuiUtils.drawLeatherElement(guiGraphics, x + 5, moveY, moveWidth, MOVE_ENTRY_HEIGHT);
            GuiUtils.drawMetalFrame(guiGraphics, x + 5, moveY, moveWidth, MOVE_ENTRY_HEIGHT, 1, false);
            
            int textX = x + 15;
            int textY = moveY + 8;
            
            // Информация о ходе
            MutableComponent moveText = Component.translatable("screen.bm_falkye.round", move.round)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal(move.playerName).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                .append(Component.literal(" - ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
            
            // Действие
            MutableComponent action;
            switch (move.action) {
                case "play_card":
                    action = Component.translatable("screen.bm_falkye.played_card");
                    if (move.cardId != null && !move.cardId.isEmpty()) {
                        com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(move.cardId);
                        if (card != null) {
                            action = action.append(Component.literal(" " + card.getName())
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));
                        }
                    }
                    action = action.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                    break;
                case "pass":
                    action = Component.translatable("screen.bm_falkye.passed")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                    break;
                case "use_leader":
                    action = Component.translatable("screen.bm_falkye.used_leader")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
                    break;
                default:
                    action = Component.literal(move.action)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            }
            
            moveText = moveText.append(action);
            
            // Обрезаем текст, если слишком длинный
            int maxWidth = moveWidth - 30;
            if (this.font.width(moveText) > maxWidth) {
                String textStr = moveText.getString();
                textStr = this.font.plainSubstrByWidth(textStr, maxWidth - 5) + "...";
                moveText = Component.literal(textStr).withStyle(moveText.getStyle());
            }
            
            guiGraphics.drawString(this.font, moveText, textX, textY, 0xFFFFFF, false);
            
            moveY += MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || moves.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int movesListY = layout.getY(22) + 30;
        int movesListHeight = layout.getGuiHeight() - movesListY - layout.getHeight(10);
        int movesListX = layout.getX(3);
        int movesListWidth = layout.getGuiWidth() - movesListX * 2;
        
        if (mouseX >= movesListX && mouseX <= movesListX + movesListWidth &&
            mouseY >= movesListY && mouseY <= movesListY + movesListHeight) {
            
            int visibleCount = movesListHeight / (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING);
            int totalCount = moves.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING);
            int scrollStep = MOVE_ENTRY_HEIGHT + MOVE_ENTRY_SPACING;
            
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
    public boolean isPauseScreen() {
        return false;
    }
}
