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
 * Экран для просмотра матчей турнира в качестве зрителя
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class TournamentSpectatorScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 850;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private final Screen parentScreen;
    private final String tournamentId;
    private final String tournamentName;
    
    private List<MatchInfo> activeMatches = new ArrayList<>();
    private int scrollOffset = 0;
    
    private static final int MATCH_ENTRY_HEIGHT = 90;
    private static final int MATCH_ENTRY_SPACING = 10;
    
    public TournamentSpectatorScreen(Screen parentScreen, String tournamentId, String tournamentName) {
        super(Component.translatable("screen.bm_falkye.tournament_spectator_title"));
        this.parentScreen = parentScreen;
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        requestMatches();
    }
    
    private void requestMatches() {
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.RequestTournamentMatchesPacket(tournamentId));
    }
    
    public void updateMatches(List<MatchInfo> matches) {
        this.activeMatches = new ArrayList<>(matches);
        scrollOffset = 0;
        if (this.minecraft != null && this.minecraft.screen == this) {
            this.init();
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
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
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
                requestMatches();
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    private void watchMatch(MatchInfo match) {
        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.WatchTournamentMatchPacket(tournamentId, match.player1UUID, match.player2UUID));
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
        MutableComponent title = Component.translatable("screen.bm_falkye.tournament_spectator_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Название турнира
        MutableComponent tournamentNameText = Component.literal(tournamentName)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        graphics.drawString(this.font, tournamentNameText, layout.getX(5), layout.getY(8), 0xFFFFFF, false);
        
        // Разделитель
        int dividerY = layout.getY(11);
        graphics.fill(guiX + layout.getWidth(5), dividerY, 
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFFA500);
        
        // Область списка матчей
        int matchesY = layout.getY(13);
        int matchesHeight = GUI_HEIGHT - matchesY - layout.getHeight(8);
        int matchesX = layout.getX(3);
        int matchesWidth = GUI_WIDTH - matchesX * 2;
        
        // Обрезка списка матчей
        graphics.enableScissor(matchesX, matchesY, matchesX + matchesWidth, matchesY + matchesHeight);
        
        if (activeMatches.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_active_matches")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), matchesY + matchesHeight / 2, 0x888888);
        } else {
            renderMatchesList(graphics, matchesX, matchesY, matchesWidth, matchesHeight, mouseX, mouseY);
        }
        
        graphics.disableScissor();
        
        // Индикатор прокрутки
        if (!activeMatches.isEmpty()) {
            int visibleCount = matchesHeight / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
            int totalCount = activeMatches.size();
            
            if (totalCount > visibleCount) {
                int scrollBarX = layout.getRightX(4, 2);
                int scrollBarY = matchesY;
                int scrollBarHeight = matchesHeight;
                int scrollBarWidth = 4;
                
                // Фон полосы прокрутки
                graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                    scrollBarY + scrollBarHeight, 0x66000000);
                
                // Ползунок
                int maxScroll = Math.max(1, totalCount - visibleCount);
                int sliderHeight = Math.max(20, (int)((double)visibleCount / totalCount * scrollBarHeight));
                int scrollStep = MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING;
                int currentScrollIndex = scrollOffset / scrollStep;
                int sliderY = scrollBarY + (int)((double)currentScrollIndex / maxScroll * (scrollBarHeight - sliderHeight));
                graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                    sliderY + sliderHeight, 0xFF8B7355);
            }
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderMatchesList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int visibleCount = height / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
        int startIndex = scrollOffset / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, activeMatches.size());
        
        int matchY = y - (scrollOffset % (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= activeMatches.size()) break;
            
            MatchInfo match = activeMatches.get(i);
            int entryWidth = width - 10;
            
            boolean isHovered = mouseX >= x && mouseX <= x + entryWidth && 
                               mouseY >= matchY && mouseY <= matchY + MATCH_ENTRY_HEIGHT;
            
            // Фон карточки матча
            GuiUtils.drawLeatherElement(graphics, x + 5, matchY, entryWidth, MATCH_ENTRY_HEIGHT);
            int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
            graphics.fill(x + 7, matchY + 2, x + entryWidth - 3, 
                matchY + MATCH_ENTRY_HEIGHT - 2, bgColor);
            GuiUtils.drawMetalFrame(graphics, x + 5, matchY, entryWidth, MATCH_ENTRY_HEIGHT, 1, false);
            
            int textX = x + 15;
            int textY = matchY + 10;
            int lineHeight = 15;
            
            // Раунд турнира
            MutableComponent round = Component.translatable("screen.bm_falkye.tournament_round", match.round)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, round, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Игроки
            MutableComponent vs = Component.empty()
                .append(Component.literal(match.player1Name).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                .append(Component.translatable("screen.bm_falkye.vs").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal(match.player2Name).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            graphics.drawString(this.font, vs, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Счёт
            MutableComponent score = Component.translatable("screen.bm_falkye.score", 
                match.roundsWon1, match.roundsWon2)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, score, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Текущий раунд игры
            MutableComponent gameRound = Component.translatable("screen.bm_falkye.game_round", 
                match.currentGameRound)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
            graphics.drawString(this.font, gameRound, textX, textY, 0xFFFFFF, false);
            
            // Кнопка "Смотреть" (текст)
            MutableComponent watchText = Component.translatable("screen.bm_falkye.watch")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
            int watchX = x + entryWidth - this.font.width(watchText) - 15;
            graphics.drawString(this.font, watchText, watchX, matchY + 35, 0xFFFFFF, false);
            
            matchY += MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !activeMatches.isEmpty()) {
            int matchesY = layout.getY(13);
            int matchesHeight = layout.getGuiHeight() - matchesY - layout.getHeight(8);
            int matchesX = layout.getX(3);
            int matchesWidth = layout.getGuiWidth() - matchesX * 2;
            
            int visibleCount = matchesHeight / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
            int startIndex = scrollOffset / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, activeMatches.size());
            
            int matchY = matchesY - (scrollOffset % (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= activeMatches.size()) break;
                
                MatchInfo match = activeMatches.get(i);
                int entryWidth = matchesWidth - 10;
                
                if (mouseX >= matchesX && mouseX <= matchesX + entryWidth &&
                    mouseY >= matchY && mouseY <= matchY + MATCH_ENTRY_HEIGHT) {
                    
                    watchMatch(match);
                    return true;
                }
                
                matchY += MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || activeMatches.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int matchesY = layout.getY(13);
        int matchesHeight = layout.getGuiHeight() - matchesY - layout.getHeight(8);
        int matchesX = layout.getX(3);
        int matchesWidth = layout.getGuiWidth() - matchesX * 2;
        
        if (mouseX >= matchesX && mouseX <= matchesX + matchesWidth &&
            mouseY >= matchesY && mouseY <= matchesY + matchesHeight) {
            
            int visibleCount = matchesHeight / (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
            int totalCount = activeMatches.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING);
            int scrollStep = MATCH_ENTRY_HEIGHT + MATCH_ENTRY_SPACING;
            
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
    
    /**
     * Информация о матче для отображения
     */
    public static class MatchInfo {
        public final String tournamentId;
        public final int round;
        public final UUID player1UUID;
        public final String player1Name;
        public final UUID player2UUID;
        public final String player2Name;
        public final int currentGameRound;
        public final int roundsWon1;
        public final int roundsWon2;
        
        public MatchInfo(String tournamentId, int round, UUID player1UUID, String player1Name,
                        UUID player2UUID, String player2Name, int currentGameRound,
                        int roundsWon1, int roundsWon2) {
            this.tournamentId = tournamentId;
            this.round = round;
            this.player1UUID = player1UUID;
            this.player1Name = player1Name;
            this.player2UUID = player2UUID;
            this.player2Name = player2Name;
            this.currentGameRound = currentGameRound;
            this.roundsWon1 = roundsWon1;
            this.roundsWon2 = roundsWon2;
        }
    }
}
