package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.evolution.CardEvolutionTree;
import com.bmfalkye.evolution.CardEvolutionSystem;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Экран эволюции карт - просмотр и улучшение карт
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class CardEvolutionScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 950;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 800;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private List<Card> playerCards = new ArrayList<>();
    private List<Card> filteredCards = new ArrayList<>();
    private int listScrollOffset = 0;
    private int detailsScrollOffset = 0;
    private String selectedCardId = null;
    
    private Map<String, NetworkHandler.EvolutionData> evolutionData = new HashMap<>();
    private int soulDust = 0;
    
    private final Screen parentScreen;
    
    // Параметры прокрутки
    private static final int CARD_ENTRY_HEIGHT = 65;
    private static final int CARD_ENTRY_SPACING = 5;
    
    public CardEvolutionScreen() {
        this(null);
    }
    
    public CardEvolutionScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.card_evolution_title"));
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
        listScrollOffset = 0;
    }
    
    public void updateEvolutionData(Map<String, NetworkHandler.EvolutionData> evolutionData, int soulDust) {
        this.evolutionData = new HashMap<>(evolutionData);
        this.soulDust = soulDust;
        detailsScrollOffset = 0;
    }
    
    private void updateFilters() {
        filteredCards = new ArrayList<>(playerCards);
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
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardCollectionPacket());
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardEvolutionPacket());
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
        MutableComponent title = Component.translatable("screen.bm_falkye.card_evolution_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Пыль Душ справа
        MutableComponent soulDustText = Component.translatable("screen.bm_falkye.soul_dust", soulDust)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE));
        int soulDustX = layout.getRightX(this.font.width(soulDustText), 3);
        graphics.drawString(this.font, soulDustText, soulDustX, titleY, 0xFFFFFF, false);
        
        // Область списка карт слева
        int listX = layout.getX(2);
        int listY = layout.getY(10);
        int listWidth = layout.getWidth(32);
        int listHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8);
        
        // Область деталей справа
        int detailsX = layout.getX(36);
        int detailsY = layout.getY(10);
        int detailsWidth = layout.getWidth(62);
        int detailsHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8);
        
        // Список карт слева
        renderCardList(graphics, listX, listY, listWidth, listHeight, mouseX, mouseY);
        
        // Детали выбранной карты справа
        if (selectedCardId != null) {
            renderCardDetails(graphics, detailsX, detailsY, detailsWidth, detailsHeight, mouseX, mouseY);
        } else {
            MutableComponent hint = Component.translatable("screen.bm_falkye.select_card_evolution_hint")
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
    
    private void renderCardList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Заголовок списка
        MutableComponent listTitle = Component.translatable("screen.bm_falkye.your_cards")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true));
        graphics.drawString(this.font, listTitle, x, y, 0xFFFFFF, false);
        
        int cardListY = y + 20;
        int cardListHeight = height - 25;
        
        // Обрезка списка карт
        graphics.enableScissor(x, cardListY, x + width, cardListY + cardListHeight);
        
        if (filteredCards.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_cards")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), cardListY + cardListHeight / 2, 0x888888);
        } else {
            int visibleCards = cardListHeight / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
            int startIndex = listScrollOffset / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCards + 1, filteredCards.size());
            
            int cardY = cardListY - (listScrollOffset % (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= filteredCards.size()) break;
                
                Card card = filteredCards.get(i);
                if (card == null) continue;
                
                NetworkHandler.EvolutionData data = evolutionData.get(card.getId());
                int cardLevel = data != null ? data.level : 1;
                int experience = data != null ? data.experience : 0;
                int expForNext = data != null ? data.experienceForNextLevel : 100;
                
                boolean isSelected = card.getId().equals(selectedCardId);
                boolean isHovered = mouseX >= x && mouseX <= x + width && 
                                   mouseY >= cardY && mouseY <= cardY + CARD_ENTRY_HEIGHT;
                
                // Фон записи карты
                int bgColor = isSelected ? 0xCCFFAA00 : (isHovered ? 0x88FFAA00 : 0x66000000);
                GuiUtils.drawRoundedRect(graphics, x, cardY, width - 5, CARD_ENTRY_HEIGHT, bgColor);
                
                if (isSelected) {
                    GuiUtils.drawRoundedBorder(graphics, x, cardY, width - 5, CARD_ENTRY_HEIGHT, 0xFFFFAA00, 2);
                }
                
                // Название и уровень
                String cardName = card.getName();
                int maxNameWidth = width - 30;
                if (this.font.width(cardName) > maxNameWidth) {
                    cardName = this.font.plainSubstrByWidth(cardName, maxNameWidth - 5) + "...";
                }
                
                MutableComponent name = Component.literal(cardName)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
                MutableComponent levelText = Component.translatable("screen.bm_falkye.level", cardLevel)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
                graphics.drawString(this.font, name, x + 8, cardY + 5, 0xFFFFFF, false);
                graphics.drawString(this.font, levelText, x + 8, cardY + 18, 0xFFFFFF, false);
                
                // Прогресс опыта
                float progress = expForNext > 0 ? (float) experience / expForNext : 0f;
                int barWidth = width - 16;
                int barHeight = 10;
                int barX = x + 8;
                int barY = cardY + 32;
                
                // Фон прогресс-бара
                graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
                // Прогресс
                int progressWidth = (int)(barWidth * progress);
                if (progressWidth > 0) {
                    graphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00AA00);
                }
                // Рамка прогресс-бара
                graphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF555555);
                graphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFF555555);
                graphics.fill(barX, barY, barX + 1, barY + barHeight, 0xFF555555);
                graphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                
                // Текст опыта
                MutableComponent expText = Component.translatable("screen.bm_falkye.experience", 
                    experience, expForNext)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                int expTextX = barX + (barWidth - this.font.width(expText)) / 2;
                graphics.drawString(this.font, expText, expTextX, barY + 2, 0xFFFFFF, false);
                
                cardY += CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING;
            }
        }
        
        graphics.disableScissor();
        
        // Индикатор прокрутки
        int visibleCards = cardListHeight / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
        if (filteredCards.size() > visibleCards) {
            int scrollBarX = x + width - 8;
            int scrollBarY = cardListY;
            int scrollBarHeight = cardListHeight;
            int scrollBarWidth = 4;
            
            // Фон полосы прокрутки
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                scrollBarY + scrollBarHeight, 0x66000000);
            
            // Ползунок
            int maxScroll = Math.max(1, filteredCards.size() - visibleCards);
            int sliderHeight = Math.max(20, (int)((double)visibleCards / filteredCards.size() * scrollBarHeight));
            int sliderY = scrollBarY + (int)((double)listScrollOffset / (maxScroll * (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING)) * 
                (scrollBarHeight - sliderHeight));
            graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
        }
    }
    
    private void renderCardDetails(GuiGraphics graphics, int x, int y, int width, int height, 
                                     int mouseX, int mouseY) {
        Card card = CardRegistry.getCard(selectedCardId);
        if (card == null) return;
        
        // Обрезка деталей
        graphics.enableScissor(x, y, x + width, y + height);
        
        NetworkHandler.EvolutionData data = evolutionData.get(selectedCardId);
        int level = data != null ? data.level : 1;
        int experience = data != null ? data.experience : 0;
        int expForNext = data != null ? data.experienceForNextLevel : 100;
        Set<String> unlockedBranches = data != null ? data.unlockedBranches : new HashSet<>();
        
        // Фон панели деталей
        GuiUtils.drawWoodenPanel(graphics, x, y, width, height, false);
        GuiUtils.drawMetalFrame(graphics, x, y, width, height, 2, true);
        
        int textX = x + 10;
        int textY = y + 10 - detailsScrollOffset;
        int lineHeight = 16;
        
        // Информация о карте
        MutableComponent name = Component.literal(card.getName())
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        graphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 5;
        
        MutableComponent levelText = Component.translatable("screen.bm_falkye.level", level)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        graphics.drawString(this.font, levelText, textX, textY, 0xFFFFFF, false);
        textY += lineHeight;
        
        MutableComponent expText = Component.translatable("screen.bm_falkye.experience", 
            experience, expForNext)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        graphics.drawString(this.font, expText, textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 5;
        
        // Прогресс-бар опыта
        float progress = expForNext > 0 ? (float) experience / expForNext : 0f;
        int barWidth = width - 20;
        int barHeight = 14;
        int barX = textX;
        int barY = textY;
        
        // Фон прогресс-бара
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        // Прогресс
        int progressWidth = (int)(barWidth * progress);
        if (progressWidth > 0) {
            graphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00AA00);
        }
        // Рамка прогресс-бара
        GuiUtils.drawRoundedBorder(graphics, barX, barY, barWidth, barHeight, 0xFF555555, 1);
        
        textY += barHeight + 15;
        
        // Разделитель
        graphics.fill(textX, textY, x + width - 10, textY + 1, 0xFF8B7355);
        textY += 10;
        
        // Древо эволюции
        CardEvolutionTree.EvolutionTreeData tree = CardEvolutionTree.getEvolutionTree(selectedCardId);
        if (tree != null) {
            MutableComponent branchesTitle = Component.translatable("screen.bm_falkye.evolution_branches")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true));
            graphics.drawString(this.font, branchesTitle, textX, textY, 0xFFFFFF, false);
            textY += lineHeight + 10;
            
            for (CardEvolutionTree.EvolutionBranch branch : tree.getBranches()) {
                boolean isUnlocked = unlockedBranches.contains(branch.getId());
                
                // Фон ветки
                int branchBgColor = isUnlocked ? 0x4400AA00 : 0x44444444;
                int branchHeight = branch.getUpgrades().size() * 60 + 30;
                GuiUtils.drawRoundedRect(graphics, textX, textY, width - 20, branchHeight, branchBgColor);
                GuiUtils.drawRoundedBorder(graphics, textX, textY, width - 20, branchHeight, 
                    isUnlocked ? 0xFF00AA00 : 0xFF8B7355, 1);
                
                int branchTextY = textY + 5;
                
                // Название ветки
                MutableComponent branchName = Component.literal(branch.getName())
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
                MutableComponent branchStatus = Component.translatable(isUnlocked ? 
                    "screen.bm_falkye.unlocked" : "screen.bm_falkye.locked")
                    .withStyle(Style.EMPTY.withColor(isUnlocked ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                graphics.drawString(this.font, branchName, textX + 5, branchTextY, 0xFFFFFF, false);
                int statusX = textX + width - 25 - this.font.width(branchStatus);
                graphics.drawString(this.font, branchStatus, statusX, branchTextY, 0xFFFFFF, false);
                branchTextY += lineHeight + 5;
                
                // Улучшения в ветке
                for (CardEvolutionTree.EvolutionUpgrade upgrade : branch.getUpgrades()) {
                    boolean upgradeAvailable = level >= upgrade.getRequiredLevel();
                    boolean upgradeUnlocked = isUnlocked;
                    
                    // Название улучшения
                    ChatFormatting upgradeColor = upgradeUnlocked ? ChatFormatting.GREEN : 
                                                 (upgradeAvailable ? ChatFormatting.WHITE : ChatFormatting.GRAY);
                    MutableComponent upgradeName = Component.literal(upgradeUnlocked ? "✓ " : "- " + upgrade.getName())
                        .withStyle(Style.EMPTY.withColor(upgradeColor));
                    graphics.drawString(this.font, upgradeName, textX + 10, branchTextY, 0xFFFFFF, false);
                    branchTextY += lineHeight;
                    
                    // Описание улучшения
                    String description = upgrade.getDescription();
                    int maxDescWidth = width - 40;
                    if (this.font.width(description) > maxDescWidth) {
                        description = this.font.plainSubstrByWidth(description, maxDescWidth - 5) + "...";
                    }
                    MutableComponent upgradeDesc = Component.literal(description)
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                    graphics.drawString(this.font, upgradeDesc, textX + 15, branchTextY, 0x888888, false);
                    branchTextY += lineHeight;
                    
                    // Требуемый уровень
                    MutableComponent reqLevel = Component.translatable("screen.bm_falkye.required_level", 
                        upgrade.getRequiredLevel())
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                    graphics.drawString(this.font, reqLevel, textX + 15, branchTextY, 0x888888, false);
                    branchTextY += lineHeight + 5;
                }
                
                // Кнопка открытия ветки
                if (!isUnlocked && level >= branch.getUpgrades().get(0).getRequiredLevel()) {
                    int cost = CardEvolutionSystem.getBranchUnlockCost(selectedCardId, branch.getId());
                    boolean canAfford = soulDust >= cost;
                    
                    int buttonX = textX + width - 30 - 140;
                    int buttonY = textY + 5;
                    int buttonWidth = 140;
                    int buttonHeight = 20;
                    
                    MutableComponent buttonText = Component.translatable("screen.bm_falkye.unlock_branch", cost)
                        .withStyle(Style.EMPTY.withColor(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED));
                    
                    // Рендерим кнопку
                    GuiUtils.drawRoundedRect(graphics, buttonX, buttonY, buttonWidth, buttonHeight, 
                        canAfford ? 0xAA00AA00 : 0xAAAA0000);
                    GuiUtils.drawRoundedBorder(graphics, buttonX, buttonY, buttonWidth, buttonHeight, 
                        canAfford ? 0xFF00AA00 : 0xFFAA0000, 1);
                    
                    int textWidth = this.font.width(buttonText);
                    int textXPos = buttonX + (buttonWidth - textWidth) / 2;
                    graphics.drawString(this.font, buttonText, textXPos, buttonY + 6, 0xFFFFFF, false);
                }
                
                textY += branchHeight + 10;
            }
        } else {
            MutableComponent noTree = Component.translatable("screen.bm_falkye.no_evolution_tree")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, noTree, 
                layout.getCenteredX(this.font.width(noTree)), textY + 50, 0x888888);
        }
        
        graphics.disableScissor();
        
        // Индикатор прокрутки для деталей (если нужно)
        // TODO: Добавить прокрутку деталей, если контент не помещается
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            int listX = layout.getX(2);
            int listY = layout.getY(10) + 20;
            int listWidth = layout.getWidth(32);
            int listHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8) - 25;
            
            // Проверяем клик по картам в списке
            if (mouseX >= listX && mouseX <= listX + listWidth && 
                mouseY >= listY && mouseY <= listY + listHeight) {
                
                int visibleCards = listHeight / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
                int startIndex = listScrollOffset / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
                int endIndex = Math.min(startIndex + visibleCards + 1, filteredCards.size());
                
                int cardY = listY - (listScrollOffset % (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING));
                
                for (int i = startIndex; i < endIndex; i++) {
                    if (i >= filteredCards.size()) break;
                    
                    Card card = filteredCards.get(i);
                    if (card != null && mouseX >= listX && mouseX < listX + listWidth - 5 && 
                        mouseY >= cardY && mouseY < cardY + CARD_ENTRY_HEIGHT) {
                        
                        if (!card.getId().equals(selectedCardId)) {
                            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        }
                        selectedCardId = card.getId();
                        detailsScrollOffset = 0;
                        return true;
                    }
                    cardY += CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING;
                }
            }
            
            // Проверяем клик по кнопкам открытия веток
            if (selectedCardId != null) {
                CardEvolutionTree.EvolutionTreeData tree = CardEvolutionTree.getEvolutionTree(selectedCardId);
                if (tree != null) {
                    NetworkHandler.EvolutionData data = evolutionData.get(selectedCardId);
                    int level = data != null ? data.level : 1;
                    Set<String> unlockedBranches = data != null ? data.unlockedBranches : new HashSet<>();
                    
                    int detailsX = layout.getX(36);
                    int detailsY = layout.getY(10);
                    int detailsWidth = layout.getWidth(62);
                    
                    int textX = detailsX + 10;
                    int textY = detailsY + 75 - detailsScrollOffset;
                    
                    for (CardEvolutionTree.EvolutionBranch branch : tree.getBranches()) {
                        boolean isUnlocked = unlockedBranches.contains(branch.getId());
                        
                        if (!isUnlocked && level >= branch.getUpgrades().get(0).getRequiredLevel()) {
                            int cost = CardEvolutionSystem.getBranchUnlockCost(selectedCardId, branch.getId());
                            if (soulDust >= cost) {
                                int buttonX = textX + detailsWidth - 30 - 140;
                                int buttonY = textY + 5;
                                int buttonWidth = 140;
                                int buttonHeight = 20;
                                
                                if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && 
                                    mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
                                    
                                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                                    NetworkHandler.INSTANCE.sendToServer(
                                        new NetworkHandler.UnlockBranchPacket(selectedCardId, branch.getId()));
                                    return true;
                                }
                            }
                        }
                        
                        textY += branch.getUpgrades().size() * 60 + 40;
                    }
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
        
        // Проверяем, находится ли мышь в области списка карт
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleCards = listHeight / (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
            int totalCards = filteredCards.size();
            int maxScroll = Math.max(0, totalCards - visibleCards) * (CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING);
            int scrollStep = CARD_ENTRY_HEIGHT + CARD_ENTRY_SPACING;
            
            if (delta < 0 && listScrollOffset < maxScroll) {
                listScrollOffset = Math.min(maxScroll, listScrollOffset + scrollStep);
                return true;
            } else if (delta > 0 && listScrollOffset > 0) {
                listScrollOffset = Math.max(0, listScrollOffset - scrollStep);
                return true;
            }
        }
        
        // Прокрутка деталей (если мышь в области деталей)
        int detailsX = layout.getX(36);
        int detailsY = layout.getY(10);
        int detailsWidth = layout.getWidth(62);
        int detailsHeight = GUI_HEIGHT - layout.getY(10) - layout.getHeight(8);
        
        if (mouseX >= detailsX && mouseX <= detailsX + detailsWidth &&
            mouseY >= detailsY && mouseY <= detailsY + detailsHeight) {
            
            int scrollStep = 20;
            if (delta < 0) {
                detailsScrollOffset = Math.min(detailsScrollOffset + scrollStep, Integer.MAX_VALUE);
                return true;
            } else if (delta > 0 && detailsScrollOffset > 0) {
                detailsScrollOffset = Math.max(0, detailsScrollOffset - scrollStep);
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
