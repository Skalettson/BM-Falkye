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
 * Красивый экран ежедневных наград с календарём и заданиями
 */
public class DailyRewardsScreen extends Screen {
    private static final int GUI_WIDTH = 700;
    private static final int GUI_HEIGHT = 450;
    
    private int guiX;
    private int guiY;
    
    private int day = 1;
    private boolean claimed = false;
    private int streak = 0;
    
    private static class QuestInfo {
        String description;
        int target;
        int progress;
        int rewardXP;
        int rewardCoins;
    }
    
    private List<QuestInfo> quests = new ArrayList<>();
    private boolean dataLoaded = false;
    private final net.minecraft.client.gui.screens.Screen parentScreen;
    
    public DailyRewardsScreen() {
        this(null);
    }
    
    public DailyRewardsScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.literal("§d§lЕЖЕДНЕВНЫЕ НАГРАДЫ"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Очищаем все виджеты перед добавлением новых (предотвращает дублирование)
        this.clearWidgets();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Запрашиваем данные с сервера
        if (!dataLoaded) {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
        }
        
        // Кнопка "Получить награду" (позиционируем внизу с достаточным отступом)
        int claimButtonY = guiY + GUI_HEIGHT - 35; // Отступ 35px от низа
        if (claimed) {
            Button claimButton = new StyledCardCollectionButton(
                guiX + GUI_WIDTH / 2 - 100, claimButtonY, 200, 20,
                Component.literal("§7Награда уже получена"),
                (btn) -> {}
            );
            this.addRenderableWidget(claimButton);
        } else {
            Button claimButton = new StyledCardCollectionButton(
                guiX + GUI_WIDTH / 2 - 100, claimButtonY, 200, 20,
                Component.literal("§a§lПолучить награду"),
                (btn) -> {
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.ClaimDailyRewardPacket());
                    dataLoaded = false;
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
                }
            );
            this.addRenderableWidget(claimButton);
        }
        
        // Кнопка "Обновить"
        Button refreshButton = new StyledCardCollectionButton(
            guiX + GUI_WIDTH - 110, guiY + 15, 90, 20,
            Component.literal("§eОбновить"),
            (btn) -> {
                dataLoaded = false;
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
        
        // Кнопка "Назад" (позиционируем внизу с достаточным отступом)
        int backButtonY = guiY + GUI_HEIGHT - 35; // Отступ 35px от низа
        Button backButton = new StyledCardCollectionButton(
            guiX + 20, backButtonY, 100, 20,
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
    
    public void updateDailyRewards(FriendlyByteBuf data) {
        day = data.readInt();
        claimed = data.readBoolean();
        streak = data.readInt();
        
        quests.clear();
        int questCount = data.readInt();
        for (int i = 0; i < questCount; i++) {
            QuestInfo quest = new QuestInfo();
            quest.description = data.readUtf();
            quest.target = data.readInt();
            quest.progress = data.readInt();
            quest.rewardXP = data.readInt();
            quest.rewardCoins = data.readInt();
            quests.add(quest);
        }
        
        dataLoaded = true;
        this.init(); // Переинициализируем для обновления кнопок
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§d§l══════ ЕЖЕДНЕВНЫЕ НАГРАДЫ ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF9C27B0);
        
        int y = guiY + 55;
        
        if (!dataLoaded) {
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Загрузка данных..."),
                guiX + GUI_WIDTH / 2, y + 50, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        
        // Левая колонка - календарь наград
        int leftX = guiX + 30;
        guiGraphics.drawString(this.font, Component.literal("§6§lКалендарь наград (7 дней)"), 
            leftX, y, 0xFFFFFF, false);
        y += 25;
        
        // Серия
        guiGraphics.drawString(this.font, Component.literal("§7Серия: §e" + streak + " дней"), 
            leftX, y, 0xFFFFFF, false);
        y += 25;
        
        // Календарь (7 дней)
        int dayCardWidth = 80;
        int dayCardHeight = 60;
        int daySpacing = 10;
        int calendarStartX = leftX;
        int calendarY = y;
        
        for (int d = 1; d <= 7; d++) {
            int dayX = calendarStartX + (d - 1) * (dayCardWidth + daySpacing);
            
            // Цвет в зависимости от статуса
            int bgColor;
            int borderColor;
            if (d < day) {
                bgColor = 0xFF4CAF50; // Получено
                borderColor = 0xFF66BB6A;
            } else if (d == day) {
                bgColor = claimed ? 0xFF4CAF50 : 0xFFFFA500; // Текущий день
                borderColor = claimed ? 0xFF66BB6A : 0xFFFFB74D;
            } else {
                bgColor = 0xFF404040; // Недоступно
                borderColor = 0xFF505050;
            }
            
            // ПЕРЕПИСАНО: Карточка дня в скевоморфном стиле
            GuiUtils.drawLeatherElement(guiGraphics, dayX, calendarY, dayCardWidth, dayCardHeight);
            // Цветной оверлей
            guiGraphics.fill(dayX, calendarY, dayX + dayCardWidth, calendarY + dayCardHeight, bgColor);
            GuiUtils.drawMetalFrame(guiGraphics, dayX, calendarY, dayCardWidth, dayCardHeight, 2, false);
            
            // День
            guiGraphics.drawString(this.font, Component.literal("§7День " + d), 
                dayX + 5, calendarY + 5, 0xFFFFFF, false);
            
            // Награда (пример)
            String reward = d <= 3 ? "§e" + (d * 10) + " монет" : 
                d <= 5 ? "§b" + (d * 5) + " XP" : "§dРедкая карта";
            guiGraphics.drawString(this.font, Component.literal(reward), 
                dayX + 5, calendarY + 20, 0xFFFFFF, false);
            
            // Статус
            if (d < day) {
                guiGraphics.drawString(this.font, Component.literal("§a✓"), 
                    dayX + dayCardWidth - 15, calendarY + 5, 0xFFFFFF, false);
            } else if (d == day && !claimed) {
                guiGraphics.drawString(this.font, Component.literal("§e!"), 
                    dayX + dayCardWidth - 15, calendarY + 5, 0xFFFFFF, false);
            }
        }
        
        // Правая колонка - задания
        int rightX = guiX + GUI_WIDTH / 2 + 20;
        int questY = guiY + 55;
        
        guiGraphics.drawString(this.font, Component.literal("§6§lЕжедневные задания"), 
            rightX, questY, 0xFFFFFF, false);
        questY += 25;
        
        // Вычисляем максимальную высоту для заданий, чтобы не перекрывать кнопки
        // Кнопки находятся на высоте guiY + GUI_HEIGHT - 35, оставляем отступ 10px
        int buttonAreaY = guiY + GUI_HEIGHT - 35;
        int maxQuestAreaHeight = buttonAreaY - questY - 15; // До кнопок минус отступ 15px
        int questCardHeight = 60;
        int questSpacing = 5;
        int maxVisibleQuests = Math.max(1, (maxQuestAreaHeight + questSpacing) / (questCardHeight + questSpacing));
        
        if (quests.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("§7Нет активных заданий"), 
                rightX, questY, 0xFFFFFF, false);
        } else {
            int maxQuests = Math.min(quests.size(), maxVisibleQuests);
            for (int i = 0; i < maxQuests; i++) {
                QuestInfo quest = quests.get(i);
                int questCardY = questY + i * (questCardHeight + questSpacing);
                
                // Проверяем, что задание не выходит за границы (с отступом 15px от кнопок)
                if (questCardY + questCardHeight > buttonAreaY - 15) {
                    break; // Прекращаем рендеринг, если выходим за границы
                }
                
                // ПЕРЕПИСАНО: Фон задания в скевоморфном стиле
                GuiUtils.drawLeatherElement(guiGraphics, rightX, questCardY, GUI_WIDTH / 2 - 40, questCardHeight);
                GuiUtils.drawMetalFrame(guiGraphics, rightX, questCardY, GUI_WIDTH / 2 - 40, questCardHeight, 1, false);
                
                // Описание
                guiGraphics.drawString(this.font, Component.literal("§7" + quest.description), 
                    rightX + 5, questCardY + 5, 0xFFFFFF, false);
                
                // Прогресс
                float questProgress = Math.min(1.0f, (float) quest.progress / quest.target);
                int progressBarWidth = GUI_WIDTH / 2 - 50;
                int progressBarHeight = 8;
                int progressBarX = rightX + 5;
                int progressBarY = questCardY + 20;
                
                // Простой прогресс-бар
                int progressBgColor = 0xFF3A3A3A;
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, progressBgColor);
                if (questProgress > 0) {
                    int fillColor = 0xFF9C27B0;
                    guiGraphics.fill(progressBarX + 1, progressBarY + 1, 
                        progressBarX + (int)(progressBarWidth * questProgress) - 1, 
                        progressBarY + progressBarHeight - 1, fillColor);
                }
                
                // Текст прогресса
                String progressText = quest.progress + " / " + quest.target;
                guiGraphics.drawString(this.font, Component.literal("§7" + progressText), 
                    progressBarX, progressBarY + 12, 0xFFFFFF, false);
                
                // Награда
                String rewardText = "§e+" + quest.rewardXP + " XP";
                if (quest.rewardCoins > 0) {
                    rewardText += " §6+" + quest.rewardCoins + " монет";
                }
                guiGraphics.drawString(this.font, Component.literal(rewardText), 
                    rightX + 5, questCardY + 45, 0xFFFFFF, false);
                
                // Статус выполнения
                if (quest.progress >= quest.target) {
                    guiGraphics.drawString(this.font, Component.literal("§a✓ Выполнено"), 
                        rightX + GUI_WIDTH / 2 - 80, questCardY + 45, 0xFFFFFF, false);
                }
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
