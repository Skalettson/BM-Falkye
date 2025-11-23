package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
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
 * Экран магазина карт
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class CardShopScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private int selectedCategory = 0; // 0 = все, 1 = обычные, 2 = редкие, и т.д.
    private com.bmfalkye.network.ShopPackets.SendShopItemsPacket shopData = null;
    private int playerCoins = 0;
    private java.util.List<String> shopCardIds = new java.util.ArrayList<>();
    private java.util.List<Integer> shopCardPrices = new java.util.ArrayList<>();
    private int scrollOffset = 0;
    
    private static final int CARD_ROW_COUNT = 2;
    private static final int CARD_COLUMN_COUNT = 4;
    private static final int CARDS_PER_PAGE = CARD_ROW_COUNT * CARD_COLUMN_COUNT;
    
    public CardShopScreen(Screen parent) {
        super(Component.translatable("screen.bm_falkye.card_shop_title"));
        this.parentScreen = parent;
    }
    
    public void updateShopItems(com.bmfalkye.network.ShopPackets.SendShopItemsPacket data) {
        this.shopData = data;
        this.playerCoins = data.getPlayerCoins();
        this.shopCardIds = data.getCardIds();
        this.shopCardPrices = data.getCardPrices();
        this.scrollOffset = 0;
        this.init();
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getBottomY(layout.getHeight(5), 2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                this.onClose();
            });
        buttons.add(backButton);
        addRenderableWidget(backButton);
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                requestShopItems();
            });
        buttons.add(refreshButton);
        addRenderableWidget(refreshButton);
        
        // Кнопки категорий
        String[] categoryKeys = {
            "screen.bm_falkye.category_all",
            "screen.bm_falkye.category_common",
            "screen.bm_falkye.category_rare",
            "screen.bm_falkye.category_epic",
            "screen.bm_falkye.category_legendary"
        };
        
        int categoryButtonWidth = layout.getWidth(14);
        int categoryButtonHeight = layout.getHeight(5);
        int categoryButtonSpacing = layout.getSpacing();
        int categoryStartX = layout.getX(20);
        int categoryY = layout.getY(2);
        
        for (int i = 0; i < categoryKeys.length; i++) {
            final int category = i;
            int categoryX = categoryStartX + i * (categoryButtonWidth + categoryButtonSpacing);
            
            // Проверяем, не выходит ли кнопка за границы
            if (categoryX + categoryButtonWidth > layout.getGuiX() + layout.getGuiWidth() - layout.getSpacing()) {
                break;
            }
            
            MutableComponent categoryText = Component.translatable(categoryKeys[i]);
            if (selectedCategory == category) {
                categoryText = categoryText.withStyle(Style.EMPTY
                    .withColor(ChatFormatting.GOLD).withBold(true));
            } else {
                categoryText = categoryText.withStyle(Style.EMPTY
                    .withColor(ChatFormatting.GRAY));
            }
            
            Button categoryButton = GuiUtils.createStyledButton(
                categoryX, categoryY, categoryButtonWidth, categoryButtonHeight,
                categoryText,
                b -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    selectedCategory = category;
                    scrollOffset = 0;
                    init();
                });
            buttons.add(categoryButton);
            addRenderableWidget(categoryButton);
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
        // Пересчитываем layout при изменении размера экрана
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
        MutableComponent title = Component.translatable("screen.bm_falkye.card_shop_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Баланс монет справа
        MutableComponent coinsText = Component.translatable("screen.bm_falkye.coins", playerCoins)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
        int coinsX = layout.getRightX(this.font.width(coinsText), 3);
        guiGraphics.drawString(this.font, coinsText, coinsX, titleY, 0xFFFFFF, false);
        
        // Область отображения карт
        int cardsAreaY = layout.getY(10);
        int cardsAreaHeight = GUI_HEIGHT - cardsAreaY - layout.getHeight(10);
        int cardsAreaX = layout.getX(3);
        int cardsAreaWidth = GUI_WIDTH - cardsAreaX * 2;
        
        // Обрезка области карт
        guiGraphics.enableScissor(cardsAreaX, cardsAreaY, cardsAreaX + cardsAreaWidth, 
            cardsAreaY + cardsAreaHeight);
        
        if (shopCardIds == null || shopCardIds.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_cards_available")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), cardsAreaY + cardsAreaHeight / 2, 0xAAAAAA);
        } else {
            // Фильтруем карты по категории
            List<ShopCard> filteredCards = getFilteredCards();
            
            // Рендерим карты сеткой
            renderCardGrid(guiGraphics, cardsAreaX, cardsAreaY, cardsAreaWidth, cardsAreaHeight, 
                filteredCards, mouseX, mouseY);
        }
        
        guiGraphics.disableScissor();
        
        // Рендерим кнопки
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    private List<ShopCard> getFilteredCards() {
        List<ShopCard> filtered = new ArrayList<>();
        
        for (int i = 0; i < shopCardIds.size(); i++) {
            String cardId = shopCardIds.get(i);
            Card card = CardRegistry.getCard(cardId);
            if (card == null) continue;
            
            boolean include = false;
            if (selectedCategory == 0) {
                include = true; // Все
            } else {
                CardRarity rarity = card.getRarity();
                include = (selectedCategory == 1 && rarity == CardRarity.COMMON) ||
                         (selectedCategory == 2 && rarity == CardRarity.RARE) ||
                         (selectedCategory == 3 && rarity == CardRarity.EPIC) ||
                         (selectedCategory == 4 && rarity == CardRarity.LEGENDARY);
            }
            
            if (include) {
                int price = i < shopCardPrices.size() ? shopCardPrices.get(i) : 0;
                filtered.add(new ShopCard(card, price));
            }
        }
        
        return filtered;
    }
    
    private void renderCardGrid(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                 List<ShopCard> cards, int mouseX, int mouseY) {
        List<ShopCard> visibleCards = getVisibleCards(cards);
        
        // Размеры карты
        int cardWidth = (width - layout.getSpacing() * (CARD_COLUMN_COUNT + 1)) / CARD_COLUMN_COUNT;
        int cardHeight = (int)(cardWidth * 1.5f);
        int cardSpacingX = layout.getSpacing();
        int cardSpacingY = layout.getSpacing();
        
        // Ограничиваем высоту, чтобы карты помещались
        int maxCardHeight = (height - cardSpacingY * (CARD_ROW_COUNT + 1)) / CARD_ROW_COUNT;
        if (cardHeight > maxCardHeight) {
            cardHeight = maxCardHeight;
            cardWidth = (int)(cardHeight / 1.5f);
        }
        
        int cardX = x + cardSpacingX;
        int cardY = y + cardSpacingY;
        
        for (int i = 0; i < visibleCards.size(); i++) {
            ShopCard shopCard = visibleCards.get(i);
            Card card = shopCard.card;
            int price = shopCard.price;
            
            int row = i / CARD_COLUMN_COUNT;
            int col = i % CARD_COLUMN_COUNT;
            
            int currentCardX = cardX + col * (cardWidth + cardSpacingX);
            int currentCardY = cardY + row * (cardHeight + cardSpacingY);
            
            // Проверяем, видна ли карта
            if (currentCardY + cardHeight > y + height) {
                break;
            }
            
            boolean canAfford = playerCoins >= price;
            boolean isHovered = mouseX >= currentCardX && mouseX <= currentCardX + cardWidth &&
                               mouseY >= currentCardY && mouseY <= currentCardY + cardHeight;
            
            // Фон карты
            int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
            if (!canAfford) {
                bgColor = isHovered ? 0xAAFF0000 : 0x66FF0000;
            }
            GuiUtils.drawRoundedRect(guiGraphics, currentCardX, currentCardY, cardWidth, cardHeight, bgColor);
            
            // Рамка карты
            int borderColor = canAfford ? 0xFF8B7355 : 0xFFAA0000;
            GuiUtils.drawRoundedBorder(guiGraphics, currentCardX, currentCardY, cardWidth, cardHeight, 
                borderColor, 2);
            
            // Рендерим карту (упрощённо)
            int cardImageSize = Math.min(cardWidth - 20, cardHeight - 40);
            int cardImageX = currentCardX + (cardWidth - cardImageSize) / 2;
            int cardImageY = currentCardY + 10;
            
            // Заглушка карты (можно заменить на реальный рендеринг)
            SimpleCardRenderer.renderCard(guiGraphics, this.font, card, 
                cardImageX, cardImageY, cardImageSize, (int)(cardImageSize * 1.5f),
                isHovered ? mouseX : -1, isHovered ? mouseY : -1, false, isHovered, null);
            
            // Цена внизу карты
            MutableComponent priceText = Component.translatable("screen.bm_falkye.price", price)
                .withStyle(Style.EMPTY.withColor(canAfford ? ChatFormatting.GOLD : ChatFormatting.RED));
            int priceX = currentCardX + (cardWidth - this.font.width(priceText)) / 2;
            int priceY = currentCardY + cardHeight - 15;
            guiGraphics.drawString(this.font, priceText, priceX, priceY, 0xFFFFFF, false);
        }
    }
    
    private List<ShopCard> getVisibleCards(List<ShopCard> cards) {
        int startIndex = scrollOffset * CARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + CARDS_PER_PAGE, cards.size());
        
        List<ShopCard> visible = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            visible.add(cards.get(i));
        }
        return visible;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && shopCardIds != null && !shopCardIds.isEmpty()) {
            List<ShopCard> filteredCards = getFilteredCards();
            List<ShopCard> visibleCards = getVisibleCards(filteredCards);
            
            int cardsAreaY = layout.getY(10);
            int cardsAreaHeight = layout.getGuiHeight() - cardsAreaY - layout.getHeight(10);
            int cardsAreaX = layout.getX(3);
            int cardsAreaWidth = layout.getGuiWidth() - cardsAreaX * 2;
            
            int cardWidth = (cardsAreaWidth - layout.getSpacing() * (CARD_COLUMN_COUNT + 1)) / CARD_COLUMN_COUNT;
            int cardHeight = (int)(cardWidth * 1.5f);
            int cardSpacingX = layout.getSpacing();
            int cardSpacingY = layout.getSpacing();
            
            int cardX = cardsAreaX + cardSpacingX;
            int cardY = cardsAreaY + cardSpacingY;
            
            for (int i = 0; i < visibleCards.size(); i++) {
                ShopCard shopCard = visibleCards.get(i);
                Card card = shopCard.card;
                int price = shopCard.price;
                
                int row = i / CARD_COLUMN_COUNT;
                int col = i % CARD_COLUMN_COUNT;
                
                int currentCardX = cardX + col * (cardWidth + cardSpacingX);
                int currentCardY = cardY + row * (cardHeight + cardSpacingY);
                
                if (mouseX >= currentCardX && mouseX <= currentCardX + cardWidth &&
                    mouseY >= currentCardY && mouseY <= currentCardY + cardHeight) {
                    
                    if (playerCoins >= price) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                            new com.bmfalkye.network.ShopPackets.BuyCardPacket(card.getId(), price));
                    } else {
                        if (minecraft != null && minecraft.player != null) {
                            MutableComponent message = Component.translatable("screen.bm_falkye.not_enough_coins")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                            minecraft.player.sendSystemMessage(message);
                        }
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || shopCardIds == null || shopCardIds.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int cardsAreaY = layout.getY(10);
        int cardsAreaHeight = layout.getGuiHeight() - cardsAreaY - layout.getHeight(10);
        int cardsAreaX = layout.getX(3);
        int cardsAreaWidth = layout.getGuiWidth() - cardsAreaX * 2;
        
        if (mouseX >= cardsAreaX && mouseX <= cardsAreaX + cardsAreaWidth &&
            mouseY >= cardsAreaY && mouseY <= cardsAreaY + cardsAreaHeight) {
            
            List<ShopCard> filteredCards = getFilteredCards();
            int totalPages = (int)Math.ceil((double)filteredCards.size() / CARDS_PER_PAGE);
            
            if (delta < 0 && scrollOffset < totalPages - 1) {
                scrollOffset++;
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset--;
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        if (parentScreen != null && minecraft != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private static class ShopCard {
        final Card card;
        final int price;
        
        ShopCard(Card card, int price) {
            this.card = card;
            this.price = price;
        }
    }
}
