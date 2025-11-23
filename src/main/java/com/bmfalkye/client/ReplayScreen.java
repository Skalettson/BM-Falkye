package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Красивый экран реплеев с фильтрацией
 */
public class ReplayScreen extends Screen {
    private static final int GUI_WIDTH = 700;
    private static final int GUI_HEIGHT = 450;
    
    private int guiX;
    private int guiY;
    private int scrollOffset = 0;
    
    private static class ReplayInfo {
        String replayId; // ID реплея для запроса
        String player1Name;
        String player2Name;
        String winnerName;
        int roundsWon1;
        int roundsWon2;
        long timestamp;
    }
    
    private List<ReplayInfo> replays = new ArrayList<>();
    private boolean dataLoaded = false;
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    private java.util.List<Button> viewButtons = new ArrayList<>(); // Кнопки просмотра
    
    public ReplayScreen() {
        this(null);
    }
    
    public ReplayScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§7§lРЕПЛЕИ"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Удаляем старые кнопки просмотра
        for (Button btn : viewButtons) {
            this.removeWidget(btn);
        }
        viewButtons.clear();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + GUI_HEIGHT - 35, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
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
        
        // Создаём кнопки просмотра для каждого реплея
        if (dataLoaded && !replays.isEmpty()) {
            int startIndex = scrollOffset / 90;
            int visibleCount = Math.min(replays.size() - startIndex, 4);
            int y = guiY + 55;
            
            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                if (index >= replays.size()) break;
                
                ReplayInfo replay = replays.get(index);
                int cardY = y + i * 90;
                String finalReplayId = replay.replayId;
                
                Button viewButton = new StyledCardCollectionButton(
                    guiX + GUI_WIDTH - 200, cardY + 55, 160, 20,
                    Component.literal("§aПросмотреть"),
                    (btn) -> {
                        // Запрашиваем данные реплея
                        NetworkHandler.INSTANCE.sendToServer(
                            new NetworkHandler.RequestReplayPacket(finalReplayId));
                    }
                );
                this.addRenderableWidget(viewButton);
                viewButtons.add(viewButton);
            }
        }
    }
    
    public void updateReplays(FriendlyByteBuf data) {
        replays.clear();
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            ReplayInfo info = new ReplayInfo();
            info.replayId = data.readUtf(); // Читаем replayId
            info.player1Name = data.readUtf();
            info.player2Name = data.readUtf();
            info.winnerName = data.readUtf();
            info.roundsWon1 = data.readInt();
            info.roundsWon2 = data.readInt();
            info.timestamp = data.readLong();
            replays.add(info);
        }
        dataLoaded = true;
        // Переинициализируем экран для добавления кнопок только если экран активен
        if (this.minecraft != null && this.minecraft.screen == this) {
            this.init();
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§7§l══════ СОХРАНЁННЫЕ РЕПЛЕИ ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF808080);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка реплеев..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        if (replays.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Нет сохранённых реплеев"),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Результаты ваших игр будут сохраняться здесь"),
                guiX + GUI_WIDTH / 2, y + 70, 0xCCCCCC);
        } else {
            // Список реплеев
            int startIndex = scrollOffset / 90;
            int visibleCount = Math.min(replays.size() - startIndex, 4);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                if (index >= replays.size()) break;
                
                ReplayInfo replay = replays.get(index);
                int cardY = y + i * 90;
                
                // ПЕРЕПИСАНО: Фон карточки реплея в скевоморфном стиле
                GuiUtils.drawLeatherElement(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 85);
                GuiUtils.drawMetalFrame(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 85, 1, false);
                
                // Игроки
                guiGraphics.drawString(this.font, Component.literal("§e" + replay.player1Name + " §7vs §e" + replay.player2Name), 
                    guiX + 40, cardY + 10, 0xFFFFFF, false);
                
                // Счёт
                String score = replay.roundsWon1 + " - " + replay.roundsWon2;
                guiGraphics.drawString(this.font, Component.literal("§7Счёт: §f" + score), 
                    guiX + 40, cardY + 25, 0xFFFFFF, false);
                
                // Победитель
                guiGraphics.drawString(this.font, Component.literal("§aПобедитель: §f" + replay.winnerName), 
                    guiX + 40, cardY + 40, 0xFFFFFF, false);
                
                // Дата
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                String dateStr = sdf.format(new Date(replay.timestamp));
                guiGraphics.drawString(this.font, Component.literal("§7" + dateStr), 
                    guiX + GUI_WIDTH - 200, cardY + 25, 0xFFFFFF, false);
            }
            
            // Скроллбар
            if (replays.size() > 4) {
                int scrollbarHeight = (GUI_HEIGHT - 100);
                int scrollbarY = guiY + 55;
                int scrollbarX = guiX + GUI_WIDTH - 25;
                int thumbHeight = Math.max(20, scrollbarHeight * 4 / replays.size());
                int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                    Math.max(1, (replays.size() - 4) * 90);
                
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight, 0xFF404040);
                guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF808080);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (replays.size() > 4) {
            scrollOffset = (int) Math.max(0, Math.min((replays.size() - 4) * 90, 
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
