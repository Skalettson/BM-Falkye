package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * Красивый экран сезона с визуализацией прогресса
 */
public class SeasonScreen extends Screen {
    private static final int GUI_WIDTH = 600;
    private static final int GUI_HEIGHT = 400;
    
    private int guiX;
    private int guiY;
    
    private int seasonNumber = 1;
    private int seasonLevel = 1;
    private int seasonXP = 0;
    private int xpForNextLevel = 100;
    private int daysRemaining = 30;
    
    private boolean dataLoaded = false;
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    
    public SeasonScreen() {
        this(null);
    }
    
    public SeasonScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§a§lСЕЗОН"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestSeasonPacket());
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + GUI_HEIGHT - 35, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestSeasonPacket());
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
    
    public void updateSeason(FriendlyByteBuf data) {
        seasonNumber = data.readInt();
        seasonLevel = data.readInt();
        seasonXP = data.readInt();
        xpForNextLevel = data.readInt();
        daysRemaining = data.readInt();
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
            Component.literal("§a§l══════ СЕЗОН " + seasonNumber + " ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF4CAF50);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка данных сезона..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        // Информация о сезоне
        guiGraphics.drawString(this.font, Component.literal("§7Осталось дней: §f" + daysRemaining), 
            guiX + 30, y, 0xFFFFFF, false);
        y += 25;
        
        // Уровень сезона
        guiGraphics.drawString(this.font, Component.literal("§a§lУровень сезона: §f" + seasonLevel + " / 30"), 
            guiX + 30, y, 0xFFFFFF, false);
        y += 30;
        
        // Прогресс-бар уровня
        int progressBarWidth = GUI_WIDTH - 60;
        int progressBarHeight = 20;
        int progressBarX = guiX + 30;
        int progressBarY = y;
        
        // Фон прогресс-бара
        GuiUtils.drawRoundedRect(guiGraphics, progressBarX, progressBarY, progressBarWidth, progressBarHeight, 0xFF2A2A2A);
        
        // Заполнение прогресс-бара
        float progress = seasonLevel >= 30 ? 1.0f : (float) seasonXP / xpForNextLevel;
        int filledWidth = (int) (progress * progressBarWidth);
        if (filledWidth > 0) {
            GuiUtils.drawRoundedRect(guiGraphics, progressBarX, progressBarY, filledWidth, progressBarHeight, 0xFF4CAF50);
        }
        
        // Текст прогресса
        String progressText = seasonLevel >= 30 ? "§aМАКСИМАЛЬНЫЙ УРОВЕНЬ" : 
            "§7" + seasonXP + " / " + xpForNextLevel + " XP";
        int textWidth = this.font.width(progressText);
        guiGraphics.drawString(this.font, Component.literal(progressText), 
            progressBarX + (progressBarWidth - textWidth) / 2, 
            progressBarY + (progressBarHeight - 9) / 2, 0xFFFFFF, false);
        
        y += 40;
        
        // Визуализация уровней (мини-карточки)
        guiGraphics.drawString(this.font, Component.literal("§6§lПрогресс по уровням:"), 
            guiX + 30, y, 0xFFFFFF, false);
        y += 25;
        
        int levelsPerRow = 10;
        int levelCardSize = 25;
        int levelSpacing = 5;
        int startX = guiX + 30;
        
        for (int level = 1; level <= 30; level++) {
            int row = (level - 1) / levelsPerRow;
            int col = (level - 1) % levelsPerRow;
            int levelX = startX + col * (levelCardSize + levelSpacing);
            int levelY = y + row * (levelCardSize + levelSpacing);
            
            // Цвет в зависимости от статуса уровня
            int color;
            if (level < seasonLevel) {
                color = 0xFF4CAF50; // Пройден
            } else if (level == seasonLevel) {
                color = 0xFFFFA500; // Текущий
            } else {
                color = 0xFF404040; // Недоступен
            }
            
            // ПЕРЕПИСАНО: Карточка уровня в скевоморфном стиле
            GuiUtils.drawLeatherElement(guiGraphics, levelX, levelY, levelCardSize, levelCardSize);
            // Цветной оверлей
            guiGraphics.fill(levelX, levelY, levelX + levelCardSize, levelY + levelCardSize, color);
            GuiUtils.drawMetalFrame(guiGraphics, levelX, levelY, levelCardSize, levelCardSize, 1, false);
            
            // Номер уровня
            String levelText = String.valueOf(level);
            int textX = levelX + (levelCardSize - this.font.width(levelText)) / 2;
            int textY = levelY + (levelCardSize - 9) / 2;
            guiGraphics.drawString(this.font, Component.literal(levelText), 
                textX, textY, 0xFFFFFF, false);
        }
        
        // Легенда
        y += 100;
        guiGraphics.drawString(this.font, Component.literal("§a■ Пройден  §e■ Текущий  §7■ Недоступен"), 
            guiX + 30, y, 0xFFFFFF, false);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
