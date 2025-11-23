package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран магазина карт
 */
public class CardShopScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 400;
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private int selectedCategory = 0; // 0 = все, 1 = обычные, 2 = редкие, и т.д.
    private com.bmfalkye.network.ShopPackets.SendShopItemsPacket shopData = null;
    private int playerCoins = 0;
    private java.util.List<String> shopCardIds = new java.util.ArrayList<>();
    private java.util.List<Integer> shopCardPrices = new java.util.ArrayList<>();
    private int scrollOffset = 0;
    private static final int CARDS_PER_PAGE = 6;
    
    public CardShopScreen(Screen parent) {
        super(Component.literal("Магазин карт"));
        this.parentScreen = parent;
    }
    
    public void updateShopItems(com.bmfalkye.network.ShopPackets.SendShopItemsPacket data) {
        this.shopData = data;
        this.playerCoins = data.getPlayerCoins();
        this.shopCardIds = data.getCardIds();
        this.shopCardPrices = data.getCardPrices();
        this.init(); // Переинициализируем экран
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 0.9, 550, 350);
        
        // Кнопка "Назад" - скевоморфный стиль (адаптивно)
        int backButtonWidth = layout.getWidth(15);
        int backButtonHeight = layout.getHeight(5);
        buttons.add(GuiUtils.createStyledButton(layout.getX(3), layout.getY(88), backButtonWidth, backButtonHeight,
            Component.literal("Назад"), b -> onClose()));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопка "Обновить" - скевоморфный стиль (адаптивно)
        int refreshButtonWidth = layout.getWidth(18);
        int refreshButtonHeight = layout.getHeight(5);
        buttons.add(GuiUtils.createStyledButton(layout.getX(3), layout.getY(10), refreshButtonWidth, refreshButtonHeight,
            Component.literal("Обновить"), b -> {
            requestShopItems();
        }));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопки категорий - скевоморфный стиль (адаптивно, с правильным расположением)
        String[] categories = {"Все", "Обычные", "Редкие", "Эпические", "Легендарные"};
        int categoryButtonWidth = layout.getWidth(12); // Адаптивная ширина
        int categoryButtonHeight = layout.getHeight(5); // Адаптивная высота
        int categoryButtonSpacing = layout.getSpacing(); // Адаптивный отступ
        int categoryStartX = layout.getX(25); // Начинаем после кнопки "Обновить" с отступом
        
        for (int i = 0; i < categories.length; i++) {
            final int category = i;
            // Вычисляем позицию с учётом адаптивных размеров
            int categoryX = categoryStartX + i * (categoryButtonWidth + categoryButtonSpacing);
            
            // Проверяем, не выходит ли кнопка за границы экрана
            if (categoryX + categoryButtonWidth > layout.getGuiX() + layout.getGuiWidth() - layout.getSpacing()) {
                break; // Прекращаем создание кнопок, если выходим за границы
            }
            
            buttons.add(GuiUtils.createStyledButton(categoryX, layout.getY(10), categoryButtonWidth, categoryButtonHeight,
                Component.literal(categories[i]), b -> {
                selectedCategory = category;
                scrollOffset = 0; // Сбрасываем прокрутку при смене категории
                init(); // Обновляем список
            }));
            addRenderableWidget(buttons.get(buttons.size() - 1));
        }
        
        // Отображаем карты магазина
        if (shopCardIds != null && !shopCardIds.isEmpty()) {
            java.util.List<String> filteredCardIds = new java.util.ArrayList<>();
            java.util.List<Integer> filteredCardPrices = new java.util.ArrayList<>();
            
            // Фильтруем по категории
            for (int i = 0; i < shopCardIds.size(); i++) {
                String cardId = shopCardIds.get(i);
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                if (card != null) {
                    boolean include = false;
                    if (selectedCategory == 0) {
                        include = true; // Все
                    } else {
                        com.bmfalkye.cards.CardRarity rarity = card.getRarity();
                        include = (selectedCategory == 1 && rarity == com.bmfalkye.cards.CardRarity.COMMON) ||
                                 (selectedCategory == 2 && rarity == com.bmfalkye.cards.CardRarity.RARE) ||
                                 (selectedCategory == 3 && rarity == com.bmfalkye.cards.CardRarity.EPIC) ||
                                 (selectedCategory == 4 && rarity == com.bmfalkye.cards.CardRarity.LEGENDARY);
                    }
                    
                    if (include) {
                        filteredCardIds.add(cardId);
                        filteredCardPrices.add(i < shopCardPrices.size() ? shopCardPrices.get(i) : 0);
                    }
                }
            }
            
            int startY = layout.getY(20); // Начинаем ниже кнопок категорий
            int itemSpacing = layout.getHeight(6); // Адаптивный отступ между элементами
            int visibleCount = Math.min(filteredCardIds.size() - scrollOffset, CARDS_PER_PAGE);
            
            // Вычисляем максимальную высоту области для карт (до кнопок прокрутки)
            int maxItemsAreaHeight = layout.getY(85) - startY - itemSpacing;
            int maxVisibleItems = Math.max(1, maxItemsAreaHeight / (itemSpacing + layout.getHeight(5)));
            visibleCount = Math.min(visibleCount, maxVisibleItems);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset + i;
                if (index < filteredCardIds.size()) {
                    final String cardId = filteredCardIds.get(index);
                    final int price = filteredCardPrices.get(index);
                    com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                    
                    if (card != null) {
                        int yPos = startY + i * itemSpacing;
                        
                        // Проверяем, не выходит ли элемент за границы
                        if (yPos + itemSpacing > layout.getY(85)) {
                            break; // Прекращаем, если выходим за границы
                        }
                        
                        boolean canAfford = playerCoins >= price;
                        
                        // Кнопка покупки - скевоморфный стиль (адаптивно)
                        int buyButtonWidth = layout.getWidth(28); // Адаптивная ширина
                        int buyButtonHeight = layout.getHeight(5); // Адаптивная высота
                        String buyButtonText = "Купить (" + price + ")";
                        
                        // Обрезаем текст кнопки, если он слишком длинный
                        int maxTextWidth = buyButtonWidth - 10; // Оставляем отступы
                        if (font.width(buyButtonText) > maxTextWidth) {
                            buyButtonText = font.plainSubstrByWidth(buyButtonText, maxTextWidth - 10) + "...";
                        }
                        
                        buttons.add(GuiUtils.createStyledButton(layout.getX(3), yPos, buyButtonWidth, buyButtonHeight,
                            Component.literal(buyButtonText), 
                            b -> {
                                if (canAfford) {
                                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                        new com.bmfalkye.network.ShopPackets.BuyCardPacket(cardId, price));
                                } else {
                                    minecraft.player.sendSystemMessage(Component.literal(
                                        "§cНедостаточно монет!"));
                                }
                            }));
                        addRenderableWidget(buttons.get(buttons.size() - 1));
                    }
                }
            }
            
            // Кнопки прокрутки - скевоморфный стиль (адаптивно)
            int scrollButtonWidth = layout.getWidth(8);
            int scrollButtonHeight = layout.getHeight(4);
            int scrollButtonSpacing = layout.getSpacing();
            
            if (scrollOffset > 0) {
                buttons.add(GuiUtils.createStyledButton(layout.getX(3), layout.getY(88), scrollButtonWidth, scrollButtonHeight,
                    Component.literal("↑"), b -> {
                    scrollOffset = Math.max(0, scrollOffset - CARDS_PER_PAGE);
                    init();
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
            }
            
            if (scrollOffset + CARDS_PER_PAGE < filteredCardIds.size()) {
                int downButtonX = layout.getX(3) + scrollButtonWidth + scrollButtonSpacing;
                // Проверяем, не выходит ли кнопка за границы
                if (downButtonX + scrollButtonWidth <= layout.getGuiX() + layout.getGuiWidth() - layout.getSpacing()) {
                    buttons.add(GuiUtils.createStyledButton(downButtonX, layout.getY(88), scrollButtonWidth, scrollButtonHeight,
                        Component.literal("↓"), b -> {
                        scrollOffset = Math.min(filteredCardIds.size() - CARDS_PER_PAGE, 
                            scrollOffset + CARDS_PER_PAGE);
                        init();
                    }));
                    addRenderableWidget(buttons.get(buttons.size() - 1));
                }
            }
        }
        
        // Запрашиваем данные магазина при открытии
        if (shopData == null) {
            requestShopItems();
        }
    }
    
    private void requestShopItems() {
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.ShopPackets.RequestShopItemsPacket());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), true);
        GuiUtils.drawMetalFrame(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), 3, true); // Золотая рамка для магазина
        
        // Заголовок (адаптивно, с проверкой границ)
        String title = "Магазин карт";
        int titleX = layout.getX(3);
        int titleY = layout.getY(3);
        // Тень заголовка (черная)
        guiGraphics.drawString(font, title, titleX + 2, titleY + 2, 0x000000);
        // Сам заголовок
        guiGraphics.drawString(font, title, titleX, titleY, 0xFFFFFF);
        
        // Баланс монет (адаптивно, справа от заголовка)
        String coinsText = "Монеты: " + playerCoins;
        int coinsTextWidth = font.width(coinsText);
        int coinsX = layout.getGuiX() + layout.getGuiWidth() - coinsTextWidth - layout.getSpacing();
        // Проверяем, не перекрывается ли с заголовком
        if (coinsX < titleX + font.width(title) + layout.getSpacing()) {
            coinsX = titleX + font.width(title) + layout.getSpacing();
        }
        guiGraphics.drawString(font, coinsText, coinsX, titleY, 0xFFD700);
        
        if (shopCardIds != null && !shopCardIds.isEmpty()) {
            java.util.List<String> filteredCardIds = new java.util.ArrayList<>();
            java.util.List<Integer> filteredCardPrices = new java.util.ArrayList<>();
            
            // Фильтруем по категории
            for (int i = 0; i < shopCardIds.size(); i++) {
                String cardId = shopCardIds.get(i);
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                if (card != null) {
                    boolean include = false;
                    if (selectedCategory == 0) {
                        include = true;
                    } else {
                        com.bmfalkye.cards.CardRarity rarity = card.getRarity();
                        include = (selectedCategory == 1 && rarity == com.bmfalkye.cards.CardRarity.COMMON) ||
                                 (selectedCategory == 2 && rarity == com.bmfalkye.cards.CardRarity.RARE) ||
                                 (selectedCategory == 3 && rarity == com.bmfalkye.cards.CardRarity.EPIC) ||
                                 (selectedCategory == 4 && rarity == com.bmfalkye.cards.CardRarity.LEGENDARY);
                    }
                    
                    if (include) {
                        filteredCardIds.add(cardId);
                        filteredCardPrices.add(i < shopCardPrices.size() ? shopCardPrices.get(i) : 0);
                    }
                }
            }
            
            int startY = layout.getY(20); // Начинаем ниже кнопок категорий
            int itemSpacing = layout.getHeight(6); // Адаптивный отступ между элементами
            int visibleCount = Math.min(filteredCardIds.size() - scrollOffset, CARDS_PER_PAGE);
            
            // Вычисляем максимальную высоту области для карт
            int maxItemsAreaHeight = layout.getY(85) - startY - itemSpacing;
            int maxVisibleItems = Math.max(1, maxItemsAreaHeight / (itemSpacing + layout.getHeight(5)));
            visibleCount = Math.min(visibleCount, maxVisibleItems);
            
            // Позиция текста (справа от кнопки покупки)
            int textStartX = layout.getX(35); // Адаптивная позиция
            int maxTextWidth = layout.getGuiWidth() - textStartX - layout.getSpacing() * 2; // Максимальная ширина текста
            
            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset + i;
                if (index < filteredCardIds.size()) {
                    String cardId = filteredCardIds.get(index);
                    int price = filteredCardPrices.get(index);
                    com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                    
                    if (card != null) {
                        int yPos = startY + i * itemSpacing;
                        
                        // Проверяем, не выходит ли элемент за границы
                        if (yPos + itemSpacing > layout.getY(85)) {
                            break; // Прекращаем, если выходим за границы
                        }
                        
                        boolean canAfford = playerCoins >= price;
                        
                        // Название карты (с обрезкой, если слишком длинное)
                        String cardName = card.getName();
                        if (font.width(cardName) > maxTextWidth) {
                            cardName = font.plainSubstrByWidth(cardName, maxTextWidth - 10) + "...";
                        }
                        guiGraphics.drawString(font, cardName, 
                            textStartX, yPos, canAfford ? 0xFFFFFF : 0x888888);
                        
                        // Цена (с обрезкой, если слишком длинная)
                        String priceText = "Цена: " + price + " монет";
                        if (font.width(priceText) > maxTextWidth) {
                            priceText = font.plainSubstrByWidth(priceText, maxTextWidth - 10) + "...";
                        }
                        guiGraphics.drawString(font, priceText, 
                            textStartX, yPos + font.lineHeight + 2, canAfford ? 0xFFD700 : 0x888888);
                    }
                }
            }
        } else {
            guiGraphics.drawString(font, "Нет доступных карт для покупки", 
                layout.getX(5), layout.getY(50), 0xAAAAAA);
        }
        
        // Кастомный рендеринг кнопок в скевоморфном стиле
        for (Button button : buttons) {
            if (button != null) {
                // Определяем, выбрана ли кнопка (для категорий)
                boolean isSelected = false;
                String buttonText = button.getMessage().getString();
                if (buttonText.equals("Все")) {
                    isSelected = selectedCategory == 0;
                } else if (buttonText.equals("Обычные")) {
                    isSelected = selectedCategory == 1;
                } else if (buttonText.equals("Редкие")) {
                    isSelected = selectedCategory == 2;
                } else if (buttonText.equals("Эпические")) {
                    isSelected = selectedCategory == 3;
                } else if (buttonText.equals("Легендарные")) {
                    isSelected = selectedCategory == 4;
                }
                GuiUtils.renderStyledButton(guiGraphics, font, button, mouseX, mouseY, isSelected);
            }
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они уже отрисованы)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    @Override
    public void onClose() {
        if (parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
}

