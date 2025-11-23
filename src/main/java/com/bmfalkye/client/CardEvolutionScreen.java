package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.evolution.CardEvolutionTree;
import com.bmfalkye.evolution.CardEvolutionSystem;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Экран эволюции карт - просмотр и улучшение карт
 */
public class CardEvolutionScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 500;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private List<Card> playerCards = new ArrayList<>();
    private List<Card> filteredCards = new ArrayList<>();
    private int scrollOffset = 0;
    private String selectedCardId = null;
    
    private Map<String, NetworkHandler.EvolutionData> evolutionData = new HashMap<>();
    private int soulDust = 0;
    
    private final Screen parentScreen;
    
    public CardEvolutionScreen() {
        this(null);
    }
    
    public CardEvolutionScreen(Screen parentScreen) {
        super(Component.literal("§6§lЭВОЛЮЦИЯ КАРТ"));
        this.parentScreen = parentScreen;
        loadData();
    }
    
    private void loadData() {
        // Загружаем коллекцию карт
        java.util.List<String> cached = com.bmfalkye.client.ClientPacketHandler.getCachedCardCollection();
        if (!cached.isEmpty()) {
            updateCollection(cached);
        } else {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardCollectionPacket());
        }
        
        // Загружаем данные эволюции
        Map<String, NetworkHandler.EvolutionData> cachedEvolution = 
            com.bmfalkye.client.ClientPacketHandler.getCachedEvolutionData();
        if (!cachedEvolution.isEmpty()) {
            updateEvolutionData(cachedEvolution, com.bmfalkye.client.ClientPacketHandler.getCachedSoulDust());
        } else {
            NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardEvolutionPacket());
        }
    }
    
    public void updateCollection(List<String> cardIds) {
        this.playerCards = cardIds.stream()
            .map(CardRegistry::getCard)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        updateFilters();
    }
    
    public void updateEvolutionData(Map<String, NetworkHandler.EvolutionData> evolutionData, int soulDust) {
        this.evolutionData = new HashMap<>(evolutionData);
        this.soulDust = soulDust;
    }
    
    private void updateFilters() {
        filteredCards = new ArrayList<>(playerCards);
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
        
        // Кнопка "Обновить"
        Button refreshButton = createStyledButton(
            layout.getX(83), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§eОбновить"),
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardCollectionPacket());
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardEvolutionPacket());
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
        graphics.drawString(font, "§6§lЭВОЛЮЦИЯ КАРТ", guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        // Пыль Душ
        graphics.drawString(font, "§dПыль Душ: §f" + soulDust, guiX + GUI_WIDTH - 150, guiY + 15, 0xFFFFFF, false);
        
        // Список карт слева
        renderCardList(graphics, guiX + 10, guiY + 40, 250, GUI_HEIGHT - 50, mouseX, mouseY);
        
        // Детали выбранной карты справа
        if (selectedCardId != null) {
            renderCardDetails(graphics, guiX + 270, guiY + 40, GUI_WIDTH - 280, GUI_HEIGHT - 50, mouseX, mouseY);
        } else {
            graphics.drawString(font, "§7Выберите карту для просмотра эволюции", 
                guiX + 280, guiY + 60, 0x888888, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderCardList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        graphics.drawString(font, "§lВаши карты:", x, y, 0xFFFFFF, false);
        
        int cardY = y + 20;
        int cardsPerPage = (height - 20) / 60;
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + cardsPerPage, filteredCards.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Card card = filteredCards.get(i);
            if (card == null) continue;
            
            NetworkHandler.EvolutionData data = evolutionData.get(card.getId());
            int level = data != null ? data.level : 1;
            int experience = data != null ? data.experience : 0;
            int expForNext = data != null ? data.experienceForNextLevel : 100;
            
            boolean isSelected = card.getId().equals(selectedCardId);
            int bgColor = isSelected ? 0x44FFAA00 : 0x44000000;
            
            // Фон карты
            graphics.fill(x, cardY, x + width - 5, cardY + 55, bgColor);
            
            // Название и уровень
            String cardName = card.getName();
            if (cardName.length() > 20) {
                cardName = cardName.substring(0, 17) + "...";
            }
            graphics.drawString(font, "§f" + cardName + " §6[Ур. " + level + "]", 
                x + 5, cardY + 5, 0xFFFFFF, false);
            
            // Прогресс опыта
            float progress = expForNext > 0 ? (float) experience / expForNext : 0f;
            int barWidth = width - 15;
            int barHeight = 8;
            graphics.fill(x + 5, cardY + 20, x + 5 + barWidth, cardY + 20 + barHeight, 0xFF333333);
            graphics.fill(x + 5, cardY + 20, x + 5 + (int)(barWidth * progress), 
                cardY + 20 + barHeight, 0xFF00AA00);
            graphics.drawString(font, "§7" + experience + "/" + expForNext + " опыта", 
                x + 5, cardY + 30, 0x888888, false);
            
            // Выделение выбранной карты
            if (isSelected) {
                graphics.fill(x, cardY, x + width - 5, cardY + 55, 0x44FFAA00);
            }
            
            cardY += 60;
        }
        
        // Скролл (обрабатывается в mouseClicked)
    }
    
    private void renderCardDetails(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        Card card = CardRegistry.getCard(selectedCardId);
        if (card == null) return;
        
        NetworkHandler.EvolutionData data = evolutionData.get(selectedCardId);
        int level = data != null ? data.level : 1;
        int experience = data != null ? data.experience : 0;
        int expForNext = data != null ? data.experienceForNextLevel : 100;
        Set<String> unlockedBranches = data != null ? data.unlockedBranches : new HashSet<>();
        
        // Информация о карте
        graphics.drawString(font, "§6§l" + card.getName(), x, y, 0xFFFFFF, false);
        graphics.drawString(font, "§7Уровень: §f" + level, x, y + 20, 0xFFFFFF, false);
        graphics.drawString(font, "§7Опыт: §f" + experience + "/" + expForNext, x, y + 35, 0xFFFFFF, false);
        
        // Прогресс-бар опыта
        float progress = expForNext > 0 ? (float) experience / expForNext : 0f;
        int barWidth = width - 20;
        int barHeight = 12;
        graphics.fill(x, y + 50, x + barWidth, y + 50 + barHeight, 0xFF333333);
        graphics.fill(x, y + 50, x + (int)(barWidth * progress), y + 50 + barHeight, 0xFF00AA00);
        
        // Древо эволюции
        CardEvolutionTree.EvolutionTreeData tree = CardEvolutionTree.getEvolutionTree(selectedCardId);
        if (tree != null) {
            graphics.drawString(font, "§lВетки улучшений:", x, y + 80, 0xFFFFFF, false);
            
            int branchY = y + 100;
            for (CardEvolutionTree.EvolutionBranch branch : tree.getBranches()) {
                boolean isUnlocked = unlockedBranches.contains(branch.getId());
                String branchStatus = isUnlocked ? "§a[Открыто]" : "§7[Закрыто]";
                
                graphics.drawString(font, "§6" + branch.getName() + " " + branchStatus, 
                    x, branchY, 0xFFFFFF, false);
                
                // Улучшения в ветке
                int upgradeY = branchY + 15;
                for (CardEvolutionTree.EvolutionUpgrade upgrade : branch.getUpgrades()) {
                    boolean upgradeAvailable = level >= upgrade.getRequiredLevel();
                    boolean upgradeUnlocked = isUnlocked; // Упрощённая логика
                    
                    String upgradeText = "  §7- " + upgrade.getName();
                    if (upgradeAvailable) {
                        upgradeText = "  §f- " + upgrade.getName();
                    }
                    if (upgradeUnlocked) {
                        upgradeText = "  §a✓ " + upgrade.getName();
                    }
                    
                    graphics.drawString(font, upgradeText, x + 10, upgradeY, 0xFFFFFF, false);
                    graphics.drawString(font, "§7    " + upgrade.getDescription(), 
                        x + 10, upgradeY + 12, 0x888888, false);
                    graphics.drawString(font, "§7    Требуется уровень: §f" + upgrade.getRequiredLevel(), 
                        x + 10, upgradeY + 24, 0x888888, false);
                    
                    upgradeY += 40;
                }
                
                // Кнопка открытия ветки
                if (!isUnlocked && level >= branch.getUpgrades().get(0).getRequiredLevel()) {
                    int cost = CardEvolutionSystem.getBranchUnlockCost(selectedCardId, branch.getId());
                    String buttonText = "Открыть за " + cost + " Пыли Душ";
                    boolean canAfford = soulDust >= cost;
                    
                    int buttonX = x + width - 150;
                    int buttonY = branchY;
                    int buttonWidth = 140;
                    int buttonHeight = 20;
                    
                    int buttonColor = canAfford ? 0xFF00AA00 : 0xFFAA0000;
                    graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
                    graphics.drawString(font, buttonText, buttonX + 5, buttonY + 6, 0xFFFFFF, false);
                    
                    // Кнопка будет обработана в mouseClicked
                }
                
                branchY = upgradeY + 20;
            }
        } else {
            graphics.drawString(font, "§7Для этой карты нет древа эволюции", x, y + 80, 0x888888, false);
        }
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
            
            // Проверяем клик по картам в списке
            int cardListX = guiX + 10;
            int cardListY = guiY + 40;
            int cardListWidth = 250;
            int cardListHeight = GUI_HEIGHT - 50;
            
            int cardsPerPage = (cardListHeight - 20) / 60;
            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + cardsPerPage, filteredCards.size());
            
            int cardY = cardListY + 20;
            for (int i = startIndex; i < endIndex; i++) {
                Card card = filteredCards.get(i);
                if (card != null && mouseX >= cardListX && mouseX < cardListX + cardListWidth - 5 && 
                    mouseY >= cardY && mouseY < cardY + 55) {
                    selectedCardId = card.getId();
                    return true;
                }
                cardY += 60;
            }
            
            // Проверяем клик по кнопкам открытия веток
            if (selectedCardId != null) {
                CardEvolutionTree.EvolutionTreeData tree = CardEvolutionTree.getEvolutionTree(selectedCardId);
                if (tree != null) {
                    NetworkHandler.EvolutionData data = evolutionData.get(selectedCardId);
                    int level = data != null ? data.level : 1;
                    Set<String> unlockedBranches = data != null ? data.unlockedBranches : new HashSet<>();
                    
                    int detailX = guiX + 270;
                    int detailY = guiY + 40;
                    int detailWidth = GUI_WIDTH - 280;
                    int detailHeight = GUI_HEIGHT - 50;
                    
                    int branchY = detailY + 100;
                    for (CardEvolutionTree.EvolutionBranch branch : tree.getBranches()) {
                        boolean isUnlocked = unlockedBranches.contains(branch.getId());
                        
                        if (!isUnlocked && level >= branch.getUpgrades().get(0).getRequiredLevel()) {
                            int cost = CardEvolutionSystem.getBranchUnlockCost(selectedCardId, branch.getId());
                            if (soulDust >= cost) {
                                int buttonX = detailX + detailWidth - 150;
                                int buttonY = branchY;
                                int buttonWidth = 140;
                                int buttonHeight = 20;
                                
                                if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && 
                                    mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
                                    NetworkHandler.INSTANCE.sendToServer(
                                        new NetworkHandler.UnlockBranchPacket(selectedCardId, branch.getId()));
                                    return true;
                                }
                            }
                        }
                        
                        branchY += branch.getUpgrades().size() * 40 + 20;
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else {
            scrollOffset = Math.min(filteredCards.size() - 10, scrollOffset + 1);
        }
        return true;
    }
}

