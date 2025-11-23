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

import java.util.ArrayList;
import java.util.List;

/**
 * Красивый экран турниров с детальной информацией
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class TournamentScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 550;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    private int scrollOffset = 0;
    
    private static class TournamentInfo {
        String name;
        String id;
        String type;
        int participants;
        int maxParticipants;
        int entryFee;
        int prizePool;
        int currentRound;
        boolean started;
        boolean finished;
        long startTime;
        long endTime;
    }
    
    /**
     * Форматирует время в читаемый вид
     */
    private String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "0м";
        }
        
        long totalSeconds = milliseconds / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        
        if (days > 0) {
            return days + "д " + hours + "ч " + minutes + "м";
        } else if (hours > 0) {
            return hours + "ч " + minutes + "м";
        } else if (minutes > 0) {
            return minutes + "м";
        } else {
            return totalSeconds + "с";
        }
    }
    
    private List<TournamentInfo> tournaments = new ArrayList<>();
    private boolean dataLoaded = false;
    private final Screen parentScreen;
    
    private static final int TOURNAMENT_CARD_HEIGHT = 110;
    private static final int TOURNAMENT_CARD_SPACING = 10;
    
    public TournamentScreen() {
        this(null);
    }
    
    public TournamentScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.tournaments_title"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestTournamentsPacket());
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
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestTournamentsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    public void updateTournaments(FriendlyByteBuf data) {
        tournaments.clear();
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            TournamentInfo info = new TournamentInfo();
            info.name = data.readUtf();
            info.id = data.readUtf();
            info.type = data.readUtf();
            info.participants = data.readInt();
            info.maxParticipants = data.readInt();
            info.entryFee = data.readInt();
            info.prizePool = data.readInt();
            info.currentRound = data.readInt();
            info.started = data.readBoolean();
            info.finished = data.readBoolean();
            info.startTime = data.readLong();
            info.endTime = data.readLong();
            tournaments.add(info);
        }
        dataLoaded = true;
        scrollOffset = 0; // Сбрасываем прокрутку при обновлении
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable("screen.bm_falkye.active_tournaments")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GOLD)
                .withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFFA500);
        
        // Область контента
        int contentY = layout.getY(11);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(8);
        int contentStartX = layout.getX(3);
        int contentEndX = layout.getRightX(layout.getWidth(94), 3);
        
        // Обрезка контента
        guiGraphics.enableScissor(contentStartX, contentY, contentEndX, contentY + contentHeight);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_tournaments")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), contentY + contentHeight / 2, 0xFFFFFF);
        } else if (tournaments.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_active_tournaments")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int emptyY = contentY + contentHeight / 2;
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), emptyY, 0xFFFFFF);
            
            MutableComponent hint = Component.translatable("screen.bm_falkye.watch_for_tournaments")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
            guiGraphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), emptyY + 20, 0xCCCCCC);
        } else {
            // Список турниров
            renderTournamentList(guiGraphics, contentStartX, contentY, 
                contentEndX - contentStartX, contentHeight);
        }
        
        guiGraphics.disableScissor();
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, btn, mouseX, mouseY, false);
            }
        }
        
        // Индикаторы прокрутки
        if (dataLoaded && !tournaments.isEmpty()) {
            int visibleCount = contentHeight / (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING);
            int totalCount = tournaments.size();
            
            if (totalCount > visibleCount) {
                int scrollBarX = layout.getRightX(4, 2);
                int scrollBarY = contentY;
                int scrollBarHeight = contentHeight;
                int scrollBarWidth = 4;
                
                // Фон полосы прокрутки
                guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                    scrollBarY + scrollBarHeight, 0x66000000);
                
                // Ползунок
                int maxScroll = Math.max(1, totalCount - visibleCount);
                int sliderHeight = Math.max(20, (int)((double)visibleCount / totalCount * scrollBarHeight));
                int scrollStep = TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING;
                int currentScrollIndex = scrollOffset / scrollStep;
                int sliderY = scrollBarY + (int)((double)currentScrollIndex / maxScroll * (scrollBarHeight - sliderHeight));
                guiGraphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                    sliderY + sliderHeight, 0xFF8B7355);
            }
        }
    }
    
    private void renderTournamentList(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int visibleCount = height / (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING);
        int startIndex = scrollOffset / (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, tournaments.size());
        
        int cardY = y - (scrollOffset % (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= tournaments.size()) break;
            
            TournamentInfo t = tournaments.get(i);
            
            // Фон карточки турнира
            int cardWidth = width - 10;
            GuiUtils.drawLeatherElement(guiGraphics, x + 5, cardY, cardWidth, TOURNAMENT_CARD_HEIGHT);
            GuiUtils.drawMetalFrame(guiGraphics, x + 5, cardY, cardWidth, TOURNAMENT_CARD_HEIGHT, 2, true);
            
            int textX = x + 15;
            int textY = cardY + 10;
            int lineHeight = 15;
            
            // Название
            MutableComponent name = Component.literal(t.name)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
            guiGraphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
            textY += lineHeight + 5;
            
            // ID
            MutableComponent id = Component.translatable("screen.bm_falkye.tournament_id", t.id)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, id, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Участники
            MutableComponent participants = Component.translatable("screen.bm_falkye.participants", 
                t.participants, t.maxParticipants)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, participants, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Взнос и призовой фонд
            MutableComponent entryFee = Component.translatable("screen.bm_falkye.entry_fee", t.entryFee)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, entryFee, textX, textY, 0xFFFFFF, false);
            
            MutableComponent prizePool = Component.translatable("screen.bm_falkye.prize_pool", t.prizePool)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            int prizeX = x + cardWidth - this.font.width(prizePool) - 15;
            guiGraphics.drawString(this.font, prizePool, prizeX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Статус
            MutableComponent status;
            if (t.finished) {
                status = Component.translatable("screen.bm_falkye.tournament_finished")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            } else if (t.started) {
                status = Component.translatable("screen.bm_falkye.tournament_active", t.currentRound)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            } else {
                status = Component.translatable("screen.bm_falkye.tournament_waiting")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            }
            guiGraphics.drawString(this.font, status, textX, textY, 0xFFFFFF, false);
            
            // Время
            long currentTime = System.currentTimeMillis();
            long timeRemaining = Math.max(0, t.endTime - currentTime);
            String timeStr = formatTime(timeRemaining);
            MutableComponent time = Component.translatable("screen.bm_falkye.time_remaining", timeStr)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int timeX = x + cardWidth - this.font.width(time) - 15;
            guiGraphics.drawString(this.font, time, timeX, textY, 0xFFFFFF, false);
            
            cardY += TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || !dataLoaded || tournaments.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int contentY = layout.getY(11);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(8);
        int guiX = layout.getGuiX();
        int contentStartX = layout.getX(3);
        int contentEndX = layout.getRightX(layout.getWidth(94), 3);
        
        // Проверяем, находится ли мышь в области контента
        if (mouseX >= contentStartX && mouseX <= contentEndX &&
            mouseY >= contentY && mouseY <= contentY + contentHeight) {
            
            int visibleCount = contentHeight / (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING);
            int totalCount = tournaments.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * (TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING);
            int scrollStep = TOURNAMENT_CARD_HEIGHT + TOURNAMENT_CARD_SPACING;
            
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
}
