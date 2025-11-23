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
 * Красивый экран ежедневных наград с календарём и заданиями
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class DailyRewardsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    
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
    private final Screen parentScreen;
    
    public DailyRewardsScreen() {
        this(null);
    }
    
    public DailyRewardsScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.daily_rewards_title"));
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
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
        }
        
        // Кнопка "Получить награду"
        MutableComponent claimText = Component.translatable("screen.bm_falkye.claim_reward")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
        if (claimed) {
            claimText = Component.translatable("screen.bm_falkye.reward_already_claimed")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        }
        
        Button claimButton = GuiUtils.createStyledButton(
            layout.getCenteredX(layout.getWidth(30)), layout.getBottomY(layout.getHeight(6), 2), 
            layout.getWidth(30), layout.getHeight(6),
            claimText,
            (btn) -> {
                if (!claimed && minecraft != null) {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.ClaimDailyRewardPacket());
                    dataLoaded = false;
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
                }
            }
        );
        this.addRenderableWidget(claimButton);
        
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
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDailyRewardsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
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
        this.init();
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
        MutableComponent title = Component.translatable("screen.bm_falkye.daily_rewards_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true));
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
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFF9C27B0);
        
        int contentY = layout.getY(11);
        int contentHeight = GUI_HEIGHT - contentY - layout.getHeight(12);
        
        if (!dataLoaded) {
            MutableComponent loading = Component.translatable("screen.bm_falkye.loading_data")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, loading, 
                layout.getCenteredX(this.font.width(loading)), contentY + contentHeight / 2, 0xFFFFFF);
        } else {
            // Левая колонка - календарь наград
            int calendarX = layout.getX(3);
            int calendarY = contentY;
            renderCalendar(guiGraphics, calendarX, calendarY, layout.getWidth(45), contentHeight);
            
            // Правая колонка - задания
            int questsX = layout.getX(50);
            int questsY = contentY;
            renderQuests(guiGraphics, questsX, questsY, layout.getWidth(47), contentHeight);
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderCalendar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Заголовок календаря
        MutableComponent calendarTitle = Component.translatable("screen.bm_falkye.rewards_calendar", 7)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, calendarTitle, x, y, 0xFFFFFF, false);
        y += 20;
        
        // Серия
        MutableComponent streakText = Component.translatable("screen.bm_falkye.streak_days", streak)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
        guiGraphics.drawString(this.font, streakText, x, y, 0xFFFFFF, false);
        y += 25;
        
        // Календарь (7 дней)
        int dayCardWidth = (width - layout.getSpacing() * 6) / 7;
        int dayCardHeight = (int)(dayCardWidth * 1.2f);
        int daySpacing = layout.getSpacing();
        int calendarStartX = x;
        int calendarY = y;
        
        for (int d = 1; d <= 7; d++) {
            int dayX = calendarStartX + (d - 1) * (dayCardWidth + daySpacing);
            
            // Цвет в зависимости от статуса
            int bgColor;
            int borderColor;
            if (d < day) {
                bgColor = 0xAA4CAF50; // Получено
                borderColor = 0xFF66BB6A;
            } else if (d == day) {
                bgColor = claimed ? 0xAA4CAF50 : 0xAAFFA500; // Текущий день
                borderColor = claimed ? 0xFF66BB6A : 0xFFFFB74D;
            } else {
                bgColor = 0xAA404040; // Недоступно
                borderColor = 0xFF505050;
            }
            
            // Карточка дня
            GuiUtils.drawLeatherElement(guiGraphics, dayX, calendarY, dayCardWidth, dayCardHeight);
            // Цветной оверлей
            guiGraphics.fill(dayX + 2, calendarY + 2, dayX + dayCardWidth - 2, 
                calendarY + dayCardHeight - 2, bgColor);
            GuiUtils.drawMetalFrame(guiGraphics, dayX, calendarY, dayCardWidth, dayCardHeight, 2, false);
            
            // День
            MutableComponent dayText = Component.translatable("screen.bm_falkye.day", d)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            guiGraphics.drawString(this.font, dayText, dayX + 5, calendarY + 5, 0xFFFFFF, false);
            
            // Награда (пример)
            MutableComponent rewardText;
            if (d <= 3) {
                rewardText = Component.translatable("screen.bm_falkye.coins_reward", d * 10)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            } else if (d <= 5) {
                rewardText = Component.translatable("screen.bm_falkye.xp_reward", d * 5)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
            } else {
                rewardText = Component.translatable("screen.bm_falkye.rare_card")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE));
            }
            List<net.minecraft.util.FormattedCharSequence> rewardLines = this.font.split(rewardText, dayCardWidth - 10);
            int rewardY = calendarY + 20;
            for (net.minecraft.util.FormattedCharSequence line : rewardLines) {
                if (rewardY > calendarY + dayCardHeight - 15) break;
                guiGraphics.drawString(this.font, line, dayX + 5, rewardY, 0xFFFFFF, false);
                rewardY += 10;
            }
            
            // Статус
            if (d < day) {
                guiGraphics.drawString(this.font, "✓", dayX + dayCardWidth - 15, 
                    calendarY + 5, 0xFF00FF00, false);
            } else if (d == day && !claimed) {
                guiGraphics.drawString(this.font, "!", dayX + dayCardWidth - 15, 
                    calendarY + 5, 0xFFFFFF00, false);
            }
        }
    }
    
    private void renderQuests(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Заголовок заданий
        MutableComponent questsTitle = Component.translatable("screen.bm_falkye.daily_quests")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        guiGraphics.drawString(this.font, questsTitle, x, y, 0xFFFFFF, false);
        y += 25;
        
        // Область заданий
        int questsAreaY = y;
        int questsAreaHeight = height - 25;
        
        // Обрезка заданий
        guiGraphics.enableScissor(x, questsAreaY, x + width, questsAreaY + questsAreaHeight);
        
        if (quests.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_active_quests")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), questsAreaY + questsAreaHeight / 2, 0x888888);
        } else {
            int questCardHeight = 70;
            int questSpacing = 5;
            int visibleQuests = questsAreaHeight / (questCardHeight + questSpacing);
            
            for (int i = 0; i < Math.min(quests.size(), visibleQuests + 1); i++) {
                QuestInfo quest = quests.get(i);
                int questCardY = questsAreaY + i * (questCardHeight + questSpacing);
                
                // Фон задания
                GuiUtils.drawLeatherElement(guiGraphics, x, questCardY, width, questCardHeight);
                GuiUtils.drawMetalFrame(guiGraphics, x, questCardY, width, questCardHeight, 1, false);
                
                int questX = x + 5;
                int questY = questCardY + 5;
                
                // Описание
                MutableComponent desc = Component.literal(quest.description)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(desc, width - 10);
                for (int j = 0; j < Math.min(descLines.size(), 2); j++) {
                    guiGraphics.drawString(this.font, descLines.get(j), questX, questY + j * 10, 0xFFFFFF, false);
                }
                questY += 25;
                
                // Прогресс-бар
                float questProgress = quest.target > 0 ? 
                    Math.min(1.0f, (float) quest.progress / quest.target) : 0f;
                int progressBarWidth = width - 10;
                int progressBarHeight = 10;
                int progressBarX = questX;
                int progressBarY = questY;
                
                // Фон прогресс-бара
                guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, 
                    progressBarY + progressBarHeight, 0xFF3A3A3A);
                // Прогресс
                if (questProgress > 0) {
                    int progressWidth = (int)(progressBarWidth * questProgress);
                    guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, 
                        progressBarY + progressBarHeight, 0xFF9C27B0);
                }
                // Рамка
                GuiUtils.drawRoundedBorder(guiGraphics, progressBarX, progressBarY, 
                    progressBarWidth, progressBarHeight, 0xFF555555, 1);
                
                // Текст прогресса
                MutableComponent progressText = Component.translatable("screen.bm_falkye.progress", 
                    quest.progress, quest.target)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                int progressTextX = progressBarX + (progressBarWidth - this.font.width(progressText)) / 2;
                guiGraphics.drawString(this.font, progressText, progressTextX, progressBarY + 2, 0xFFFFFF, false);
                questY += 15;
                
                // Награда
                MutableComponent rewardText = Component.empty();
                if (quest.rewardXP > 0) {
                    rewardText = rewardText.append(Component.translatable("screen.bm_falkye.reward_xp", quest.rewardXP)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));
                }
                if (quest.rewardCoins > 0) {
                    if (quest.rewardXP > 0) {
                        rewardText = rewardText.append(Component.literal(" "));
                    }
                    rewardText = rewardText.append(Component.translatable("screen.bm_falkye.reward_coins", quest.rewardCoins)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                }
                guiGraphics.drawString(this.font, rewardText, questX, questY, 0xFFFFFF, false);
                
                // Статус выполнения
                if (quest.progress >= quest.target) {
                    MutableComponent completed = Component.translatable("screen.bm_falkye.completed")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                    int completedX = x + width - this.font.width(completed) - 5;
                    guiGraphics.drawString(this.font, completed, completedX, questY, 0xFFFFFF, false);
                }
            }
        }
        
        guiGraphics.disableScissor();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
