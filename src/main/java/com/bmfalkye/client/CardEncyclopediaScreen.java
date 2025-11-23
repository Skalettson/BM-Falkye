package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.CardTextures;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.client.gui.AdaptiveLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Энциклопедия карт с фильтрами
 * Реализовано в стиле BM Characters с использованием стандартных компонентов Minecraft
 */
public class CardEncyclopediaScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 400;
    private static final int MIN_GUI_WIDTH = 500;
    private static final int MIN_GUI_HEIGHT = 350;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    // Адаптивные размеры
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    private List<Card> allCards;
    private List<Card> filteredCards;
    private int scrollOffset = 0;
    private String searchQuery = "";
    private String selectedFaction = "Все";
    private CardRarity selectedRarity = null;
    private Card.CardType selectedType = null;
    
    private EditBox searchBox;
    private int cardsPerPage = 12;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Список кнопок для кастомного рендеринга (как в CardCollectionScreen)
    private final java.util.List<Button> buttons = new java.util.ArrayList<>();
    private final net.minecraft.client.gui.screens.Screen parentScreen;

    public CardEncyclopediaScreen() {
        this(com.bmfalkye.client.ClientPacketHandler.getMainMenuParent());
    }
    
    public CardEncyclopediaScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.encyclopedia"));
        this.parentScreen = parentScreen;
        this.allCards = CardRegistry.getAllCards();
        this.filteredCards = new ArrayList<>(allCards);
    }

    @Override
    protected void init() {
        super.init();
        
        // Очищаем список кнопок
        buttons.clear();
        
        // Инициализируем систему автоматической адаптации
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Поле поиска (адаптивное позиционирование)
        this.searchBox = new EditBox(this.font, layout.getX(3), layout.getY(15), layout.getWidth(50), 20, Component.literal("Поиск..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);
        
        // Кнопки фильтров по фракциям (стилизованные, как в коллекции)
        int factionX = layout.getX(3);
        int factionY = layout.getY(24); // Адаптивное позиционирование
        Button btn1 = createStyledFilterButton(factionX, factionY, 70, 22, Component.literal("Все"), 
            (btn) -> { selectedFaction = "Все"; updateFilters(); });
        this.addRenderableWidget(btn1);
        buttons.add(btn1);
        Button btn2 = createStyledFilterButton(factionX + 80, factionY, 110, 22, Component.literal("Дом Пламени"), 
            (btn) -> { selectedFaction = "Дом Пламени"; updateFilters(); });
        this.addRenderableWidget(btn2);
        buttons.add(btn2);
        Button btn3 = createStyledFilterButton(factionX + 200, factionY, 110, 22, Component.literal("Дозорные Руин"), 
            (btn) -> { selectedFaction = "Дозорные Руин"; updateFilters(); });
        this.addRenderableWidget(btn3);
        buttons.add(btn3);
        Button btn4 = createStyledFilterButton(factionX + 320, factionY, 110, 22, Component.literal("Дети Рощения"), 
            (btn) -> { selectedFaction = "Дети Рощения"; updateFilters(); });
        this.addRenderableWidget(btn4);
        buttons.add(btn4);
        
        // Кнопки фильтров по редкости (стилизованные, как в коллекции)
        int rarityY = factionY + 30; // Увеличен отступ между рядами
        Button btn5 = createStyledFilterButton(factionX, rarityY, 90, 22, Component.literal("Все редкости"), 
            (btn) -> { selectedRarity = null; updateFilters(); });
        this.addRenderableWidget(btn5);
        buttons.add(btn5);
        Button btn6 = createStyledFilterButton(factionX + 100, rarityY, 80, 22, Component.literal("Обычные"), 
            (btn) -> { selectedRarity = CardRarity.COMMON; updateFilters(); });
        this.addRenderableWidget(btn6);
        buttons.add(btn6);
        Button btn7 = createStyledFilterButton(factionX + 190, rarityY, 80, 22, Component.literal("Редкие"), 
            (btn) -> { selectedRarity = CardRarity.RARE; updateFilters(); });
        this.addRenderableWidget(btn7);
        buttons.add(btn7);
        Button btn8 = createStyledFilterButton(factionX + 280, rarityY, 90, 22, Component.literal("Эпические"), 
            (btn) -> { selectedRarity = CardRarity.EPIC; updateFilters(); });
        this.addRenderableWidget(btn8);
        buttons.add(btn8);
        Button btn9 = createStyledFilterButton(factionX + 380, rarityY, 110, 22, Component.literal("Легендарные"), 
            (btn) -> { selectedRarity = CardRarity.LEGENDARY; updateFilters(); });
        this.addRenderableWidget(btn9);
        buttons.add(btn9);
        
        // Кнопки навигации (стилизованные, адаптивное позиционирование)
        Button rulesBtn = createStyledButton(layout.getX(3), layout.getBottomY(25, 8), 100, 25, 
            Component.translatable("button.bm_falkye.rules"), 
            (btn) -> this.minecraft.setScreen(new FalkyeTutorialScreen()));
        this.addRenderableWidget(rulesBtn);
        buttons.add(rulesBtn);
        
        Button backBtn = createStyledButton(layout.getCenteredX(100), layout.getBottomY(25, 8), 100, 25, 
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
    
    /**
     * Создаёт стилизованную кнопку фильтра с закруглёнными углами
     */
    private Button createStyledFilterButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return GuiUtils.createStyledButton(x, y, width, height, text, onPress);
    }
    
    /**
     * Создаёт стилизованную кнопку с закруглёнными углами
     */
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return GuiUtils.createStyledButton(x, y, width, height, text, onPress);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                             MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            this.GUI_WIDTH = layout.getGuiWidth();
            this.GUI_HEIGHT = layout.getGuiHeight();
        }
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // ПЕРЕПИСАНО: Рисуем фон окна в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок (адаптивное позиционирование)
        Component titleComponent = Component.translatable("screen.bm_falkye.encyclopedia");
        int titleWidth = this.font.width(titleComponent);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        // Тень заголовка (черная)
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, titleY, 0xFFFFFF, false);
        
        // Статистика (адаптивное позиционирование)
        guiGraphics.drawString(this.font, 
            Component.literal("§7Всего карт: §f" + allCards.size() + " §7| Показано: §f" + filteredCards.size()), 
            layout.getX(3), layout.getY(7), 0xFFFFFF, false);
        
        // Информация о коллекции (верхняя граница области скроллинга) - как в коллекции
        int infoY = layout.getY(38);
        guiGraphics.drawString(this.font, 
            Component.literal("§7Карт в энциклопедии: §f" + filteredCards.size()), 
            layout.getX(3), infoY, 0xFFFFFF, false);
        
        // Область для карт (начинается после информации)
        int cardsAreaStartY = infoY + layout.getSpacing() * 2; // Отступ от "Карт в энциклопедии: X"
        int cardsAreaEndY = layout.getBottomY(0, 8); // Выше кнопок
        int cardsAreaHeight = cardsAreaEndY - cardsAreaStartY;
        
        // Параметры карт
        int cardWidth = 80;
        int cardHeight = 120;
        int spacing = 10;
        int cardsPerRow = 6;
        int rowHeight = cardHeight + spacing;
        
        // Включаем обрезку для области карт (scissor testing)
        guiGraphics.enableScissor(layout.getX(3), cardsAreaStartY, layout.getX(97), cardsAreaEndY);
        
        // Рендерим карты (без tooltip для оптимизации)
        int startX = layout.getX(5);
        int startY = cardsAreaStartY;
        
        int totalRows = (filteredCards.size() + cardsPerRow - 1) / cardsPerRow;
        int visibleStart = Math.max(0, scrollOffset / cardsPerRow);
        int visibleEnd = Math.min(totalRows, visibleStart + (cardsAreaHeight / rowHeight) + 1);
        
        Card hoveredCard = null;
        int hoveredCardX = 0;
        int hoveredCardY = 0;
        
        for (int row = visibleStart; row < visibleEnd; row++) {
            for (int col = 0; col < cardsPerRow; col++) {
                int cardIndex = row * cardsPerRow + col;
                if (cardIndex >= filteredCards.size()) break;
                
                Card card = filteredCards.get(cardIndex);
                int x = startX + col * (cardWidth + spacing);
                int y = startY + (row - visibleStart) * rowHeight;
                
                // Рендерим карту без tooltip (tooltip будет показан отдельно)
                renderCard(guiGraphics, card, x, y, -1, -1);
                
                // Проверяем наведение мыши ТОЛЬКО в видимой области (исправление бага)
                // Важно: проверяем, что мышь находится в пределах видимой области карты
                if (mouseX >= x && mouseX <= x + cardWidth && 
                    mouseY >= y && mouseY <= y + cardHeight &&
                    mouseX >= layout.getX(3) && mouseX <= layout.getX(97) &&
                    mouseY >= cardsAreaStartY && mouseY <= cardsAreaEndY) {
                    hoveredCard = card;
                    hoveredCardX = mouseX;
                    hoveredCardY = mouseY;
                }
            }
        }
        
        // Рендерим tooltip только для карты, на которую наведена мышь (оптимизация)
        if (hoveredCard != null) {
            renderCardTooltip(guiGraphics, hoveredCard, hoveredCardX, hoveredCardY);
        }
        
        // Отключаем обрезку
        guiGraphics.disableScissor();
        
        // Кнопки прокрутки (стилизованные)
        if (scrollOffset > 0) {
            Button scrollUpBtn = createStyledButton(layout.getRightX(20, 3), layout.getCenteredY(40) - 20, 20, 20, 
                Component.literal("▲"), 
                (btn) -> { 
                    scrollOffset = Math.max(0, scrollOffset - cardsPerRow); 
                    this.clearWidgets(); 
                    this.init(); 
                });
            this.addRenderableWidget(scrollUpBtn);
            buttons.add(scrollUpBtn);
        }
        if (scrollOffset + (cardsAreaHeight / rowHeight + 1) * cardsPerRow < filteredCards.size()) {
            Button scrollDownBtn = createStyledButton(layout.getRightX(20, 3), layout.getCenteredY(40) + 20, 20, 20, 
                Component.literal("▼"), 
                (btn) -> { 
                    scrollOffset = Math.min(filteredCards.size() - cardsPerRow, scrollOffset + cardsPerRow); 
                    this.clearWidgets(); 
                    this.init(); 
                });
            this.addRenderableWidget(scrollDownBtn);
            buttons.add(scrollDownBtn);
        }
        
        // Информация о странице
        if (filteredCards.size() > cardsPerPage) {
            int totalPages = (filteredCards.size() + cardsPerPage - 1) / cardsPerPage;
            int currentPage = (scrollOffset / cardsPerPage) + 1;
            guiGraphics.drawString(this.font, 
                Component.literal("§7Страница: §f" + currentPage + " §7/ §f" + totalPages), 
                layout.getX(3), layout.getBottomY(0, 12), 0xFFFFFF, false);
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они будут отрисованы кастомно)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок со скруглёнными углами (как в коллекции)
        for (Button button : buttons) {
            if (button != null) {
                // Определяем, выбрана ли кнопка (для фильтров)
                boolean isSelected = false;
                String buttonText = button.getMessage().getString();
                if (buttonText.equals("Все") && selectedFaction.equals("Все")) {
                    isSelected = true;
                } else if (buttonText.equals("Дом Пламени") && selectedFaction.equals("Дом Пламени")) {
                    isSelected = true;
                } else if (buttonText.equals("Дозорные Руин") && selectedFaction.equals("Дозорные Руин")) {
                    isSelected = true;
                } else if (buttonText.equals("Дети Рощения") && selectedFaction.equals("Дети Рощения")) {
                    isSelected = true;
                } else if (buttonText.equals("Все редкости") && selectedRarity == null) {
                    isSelected = true;
                } else if (buttonText.equals("Обычные") && selectedRarity == CardRarity.COMMON) {
                    isSelected = true;
                } else if (buttonText.equals("Редкие") && selectedRarity == CardRarity.RARE) {
                    isSelected = true;
                } else if (buttonText.equals("Эпические") && selectedRarity == CardRarity.EPIC) {
                    isSelected = true;
                } else if (buttonText.equals("Легендарные") && selectedRarity == CardRarity.LEGENDARY) {
                    isSelected = true;
                }
                
                // Проверяем, есть ли иконка фракции для этой кнопки
                ResourceLocation factionIcon = CardTextures.getFactionIcon(buttonText);
                boolean hasFactionIcon = factionIcon != null && CardTextures.hasFactionIcon(buttonText);
                
                if (hasFactionIcon) {
                    // Рендерим кнопку с иконкой фракции
                    renderButtonWithFactionIcon(guiGraphics, button, mouseX, mouseY, isSelected, factionIcon);
                } else {
                    // Обычная кнопка без иконки
                    GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, isSelected);
                }
            }
        }
    }
    
    /**
     * Рендерит кнопку с иконкой фракции (как в CardCollectionScreen)
     * Использует новую текстуру mod_button.png
     */
    private void renderButtonWithFactionIcon(GuiGraphics guiGraphics, Button button, int mouseX, int mouseY, 
                                             boolean isSelected, ResourceLocation factionIcon) {
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        
        // Рендерим фон кнопки с новой текстурой
        GuiUtils.renderButtonBackground(guiGraphics, button, isSelected);
        
        // Иконка фракции (адаптивный размер)
        int iconSize = Math.max(12, Math.min(20, (int)(height * 0.6f)));
        int iconX = x + 6;
        int iconY = y + (height - iconSize) / 2;
        guiGraphics.blit(factionIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        
        // Текст (сдвинут вправо, чтобы не перекрывался с иконкой)
        int textOffset = iconSize + 6;
        String text = button.getMessage().getString();
        int textX = x + textOffset;
        int textY = y + (height - this.font.lineHeight) / 2;
        boolean hovered = button.isHovered();
        int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFF0F0F0 : 0xFFE0E0E0);
        // Рисуем текст с тенью для лучшей читаемости
        guiGraphics.drawString(this.font, Component.literal(text), textX + 1, textY + 1, 0x000000, false);
        guiGraphics.drawString(this.font, Component.literal(text), textX, textY, textColor, false);
    }

    private void renderCard(GuiGraphics guiGraphics, Card card, int x, int y, int mouseX, int mouseY) {
        int width = 80;
        int height = 120;
        
        // Используем общий метод отрисовки карт в стиле редактора колод
        // Tooltip рендерится отдельно для оптимизации производительности
        boolean showTooltip = mouseX >= 0 && mouseY >= 0; // Проверяем, что координаты валидны
        SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, width, height, 
                               mouseX, mouseY, false, showTooltip);
    }
    
    /**
     * Рендерит tooltip для карты (оптимизация производительности)
     */
    private void renderCardTooltip(GuiGraphics guiGraphics, Card card, int mouseX, int mouseY) {
        // Разбиваем описание на строки для корректного отображения
        int maxTooltipWidth = 200; // Максимальная ширина tooltip
        List<net.minecraft.util.FormattedCharSequence> descriptionLines = this.font.split(
            Component.literal("§7" + card.getDescription()), maxTooltipWidth);
        
        // Собираем tooltip: название, описание (разбитое на строки), сила, редкость
        List<net.minecraft.util.FormattedCharSequence> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.literal("§6" + card.getName()).getVisualOrderText());
        tooltip.addAll(descriptionLines); // Добавляем все строки описания
        tooltip.add(Component.literal("§eСила: §f" + card.getPower()).getVisualOrderText());
        tooltip.add(Component.literal("§bРедкость: §f" + card.getRarity().getDisplayName()).getVisualOrderText());
        
        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.toLowerCase();
        updateFilters();
    }

    private void updateFilters() {
        filteredCards = allCards.stream()
            .filter(card -> {
                // Фильтр по поиску
                if (!searchQuery.isEmpty()) {
                    if (!card.getName().toLowerCase().contains(searchQuery) &&
                        !card.getDescription().toLowerCase().contains(searchQuery)) {
                        return false;
                    }
                }
                
                // Фильтр по фракции
                if (!selectedFaction.equals("Все") && !card.getFaction().equals(selectedFaction)) {
                    return false;
                }
                
                // Фильтр по редкости
                if (selectedRarity != null && card.getRarity() != selectedRarity) {
                    return false;
                }
                
                // Фильтр по типу
                if (selectedType != null && card.getType() != selectedType) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        scrollOffset = 0; // Сбрасываем прокрутку при изменении фильтров
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
