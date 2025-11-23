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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Красивый экран реплеев с фильтрацией
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class ReplayScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 850;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private int scrollOffset = 0;
    
    private static class ReplayInfo {
        String replayId;
        String player1Name;
        String player2Name;
        String winnerName;
        int roundsWon1;
        int roundsWon2;
        long timestamp;
    }
    
    private List<ReplayInfo> replays = new ArrayList<>();
    private boolean dataLoaded = false;
    private final Screen parentScreen;
    
    private static final int REPLAY_CARD_HEIGHT = 95;
    private static final int REPLAY_CARD_SPACING = 10;
    
    public ReplayScreen() {
        this(null);
    }
    
    public ReplayScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.replays_title"));
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
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
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
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    public void updateReplays(FriendlyByteBuf data) {
        replays.clear();
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            ReplayInfo info = new ReplayInfo();
            info.replayId = data.readUtf();
            info.player1Name = data.readUtf();
            info.player2Name = data.readUtf();
            info.winnerName = data.readUtf();
            info.roundsWon1 = data.readInt();
            info.roundsWon2 = data.readInt();
            info.timestamp = data.readLong();
            replays.add(info);
        }
        dataLoaded = true;
        scrollOffset = 0;
        if (this.minecraft != null && this.minecraft.screen == this) {
            this.init();
        }
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
        MutableComponent title = Component.translatable("screen.bm_falkye.saved_replays")
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
        
        // Область списка реплеев
        int listY = layout.getY(11);
        int listHeight = GUI_HEIGHT - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = GUI_WIDTH - listX * 2;
        
        // Обрезка списка реплеев
        guiGraphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_replays")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), listY + listHeight / 2, 0xFFFFFF);
        } else if (replays.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_replays")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), listY + listHeight / 2, 0xFFFFFF);
            
            MutableComponent hint = Component.translatable("screen.bm_falkye.replays_will_be_saved_here")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
            guiGraphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), listY + listHeight / 2 + 20, 0xCCCCCC);
        } else {
            renderReplaysList(guiGraphics, listX, listY, listWidth, listHeight, mouseX, mouseY);
        }
        
        guiGraphics.disableScissor();
        
        // Индикатор прокрутки
        if (dataLoaded && !replays.isEmpty()) {
            int visibleCount = listHeight / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
            int totalCount = replays.size();
            
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
                int scrollStep = REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING;
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
    
    private void renderReplaysList(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                   int mouseX, int mouseY) {
        int visibleCount = height / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
        int startIndex = scrollOffset / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, replays.size());
        
        int replayY = y - (scrollOffset % (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= replays.size()) break;
            
            ReplayInfo replay = replays.get(i);
            int cardWidth = width - 10;
            
            boolean isHovered = mouseX >= x && mouseX <= x + cardWidth && 
                               mouseY >= replayY && mouseY <= replayY + REPLAY_CARD_HEIGHT;
            
            // Фон карточки реплея
            GuiUtils.drawLeatherElement(guiGraphics, x + 5, replayY, cardWidth, REPLAY_CARD_HEIGHT);
            // Оверлей
            int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
            guiGraphics.fill(x + 7, replayY + 2, x + cardWidth - 3, 
                replayY + REPLAY_CARD_HEIGHT - 2, bgColor);
            GuiUtils.drawMetalFrame(guiGraphics, x + 5, replayY, cardWidth, REPLAY_CARD_HEIGHT, 1, false);
            
            int textX = x + 15;
            int textY = replayY + 10;
            int lineHeight = 15;
            
            // Игроки
            MutableComponent vs = Component.empty()
                .append(Component.literal(replay.player1Name).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                .append(Component.translatable("screen.bm_falkye.vs").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal(replay.player2Name).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            guiGraphics.drawString(this.font, vs, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Счёт
            MutableComponent score = Component.translatable("screen.bm_falkye.score", 
                replay.roundsWon1, replay.roundsWon2)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, score, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Победитель
            MutableComponent winner = Component.translatable("screen.bm_falkye.winner", replay.winnerName)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            guiGraphics.drawString(this.font, winner, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Дата
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            String dateStr = sdf.format(new Date(replay.timestamp));
            MutableComponent date = Component.literal(dateStr)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int dateX = x + cardWidth - this.font.width(date) - 15;
            guiGraphics.drawString(this.font, date, dateX, replayY + 10, 0xFFFFFF, false);
            
            // Кнопка "Просмотреть"
            int buttonX = x + cardWidth - 140;
            int buttonY = replayY + REPLAY_CARD_HEIGHT - 30;
            int buttonWidth = 130;
            int buttonHeight = 25;
            
            boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                                   mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            
            int buttonColor = buttonHovered ? 0xAA00AA00 : 0xAA008800;
            GuiUtils.drawRoundedRect(guiGraphics, buttonX, buttonY, buttonWidth, buttonHeight, buttonColor);
            GuiUtils.drawRoundedBorder(guiGraphics, buttonX, buttonY, buttonWidth, buttonHeight, 
                0xFF00AA00, 1);
            
            MutableComponent viewText = Component.translatable("screen.bm_falkye.view")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            int viewTextX = buttonX + (buttonWidth - this.font.width(viewText)) / 2;
            guiGraphics.drawString(this.font, viewText, viewTextX, buttonY + 8, 0xFFFFFF, false);
            
            replayY += REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && dataLoaded && !replays.isEmpty()) {
            int listY = layout.getY(11);
            int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
            int listX = layout.getX(3);
            int listWidth = layout.getGuiWidth() - listX * 2;
            
            int visibleCount = listHeight / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
            int startIndex = scrollOffset / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, replays.size());
            
            int replayY = listY - (scrollOffset % (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= replays.size()) break;
                
                ReplayInfo replay = replays.get(i);
                int cardWidth = listWidth - 10;
                int buttonX = listX + cardWidth - 140;
                int buttonY = replayY + REPLAY_CARD_HEIGHT - 30;
                int buttonWidth = 130;
                int buttonHeight = 25;
                
                if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.RequestReplayPacket(replay.replayId));
                    return true;
                }
                
                replayY += REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || !dataLoaded || replays.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int listY = layout.getY(11);
        int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = layout.getGuiWidth() - listX * 2;
        
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleCount = listHeight / (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
            int totalCount = replays.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING);
            int scrollStep = REPLAY_CARD_HEIGHT + REPLAY_CARD_SPACING;
            
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
