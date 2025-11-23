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
 * Красивый экран событий с таймерами
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class EventsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 850;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private int scrollOffset = 0;
    
    private static class EventInfo {
        String id;
        String name;
        String description;
        String type;
        boolean participated;
        long timeRemaining;
        int rewardXP;
        int rewardCoins;
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
    
    private List<EventInfo> events = new ArrayList<>();
    private boolean dataLoaded = false;
    private final Screen parentScreen;
    
    private static final int EVENT_CARD_HEIGHT = 120;
    private static final int EVENT_CARD_SPACING = 10;
    
    public EventsScreen() {
        this(null);
    }
    
    public EventsScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.events_title"));
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
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestEventsPacket());
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
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestEventsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    public void updateEvents(FriendlyByteBuf data) {
        events.clear();
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            EventInfo event = new EventInfo();
            event.id = data.readUtf();
            event.name = data.readUtf();
            event.description = data.readUtf();
            event.type = data.readUtf();
            event.participated = data.readBoolean();
            event.timeRemaining = data.readLong();
            event.rewardXP = data.readInt();
            event.rewardCoins = data.readInt();
            
            // Читаем квесты (если есть)
            int questCount = data.readInt();
            for (int j = 0; j < questCount; j++) {
                data.readUtf(); // type
                data.readInt(); // target
                data.readInt(); // progress
                data.readBoolean(); // completed
                data.readInt(); // rewardXP
                data.readInt(); // rewardCoins
                boolean hasCard = data.readBoolean();
                if (hasCard) {
                    data.readUtf(); // cardId
                    data.readUtf(); // cardName
                }
            }
            
            events.add(event);
        }
        dataLoaded = true;
        scrollOffset = 0;
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
        MutableComponent title = Component.translatable("screen.bm_falkye.active_events")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFF673AB7);
        
        // Область событий
        int eventsY = layout.getY(11);
        int eventsHeight = GUI_HEIGHT - eventsY - layout.getHeight(10);
        int eventsX = layout.getX(3);
        int eventsWidth = GUI_WIDTH - eventsX * 2;
        
        // Обрезка событий
        guiGraphics.enableScissor(eventsX, eventsY, eventsX + eventsWidth, eventsY + eventsHeight);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_events")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), eventsY + eventsHeight / 2, 0xFFFFFF);
        } else if (events.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_active_events")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), eventsY + eventsHeight / 2, 0xFFFFFF);
            
            MutableComponent hint = Component.translatable("screen.bm_falkye.watch_for_events")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
            guiGraphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), eventsY + eventsHeight / 2 + 20, 0xCCCCCC);
        } else {
            renderEventsList(guiGraphics, eventsX, eventsY, eventsWidth, eventsHeight);
        }
        
        guiGraphics.disableScissor();
        
        // Индикатор прокрутки
        if (dataLoaded && !events.isEmpty()) {
            int visibleCount = eventsHeight / (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING);
            int totalCount = events.size();
            
            if (totalCount > visibleCount) {
                int scrollBarX = layout.getRightX(4, 2);
                int scrollBarY = eventsY;
                int scrollBarHeight = eventsHeight;
                int scrollBarWidth = 4;
                
                // Фон полосы прокрутки
                guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                    scrollBarY + scrollBarHeight, 0x66000000);
                
                // Ползунок
                int maxScroll = Math.max(1, totalCount - visibleCount);
                int sliderHeight = Math.max(20, (int)((double)visibleCount / totalCount * scrollBarHeight));
                int scrollStep = EVENT_CARD_HEIGHT + EVENT_CARD_SPACING;
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
    
    private void renderEventsList(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int visibleCount = height / (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING);
        int startIndex = scrollOffset / (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, events.size());
        
        int cardY = y - (scrollOffset % (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= events.size()) break;
            
            EventInfo event = events.get(i);
            int cardWidth = width - 10;
            
            // Фон карточки события
            GuiUtils.drawLeatherElement(guiGraphics, x + 5, cardY, cardWidth, EVENT_CARD_HEIGHT);
            GuiUtils.drawMetalFrame(guiGraphics, x + 5, cardY, cardWidth, EVENT_CARD_HEIGHT, 2, false);
            
            int textX = x + 15;
            int textY = cardY + 10;
            int lineHeight = 15;
            
            // Название
            MutableComponent name = Component.literal(event.name)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withBold(true));
            guiGraphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
            textY += lineHeight + 5;
            
            // ID
            MutableComponent id = Component.translatable("screen.bm_falkye.event_id", event.id)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, id, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Описание
            String description = event.description;
            int maxDescWidth = cardWidth - 30;
            if (this.font.width(description) > maxDescWidth) {
                description = this.font.plainSubstrByWidth(description, maxDescWidth - 5) + "...";
            }
            MutableComponent desc = Component.literal(description)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawString(this.font, desc, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
            
            // Таймер
            long currentTimeRemaining = Math.max(0, event.timeRemaining);
            String timeStr = formatTime(currentTimeRemaining);
            MutableComponent time = Component.translatable("screen.bm_falkye.time_remaining", timeStr)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            guiGraphics.drawString(this.font, time, textX, textY, 0xFFFFFF, false);
            
            // Статус участия
            MutableComponent participationStatus = Component.translatable(event.participated ? 
                "screen.bm_falkye.participated" : "screen.bm_falkye.not_participated")
                .withStyle(Style.EMPTY.withColor(event.participated ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            int statusX = x + cardWidth - this.font.width(participationStatus) - 15;
            guiGraphics.drawString(this.font, participationStatus, statusX, cardY + 10, 0xFFFFFF, false);
            
            // Награды
            MutableComponent rewards = Component.empty();
            if (event.rewardXP > 0) {
                rewards = rewards.append(Component.translatable("screen.bm_falkye.reward_xp", event.rewardXP)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));
            }
            if (event.rewardCoins > 0) {
                if (event.rewardXP > 0) {
                    rewards = rewards.append(Component.literal(" "));
                }
                rewards = rewards.append(Component.translatable("screen.bm_falkye.reward_coins", event.rewardCoins)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            }
            int rewardsX = x + cardWidth - this.font.width(rewards) - 15;
            guiGraphics.drawString(this.font, rewards, rewardsX, cardY + 25, 0xFFFFFF, false);
            
            // Прогресс-бар времени
            long eventDuration = 604800000L; // 7 дней
            float timeProgress = Math.min(1.0f, (float) currentTimeRemaining / eventDuration);
            int progressBarWidth = cardWidth - 30;
            int progressBarHeight = 8;
            int progressBarX = textX;
            int progressBarY = cardY + EVENT_CARD_HEIGHT - 20;
            
            // Фон прогресс-бара
            guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, 
                progressBarY + progressBarHeight, 0xFF3A3A3A);
            // Прогресс
            if (timeProgress > 0) {
                int progressWidth = (int)(progressBarWidth * timeProgress);
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, 
                    progressBarY + progressBarHeight, 0xFF673AB7);
            }
            // Рамка
            GuiUtils.drawRoundedBorder(guiGraphics, progressBarX, progressBarY, 
                progressBarWidth, progressBarHeight, 0xFF555555, 1);
            
            cardY += EVENT_CARD_HEIGHT + EVENT_CARD_SPACING;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || !dataLoaded || events.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int eventsY = layout.getY(11);
        int eventsHeight = layout.getGuiHeight() - eventsY - layout.getHeight(10);
        int eventsX = layout.getX(3);
        int eventsWidth = layout.getGuiWidth() - eventsX * 2;
        
        if (mouseX >= eventsX && mouseX <= eventsX + eventsWidth &&
            mouseY >= eventsY && mouseY <= eventsY + eventsHeight) {
            
            int visibleCount = eventsHeight / (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING);
            int totalCount = events.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (EVENT_CARD_HEIGHT + EVENT_CARD_SPACING);
            int scrollStep = EVENT_CARD_HEIGHT + EVENT_CARD_SPACING;
            
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
