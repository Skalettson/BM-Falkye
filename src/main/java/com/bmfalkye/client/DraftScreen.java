package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран драфта - выбор карт для "Великого Турнира"
 */
public class DraftScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 800;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    // Данные драфта
    private List<Card> currentChoice = new ArrayList<>(); // Текущие 3 карты на выбор
    private int currentChoiceIndex = 0; // Текущий выбор (0-29)
    private int totalChoices = 30;
    private List<String> selectedCards = new ArrayList<>(); // Уже выбранные карты
    private boolean draftCompleted = false;
    
    private final Screen parentScreen;
    
    public DraftScreen() {
        this(null);
    }
    
    public DraftScreen(Screen parentScreen) {
        super(Component.literal("§6§lВЕЛИКИЙ ТУРНИР - ДРАФТ"));
        this.parentScreen = parentScreen;
        requestDraftData();
    }
    
    private void requestDraftData() {
        // Запрашиваем данные драфта с сервера
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDraftDataPacket());
    }
    
    public void updateDraftData(List<String> cardIds1, List<String> cardIds2, List<String> cardIds3, 
                               int currentIndex, List<String> selected, boolean completed) {
        // Обновляем текущий выбор
        currentChoice.clear();
        if (cardIds1 != null && !cardIds1.isEmpty()) {
            Card card1 = CardRegistry.getCard(cardIds1.get(0));
            if (card1 != null) currentChoice.add(card1);
        }
        if (cardIds2 != null && !cardIds2.isEmpty()) {
            Card card2 = CardRegistry.getCard(cardIds2.get(0));
            if (card2 != null) currentChoice.add(card2);
        }
        if (cardIds3 != null && !cardIds3.isEmpty()) {
            Card card3 = CardRegistry.getCard(cardIds3.get(0));
            if (card3 != null) currentChoice.add(card3);
        }
        
        this.currentChoiceIndex = currentIndex;
        this.selectedCards = new ArrayList<>(selected);
        this.draftCompleted = completed;
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
        
        // Кнопки выбора карт (если есть выбор)
        if (!draftCompleted && currentChoice.size() == 3) {
            int cardButtonWidth = layout.getWidth(25);
            int cardButtonHeight = layout.getHeight(40);
            int cardSpacing = layout.getWidth(3);
            int totalWidth = cardButtonWidth * 3 + cardSpacing * 2;
            int startX = (width - totalWidth) / 2;
            int cardY = layout.getY(50);
            
            for (int i = 0; i < 3; i++) {
                final int cardIndex = i;
                Card card = currentChoice.get(i);
                if (card != null) {
                    Button cardButton = createStyledButton(
                        startX + i * (cardButtonWidth + cardSpacing),
                        cardY,
                        cardButtonWidth,
                        cardButtonHeight,
                        Component.literal("§6Выбрать"),
                        (btn) -> selectCard(cardIndex)
                    );
                    this.addRenderableWidget(cardButton);
                }
            }
        }
        
        // Кнопка начала арены (если драфт завершён)
        if (draftCompleted) {
            Button startArenaButton = createStyledButton(
                layout.getX(40), layout.getY(60), layout.getWidth(20), layout.getHeight(6),
                Component.literal("§6§lНачать Арену"),
                (btn) -> {
                    // Отправляем запрос на начало арены
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                        new com.bmfalkye.network.NetworkHandler.StartArenaPacket());
                }
            );
            this.addRenderableWidget(startArenaButton);
        }
    }
    
    private void selectCard(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= currentChoice.size()) {
            return;
        }
        
        // Отправляем выбор на сервер
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.SelectDraftCardPacket(currentChoiceIndex, cardIndex));
        
        // Обновляем локально (сервер подтвердит)
        Card selectedCard = currentChoice.get(cardIndex);
        if (selectedCard != null) {
            selectedCards.add(selectedCard.getId());
            currentChoiceIndex++;
            
            if (currentChoiceIndex >= totalChoices) {
                draftCompleted = true;
            }
            
            // Пересоздаём экран для обновления
            init();
        }
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
        String title = draftCompleted ? "§6§lДРАФТ ЗАВЕРШЁН" : "§6§lВЕЛИКИЙ ТУРНИР - ДРАФТ";
        graphics.drawString(font, title, guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        // Прогресс
        String progress = "Выбор " + (currentChoiceIndex + 1) + " из " + totalChoices;
        graphics.drawString(font, "§7" + progress, guiX + 20, guiY + 35, 0x888888, false);
        
        // Выбранные карты
        graphics.drawString(font, "§7Выбрано карт: §f" + selectedCards.size() + " / 30", 
            guiX + 20, guiY + 50, 0x888888, false);
        
        if (draftCompleted) {
            // Драфт завершён - показываем кнопку начала арены
            graphics.drawString(font, "§a§lДрафт успешно завершён!", 
                guiX + GUI_WIDTH / 2 - 100, guiY + GUI_HEIGHT / 2, 0x00FF00, false);
            graphics.drawString(font, "§7Нажмите кнопку ниже, чтобы начать арену", 
                guiX + GUI_WIDTH / 2 - 120, guiY + GUI_HEIGHT / 2 + 20, 0x888888, false);
        } else if (currentChoice.size() == 3) {
            // Рисуем 3 карты на выбор
            int cardWidth = 200;
            int cardHeight = 280;
            int cardSpacing = 30;
            int totalWidth = cardWidth * 3 + cardSpacing * 2;
            int startX = guiX + (GUI_WIDTH - totalWidth) / 2;
            int cardY = guiY + 80;
            
            for (int i = 0; i < 3; i++) {
                Card card = currentChoice.get(i);
                if (card != null) {
                    renderCard(graphics, card, startX + i * (cardWidth + cardSpacing), cardY, 
                               cardWidth, cardHeight, mouseX, mouseY);
                }
            }
            
            // Инструкция
            graphics.drawString(font, "§7Выберите одну из трёх карт", 
                guiX + GUI_WIDTH / 2 - 80, guiY + GUI_HEIGHT - 50, 0x888888, false);
        } else {
            // Загрузка
            graphics.drawString(font, "§7Загрузка выбора карт...", 
                guiX + GUI_WIDTH / 2 - 80, guiY + GUI_HEIGHT / 2, 0x888888, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderCard(GuiGraphics graphics, Card card, int x, int y, int width, int height, 
                           int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Фон карты
        int bgColor = isHovered ? 0xFF444444 : 0xFF333333;
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1A1A1A);
        
        // Рамка по редкости
        String rarityColor = getRarityColor(card.getRarity());
        graphics.fill(x, y, x + width, y + 3, parseColor(rarityColor + "FF"));
        
        // Название карты
        String name = card.getName();
        if (name.length() > 20) {
            name = name.substring(0, 17) + "...";
        }
        graphics.drawString(font, rarityColor + name, x + 5, y + 10, 0xFFFFFF, false);
        
        // Сила карты
        graphics.drawString(font, "§fСила: §e" + card.getPower(), x + 5, y + 25, 0xFFFFFF, false);
        
        // Тип карты
        String typeName = card.getType().toString();
        graphics.drawString(font, "§7Тип: §f" + typeName, 
            x + 5, y + 40, 0x888888, false);
        
        // Описание (сокращённое)
        String description = card.getDescription();
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        // Разбиваем описание на строки
        int descY = y + 60;
        int maxWidth = width - 10;
        String[] words = description.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.length() > 0 ? line + " " + word : word;
            if (font.width(testLine) > maxWidth) {
                if (line.length() > 0) {
                    graphics.drawString(font, "§7" + line.toString(), x + 5, descY, 0x888888, false);
                    descY += 12;
                    line = new StringBuilder(word);
                } else {
                    graphics.drawString(font, "§7" + word, x + 5, descY, 0x888888, false);
                    descY += 12;
                }
            } else {
                line.append(line.length() > 0 ? " " + word : word);
            }
        }
        if (line.length() > 0) {
            graphics.drawString(font, "§7" + line.toString(), x + 5, descY, 0x888888, false);
        }
        
        // Редкость внизу
        graphics.drawString(font, rarityColor + "§l" + card.getRarity().getDisplayName(), 
            x + 5, y + height - 20, 0xFFFFFF, false);
    }
    
    private String getRarityColor(com.bmfalkye.cards.CardRarity rarity) {
        if (rarity == com.bmfalkye.cards.CardRarity.COMMON) return "§f";
        if (rarity == com.bmfalkye.cards.CardRarity.RARE) return "§b";
        if (rarity == com.bmfalkye.cards.CardRarity.EPIC) return "§5";
        if (rarity == com.bmfalkye.cards.CardRarity.LEGENDARY) return "§6";
        return "§f";
    }
    
    private int parseColor(String colorCode) {
        // Простой парсер цветов Minecraft
        if (colorCode.startsWith("§")) {
            colorCode = colorCode.substring(2);
        }
        try {
            return Integer.parseUnsignedInt(colorCode, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
}

