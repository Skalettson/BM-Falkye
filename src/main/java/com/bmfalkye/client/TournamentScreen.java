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
 * Красивый экран турниров с детальной информацией
 */
public class TournamentScreen extends Screen {
    private static final int GUI_WIDTH = 700;
    private static final int GUI_HEIGHT = 450;
    
    private int guiX;
    private int guiY;
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
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    
    public TournamentScreen() {
        this(null);
    }
    
    public TournamentScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§6§lТУРНИРЫ"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestTournamentsPacket());
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + GUI_HEIGHT - 35, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestTournamentsPacket());
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
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Фон
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, true); // Золотая рамка для турнира
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§6§l══════ АКТИВНЫЕ ТУРНИРЫ ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFFFFA500);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка турниров..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        if (tournaments.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Нет активных турниров"),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Следите за объявлениями о новых турнирах!"),
                guiX + GUI_WIDTH / 2, y + 70, 0xCCCCCC);
        } else {
            // Список турниров
            int startIndex = scrollOffset / 100;
            int visibleCount = Math.min(tournaments.size() - startIndex, 3);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                if (index >= tournaments.size()) break;
                
                TournamentInfo t = tournaments.get(index);
                int cardY = y + i * 100; // Увеличиваем высоту для большего количества информации
                
                // Фон карточки турнира
                // ПЕРЕПИСАНО: Фон карточки турнира в скевоморфном стиле
                GuiUtils.drawLeatherElement(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 95);
                GuiUtils.drawMetalFrame(guiGraphics, guiX + 30, cardY, GUI_WIDTH - 60, 95, 1, true);
                
                // Название
                guiGraphics.drawString(this.font, Component.literal("§6§l" + t.name), 
                    guiX + 40, cardY + 10, 0xFFFFFF, false);
                
                // ID (короткий)
                guiGraphics.drawString(this.font, Component.literal("§7ID: §f" + t.id), 
                    guiX + 40, cardY + 25, 0xFFFFFF, false);
                
                // Участники
                guiGraphics.drawString(this.font, Component.literal("§7Участников: §f" + t.participants + 
                    " / " + t.maxParticipants), 
                    guiX + 40, cardY + 40, 0xFFFFFF, false);
                
                // Взнос
                guiGraphics.drawString(this.font, Component.literal("§7Взнос: §e" + t.entryFee + " монет"), 
                    guiX + 40, cardY + 55, 0xFFFFFF, false);
                
                // Призовой фонд
                guiGraphics.drawString(this.font, Component.literal("§7Призовой фонд: §a" + t.prizePool + " монет"), 
                    guiX + 40, cardY + 70, 0xFFFFFF, false);
                
                // Статус
                String status = t.finished ? "§cЗавершён" : t.started ? "§aИдёт" : "§eОжидание";
                if (t.started) {
                    status += " (Раунд " + t.currentRound + ")";
                }
                guiGraphics.drawString(this.font, Component.literal("§7Статус: " + status), 
                    guiX + 40, cardY + 85, 0xFFFFFF, false);
                
                // Время
                long currentTime = System.currentTimeMillis();
                long timeRemaining = Math.max(0, t.endTime - currentTime);
                String timeStr = formatTime(timeRemaining);
                guiGraphics.drawString(this.font, Component.literal("§7Осталось: §f" + timeStr), 
                    guiX + GUI_WIDTH - 200, cardY + 30, 0xFFFFFF, false);
            }
            
            // Скроллбар (если нужно)
            if (tournaments.size() > 3) {
                int scrollbarHeight = (GUI_HEIGHT - 100);
                int scrollbarY = guiY + 55;
                int scrollbarX = guiX + GUI_WIDTH - 25;
                int thumbHeight = Math.max(20, scrollbarHeight * 3 / tournaments.size());
                int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                    Math.max(1, (tournaments.size() - 3) * 100);
                
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight, 0xFF404040);
                guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFFFFA500);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tournaments.size() > 3) {
            scrollOffset = (int) Math.max(0, Math.min((tournaments.size() - 3) * 100, 
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
