package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Красивый экран событий с таймерами
 */
public class EventsScreen extends Screen {
    private static final int GUI_WIDTH = 700;
    private static final int GUI_HEIGHT = 450;
    
    private int guiX;
    private int guiY;
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
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    
    public EventsScreen() {
        this(null);
    }
    
    public EventsScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§5§lСОБЫТИЯ"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestEventsPacket());
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + GUI_HEIGHT - 35, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestEventsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
        
        // Кнопка "Назад"
        Button backButton = new StyledCardCollectionButton(
            guiX + 20, guiY + GUI_HEIGHT - 35, 100, 20,
            Component.literal("§7Назад"),
            (btn) -> {
                if (parentScreen != null) {
                    this.minecraft.setScreen(parentScreen);
                } else {
                    this.onClose();
                }
            }
        );
        this.addRenderableWidget(backButton);
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
            
            // Читаем квасты (если есть)
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
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§5§l══════ АКТИВНЫЕ СОБЫТИЯ ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF673AB7);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка событий..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        if (events.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Нет активных событий"),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Следите за объявлениями о новых событиях!"),
                guiX + GUI_WIDTH / 2, y + 70, 0xCCCCCC);
        } else {
            // Список событий
            int startIndex = scrollOffset / 100;
            int visibleCount = Math.min(events.size() - startIndex, 3);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                if (index >= events.size()) break;
                
                EventInfo event = events.get(index);
                int cardY = y + i * 100;
                
                // ПЕРЕПИСАНО: Фон карточки события в скевоморфном стиле
                GuiUtils.drawLeatherElement(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 95);
                GuiUtils.drawMetalFrame(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 95, 2, false);
                
                // Название
                guiGraphics.drawString(this.font, Component.literal("§5§l" + event.name), 
                    guiX + 40, cardY + 10, 0xFFFFFF, false);
                
                // ID (короткий)
                guiGraphics.drawString(this.font, Component.literal("§7ID: §f" + event.id), 
                    guiX + 40, cardY + 25, 0xFFFFFF, false);
                
                // Описание
                String desc = event.description.length() > 60 ? 
                    event.description.substring(0, 57) + "..." : event.description;
                guiGraphics.drawString(this.font, Component.literal("§7" + desc), 
                    guiX + 40, cardY + 40, 0xFFFFFF, false);
                
                // Таймер (используем реальное время из события)
                long currentTimeRemaining = Math.max(0, event.timeRemaining);
                String timeStr = formatTime(currentTimeRemaining);
                guiGraphics.drawString(this.font, Component.literal("§7Осталось: §f" + timeStr), 
                    guiX + 40, cardY + 55, 0xFFFFFF, false);
                
                // Статус участия
                String participationStatus = event.participated ? "§aУчаствовали" : "§7Не участвовали";
                guiGraphics.drawString(this.font, Component.literal(participationStatus), 
                    guiX + 40, cardY + 70, 0xFFFFFF, false);
                
                // Награды
                String rewards = "§7Награды: ";
                if (event.rewardXP > 0) {
                    rewards += "§b+" + event.rewardXP + " XP ";
                }
                if (event.rewardCoins > 0) {
                    rewards += "§e+" + event.rewardCoins + " монет";
                }
                guiGraphics.drawString(this.font, Component.literal(rewards), 
                    guiX + 40, cardY + 55, 0xFFFFFF, false);
                
                // Прогресс-бар времени (визуализация оставшегося времени)
                int progressBarWidth = GUI_WIDTH - 100;
                int progressBarHeight = 8;
                int progressBarX = guiX + 40;
                int progressBarY = cardY + 75;
                
                // Предполагаем, что событие длится 7 дней (604800000 мс)
                long eventDuration = 604800000L;
                float timeProgress = Math.min(1.0f, (float) currentTimeRemaining / eventDuration);
                
                // Простой прогресс-бар
                int progressBgColor = 0xFF3A3A3A;
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, progressBgColor);
                if (timeProgress > 0) {
                    int fillColor = 0xFF673AB7;
                    guiGraphics.fill(progressBarX + 1, progressBarY + 1, 
                        progressBarX + (int)(progressBarWidth * timeProgress) - 1, 
                        progressBarY + progressBarHeight - 1, fillColor);
                }
            }
            
            // Скроллбар
            if (events.size() > 3) {
                int scrollbarHeight = (GUI_HEIGHT - 100);
                int scrollbarY = guiY + 55;
                int scrollbarX = guiX + GUI_WIDTH - 25;
                int thumbHeight = Math.max(20, scrollbarHeight * 3 / events.size());
                int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                    Math.max(1, (events.size() - 3) * 100);
                
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight, 0xFF404040);
                guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF673AB7);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (events.size() > 3) {
            scrollOffset = (int) Math.max(0, Math.min((events.size() - 3) * 100, 
                scrollOffset - delta * 20));
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
