package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.CardTextures;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.network.NetworkHandler;
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
 * Экран коллекции карт игрока
 * Полностью переработанный интерфейс с использованием API и библиотек
 * Улучшенное визуальное оформление с градиентами, тенями и свечением
 */
public class CardCollectionScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 400;
    private static final int MIN_GUI_WIDTH = 500;
    private static final int MIN_GUI_HEIGHT = 350;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    // Адаптивные размеры
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    private List<Card> playerCards; // Карты из коллекции игрока
    private List<Card> filteredCards;
    private int scrollOffset = 0;
    private String searchQuery = "";
    private String selectedFaction = "Все";
    private CardRarity selectedRarity = null;
    
    private EditBox searchBox;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Список кнопок для кастомного рендеринга
    private final java.util.List<Button> buttons = new java.util.ArrayList<>();
    
    // Время анимации
    private long animationTime = 0;
    
    private final net.minecraft.client.gui.screens.Screen parentScreen;

    public CardCollectionScreen() {
        this(null);
    }
    
    public CardCollectionScreen(net.minecraft.client.gui.screens.Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.card_collection"));
        this.parentScreen = parentScreen;
        // Загружаем коллекцию с сервера
        loadPlayerCollection();
    }
    
    private void loadPlayerCollection() {
        // Всегда запрашиваем актуальную коллекцию с сервера при открытии экрана
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardCollectionPacket());
        
        // Инициализируем пустые списки по умолчанию
        this.playerCards = new ArrayList<>();
        this.filteredCards = new ArrayList<>();
        
        // Показываем кэшированную коллекцию, если она есть (будет обновлено при получении данных с сервера)
        java.util.List<String> cached = com.bmfalkye.client.ClientPacketHandler.getCachedCardCollection();
        if (!cached.isEmpty()) {
            updateCollection(cached);
        }
    }
    
    /**
     * Обновляет коллекцию карт (вызывается при получении данных с сервера)
     */
    public void updateCollection(List<String> cardIds) {
        // Убираем дубликаты карт по ID, чтобы предотвратить отображение одинаковых карт несколько раз
        java.util.Set<String> uniqueCardIds = new java.util.HashSet<>(cardIds);
        this.playerCards = uniqueCardIds.stream()
            .map(CardRegistry::getCard)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        updateFilters();
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
        
        initCollectionTab();
    }
    
    private void initCollectionTab() {
        // Поле поиска с улучшенным стилем - автоматическая адаптация
        this.searchBox = new EditBox(this.font, layout.getX(3), layout.getY(15), layout.getWidth(50), layout.getHeight(5), Component.literal("Поиск..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);
        
        // Кнопки фильтров по фракциям с улучшенным визуальным оформлением
        int factionX = layout.getX(3);
        int factionY = layout.getY(24);
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
        
        // Кнопки фильтров по редкости
        int rarityY = factionY + 30;
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
        
        // Кнопка обновления коллекции - автоматическая адаптация
        Button refreshBtn = createStyledButton(layout.getX(70), layout.getY(91), layout.getWidth(17), layout.getHeight(6), 
            Component.translatable("button.bm_falkye.refresh"), 
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCardCollectionPacket());
            });
        this.addRenderableWidget(refreshBtn);
        buttons.add(refreshBtn);
        
        // Кнопка "Назад" - автоматическая адаптация
        Button backBtn = createStyledButton(layout.getX(42), layout.getY(91), layout.getWidth(17), layout.getHeight(6), 
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
     * Использует API GuiUtils для создания улучшенных кнопок
     */
    private Button createStyledFilterButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return GuiUtils.createStyledButton(x, y, width, height, text, onPress);
    }
    
    /**
     * Создаёт стилизованную кнопку с закруглёнными углами
     * Использует API GuiUtils для создания улучшенных кнопок
     */
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return GuiUtils.createStyledButton(x, y, width, height, text, onPress);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Обновляем время анимации (используем Minecraft.getMinecraft().gameTime для более плавной анимации)
        if (this.minecraft != null && this.minecraft.player != null) {
            animationTime = this.minecraft.player.level().getGameTime();
        }
        
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.getGuiWidth() != GUI_WIDTH || layout.getGuiHeight() != GUI_HEIGHT) {
            this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                           MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            this.GUI_WIDTH = layout.getGuiWidth();
            this.GUI_HEIGHT = layout.getGuiHeight();
        }
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // ПЕРЕПИСАНО: Улучшенный фон окна в скевоморфном стиле (деревянная панель)
        long time = System.currentTimeMillis();
        float pulse = (float) (0.5f + 0.2f * Math.sin(time / 2000.0f));
        
        // Деревянная панель с тенью
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        
        // Анимированная металлическая рамка (пульсирующая золотая при высоком pulse)
        boolean goldFrame = pulse > 0.6f;
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, goldFrame);
        
        // Внутреннее свечение (имитация света на дереве)
        int glowAlpha = (int) (pulse * 40);
        int glowColor = (0xFF8B7355 & 0x00FFFFFF) | (glowAlpha << 24);
        guiGraphics.fill(guiX + 3, guiY + 3, guiX + GUI_WIDTH - 3, guiY + 5, glowColor);
        guiGraphics.fill(guiX + 3, guiY + GUI_HEIGHT - 5, guiX + GUI_WIDTH - 3, guiY + GUI_HEIGHT - 3, glowColor);
        
        // Заголовок с эффектом тени
        Component title = Component.translatable("screen.bm_falkye.card_collection");
        int titleWidth = this.font.width(title);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = layout.getY(3);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Рендерим вкладку коллекции
        renderCollectionTab(guiGraphics, mouseX, mouseY, guiX, guiY);
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они будут отрисованы кастомно)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок со скруглёнными углами, градиентами и свечением
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
                    // Рендерим кнопку с иконкой фракции (адаптированная версия)
                    renderButtonWithFactionIcon(guiGraphics, button, mouseX, mouseY, isSelected, factionIcon);
                } else {
                    // Обычная кнопка без иконки с улучшенным стилем
                    GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, isSelected);
                }
            }
        }
    }
    
    private void renderCollectionTab(GuiGraphics guiGraphics, int mouseX, int mouseY, int guiX, int guiY) {
        // Информация о коллекции с улучшенным стилем
        // Используем playerCards.size() для отображения общего количества карт в коллекции
        // (не фильтрованного, чтобы показывать реальное количество)
        int cardCount = playerCards != null ? playerCards.size() : 0;
        int infoY = layout.getY(38);
        Component countText = Component.translatable("screen.bm_falkye.collection_count", cardCount);
        
        // Тень для текста
        guiGraphics.drawString(this.font, countText, layout.getX(3) + 1, infoY + 1, 0x000000, false);
        guiGraphics.drawString(this.font, countText, layout.getX(3), infoY, 0xFFFFFF, false);
        
        // Область для карт (начинается после информации о коллекции)
        int cardsAreaStartY = infoY + layout.getSpacing() * 2;
        int cardsAreaEndY = layout.getY(90);
        int cardsAreaHeight = cardsAreaEndY - cardsAreaStartY;
        
        // Параметры карт
        int cardWidth = 80;
        int cardHeight = 120;
        int cardsPerRow = 6;
        int spacing = 10;
        
        // Вычисляем, сколько карт помещается в видимой области
        int cardsPerColumn = Math.max(1, (cardsAreaHeight + spacing) / (cardHeight + spacing));
        int maxVisibleCards = cardsPerRow * cardsPerColumn;
        
        // Ограничиваем скролл
        int maxScroll = Math.max(0, filteredCards.size() - maxVisibleCards);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Область обрезки (scissor) для карт
        int scissorX = layout.getX(2);
        int scissorY = cardsAreaStartY;
        int scissorWidth = GUI_WIDTH - layout.getSpacing() * 2;
        int scissorHeight = cardsAreaHeight;
        
        // Устанавливаем область обрезки
        guiGraphics.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Рендерим только видимые карты с дополнительной проверкой
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + maxVisibleCards + 2, filteredCards.size()); // +2 для буфера
        
        for (int i = visibleStart; i < visibleEnd; i++) {
            Card card = filteredCards.get(i);
            int cardIndex = i - visibleStart;
            int row = cardIndex / cardsPerRow;
            int col = cardIndex % cardsPerRow;
            int x = layout.getX(3) + col * (cardWidth + spacing);
            int y = cardsAreaStartY + row * (cardHeight + spacing);
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Дополнительная проверка видимости перед рендерингом
            if (y + cardHeight >= cardsAreaStartY && y <= cardsAreaEndY && 
                x + cardWidth >= scissorX && x <= scissorX + scissorWidth) {
                renderCard(guiGraphics, card, x, y, mouseX, mouseY);
            }
        }
        
        // Отключаем область обрезки
        guiGraphics.disableScissor();
        
        // Улучшенные индикаторы прокрутки с анимацией
        if (filteredCards.size() > maxVisibleCards) {
            float scrollAlpha = (float) (0.5f + 0.3f * Math.sin(System.currentTimeMillis() / 500.0f));
            int scrollColor = (int) (scrollAlpha * 255) << 24 | 0xFFFFFF;
            
            if (scrollOffset > 0) {
                guiGraphics.drawString(this.font, "▲", layout.getX(95), cardsAreaStartY, scrollColor, false);
            }
            if (visibleEnd < filteredCards.size()) {
                guiGraphics.drawString(this.font, "▼", layout.getX(95), cardsAreaEndY - 15, scrollColor, false);
            }
        }
    }
    
    private void renderCard(GuiGraphics guiGraphics, Card card, int x, int y, int mouseX, int mouseY) {
        int width = 80;
        int height = 120;
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверка видимости карты перед рендерингом
        if (x + width < 0 || x > this.width || y + height < 0 || y > this.height) {
            return; // Карта не видна - не рендерим
        }
        
        // Проверяем, наведена ли мышь на карту
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Свечение только при наведении (упрощённое)
        if (hovered) {
            int glowColor = card.getRarity().getColor();
            int glowAlpha = 0x40;
            int color = (glowAlpha << 24) | (glowColor & 0x00FFFFFF);
            // Простое свечение (4 линии вместо сложного эффекта)
            guiGraphics.fill(x - 1, y - 1, x + width + 1, y, color);
            guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
            guiGraphics.fill(x - 1, y, x, y + height, color);
            guiGraphics.fill(x + width, y, x + width + 1, y + height, color);
        }
        
        // Рендерим карту (tooltip только при наведении для производительности)
        SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, width, height, 
                               mouseX, mouseY, false, hovered); // tooltip только при наведении
    }
    
    private void onSearchChanged(String query) {
        this.searchQuery = query.toLowerCase();
        updateFilters();
    }
    
    private void updateFilters() {
        // Защита от null или пустого списка
        if (playerCards == null || playerCards.isEmpty()) {
            filteredCards = new ArrayList<>();
            scrollOffset = 0;
            return;
        }
        
        // Фильтруем карты по поиску, фракции и редкости
        filteredCards = playerCards.stream()
            .filter(card -> card != null) // Дополнительная защита от null карт
            .filter(card -> {
                // Поиск
                if (!searchQuery.isEmpty()) {
                    if (!card.getName().toLowerCase().contains(searchQuery) &&
                        !card.getDescription().toLowerCase().contains(searchQuery)) {
                        return false;
                    }
                }
                
                // Фракция
                if (!selectedFaction.equals("Все") && !card.getFaction().equals(selectedFaction)) {
                    return false;
                }
                
                // Редкость
                if (selectedRarity != null && card.getRarity() != selectedRarity) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        scrollOffset = 0;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Скролл для вкладки коллекции
        int infoY = layout.getY(38);
        int cardsAreaStartY = infoY + layout.getSpacing() * 2;
        int cardsAreaEndY = layout.getY(90);
        int cardsAreaHeight = cardsAreaEndY - cardsAreaStartY;
        int cardHeight = 120;
        int spacing = 10;
        int cardsPerRow = 6;
        int cardsPerColumn = Math.max(1, (cardsAreaHeight + spacing) / (cardHeight + spacing));
        int maxVisibleCards = cardsPerRow * cardsPerColumn;
        int maxScroll = Math.max(0, filteredCards.size() - maxVisibleCards);
        
        // Скроллим по одной строке карт
        int scrollStep = cardsPerRow;
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset = Math.max(0, scrollOffset - scrollStep);
            return true;
        } else if (delta < 0 && scrollOffset < maxScroll) {
            scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Рендерит кнопку с иконкой фракции (адаптированная под размер кнопки)
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
    
    /**
     * Смешивает два цвета для анимации
     */
    private static int blendColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        int a = (int) (a1 + (a2 - a1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
