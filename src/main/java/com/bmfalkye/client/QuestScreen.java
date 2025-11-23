package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.quests.Quest;
import com.bmfalkye.quests.QuestSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран квестов - просмотр активных и завершённых квестов
 */
public class QuestScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 700;
    private static final int BASE_GUI_HEIGHT = 450;
    private static final int MIN_GUI_WIDTH = 600;
    private static final int MIN_GUI_HEIGHT = 400;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private List<String> activeQuests = new ArrayList<>();
    private List<String> completedQuests = new ArrayList<>();
    private String selectedQuestId = null;
    private boolean showingCompleted = false;
    
    private final Screen parentScreen;
    
    public QuestScreen() {
        this(null);
    }
    
    public QuestScreen(Screen parentScreen) {
        super(Component.literal("§6§lКВЕСТЫ"));
        this.parentScreen = parentScreen;
        loadQuests();
    }
    
    private void loadQuests() {
        // Запрашиваем данные квестов с сервера
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestQuestsPacket());
    }
    
    public void updateQuests(List<String> active, List<String> completed) {
        this.activeQuests = new ArrayList<>(active);
        this.completedQuests = new ArrayList<>(completed);
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Кнопка "Назад"
        Button backButton = createStyledButton(
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§7Назад"),
            (btn) -> {
                if (parentScreen != null) {
                    minecraft.setScreen(parentScreen);
                } else {
                    minecraft.setScreen(new FalkyeMainMenuScreen());
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        // Кнопка переключения между активными и завершёнными
        Button toggleButton = createStyledButton(
            layout.getX(83), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal(showingCompleted ? "§eАктивные" : "§7Завершённые"),
            (btn) -> {
                showingCompleted = !showingCompleted;
                selectedQuestId = null;
                init(); // Пересоздаём кнопки
            }
        );
        this.addRenderableWidget(toggleButton);
        
        // Кнопка "Обновить"
        Button refreshButton = createStyledButton(
            layout.getX(70), layout.getY(2), layout.getWidth(12), layout.getHeight(5),
            Component.literal("§eОбновить"),
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestQuestsPacket());
            }
        );
        this.addRenderableWidget(refreshButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
        
        // Рисуем фон
        graphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xC0101010);
        graphics.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + GUI_HEIGHT - 1, 0xFF2C2C2C);
        
        // Заголовок
        String title = showingCompleted ? "§6§lЗАВЕРШЁННЫЕ КВЕСТЫ" : "§6§lАКТИВНЫЕ КВЕСТЫ";
        graphics.drawString(font, title, guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        // Список квестов слева
        List<String> questsToShow = showingCompleted ? completedQuests : activeQuests;
        renderQuestList(graphics, guiX + 10, guiY + 40, 250, GUI_HEIGHT - 50, questsToShow);
        
        // Детали выбранного квеста справа
        if (selectedQuestId != null) {
            renderQuestDetails(graphics, guiX + 270, guiY + 40, GUI_WIDTH - 280, GUI_HEIGHT - 50);
        } else {
            graphics.drawString(font, "§7Выберите квест для просмотра деталей", 
                guiX + 280, guiY + 60, 0x888888, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderQuestList(GuiGraphics graphics, int x, int y, int width, int height, List<String> questIds) {
        int questY = y + 20;
        int questsPerPage = (height - 20) / 50;
        int startIndex = 0; // TODO: Добавить скролл
        int endIndex = Math.min(startIndex + questsPerPage, questIds.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String questId = questIds.get(i);
            Quest quest = QuestSystem.getQuest(questId);
            if (quest == null) continue;
            
            boolean isSelected = questId.equals(selectedQuestId);
            int bgColor = isSelected ? 0x44FFAA00 : 0x44000000;
            
            // Фон квеста
            graphics.fill(x, questY, x + width - 5, questY + 45, bgColor);
            
            // Название квеста
            String questName = quest.getName();
            if (questName.length() > 25) {
                questName = questName.substring(0, 22) + "...";
            }
            String rarityColor = getRarityColor(quest.getRarity());
            graphics.drawString(font, rarityColor + questName, x + 5, questY + 5, 0xFFFFFF, false);
            
            // Описание (сокращённое)
            String description = quest.getDescription();
            if (description.length() > 30) {
                description = description.substring(0, 27) + "...";
            }
            graphics.drawString(font, "§7" + description, x + 5, questY + 18, 0x888888, false);
            
            // Статус
            if (!showingCompleted) {
                graphics.drawString(font, "§aАктивен", x + 5, questY + 30, 0x00AA00, false);
            } else {
                graphics.drawString(font, "§7Завершён", x + 5, questY + 30, 0x888888, false);
            }
            
            questY += 50;
        }
    }
    
    private void renderQuestDetails(GuiGraphics graphics, int x, int y, int width, int height) {
        Quest quest = QuestSystem.getQuest(selectedQuestId);
        if (quest == null) return;
        
        // Название и описание
        String rarityColor = getRarityColor(quest.getRarity());
        graphics.drawString(font, rarityColor + "§l" + quest.getName(), x, y, 0xFFFFFF, false);
        graphics.drawString(font, "§7" + quest.getDescription(), x, y + 20, 0x888888, false);
        
        // Шаги квеста
        graphics.drawString(font, "§lШаги квеста:", x, y + 50, 0xFFFFFF, false);
        
        int stepY = y + 70;
        List<Quest.QuestStep> steps = quest.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            Quest.QuestStep step = steps.get(i);
            String stepText = "§7" + (i + 1) + ". " + step.getDescription();
            graphics.drawString(font, stepText, x + 10, stepY, 0x888888, false);
            stepY += 20;
        }
        
        // Награда
        graphics.drawString(font, "§lНаграда:", x, stepY + 10, 0xFFFFFF, false);
        Quest.QuestReward reward = quest.getReward();
        int rewardY = stepY + 30;
        
        if (!reward.getCardIds().isEmpty()) {
            graphics.drawString(font, "§6Карты: §f" + reward.getCardIds().size(), x + 10, rewardY, 0xFFFFFF, false);
            rewardY += 15;
        }
        if (reward.getCoins() > 0) {
            graphics.drawString(font, "§eМонеты: §f" + reward.getCoins(), x + 10, rewardY, 0xFFFFFF, false);
            rewardY += 15;
        }
        if (reward.getExperience() > 0) {
            graphics.drawString(font, "§aОпыт: §f" + reward.getExperience(), x + 10, rewardY, 0xFFFFFF, false);
            rewardY += 15;
        }
        if (reward.getAchievementId() != null && !reward.getAchievementId().isEmpty()) {
            graphics.drawString(font, "§dДостижение: §f" + reward.getAchievementId(), x + 10, rewardY, 0xFFFFFF, false);
        }
    }
    
    private String getRarityColor(Quest.QuestRarity rarity) {
        if (rarity == Quest.QuestRarity.COMMON) return "§f";
        if (rarity == Quest.QuestRarity.RARE) return "§b";
        if (rarity == Quest.QuestRarity.EPIC) return "§5";
        if (rarity == Quest.QuestRarity.LEGENDARY) return "§6";
        return "§f";
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            int guiX = (width - GUI_WIDTH) / 2;
            int guiY = (height - GUI_HEIGHT) / 2;
            
            List<String> questsToShow = showingCompleted ? completedQuests : activeQuests;
            int questListX = guiX + 10;
            int questListY = guiY + 40;
            int questListWidth = 250;
            int questListHeight = GUI_HEIGHT - 50;
            
            int questY = questListY + 20;
            int questsPerPage = (questListHeight - 20) / 50;
            int endIndex = Math.min(questsPerPage, questsToShow.size());
            
            for (int i = 0; i < endIndex; i++) {
                if (mouseX >= questListX && mouseX < questListX + questListWidth - 5 && 
                    mouseY >= questY && mouseY < questY + 45) {
                    selectedQuestId = questsToShow.get(i);
                    return true;
                }
                questY += 50;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

