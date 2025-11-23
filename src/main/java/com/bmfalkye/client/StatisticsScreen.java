package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Красивый и информативный экран статистики игрока
 */
public class StatisticsScreen extends Screen {
    private static final int GUI_WIDTH = 600;
    private static final int GUI_HEIGHT = 400;
    
    private int guiX;
    private int guiY;
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
    private String mostPlayedCard = "Нет данных";
    private Map<String, Integer> factionWins = new HashMap<>();
    
    private boolean dataLoaded = false;
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    
    public StatisticsScreen() {
        this(null);
    }
    
    public StatisticsScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§b§lСТАТИСТИКА"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestStatisticsPacket());
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + GUI_HEIGHT - 35, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestStatisticsPacket());
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
        
        int factionCount = data.readInt();
        factionWins.clear();
        for (int i = 0; i < factionCount; i++) {
            factionWins.put(data.readUtf(), data.readInt());
        }
        
        dataLoaded = true;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок с иконкой
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§b§l══════ СТАТИСТИКА ИГРОКА ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF4A90E2);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка статистики..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        // Основная статистика (левая колонка)
        int leftX = guiX + 30;
        int rightX = guiX + GUI_WIDTH / 2 + 20;
        
        // Заголовок секции
        guiGraphics.drawString(this.font, Component.literal("§6§lОбщая статистика"), 
            leftX, y, 0xFFFFFF, false);
        y += 20;
        
        // Игры
        guiGraphics.drawString(this.font, Component.literal("§7Всего игр: §f" + totalGames), 
            leftX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§aПобед: §f" + wins), 
            leftX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§cПоражений: §f" + losses), 
            leftX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§eПроцент побед: §f" + 
            String.format("%.1f", winRate) + "%"), 
            leftX, y, 0xFFFFFF, false);
        y += 25;
        
        // Раунды
        guiGraphics.drawString(this.font, Component.literal("§6§lРаунды"), 
            leftX, y, 0xFFFFFF, false);
        y += 20;
        guiGraphics.drawString(this.font, Component.literal("§aВыиграно: §f" + roundsWon), 
            leftX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§cПроиграно: §f" + roundsLost), 
            leftX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§eПроцент побед: §f" + 
            String.format("%.1f", roundWinRate) + "%"), 
            leftX, y, 0xFFFFFF, false);
        
        // Правая колонка
        y = guiY + 55;
        
        // Рейтинг и ранг
        guiGraphics.drawString(this.font, Component.literal("§6§lРейтинг"), 
            rightX, y, 0xFFFFFF, false);
        y += 20;
        guiGraphics.drawString(this.font, Component.literal("§7Рейтинг: §f" + rating), 
            rightX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§7Ранг: " + rank.getColorCode() + 
            rank.getDisplayName()), 
            rightX, y, 0xFFFFFF, false);
        y += 15;
        guiGraphics.drawString(this.font, Component.literal("§7Уровень: §f" + level), 
            rightX, y, 0xFFFFFF, false);
        y += 25;
        
        // Фракции
        guiGraphics.drawString(this.font, Component.literal("§6§lПобеды по фракциям"), 
            rightX, y, 0xFFFFFF, false);
        y += 20;
        for (Map.Entry<String, Integer> entry : factionWins.entrySet()) {
            guiGraphics.drawString(this.font, Component.literal("§7" + entry.getKey() + ": §a" + 
                entry.getValue()), 
                rightX, y, 0xFFFFFF, false);
            y += 15;
        }
        y += 10;
        
        // Самая используемая карта
        if (!mostPlayedCard.equals("Нет данных")) {
            guiGraphics.drawString(this.font, Component.literal("§6§lСамая используемая карта"), 
                rightX, y, 0xFFFFFF, false);
            y += 20;
            guiGraphics.drawString(this.font, Component.literal("§7" + mostPlayedCard), 
                rightX, y, 0xFFFFFF, false);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
