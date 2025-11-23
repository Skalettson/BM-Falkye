package com.bmfalkye.client;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.storage.PlayerDeckStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Редактор колод с предпросмотром
 * Реализовано в стиле BM Characters с использованием стандартных компонентов Minecraft
 */
public class DeckEditorScreen extends Screen {
    // Базовые размеры (будут адаптироваться под разрешение) - немного увеличены
    private static final int BASE_GUI_WIDTH = 650;
    private static final int BASE_GUI_HEIGHT = 450;
    private static final int MIN_GUI_WIDTH = 550;
    private static final int MIN_GUI_HEIGHT = 400;
    private static final double MAX_SCREEN_RATIO = 0.9; // Максимум 90% экрана
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    private List<Card> availableCards = new ArrayList<>(); // Карты из коллекции игрока
    private List<Card> deckCards = new ArrayList<>();
    private String deckName = "Новая колода";
    private int selectedDeckSlot = -1; // -1 = новый слот
    
    private EditBox deckNameBox;
    private int scrollOffset = 0;
    private int deckScrollOffset = 0; // Скролл для колоды
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Список сохраненных колод
    private java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> savedDecks = new java.util.ArrayList<>();
    
    // Список кнопок для кастомного рендеринга
    private final java.util.List<Button> buttons = new java.util.ArrayList<>();
    private final net.minecraft.client.gui.screens.Screen parentScreen;

    public DeckEditorScreen() {
        this(com.bmfalkye.client.ClientPacketHandler.getMainMenuParent());
    }
    
    public DeckEditorScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.deck_editor"));
        this.parentScreen = parentScreen;
        // Запрашиваем коллекцию карт игрока с сервера
        loadPlayerCollection();
        // Запрашиваем список колод с сервера
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.NetworkHandler.RequestDecksPacket());
    }
    
    /**
     * Загружает коллекцию карт игрока с сервера
     */
    private void loadPlayerCollection() {
        // Запрашиваем коллекцию с сервера
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.NetworkHandler.RequestCardCollectionPacket());
        
        // Показываем кэшированную коллекцию, если она есть
        java.util.List<String> cached = com.bmfalkye.client.ClientPacketHandler.getCachedCardCollection();
        if (!cached.isEmpty()) {
            updateCollection(cached);
        } else {
            this.availableCards = new ArrayList<>();
        }
    }
    
    /**
     * Обновляет коллекцию карт (вызывается при получении данных с сервера)
     */
    public void updateCollection(java.util.List<String> cardIds) {
        this.availableCards = cardIds.stream()
            .map(CardRegistry::getCard)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    protected void init() {
        super.init();
        
        // Очищаем виджеты перед пересозданием
        this.clearWidgets();
        buttons.clear();
        
        // Инициализируем систему автоматической адаптации
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Поле имени колоды - автоматическая адаптация
        int nameBoxWidth = layout.getWidth(28);
        this.deckNameBox = new EditBox(this.font, layout.getX(2), layout.getY(9), nameBoxWidth, layout.getHeight(4), Component.literal("Имя колоды"));
        this.deckNameBox.setMaxLength(32);
        this.deckNameBox.setValue(deckName);
        this.deckNameBox.setHint(Component.literal("Название"));
        this.deckNameBox.setResponder(value -> deckName = value);
        this.addRenderableWidget(this.deckNameBox);
        
        // Кнопки управления - автоматическая адаптация
        int buttonWidth = layout.getWidth(11);
        int buttonHeight = layout.getHeight(5);
        int buttonSpacing = layout.getSpacing();
        int buttonStartX = layout.getX(2) + nameBoxWidth + layout.getSpacing();
        int buttonY = layout.getY(9);
        
        // Кнопка сохранения (стилизованная)
        Button saveBtn = createStyledButton(buttonStartX, buttonY, buttonWidth, buttonHeight, 
            Component.translatable("button.bm_falkye.save_deck"), 
            (btn) -> saveDeck());
        this.addRenderableWidget(saveBtn);
        buttons.add(saveBtn);
        
        // Кнопка загрузки колоды (стилизованная)
        Button loadBtn = createStyledButton(buttonStartX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, 
            Component.translatable("button.bm_falkye.load_deck"), 
            (btn) -> loadDeck());
        this.addRenderableWidget(loadBtn);
        buttons.add(loadBtn);
        
        // Кнопка очистки (стилизованная)
        Button clearBtn = createStyledButton(buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight, 
            Component.translatable("button.bm_falkye.clear_deck"), 
            (btn) -> { 
                deckCards.clear(); 
                deckScrollOffset = 0;
                deckName = "Новая колода";
                deckNameBox.setValue(deckName);
                selectedDeckSlot = -1;
            });
        this.addRenderableWidget(clearBtn);
        buttons.add(clearBtn);
        
        // Кнопки выбора слота колоды (1-10) - адаптивное позиционирование
        // Вычисляем позицию так, чтобы кнопки не накладывались на кнопку "Очистить" и не выходили за разделительную полосу
        int clearButtonEndX = buttonStartX + (buttonWidth + buttonSpacing) * 3; // Конец кнопки "Очистить"
        int dividerY = layout.getY(15); // Позиция разделительной полосы
        int slotButtonWidth = layout.getWidth(3);
        int slotButtonHeight = layout.getHeight(4);
        int slotButtonSpacing = layout.getSpacing() / 2; // Отступ между кнопками
        int slotsPerRow = 5;
        
        // Вычисляем максимальную ширину для кнопок слотов (чтобы не выходить за разделительную полосу)
        int maxSlotsWidth = slotsPerRow * slotButtonWidth + (slotsPerRow - 1) * slotButtonSpacing;
        int rightPadding = layout.getSpacing(); // Отступ справа
        int slotButtonX = layout.getX(100) - maxSlotsWidth - rightPadding; // Отступ справа
        
        // Позиция Y: размещаем кнопки выше разделительной полосы, с отступом
        // Убеждаемся, что кнопки не накладываются на кнопки управления
        int slotButtonY = buttonY; // На той же высоте, что и кнопки управления
        
        // Проверяем, не накладываются ли кнопки слотов на кнопку "Очистить"
        // Если накладываются, смещаем кнопки слотов правее
        if (slotButtonX < clearButtonEndX + 15) {
            // Смещаем кнопки слотов правее, чтобы не накладывались
            slotButtonX = clearButtonEndX + 15;
        }
        
        // Проверяем, что кнопки не выходят за правый край окна
        int slotsEndX = slotButtonX + maxSlotsWidth;
        int maxAllowedX = guiX + GUI_WIDTH - rightPadding;
        if (slotsEndX > maxAllowedX) {
            // Смещаем кнопки левее, чтобы не выходить за край окна
            slotButtonX = maxAllowedX - maxSlotsWidth;
            // Если после смещения кнопки накладываются на "Очистить", уменьшаем размеры кнопок
            if (slotButtonX < clearButtonEndX + 15) {
                // Уменьшаем размеры кнопок и пересчитываем
                slotButtonWidth = Math.max(18, Math.min(22, (int)(GUI_WIDTH * 0.03))); // 3% от ширины
                slotButtonSpacing = 2; // Уменьшаем отступ
                maxSlotsWidth = slotsPerRow * slotButtonWidth + (slotsPerRow - 1) * slotButtonSpacing;
                slotButtonX = Math.max(clearButtonEndX + 15, maxAllowedX - maxSlotsWidth);
            }
        }
        
        // Проверяем, что кнопки не выходят за левый край окна
        if (slotButtonX < guiX + 15) {
            slotButtonX = guiX + 15;
            // Если всё равно не помещается, уменьшаем размеры кнопок
            if (slotButtonX + maxSlotsWidth > maxAllowedX) {
                slotButtonWidth = Math.max(16, Math.min(20, (int)(GUI_WIDTH * 0.025))); // 2.5% от ширины
                slotButtonSpacing = 2;
                maxSlotsWidth = slotsPerRow * slotButtonWidth + (slotsPerRow - 1) * slotButtonSpacing;
                slotButtonX = Math.max(guiX + 15, maxAllowedX - maxSlotsWidth);
            }
        }
        
        // Проверяем, что кнопки не выходят за разделительную полосу
        int slotsTotalHeight = 2 * slotButtonHeight + slotButtonSpacing; // 2 ряда кнопок
        if (slotButtonY + slotsTotalHeight > dividerY - 5) {
            // Смещаем кнопки выше, чтобы не выходить за разделительную полосу
            slotButtonY = dividerY - slotsTotalHeight - 5;
        }
        
        // Сохраняем финальные размеры для использования в цикле
        final int finalSlotButtonWidth = slotButtonWidth;
        final int finalSlotButtonHeight = slotButtonHeight;
        final int finalSlotButtonSpacing = slotButtonSpacing;
        final int finalSlotButtonX = slotButtonX;
        final int finalSlotButtonY = slotButtonY;
        
        for (int i = 0; i < 10; i++) {
            int slotIndex = i;
            int row = i / slotsPerRow;
            int col = i % slotsPerRow;
            int x = finalSlotButtonX + col * (finalSlotButtonWidth + finalSlotButtonSpacing);
            int y = finalSlotButtonY + row * (finalSlotButtonHeight + finalSlotButtonSpacing);
            
            String slotName = savedDecks.size() > i ? savedDecks.get(i).getDeckName() : "Слот " + (i + 1);
            Button slotBtn = createStyledButton(x, y, finalSlotButtonWidth, finalSlotButtonHeight, 
                Component.literal(String.valueOf(i + 1)), 
                (btn) -> {
                    selectedDeckSlot = slotIndex;
                    if (savedDecks.size() > slotIndex) {
                        // Загружаем существующую колоду
                        loadDeck();
                    } else {
                        // Создаем новую колоду в этом слоте
                        deckCards.clear();
                        deckName = "Колода " + (slotIndex + 1);
                        deckNameBox.setValue(deckName);
                    }
                });
            this.addRenderableWidget(slotBtn);
            buttons.add(slotBtn);
        }
        
        // Кнопка "Правила" (стилизованная) - адаптивное позиционирование
        int rulesButtonWidth = layout.getWidth(15);
        int rulesButtonHeight = layout.getHeight(5);
        Button rulesBtn = createStyledButton(layout.getX(2), layout.getY(92), rulesButtonWidth, rulesButtonHeight, 
            Component.translatable("button.bm_falkye.rules"), 
            (btn) -> this.minecraft.setScreen(new FalkyeTutorialScreen()));
        this.addRenderableWidget(rulesBtn);
        buttons.add(rulesBtn);
        
        // Кнопка назад (стилизованная) - адаптивное позиционирование
        int backButtonWidth = layout.getWidth(15);
        Button backBtn = createStyledButton(layout.getX(50) - backButtonWidth / 2, layout.getY(92), backButtonWidth, rulesButtonHeight, 
            Component.translatable("button.bm_falkye.back"),
            (btn) -> {
                if (parentScreen != null) {
                    this.minecraft.setScreen(parentScreen);
                } else {
                    this.onClose();
                }
            });
        this.addRenderableWidget(backBtn);
        buttons.add(backBtn);
    }
    
    // Метод calculateAdaptiveSize() удалён - теперь используется AdaptiveLayout
    
    /**
     * Создаёт стилизованную кнопку с закруглёнными углами
     * Использует CreativeCore если доступен
     */
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return GuiUtils.createStyledButton(x, y, width, height, text, onPress);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.getGuiWidth() != GUI_WIDTH || layout.getGuiHeight() != GUI_HEIGHT) {
            this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                           MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            this.GUI_WIDTH = layout.getGuiWidth();
            this.GUI_HEIGHT = layout.getGuiHeight();
        }
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // ПЕРЕПИСАНО: Рисуем фон окна в скевоморфном стиле (деревянная панель)
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        // Металлическая рамка
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, false);
        
        // Заголовок - адаптивное позиционирование
        Component titleComponent = Component.translatable("screen.bm_falkye.deck_editor");
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = layout.getY(3);
        // Тень заголовка (черная)
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, titleY, 0xFFFFFF, false);
        
        // Информация о колоде (адаптивное позиционирование)
        int deckCount = deckCards.size();
        int maxCards = 25;
        String countText = deckCount >= maxCards ? 
            "§c" + deckCount + " / " + maxCards : 
            "§a" + deckCount + " §7/ " + maxCards;
        int deckInfoY = layout.getY(13);
        guiGraphics.drawString(this.font, 
            Component.literal("§7Карт: " + countText), 
            layout.getX(2), deckInfoY, 0xFFFFFF, false);
        
        // Информация о выбранном слоте
        if (selectedDeckSlot >= 0) {
            String slotInfo = "§7Слот: §e" + (selectedDeckSlot + 1);
            if (savedDecks.size() > selectedDeckSlot) {
                String deckName = savedDecks.get(selectedDeckSlot).getDeckName();
                if (this.font.width(deckName) > 100) {
                    deckName = this.font.plainSubstrByWidth(deckName, 100 - 5) + "...";
                }
                slotInfo += " §7(§f" + deckName + "§7)";
            }
            guiGraphics.drawString(this.font, 
                Component.literal(slotInfo), 
                layout.getX(2) + this.font.width("§7Карт: " + countText) + layout.getSpacing(), deckInfoY, 0xFFFFFF, false);
        }
        
        // Отображение списка сохраненных колод (справа, под кнопками слотов) - адаптивное
        // Используем те же вычисления, что и в init()
        int nameBoxWidth = layout.getWidth(28);
        int buttonWidth = layout.getWidth(11);
        int buttonSpacing = layout.getSpacing();
        int buttonStartX = layout.getX(2) + nameBoxWidth + layout.getSpacing();
        int clearButtonEndX = buttonStartX + (buttonWidth + buttonSpacing) * 3;
        
        int dividerY = layout.getY(15);
        int slotButtonWidth = layout.getWidth(3);
        int slotButtonHeight = layout.getHeight(4);
        int slotButtonSpacing = layout.getSpacing() / 2;
        int slotsPerRow = 5;
        int maxSlotsWidth = slotsPerRow * slotButtonWidth + (slotsPerRow - 1) * slotButtonSpacing;
        int rightPadding = layout.getSpacing();
        int slotButtonX = layout.getX(100) - maxSlotsWidth - rightPadding;
        int slotButtonY = layout.getY(9);
        
        // Проверяем наложение на кнопку "Очистить"
        if (slotButtonX < clearButtonEndX + layout.getSpacing()) {
            slotButtonX = clearButtonEndX + layout.getSpacing();
        }
        
        // Проверяем, что кнопки не выходят за правый край окна
        int slotsEndX = slotButtonX + maxSlotsWidth;
        int maxAllowedX = layout.getX(100) - rightPadding;
        if (slotsEndX > maxAllowedX) {
            slotButtonX = maxAllowedX - maxSlotsWidth;
            if (slotButtonX < clearButtonEndX + layout.getSpacing()) {
                slotButtonWidth = layout.getWidth(2);
                slotButtonSpacing = layout.getSpacing() / 3;
                maxSlotsWidth = slotsPerRow * slotButtonWidth + (slotsPerRow - 1) * slotButtonSpacing;
                slotButtonX = Math.max(clearButtonEndX + layout.getSpacing(), maxAllowedX - maxSlotsWidth);
            }
        }
        
        // Проверяем выход за разделительную полосу
        int slotsTotalHeight = 2 * slotButtonHeight + slotButtonSpacing;
        if (slotButtonY + slotsTotalHeight > dividerY - layout.getSpacing()) {
            slotButtonY = dividerY - slotsTotalHeight - layout.getSpacing();
        }
        
        int infoPanelWidth = layout.getWidth(18);
        
        guiGraphics.drawString(this.font, 
            Component.literal("§7Слоты:"), 
            slotButtonX, slotButtonY - layout.getSpacing() * 2, 0xFFFFFF, false);
        
        // Отображаем информацию о сохраненных колодах под кнопками (адаптивно)
        int infoY = slotButtonY + slotsTotalHeight + layout.getSpacing();
        int maxInfoLines = Math.min(4, savedDecks.size());
        int lineHeight = layout.getSpacing() * 2;
        
        for (int i = 0; i < maxInfoLines; i++) {
            if (i >= savedDecks.size()) break;
            com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck = savedDecks.get(i);
            String deckInfo = "§7" + (i + 1) + ": §f" + deck.getDeckName();
            if (this.font.width(deckInfo) > infoPanelWidth) {
                deckInfo = this.font.plainSubstrByWidth(deckInfo, infoPanelWidth - 10) + "...";
            }
            guiGraphics.drawString(this.font, 
                Component.literal(deckInfo), 
                slotButtonX, infoY + i * lineHeight, 0xFFFFFF, false);
            
            // Количество карт
            String cardCount = "§7(" + deck.getCardIds().size() + ")";
            guiGraphics.drawString(this.font, 
                Component.literal(cardCount), 
                slotButtonX, infoY + i * lineHeight + 6, 0xCCCCCC, false);
        }
        if (savedDecks.size() > maxInfoLines) {
            guiGraphics.drawString(this.font, 
                Component.literal("§7...+" + (savedDecks.size() - maxInfoLines)), 
                slotButtonX, infoY + maxInfoLines * lineHeight, 0xCCCCCC, false);
        }
        
        // Разделитель (адаптивно) - используем уже вычисленное значение dividerY
        guiGraphics.fill(layout.getX(2), dividerY, layout.getX(98), dividerY + 1, 0xFF4A3A2A);
        
        // Левая панель - доступные карты
        renderAvailableCards(guiGraphics, mouseX, mouseY);
        
        // Разделитель между панелями - адаптивное позиционирование
        int verticalDividerX = layout.getX(50);
        int verticalDividerStartY = layout.getY(18);
        int verticalDividerEndY = layout.getY(90);
        guiGraphics.fill(verticalDividerX - 1, verticalDividerStartY, verticalDividerX, verticalDividerEndY, 0xFF4A3A2A);
        
        // Правая панель - колода
        renderDeck(guiGraphics, mouseX, mouseY);
        
        // Оптимизация: рендерим tooltip только для карты, на которую наведена мышь
        renderTooltipForHoveredCard(guiGraphics, mouseX, mouseY);
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они будут отрисованы кастомно)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок со скруглёнными углами (как в коллекции)
        for (Button button : buttons) {
            if (button != null) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }

    private void renderAvailableCards(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = layout.getX(2);
        int panelY = layout.getY(18);
        int panelWidth = layout.getWidth(48);
        int panelHeight = layout.getHeight(72); // Отступ снизу для кнопок
        
        guiGraphics.drawString(this.font, 
            Component.literal("§eДоступные карты:"), 
            panelX, panelY - 15, 0xFFFFFF, false);
        
        // Адаптивные размеры карт
        int cardWidth = Math.max(50, Math.min(60, (panelWidth - 20) / 4));
        int cardHeight = (int)(cardWidth * 1.5);
        int cardsPerRow = Math.max(3, Math.min(4, (panelWidth - 20) / (cardWidth + 5)));
        int startY = panelY;
        
        // Вычисляем количество видимых карт
        int rowsVisible = Math.max(1, (panelHeight - 10) / (cardHeight + 5));
        int visibleCards = Math.min(availableCards.size() - scrollOffset, rowsVisible * cardsPerRow);
        for (int i = 0; i < visibleCards; i++) {
            int cardIndex = scrollOffset + i;
            if (cardIndex >= availableCards.size()) break;
            
            Card card = availableCards.get(cardIndex);
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = panelX + col * (cardWidth + 5);
            int y = startY + row * (cardHeight + 5);
            
            // Рендерим карту в простом пиксельном стиле с улучшенной детализацией
            boolean isHovered = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight;
            SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, cardWidth, cardHeight,
                isHovered ? mouseX : -1, isHovered ? mouseY : -1, false, false);
            
            // Кнопка добавления (клик по карте) - только если карта в коллекции
            if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
                // Проверяем, что карта есть в коллекции (availableCards уже содержит только карты из коллекции)
                boolean cardInCollection = availableCards.contains(card);
                if (cardInCollection && minecraft != null && minecraft.mouseHandler.isLeftPressed()) {
                    // Проверяем лимит карт
                    if (deckCards.size() >= 25) {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§cКолода не может содержать более 25 карт!"));
                        }
                        return;
                    }
                    
                    // Проверяем дубликаты (правило: 1 карта одного вида на игрока)
                    boolean alreadyInDeck = deckCards.stream()
                        .anyMatch(c -> c.getId().equals(card.getId()));
                    
                    if (alreadyInDeck) {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§cЭта карта уже есть в колоде!"));
                        }
                        return;
                    }
                    
                    deckCards.add(card);
                    if (minecraft.player != null) {
                        minecraft.player.sendSystemMessage(Component.literal("§aКарта добавлена: " + card.getName()));
                    }
                }
            }
        }
        
        // Кнопки прокрутки (стилизованные, с правильным позиционированием)
        int scrollButtonX = panelX + panelWidth - 22;
        int scrollButtonSize = 18;
        int scrollButtonSpacing = 3;
        
        int maxScroll = Math.max(0, availableCards.size() - (rowsVisible * cardsPerRow));
        int scrollStep = cardsPerRow; // Прокрутка по одной строке
        
        if (scrollOffset > 0) {
            Button scrollUpBtn = createStyledButton(scrollButtonX, panelY, scrollButtonSize, scrollButtonSize, 
                Component.literal("▲"), 
                (btn) -> { 
                    scrollOffset = Math.max(0, scrollOffset - scrollStep); 
                });
            this.addRenderableWidget(scrollUpBtn);
            buttons.add(scrollUpBtn);
        }
        
        int scrollDownY = panelY + panelHeight - scrollButtonSize - scrollButtonSpacing - 25;
        if (scrollOffset < maxScroll) {
            Button scrollDownBtn = createStyledButton(scrollButtonX, scrollDownY, scrollButtonSize, scrollButtonSize, 
                Component.literal("▼"), 
                (btn) -> { 
                    scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep); 
                });
            this.addRenderableWidget(scrollDownBtn);
            buttons.add(scrollDownBtn);
        }
    }

    private void renderDeck(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = layout.getX(52);
        int panelY = layout.getY(18);
        int panelWidth = layout.getWidth(48);
        int panelHeight = layout.getHeight(72);
        
        guiGraphics.drawString(this.font, 
            Component.literal("§aКолода:"), 
            panelX, panelY - 15, 0xFFFFFF, false);
        
        // Адаптивные размеры карт
        int cardWidth = Math.max(50, Math.min(60, (panelWidth - 20) / 4));
        int cardHeight = (int)(cardWidth * 1.5);
        int cardsPerRow = Math.max(3, Math.min(4, (panelWidth - 20) / (cardWidth + 5)));
        
        // Вычисляем количество видимых карт
        int rowsVisible = Math.max(1, (panelHeight - 10) / (cardHeight + 5));
        int maxVisibleCards = rowsVisible * cardsPerRow;
        int startIndex = deckScrollOffset;
        int endIndex = Math.min(startIndex + maxVisibleCards, deckCards.size());
        
        // Рендерим видимые карты в колоде
        for (int i = startIndex; i < endIndex; i++) {
            Card card = deckCards.get(i);
            int displayIndex = i - startIndex;
            int row = displayIndex / cardsPerRow;
            int col = displayIndex % cardsPerRow;
            int x = panelX + col * (cardWidth + 5);
            int y = panelY + row * (cardHeight + 5);
            
            // Рендерим карту в простом пиксельном стиле с улучшенной детализацией
            boolean isHovered = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight;
            SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, cardWidth, cardHeight,
                isHovered ? mouseX : -1, isHovered ? mouseY : -1, false, false);
        }
        
        // Кнопки прокрутки для колоды
        if (deckCards.size() > maxVisibleCards) {
            int scrollButtonX = panelX + panelWidth - 22;
            int scrollButtonSize = 18;
            int scrollButtonSpacing = 3;
            
            int maxDeckScroll = Math.max(0, deckCards.size() - maxVisibleCards);
            int deckScrollStep = cardsPerRow;
            
            if (deckScrollOffset > 0) {
                Button scrollUpBtn = createStyledButton(scrollButtonX, panelY, scrollButtonSize, scrollButtonSize, 
                    Component.literal("▲"), 
                    (btn) -> { 
                        deckScrollOffset = Math.max(0, deckScrollOffset - deckScrollStep); 
                    });
                this.addRenderableWidget(scrollUpBtn);
                buttons.add(scrollUpBtn);
            }
            
            int scrollDownY = panelY + panelHeight - scrollButtonSize - scrollButtonSpacing - 25;
            if (deckScrollOffset < maxDeckScroll) {
                Button scrollDownBtn = createStyledButton(scrollButtonX, scrollDownY, scrollButtonSize, scrollButtonSize, 
                    Component.literal("▼"), 
                    (btn) -> { 
                        deckScrollOffset = Math.min(maxDeckScroll, deckScrollOffset + deckScrollStep); 
                    });
                this.addRenderableWidget(scrollDownBtn);
                buttons.add(scrollDownBtn);
            }
        }
        
        // Подсказка о добавлении карт (адаптивное позиционирование)
        int hintY = layout.getY(88);
        if (deckCards.size() < 25) {
            guiGraphics.drawString(this.font, 
                Component.literal("§7ЛКМ - добавить"), 
                panelX, hintY, 0xFFFFFF, false);
        }
        if (!deckCards.isEmpty()) {
            guiGraphics.drawString(this.font, 
                Component.literal("§7ПКМ - удалить"), 
                panelX, hintY + 12, 0xFFFFFF, false);
        }
    }

    /**
     * Рендерит tooltip для карты, на которую наведена мышь (оптимизация производительности)
     */
    private void renderTooltipForHoveredCard(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Проверяем доступные карты
        int panelX = layout.getX(2);
        int panelY = layout.getY(21);
        int panelWidth = GUI_WIDTH / 2 - 30;
        int panelHeight = GUI_HEIGHT - 140;
        int cardWidth = Math.max(50, Math.min(60, (panelWidth - 20) / 4));
        int cardHeight = (int)(cardWidth * 1.5);
        int cardsPerRow = Math.max(3, Math.min(4, (panelWidth - 20) / (cardWidth + 5)));
        int startY = panelY;
        int rowsVisible = Math.max(1, (panelHeight - 10) / (cardHeight + 5));
        int visibleCards = Math.min(availableCards.size() - scrollOffset, rowsVisible * cardsPerRow);
        
        for (int i = 0; i < visibleCards; i++) {
            int cardIndex = scrollOffset + i;
            if (cardIndex >= availableCards.size()) break;
            
            Card card = availableCards.get(cardIndex);
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = panelX + col * (cardWidth + 5);
            int y = startY + row * (cardHeight + 5);
            
            if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
                renderCardTooltip(guiGraphics, card, mouseX, mouseY, false);
                return;
            }
        }
        
        // Проверяем карты в колоде
        panelX = layout.getX(52);
        panelY = layout.getY(21);
        panelWidth = GUI_WIDTH / 2 - 30;
        panelHeight = GUI_HEIGHT - 140;
        cardWidth = Math.max(50, Math.min(60, (panelWidth - 20) / 4));
        cardHeight = (int)(cardWidth * 1.5);
        cardsPerRow = Math.max(3, Math.min(4, (panelWidth - 20) / (cardWidth + 5)));
        rowsVisible = Math.max(1, (panelHeight - 10) / (cardHeight + 5));
        int maxVisibleCards = rowsVisible * cardsPerRow;
        int startIndex = deckScrollOffset;
        int endIndex = Math.min(startIndex + maxVisibleCards, deckCards.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Card card = deckCards.get(i);
            int displayIndex = i - startIndex;
            int row = displayIndex / cardsPerRow;
            int col = displayIndex % cardsPerRow;
            int x = panelX + col * (cardWidth + 5);
            int y = panelY + row * (cardHeight + 5);
            
            if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
                renderCardTooltip(guiGraphics, card, mouseX, mouseY, true);
                return;
            }
        }
    }
    
    /**
     * Рендерит tooltip для карты
     */
    private void renderCardTooltip(GuiGraphics guiGraphics, Card card, int mouseX, int mouseY, boolean inDeck) {
        // Разбиваем описание на строки для корректного отображения
        int maxTooltipWidth = 200; // Максимальная ширина tooltip
        List<net.minecraft.util.FormattedCharSequence> descriptionLines = this.font.split(
            Component.literal("§7" + card.getDescription()), maxTooltipWidth);
        
        // Собираем tooltip: название, описание (разбитое на строки), сила, действие
        List<net.minecraft.util.FormattedCharSequence> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.literal("§6" + card.getName()).getVisualOrderText());
        tooltip.addAll(descriptionLines); // Добавляем все строки описания
        tooltip.add(Component.literal("§eСила: §f" + card.getPower()).getVisualOrderText());
        tooltip.add((inDeck ? Component.literal("§cПКМ для удаления") : Component.literal("§aЛКМ для добавления")).getVisualOrderText());
        
        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }
    
    /**
     * УДАЛЕНО: Старый метод renderSmallCard заменён на использование CardRenderer
     * Теперь карты рендерятся в новом скевоморфном стиле через CardRenderer.renderCard()
     */

    private void saveDeck() {
        if (deckName.isEmpty() || deckName.trim().isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§cВведите название колоды!"));
            }
            return;
        }
        
        if (deckCards.size() > 25) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§cКолода не может содержать более 25 карт!"));
            }
            return;
        }
        
        // Проверяем, что все карты есть в коллекции и нет дубликатов
        java.util.Set<String> availableCardIds = availableCards.stream()
            .map(Card::getId)
            .collect(java.util.stream.Collectors.toSet());
        
        java.util.List<String> validCardIds = new java.util.ArrayList<>();
        java.util.Set<String> addedIds = new java.util.HashSet<>();
        int skippedCount = 0;
        
        for (Card card : deckCards) {
            String cardId = card.getId();
            
            // Пропускаем дубликаты
            if (addedIds.contains(cardId)) {
                skippedCount++;
                continue;
            }
            
            // Проверяем, что карта есть в коллекции
            if (availableCardIds.contains(cardId)) {
                validCardIds.add(cardId);
                addedIds.add(cardId);
            } else {
                skippedCount++;
                BMFalkye.LOGGER.warn("Card {} not in collection, skipping", cardId);
            }
        }
        
        if (skippedCount > 0 && minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§eПропущено " + skippedCount + " карт (дубликаты или отсутствуют в коллекции)"));
        }
        
        // Определяем слот для сохранения
        int slotToSave = selectedDeckSlot;
        if (slotToSave < 0) {
            // Ищем первый свободный слот
            for (int i = 0; i < 10; i++) {
                if (i >= savedDecks.size()) {
                    slotToSave = i;
                    break;
                }
            }
            if (slotToSave < 0) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Component.literal("§cВсе слоты заняты! Выберите слот для перезаписи."));
                }
                return;
            }
        }
        
        // Отправляем на сервер
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.NetworkHandler.SaveDeckPacket(
                deckName.trim(), validCardIds, null, slotToSave));
        
        // Обновляем выбранный слот
        selectedDeckSlot = slotToSave;
        
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§aСохранение колоды: " + deckName + " в слот " + (slotToSave + 1)));
        }
        
        // Запрашиваем обновленный список колод
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.NetworkHandler.RequestDecksPacket());
        
        BMFalkye.LOGGER.info("Saving deck: {} with {} cards to slot {}", deckName, validCardIds.size(), slotToSave);
    }

    private void loadDeck() {
        if (selectedDeckSlot < 0 || selectedDeckSlot >= 10) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§cВыберите слот колоды для загрузки!"));
            }
            return;
        }
        
        if (selectedDeckSlot >= savedDecks.size()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§cВ этом слоте нет сохраненной колоды!"));
            }
            return;
        }
        
        // Запрашиваем колоду с сервера
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.NetworkHandler.LoadDeckPacket(selectedDeckSlot));
        
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§aЗагрузка колоды из слота " + (selectedDeckSlot + 1) + "..."));
        }
        
        BMFalkye.LOGGER.info("Loading deck from slot {}", selectedDeckSlot);
    }
    
    /**
     * Обновляет список колод (вызывается при получении данных с сервера)
     */
    public void updateDecksList(java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks) {
        this.savedDecks = new java.util.ArrayList<>(decks);
        BMFalkye.LOGGER.info("Received {} decks from server", decks.size());
        
        // Пересоздаем UI для обновления кнопок слотов
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                this.init();
            });
        }
    }
    
    /**
     * Загружает данные колоды (вызывается при получении данных с сервера)
     */
    public void loadDeckData(com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData) {
        this.deckName = deckData.getDeckName();
        if (this.deckNameBox != null) {
            this.deckNameBox.setValue(deckName);
        }
        
        // Загружаем карты (только те, что есть в коллекции)
        this.deckCards.clear();
        java.util.Set<String> availableCardIds = availableCards.stream()
            .map(Card::getId)
            .collect(java.util.stream.Collectors.toSet());
        
        int loadedCount = 0;
        int skippedCount = 0;
        for (String cardId : deckData.getCardIds()) {
            // Проверяем, что карта есть в коллекции
            if (availableCardIds.contains(cardId)) {
                Card card = CardRegistry.getCard(cardId);
                if (card != null) {
                    this.deckCards.add(card);
                    loadedCount++;
                }
            } else {
                skippedCount++;
            }
        }
        
        if (minecraft != null && minecraft.player != null) {
            if (skippedCount > 0) {
                minecraft.player.sendSystemMessage(Component.literal("§aКолода загружена: " + deckName + " (§e" + loadedCount + " карт, пропущено: " + skippedCount + "§a)"));
            } else {
                minecraft.player.sendSystemMessage(Component.literal("§aКолода загружена: " + deckName + " (§e" + loadedCount + " карт§a)"));
            }
        }
        
        BMFalkye.LOGGER.info("Loaded deck: {} with {} cards ({} skipped)", deckName, loadedCount, skippedCount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка кликов по картам в доступных картах (левая панель)
        int panelX = layout.getX(2);
        int panelY = layout.getY(21);
        int panelWidth = GUI_WIDTH / 2 - 30;
        int panelHeight = GUI_HEIGHT - 140;
        
        // Адаптивные размеры для обработки кликов
        int cardWidth = Math.max(50, Math.min(60, (panelWidth - 20) / 4));
        int cardHeight = (int)(cardWidth * 1.5);
        int cardsPerRow = Math.max(3, Math.min(4, (panelWidth - 20) / (cardWidth + 5)));
        int rowsVisible = Math.max(1, (panelHeight - 10) / (cardHeight + 5));
        int maxVisibleCards = rowsVisible * cardsPerRow;
        
        if (button == 0) { // Левый клик - добавление карты
            for (int i = 0; i < Math.min(availableCards.size() - scrollOffset, maxVisibleCards); i++) {
                int cardIndex = scrollOffset + i;
                if (cardIndex >= availableCards.size()) break;
                
                Card card = availableCards.get(cardIndex);
                int row = i / cardsPerRow;
                int col = i % cardsPerRow;
                int x = panelX + col * (cardWidth + 5);
                int y = panelY + row * (cardHeight + 5);
                
                if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
                    // Проверяем лимит карт
                    if (deckCards.size() >= 25) {
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§cКолода не может содержать более 25 карт!"));
                        }
                        return true;
                    }
                    
                    // Проверяем дубликаты (правило: 1 карта одного вида на игрока)
                    boolean alreadyInDeck = deckCards.stream()
                        .anyMatch(c -> c.getId().equals(card.getId()));
                    
                    if (alreadyInDeck) {
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§cЭта карта уже есть в колоде!"));
                        }
                        return true;
                    }
                    
                    deckCards.add(card);
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.sendSystemMessage(Component.literal("§aКарта добавлена: " + card.getName()));
                    }
                    return true;
                }
            }
        }
        
        // Обработка кликов по картам в колоде (правая панель)
        int deckPanelX = layout.getX(52);
        int deckPanelY = layout.getY(21);
        int deckPanelWidth = GUI_WIDTH / 2 - 30;
        int deckPanelHeight = GUI_HEIGHT - 140;
        
        // Адаптивные размеры для колоды
        int deckCardWidth = Math.max(50, Math.min(60, (deckPanelWidth - 20) / 4));
        int deckCardHeight = (int)(deckCardWidth * 1.5);
        int deckCardsPerRow = Math.max(3, Math.min(4, (deckPanelWidth - 20) / (deckCardWidth + 5)));
        int deckRowsVisible = Math.max(1, (deckPanelHeight - 10) / (deckCardHeight + 5));
        int deckMaxVisibleCards = deckRowsVisible * deckCardsPerRow;
        
        if (button == 1) { // Правый клик - удаление карты
            int startIndex = deckScrollOffset;
            int endIndex = Math.min(startIndex + deckMaxVisibleCards, deckCards.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                Card card = deckCards.get(i);
                int displayIndex = i - startIndex;
                int row = displayIndex / deckCardsPerRow;
                int col = displayIndex % deckCardsPerRow;
                int x = deckPanelX + col * (deckCardWidth + 5);
                int y = deckPanelY + row * (deckCardHeight + 5);
                
                if (mouseX >= x && mouseX <= x + deckCardWidth && mouseY >= y && mouseY <= y + deckCardHeight) {
                    Card removedCard = deckCards.remove(i);
                    // Обновляем скролл, если нужно
                    if (deckScrollOffset >= deckCards.size() - deckMaxVisibleCards + 1) {
                        deckScrollOffset = Math.max(0, deckCards.size() - deckMaxVisibleCards);
                    }
                    if (minecraft != null && minecraft.player != null && removedCard != null) {
                        minecraft.player.sendSystemMessage(Component.literal("§cКарта удалена: " + removedCard.getName()));
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
