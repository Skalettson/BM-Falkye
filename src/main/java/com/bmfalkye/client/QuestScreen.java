package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.quests.Quest;
import com.bmfalkye.quests.QuestSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран квестов - просмотр активных и завершённых квестов
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class QuestScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 850;
    private static final int BASE_GUI_HEIGHT = 550;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private List<String> activeQuests = new ArrayList<>();
    private List<String> completedQuests = new ArrayList<>();
    private String selectedQuestId = null;
    private boolean showingCompleted = false;
    
    private final Screen parentScreen;
    
    // Параметры прокрутки
    private int scrollOffset = 0;
    private static final int QUEST_ENTRY_HEIGHT = 55;
    private static final int QUEST_ENTRY_SPACING = 5;
    private int listScrollOffset = 0;
    
    public QuestScreen() {
        this(null);
    }
    
    public QuestScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.quests_title"));
        this.parentScreen = parentScreen;
        loadQuests();
    }
    
    private void loadQuests() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestQuestsPacket());
    }
    
    public void updateQuests(List<String> active, List<String> completed) {
        this.activeQuests = new ArrayList<>(active);
        this.completedQuests = new ArrayList<>(completed);
        this.scrollOffset = 0;
        this.listScrollOffset = 0;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    minecraft.setScreen(new FalkyeMainMenuScreen());
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        // Кнопка переключения между активными и завершёнными
        Button toggleButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(20), 2), layout.getY(2), 
            layout.getWidth(20), layout.getHeight(5),
            Component.translatable(showingCompleted ? 
                "screen.bm_falkye.show_active" : "screen.bm_falkye.show_completed")
                .withStyle(showingCompleted ? 
                    Style.EMPTY.withColor(ChatFormatting.YELLOW) :
                    Style.EMPTY.withColor(ChatFormatting.GRAY)),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                showingCompleted = !showingCompleted;
                selectedQuestId = null;
                scrollOffset = 0;
                listScrollOffset = 0;
                init();
            }
        );
        this.addRenderableWidget(toggleButton);
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getX(78), layout.getY(2), layout.getWidth(12), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                loadQuests();
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
        }
        
        this.renderBackground(graphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable(showingCompleted ? 
            "screen.bm_falkye.completed_quests_title" : "screen.bm_falkye.active_quests_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Область списка квестов слева
        int listX = layout.getX(2);
        int listY = layout.getY(10);
        int listWidth = layout.getWidth(32);
        int listHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8);
        
        // Область деталей справа
        int detailsX = layout.getX(36);
        int detailsY = layout.getY(10);
        int detailsWidth = layout.getWidth(62);
        int detailsHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8);
        
        // Список квестов слева
        List<String> questsToShow = showingCompleted ? completedQuests : activeQuests;
        renderQuestList(graphics, listX, listY, listWidth, listHeight, questsToShow, mouseX, mouseY);
        
        // Детали выбранного квеста справа
        if (selectedQuestId != null) {
            renderQuestDetails(graphics, detailsX, detailsY, detailsWidth, detailsHeight);
        } else {
            MutableComponent hint = Component.translatable("screen.bm_falkye.select_quest_hint")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), detailsY + detailsHeight / 2, 0x888888);
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderQuestList(GuiGraphics graphics, int x, int y, int width, int height, 
                                 List<String> questIds, int mouseX, int mouseY) {
        // Заголовок списка
        MutableComponent listTitle = Component.translatable("screen.bm_falkye.quest_list")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true));
        graphics.drawString(this.font, listTitle, x, y, 0xFFFFFF, false);
        
        int questListY = y + 20;
        int questListHeight = height - 25;
        
        // Обрезка списка квестов
        graphics.enableScissor(x, questListY, x + width, questListY + questListHeight);
        
        int visibleQuests = questListHeight / (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
        if (questIds.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_quests")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), questListY + questListHeight / 2, 0x888888);
        } else {
            int startIndex = listScrollOffset / (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleQuests + 1, questIds.size());
            
            int questY = questListY - (listScrollOffset % (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= questIds.size()) break;
                
                String questId = questIds.get(i);
                Quest quest = QuestSystem.getQuest(questId);
                if (quest == null) continue;
                
                boolean isSelected = questId.equals(selectedQuestId);
                boolean isHovered = mouseX >= x && mouseX <= x + width && 
                                   mouseY >= questY && mouseY <= questY + QUEST_ENTRY_HEIGHT;
                
                // Фон записи квеста
                int bgColor = isSelected ? 0xCCFFAA00 : (isHovered ? 0x88FFAA00 : 0x66000000);
                GuiUtils.drawRoundedRect(graphics, x, questY, width - 5, QUEST_ENTRY_HEIGHT, bgColor);
                
                if (isSelected) {
                    GuiUtils.drawRoundedBorder(graphics, x, questY, width - 5, QUEST_ENTRY_HEIGHT, 0xFFFFAA00, 2);
                }
                
                // Название квеста
                String questName = quest.getName();
                int maxNameWidth = width - 30;
                if (this.font.width(questName) > maxNameWidth) {
                    questName = this.font.plainSubstrByWidth(questName, maxNameWidth - 5) + "...";
                }
                
                ChatFormatting rarityColor = getRarityColor(quest.getRarity());
                MutableComponent name = Component.literal(questName)
                    .withStyle(Style.EMPTY.withColor(rarityColor));
                graphics.drawString(this.font, name, x + 8, questY + 5, 0xFFFFFF, false);
                
                // Описание (сокращённое)
                String description = quest.getDescription();
                int maxDescWidth = width - 30;
                if (this.font.width(description) > maxDescWidth) {
                    description = this.font.plainSubstrByWidth(description, maxDescWidth - 5) + "...";
                }
                MutableComponent desc = Component.literal(description)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                graphics.drawString(this.font, desc, x + 8, questY + 18, 0x888888, false);
                
                // Статус
                MutableComponent status;
                if (!showingCompleted) {
                    status = Component.translatable("screen.bm_falkye.quest_active")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                } else {
                    status = Component.translatable("screen.bm_falkye.quest_completed")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                }
                graphics.drawString(this.font, status, x + 8, questY + 32, 0xFFFFFF, false);
                
                questY += QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING;
            }
        }
        
        graphics.disableScissor();
        
        // Индикатор прокрутки
        if (questIds.size() > visibleQuests) {
            int scrollBarX = x + width - 8;
            int scrollBarY = questListY;
            int scrollBarHeight = questListHeight;
            int scrollBarWidth = 4;
            
            // Фон полосы прокрутки
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                scrollBarY + scrollBarHeight, 0x66000000);
            
            // Ползунок
            int maxScroll = Math.max(1, questIds.size() - visibleQuests);
            int sliderHeight = Math.max(20, (int)((double)visibleQuests / questIds.size() * scrollBarHeight));
            int sliderY = scrollBarY + (int)((double)listScrollOffset / (maxScroll * (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING)) * 
                (scrollBarHeight - sliderHeight));
            graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
        }
    }
    
    private void renderQuestDetails(GuiGraphics graphics, int x, int y, int width, int height) {
        Quest quest = QuestSystem.getQuest(selectedQuestId);
        if (quest == null) return;
        
        // Фон панели деталей
        GuiUtils.drawWoodenPanel(graphics, x, y, width, height, false);
        GuiUtils.drawMetalFrame(graphics, x, y, width, height, 2, true);
        
        int textX = x + 10;
        int textY = y + 10;
        int lineHeight = 15;
        
        // Название и описание
        ChatFormatting rarityColor = getRarityColor(quest.getRarity());
        MutableComponent name = Component.literal(quest.getName())
            .withStyle(Style.EMPTY.withColor(rarityColor).withBold(true));
        graphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 5;
        
        // Описание (многострочное)
        String description = quest.getDescription();
        int maxDescWidth = width - 20;
        MutableComponent descComponent = Component.literal(description)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(descComponent, maxDescWidth);
        for (net.minecraft.util.FormattedCharSequence line : descLines) {
            graphics.drawString(this.font, line, textX, textY, 0x888888, false);
            textY += lineHeight;
            if (textY > y + height - 150) break; // Ограничиваем высоту описания
        }
        textY += 5;
        
        // Разделитель
        graphics.fill(textX, textY, x + width - 10, textY + 1, 0xFF8B7355);
        textY += 10;
        
        // Шаги квеста
        MutableComponent stepsTitle = Component.translatable("screen.bm_falkye.quest_steps")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true));
        graphics.drawString(this.font, stepsTitle, textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 5;
        
        List<Quest.QuestStep> steps = quest.getSteps();
        for (int i = 0; i < steps.size() && textY < y + height - 100; i++) {
            Quest.QuestStep step = steps.get(i);
            MutableComponent stepText = Component.translatable("screen.bm_falkye.quest_step", 
                i + 1, step.getDescription())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            List<net.minecraft.util.FormattedCharSequence> stepLines = this.font.split(stepText, maxDescWidth);
            for (net.minecraft.util.FormattedCharSequence line : stepLines) {
                graphics.drawString(this.font, line, textX + 10, textY, 0x888888, false);
                textY += lineHeight;
                if (textY > y + height - 100) break;
            }
        }
        textY += 5;
        
        // Разделитель
        graphics.fill(textX, textY, x + width - 10, textY + 1, 0xFF8B7355);
        textY += 10;
        
        // Награда
        MutableComponent rewardTitle = Component.translatable("screen.bm_falkye.quest_reward")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        graphics.drawString(this.font, rewardTitle, textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 5;
        
        Quest.QuestReward reward = quest.getReward();
        
        if (!reward.getCardIds().isEmpty()) {
            MutableComponent cards = Component.translatable("screen.bm_falkye.reward_cards", 
                reward.getCardIds().size())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
            graphics.drawString(this.font, cards, textX + 10, textY, 0xFFFFFF, false);
            textY += lineHeight;
        }
        if (reward.getCoins() > 0) {
            MutableComponent coins = Component.translatable("screen.bm_falkye.reward_coins", reward.getCoins())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            graphics.drawString(this.font, coins, textX + 10, textY, 0xFFFFFF, false);
            textY += lineHeight;
        }
        if (reward.getExperience() > 0) {
            MutableComponent exp = Component.translatable("screen.bm_falkye.reward_experience", reward.getExperience())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            graphics.drawString(this.font, exp, textX + 10, textY, 0xFFFFFF, false);
            textY += lineHeight;
        }
        if (reward.getAchievementId() != null && !reward.getAchievementId().isEmpty()) {
            MutableComponent achievement = Component.translatable("screen.bm_falkye.reward_achievement", 
                reward.getAchievementId())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE));
            graphics.drawString(this.font, achievement, textX + 10, textY, 0xFFFFFF, false);
        }
    }
    
    private ChatFormatting getRarityColor(Quest.QuestRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.DARK_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            int listX = layout.getX(2);
            int listY = layout.getY(10) + 20;
            int listWidth = layout.getWidth(32);
            int listHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8) - 25;
            
            List<String> questsToShow = showingCompleted ? completedQuests : activeQuests;
            
            if (mouseX >= listX && mouseX <= listX + listWidth && 
                mouseY >= listY && mouseY <= listY + listHeight) {
                
                int visibleQuests = listHeight / (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
                int startIndex = listScrollOffset / (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
                int endIndex = Math.min(startIndex + visibleQuests + 1, questsToShow.size());
                
                int questY = listY - (listScrollOffset % (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING));
                
                for (int i = startIndex; i < endIndex; i++) {
                    if (i >= questsToShow.size()) break;
                    
                    if (mouseX >= listX && mouseX < listX + listWidth - 5 && 
                        mouseY >= questY && mouseY < questY + QUEST_ENTRY_HEIGHT) {
                        
                        String clickedQuestId = questsToShow.get(i);
                        if (!clickedQuestId.equals(selectedQuestId)) {
                            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        }
                        selectedQuestId = clickedQuestId;
                        return true;
                    }
                    questY += QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null) return super.mouseScrolled(mouseX, mouseY, delta);
        
        int listX = layout.getX(2);
        int listY = layout.getY(10) + 20;
        int listWidth = layout.getWidth(32);
        int listHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8) - 25;
        
        List<String> questsToShow = showingCompleted ? completedQuests : activeQuests;
        
        // Проверяем, находится ли мышь в области списка квестов
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleQuests = listHeight / (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
            int totalQuests = questsToShow.size();
            int maxScroll = Math.max(0, totalQuests - visibleQuests) * (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING);
            int scrollStep = QUEST_ENTRY_HEIGHT + QUEST_ENTRY_SPACING;
            
            if (delta < 0 && listScrollOffset < maxScroll) {
                listScrollOffset = Math.min(maxScroll, listScrollOffset + scrollStep);
                return true;
            } else if (delta > 0 && listScrollOffset > 0) {
                listScrollOffset = Math.max(0, listScrollOffset - scrollStep);
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

