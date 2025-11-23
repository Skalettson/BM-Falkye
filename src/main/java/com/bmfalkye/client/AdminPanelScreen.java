package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Современная админ-панель для управления игроками и картами
 * Переработанный интерфейс с улучшенным дизайном и адаптивным layout
 */
public class AdminPanelScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    // Адаптивные размеры
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Поля ввода
    private EditBox playerNameBox;
    private EditBox cardIdBox;
    private EditBox xpAmountBox;
    private EditBox levelBox;
    private EditBox cardSearchBox;
    
    // Список кнопок для кастомного рендеринга
    private final List<Button> buttons = new ArrayList<>();
    
    // Текущая вкладка
    private int currentTab = 0;
    
    // Статистика выбранного игрока
    private String selectedPlayerStats = "";
    
    // Список карт для выбора
    private List<Card> allCards = new ArrayList<>();
    private List<Card> filteredCards = new ArrayList<>();
    private int cardScrollOffset = 0;
    private Card selectedCard = null;

    // Данные для других вкладок
    private List<String> replayIds = new ArrayList<>();
    private List<String> replayPlayer1Names = new ArrayList<>();
    private List<String> replayPlayer2Names = new ArrayList<>();
    private int replayScrollOffset = 0;
    private java.util.List<Button> replayDeleteButtons = new ArrayList<>();
    private List<String> eventIds = new ArrayList<>();
    private List<String> eventNames = new ArrayList<>();
    private List<Long> eventEndTimes = new ArrayList<>();
    private int eventScrollOffset = 0;
    private List<String> ratingPlayerNames = new ArrayList<>();
    private List<Integer> ratingScores = new ArrayList<>();
    private int ratingScrollOffset = 0;
    private EditBox coinsAmountBox;
    private EditBox achievementIdBox;

    public AdminPanelScreen() {
        super(Component.translatable("screen.bm_falkye.admin_panel"));
        this.allCards = CardRegistry.getAllCards();
        this.filteredCards = new ArrayList<>(allCards);
    }

    @Override
    protected void init() {
        super.init();
        
        this.clearWidgets();
        buttons.clear();
        
        // Инициализируем систему автоматической адаптации
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Заголовок будет отрисован в render()
        
        // Вкладки - красивое расположение в сетке с адаптивным позиционированием
        int tabsPerRow = 3;
        int tabButtonWidth = layout.getWidth(15);
        int tabButtonHeight = layout.getHeight(5);
        int tabSpacing = layout.getSpacing();
        
        String[] tabKeys = {"screen.bm_falkye.admin_tab_cards", "screen.bm_falkye.admin_tab_xp", 
                             "screen.bm_falkye.admin_tab_stats", "screen.bm_falkye.admin_tab_replays",
                             "screen.bm_falkye.admin_tab_events", "screen.bm_falkye.admin_tab_seasons",
                             "screen.bm_falkye.admin_tab_achievements", "screen.bm_falkye.admin_tab_rating",
                             "screen.bm_falkye.admin_tab_coins"};
        ChatFormatting[] tabColors = {ChatFormatting.YELLOW, ChatFormatting.AQUA, ChatFormatting.GREEN,
                                       ChatFormatting.DARK_AQUA, ChatFormatting.GOLD, ChatFormatting.YELLOW,
                                       ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED, ChatFormatting.DARK_GREEN};
        
        int tabStartX = layout.getX(3);
        int tabStartY = layout.getY(10);
        
        for (int i = 0; i < tabKeys.length; i++) {
            int row = i / tabsPerRow;
            int col = i % tabsPerRow;
            int x = tabStartX + col * (tabButtonWidth + tabSpacing);
            int y = tabStartY + row * (tabButtonHeight + tabSpacing);
            
            final int tabIndex = i;
            Button tabBtn = createStyledButton(x, y, tabButtonWidth, tabButtonHeight,
                Component.translatable(tabKeys[i]).withStyle(tabColors[i]),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    currentTab = tabIndex;
                    this.init();
                });
            this.addRenderableWidget(tabBtn);
            buttons.add(tabBtn);
        }
        
        // Поле имени игрока (общее для всех вкладок) - позиционируется после вкладок с учетом реальных размеров
        int tabsRows = (tabKeys.length + tabsPerRow - 1) / tabsPerRow; // Количество строк вкладок
        int tabsTotalHeight = tabsRows * tabButtonHeight + (tabsRows - 1) * tabSpacing; // Реальная высота всех вкладок
        int playerNameY = tabStartY + tabsTotalHeight + layout.getSpacing() * 2; // Позиция после вкладок с отступом
        int inputWidth = layout.getWidth(28);
        int inputHeight = layout.getHeight(4);
        
        // Проверяем, не выходит ли поле за границы экрана
        int maxY = layout.getGuiY() + layout.getGuiHeight() - inputHeight - layout.getHeight(5) - layout.getSpacing() * 2; // Учитываем место для кнопки "Назад"
        if (playerNameY > maxY) {
            playerNameY = maxY; // Сдвигаем вверх, если выходит за границы
        }
        
        this.playerNameBox = new EditBox(this.font, layout.getX(3), playerNameY, inputWidth, inputHeight,
            Component.literal("Введите имя игрока"));
        this.playerNameBox.setMaxLength(32);
        this.playerNameBox.setHint(Component.literal("Имя игрока"));
        this.addRenderableWidget(this.playerNameBox);
        
        
        // Инициализация вкладок
        if (currentTab == 0) {
            initCardsTab();
        } else if (currentTab == 1) {
            initXPTab();
        } else if (currentTab == 2) {
            initStatsTab();
        } else if (currentTab == 3) {
            initReplaysTab();
        } else if (currentTab == 4) {
            initEventsTab();
        } else if (currentTab == 5) {
            initSeasonsTab();
        } else if (currentTab == 6) {
            initAchievementsTab();
        } else if (currentTab == 7) {
            initRatingTab();
        } else if (currentTab == 8) {
            initCoinsTab();
        }
        
        // Кнопка "Назад" - адаптивное позиционирование
        int backButtonWidth = layout.getWidth(18);
        int backButtonHeight = layout.getHeight(5);
        Button backBtn = createStyledButton(
            layout.getX(50) - backButtonWidth / 2,
            layout.getY(93),
            backButtonWidth, backButtonHeight,
            Component.translatable("button.bm_falkye.back"),
            (btn) -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.setScreen(null);
                }
            });
        this.addRenderableWidget(backBtn);
        buttons.add(backBtn);
    }
    
    private void initCardsTab() {
        int inputWidth = layout.getWidth(28);
        int inputHeight = layout.getHeight(4);
        int buttonWidth = layout.getWidth(17);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Поле поиска карт - позиционируется после поля имени игрока с учетом реальных размеров
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        
        // Проверяем, не выходит ли за границы
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3; // Учитываем кнопку "Назад"
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        this.cardSearchBox = new EditBox(this.font, layout.getX(3), contentY, inputWidth, inputHeight,
            Component.literal("Поиск..."));
        this.cardSearchBox.setMaxLength(64);
        this.cardSearchBox.setHint(Component.literal("Название или ID карты"));
        this.cardSearchBox.setResponder(this::onCardSearchChanged);
        this.addRenderableWidget(this.cardSearchBox);
        
        // Поле ID карты - позиционируется после поля поиска
        int cardIdY = contentY + inputHeight + spacing;
        if (cardIdY + inputHeight > maxContentY) {
            cardIdY = maxContentY - inputHeight; // Сдвигаем вверх, если не помещается
        }
        
        this.cardIdBox = new EditBox(this.font, layout.getX(3), cardIdY, inputWidth, inputHeight,
            Component.literal("ID карты"));
        this.cardIdBox.setMaxLength(64);
        this.cardIdBox.setEditable(false);
        this.cardIdBox.setHint(Component.literal("Выберите карту из списка"));
        this.addRenderableWidget(this.cardIdBox);
        
        // Кнопка "Выдать карту" - справа от поля ID, проверяем, не выходит ли за границы
        int buttonX = layout.getX(32);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - layout.getSpacing();
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX; // Сдвигаем влево, если выходит за границы
        }
        
        Button giveCardBtn = createStyledButton(buttonX, cardIdY, buttonWidth, buttonHeight,
            Component.literal("§aВыдать карту"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                String cardId = cardIdBox.getValue();
                if (!playerName.isEmpty() && !cardId.isEmpty()) {
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.AdminGiveCardPacket(playerName, cardId));
                }
            });
        this.addRenderableWidget(giveCardBtn);
        buttons.add(giveCardBtn);
        
        // Кнопка "Разблокировать все карты" - позиционируется после поля ID
        int unlockY = cardIdY + inputHeight + spacing * 2;
        if (unlockY + buttonHeight > maxContentY) {
            unlockY = maxContentY - buttonHeight; // Сдвигаем вверх, если не помещается
        }
        
        int unlockButtonWidth = Math.min(inputWidth + buttonWidth + spacing, layout.getGuiWidth() - layout.getX(3) * 2);
        Button unlockAllBtn = createStyledButton(layout.getX(3), unlockY, unlockButtonWidth, buttonHeight,
            Component.literal("§6Разблокировать все карты"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                if (!playerName.isEmpty()) {
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.AdminUnlockAllPacket(playerName));
                }
            });
        this.addRenderableWidget(unlockAllBtn);
        buttons.add(unlockAllBtn);
    }
    
    private void onCardSearchChanged(String query) {
        String searchQuery = query.toLowerCase();
        filteredCards = allCards.stream()
            .filter(card -> {
                if (searchQuery.isEmpty()) return true;
                return card.getId().toLowerCase().contains(searchQuery) ||
                       card.getName().toLowerCase().contains(searchQuery) ||
                       card.getDescription().toLowerCase().contains(searchQuery);
            })
            .collect(java.util.stream.Collectors.toList());
        cardScrollOffset = 0;
    }
    
    private void initXPTab() {
        int inputWidth = layout.getWidth(28);
        int inputHeight = layout.getHeight(4);
        int buttonWidth = layout.getWidth(17);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Позиционируем после поля имени игрока с учетом реальных размеров
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        // Поле количества опыта
        this.xpAmountBox = new EditBox(this.font, layout.getX(3), contentY, inputWidth, inputHeight,
            Component.literal("Количество опыта"));
        this.xpAmountBox.setMaxLength(10);
        this.xpAmountBox.setFilter(s -> s.matches("\\d*"));
        this.xpAmountBox.setHint(Component.literal("Число"));
        this.addRenderableWidget(this.xpAmountBox);
        
        // Кнопка "Выдать опыт" - проверяем границы
        int buttonX = layout.getX(32);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - layout.getSpacing();
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        Button giveXPBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§bВыдать опыт"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                String xpStr = xpAmountBox.getValue();
                if (!playerName.isEmpty() && !xpStr.isEmpty()) {
                    try {
                        int xp = Integer.parseInt(xpStr);
                        NetworkHandler.INSTANCE.sendToServer(
                            new NetworkHandler.AdminGiveXPPacket(playerName, xp));
                    } catch (NumberFormatException e) {}
                }
            });
        this.addRenderableWidget(giveXPBtn);
        buttons.add(giveXPBtn);
        
        // Поле уровня - позиционируется после поля опыта
        int levelY = contentY + inputHeight + spacing * 2;
        if (levelY + inputHeight > maxContentY) {
            levelY = maxContentY - inputHeight;
        }
        
        this.levelBox = new EditBox(this.font, layout.getX(3), levelY, inputWidth, inputHeight,
            Component.literal("Уровень (1-50)"));
        this.levelBox.setMaxLength(2);
        this.levelBox.setFilter(s -> s.matches("\\d*"));
        this.levelBox.setHint(Component.literal("1-50"));
        this.addRenderableWidget(this.levelBox);
        
        // Кнопка "Установить уровень"
        Button setLevelBtn = createStyledButton(buttonX, levelY, buttonWidth, buttonHeight,
            Component.literal("§bУстановить уровень"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                String levelStr = levelBox.getValue();
                if (!playerName.isEmpty() && !levelStr.isEmpty()) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        if (level >= 1 && level <= 50) {
                            NetworkHandler.INSTANCE.sendToServer(
                                new NetworkHandler.AdminSetLevelPacket(playerName, level));
                        }
                    } catch (NumberFormatException e) {}
                }
            });
        this.addRenderableWidget(setLevelBtn);
        buttons.add(setLevelBtn);
    }
    
    private void initStatsTab() {
        int buttonWidth = layout.getWidth(22);
        int buttonHeight = layout.getHeight(4);
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + layout.getSpacing();
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        // Проверяем, не выходит ли кнопка за границы
        int buttonX = layout.getX(3);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - layout.getSpacing();
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        // Кнопка "Показать статистику"
        Button showStatsBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§aПоказать статистику"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                if (!playerName.isEmpty()) {
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.AdminShowStatsPacket(playerName));
                }
            });
        this.addRenderableWidget(showStatsBtn);
        buttons.add(showStatsBtn);
    }
    
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
        
        // Красивый фон окна с градиентом
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true); // Золотая рамка для админ-панели
        
        // Красивый заголовок с градиентом
        Component titleComponent = Component.literal("§6§l═══════ §e§lАДМИН-ПАНЕЛЬ §6§l═══════");
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = layout.getY(3);
        
        // Тень заголовка (черная)
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, titleY, 0xFFFFFF, false);
        
        // Подзаголовок с текущей вкладкой
        String[] tabNames = {"Карты", "Опыт/Уровень", "Статистика", "Реплеи", "События", "Сезоны", "Достижения", "Рейтинг", "Монеты"};
        if (currentTab >= 0 && currentTab < tabNames.length) {
            Component subtitleComponent = Component.literal("§7Текущая вкладка: §f" + tabNames[currentTab]);
            int subtitleWidth = this.font.width(subtitleComponent);
            guiGraphics.drawString(this.font, subtitleComponent,
                guiX + (GUI_WIDTH - subtitleWidth) / 2, layout.getY(6), 0xCCCCCC, false);
        }
        
        // Подсказки для полей ввода
        int playerNameY = layout.getY(25);
        guiGraphics.drawString(this.font,
            Component.literal("§7Имя игрока:"),
            layout.getX(3), playerNameY - 12, 0xCCCCCC, false);
        
        if (currentTab == 0) {
            int searchY = layout.getY(25);
            guiGraphics.drawString(this.font,
                Component.literal("§7Поиск карты:"),
                layout.getX(3), searchY - 12, 0xCCCCCC, false);
            int cardIdY = searchY + layout.getHeight(4) + layout.getSpacing();
            guiGraphics.drawString(this.font,
                Component.literal("§7ID выбранной карты:"),
                layout.getX(3), cardIdY - 12, 0xCCCCCC, false);
        } else if (currentTab == 1) {
            int xpY = layout.getY(25);
            guiGraphics.drawString(this.font,
                Component.literal("§7Количество опыта:"),
                layout.getX(3), xpY - 12, 0xCCCCCC, false);
            int levelY = xpY + layout.getHeight(4) + layout.getSpacing() * 2;
            guiGraphics.drawString(this.font,
                Component.literal("§7Уровень (1-50):"),
                layout.getX(3), levelY - 12, 0xCCCCCC, false);
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок с подсветкой активной вкладки
        for (Button button : buttons) {
            if (button != null) {
                boolean isSelected = false;
                String buttonText = button.getMessage().getString();
                String[] tabNamesForCheck = {"Карты", "Опыт/Уровень", "Статистика", "Реплеи", "События", "Сезоны", "Достижения", "Рейтинг", "Монеты"};
                for (int i = 0; i < tabNamesForCheck.length; i++) {
                    if (currentTab == i && buttonText.contains(tabNamesForCheck[i])) {
                        isSelected = true;
                        break;
                    }
                }
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, isSelected);
            }
        }
        
        // Рендерим содержимое вкладок
        if (currentTab == 0) {
            renderCardsTab(guiGraphics, mouseX, mouseY);
        } else if (currentTab == 1) {
            renderXPTab(guiGraphics);
        } else if (currentTab == 2) {
            renderStatsTab(guiGraphics);
        } else if (currentTab == 3) {
            renderReplaysTab(guiGraphics, mouseX, mouseY);
        } else if (currentTab == 4) {
            renderEventsTab(guiGraphics, mouseX, mouseY);
        } else if (currentTab == 5) {
            renderSeasonsTab(guiGraphics);
        } else if (currentTab == 6) {
            renderAchievementsTab(guiGraphics, mouseX, mouseY);
        } else if (currentTab == 7) {
            renderRatingTab(guiGraphics, mouseX, mouseY);
        } else if (currentTab == 8) {
            renderCoinsTab(guiGraphics);
        }
    }
    
    private void renderCardsTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Список позиционируется после всех элементов вкладки с учетом реальных размеров
        // Находим максимальную Y координату среди всех элементов вкладки
        int maxElementY = playerNameBox.getY() + playerNameBox.getHeight();
        if (cardSearchBox != null) {
            maxElementY = Math.max(maxElementY, cardSearchBox.getY() + cardSearchBox.getHeight());
        }
        if (cardIdBox != null) {
            maxElementY = Math.max(maxElementY, cardIdBox.getY() + cardIdBox.getHeight());
        }
        // Проверяем кнопки вкладки
        for (Button btn : buttons) {
            if (btn != null) {
                String btnText = btn.getMessage().getString();
                if (btnText.contains("Выдать карту") || btnText.contains("Разблокировать")) {
                    maxElementY = Math.max(maxElementY, btn.getY() + btn.getHeight());
                }
            }
        }
        
        int listStartY = maxElementY + layout.getSpacing() * 2;
        int listEndY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 2; // Учитываем кнопку "Назад"
        int listHeight = listEndY - listStartY;
        int listX = layout.getX(3);
        int listWidth = layout.getWidth(45);
        
        // Проверяем, что список не выходит за границы и имеет минимальную высоту
        if (listStartY >= listEndY || listHeight < 50) {
            listStartY = layout.getY(32); // Fallback на процентное позиционирование
            listEndY = layout.getY(88);
            listHeight = listEndY - listStartY;
        }
        
        // ПЕРЕПИСАНО: Фон для списка карт в скевоморфном стиле
        GuiUtils.drawLeatherElement(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25);
        // Полупрозрачный оверлей
        guiGraphics.fill(listX - 5, listStartY - 20, listX - 5 + listWidth + 10, listStartY - 20 + listHeight + 25, 0x40000000);
        GuiUtils.drawMetalFrame(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25, 2, false);
        
        // Заголовок списка
        guiGraphics.drawString(this.font,
            Component.literal("§6§lСписок карт (§f" + filteredCards.size() + "§6§l):"),
            listX, listStartY - 15, 0xFFFFFF, false);
        
        // Информация о выбранной карте (справа)
        int infoX = layout.getX(50);
        int infoY = listStartY;
        int infoWidth = layout.getWidth(47);
        
        // Фон для информации о карте
        if (selectedCard != null) {
            // ПЕРЕПИСАНО: Фон для информации о карте в скевоморфном стиле
            GuiUtils.drawLeatherElement(guiGraphics, infoX - 5, infoY - 5, infoWidth + 10, 200);
            guiGraphics.fill(infoX - 5, infoY - 5, infoX - 5 + infoWidth + 10, infoY - 5 + 200, 0x40000000);
            GuiUtils.drawMetalFrame(guiGraphics, infoX - 5, infoY - 5, infoWidth + 10, 200, 2, false);
            
            guiGraphics.drawString(this.font,
                Component.literal("§6§lВыбранная карта:"),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 18;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7Название: §f" + selectedCard.getName()),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 14;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7ID: §f" + selectedCard.getId()),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 14;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7Сила: §f" + selectedCard.getPower()),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 14;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7Редкость: §f" + selectedCard.getRarity().getDisplayName()),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 14;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7Тип: §f" + selectedCard.getType().name()),
                infoX, infoY, 0xFFFFFF, false);
            infoY += 14;
            
            guiGraphics.drawString(this.font,
                Component.literal("§7Фракция: §f" + selectedCard.getFaction()),
                infoX, infoY, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font,
                Component.literal("§7Выберите карту из списка"),
                infoX, infoY, 0xCCCCCC, false);
        }
        
        // Заголовки колонок
        int headerY = listStartY - 2;
        int columnDividerX = listX + listWidth / 2;
        guiGraphics.drawString(this.font,
            Component.literal("§7§lID карты"),
            listX + 5, headerY, 0xAAAAAA, false);
        guiGraphics.drawString(this.font,
            Component.literal("§e§lНазвание"),
            columnDividerX + 5, headerY, 0xCCAA88, false);
        
        // Горизонтальная линия под заголовками
        guiGraphics.fill(listX, listStartY - 1, listX + listWidth, listStartY, 0x66FFFFFF);
        
        // Список карт с прокруткой
        guiGraphics.enableScissor(listX, listStartY, listX + listWidth, listEndY);
        
        int itemHeight = 22;
        int maxVisibleItems = listHeight / itemHeight;
        int startIndex = cardScrollOffset;
        int endIndex = Math.min(startIndex + maxVisibleItems, filteredCards.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Card card = filteredCards.get(i);
            int y = listStartY + (i - startIndex) * itemHeight;
            
            // Разделительная линия между строками (кроме последней)
            if (i < endIndex - 1) {
                guiGraphics.fill(listX + 5, y + itemHeight - 1, listX + listWidth - 5, y + itemHeight, 0x33FFFFFF);
            }
            
            boolean isSelected = selectedCard != null && selectedCard.getId().equals(card.getId());
            if (isSelected) {
                // ПЕРЕПИСАНО: Выделение выбранного элемента
                guiGraphics.fill(listX + 2, y - 1, listX + listWidth - 2, y - 1 + itemHeight, 0x88FFD700);
            }
            
            if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= y && mouseY < y + itemHeight) {
                if (!isSelected) {
                    // ПЕРЕПИСАНО: Hover эффект
                    guiGraphics.fill(listX + 2, y - 1, listX + listWidth - 2, y - 1 + itemHeight, 0x44FFFFFF);
                }
            }
            
            // Вертикальный разделитель между колонками
            guiGraphics.fill(columnDividerX, y, columnDividerX + 1, y + itemHeight, 0x44FFFFFF);
            
            // ID карты (левая колонка)
            String displayText = card.getId();
            int idMaxWidth = listWidth / 2 - 15;
            if (this.font.width(displayText) > idMaxWidth) {
                displayText = this.font.plainSubstrByWidth(displayText, idMaxWidth) + "...";
            }
            guiGraphics.drawString(this.font,
                Component.literal("§7" + displayText),
                listX + 5, y + 6, isSelected ? 0xFFFFFF : 0xCCCCCC, false);
            
            // Название карты (правая колонка)
            String nameText = card.getName();
            int nameMaxWidth = listWidth / 2 - 15;
            if (this.font.width(nameText) > nameMaxWidth) {
                nameText = this.font.plainSubstrByWidth(nameText, nameMaxWidth) + "...";
            }
            guiGraphics.drawString(this.font,
                Component.literal("§e" + nameText),
                columnDividerX + 5, y + 6, isSelected ? 0xFFFFAA : 0xFFCC88, false);
        }
        
        guiGraphics.disableScissor();
        
        // Индикаторы прокрутки
        if (filteredCards.size() > maxVisibleItems) {
            if (cardScrollOffset > 0) {
                guiGraphics.drawString(this.font, "▲", listX + listWidth - 15, listStartY, 0xFFFFFF, false);
            }
            if (endIndex < filteredCards.size()) {
                guiGraphics.drawString(this.font, "▼", listX + listWidth - 15, listEndY - 15, 0xFFFFFF, false);
            }
        }
    }
    
    private void renderXPTab(GuiGraphics guiGraphics) {
        int hintY = layout.getY(32);
        guiGraphics.drawString(this.font,
            Component.literal("§7Введите имя игрока и количество опыта для выдачи"),
            layout.getX(3), hintY, 0xCCCCCC, false);
        guiGraphics.drawString(this.font,
            Component.literal("§7Или установите уровень игрока (1-50)"),
            layout.getX(3), hintY + 15, 0xCCCCCC, false);
    }
    
    private void renderStatsTab(GuiGraphics guiGraphics) {
        int statsY = layout.getY(32);
        if (!selectedPlayerStats.isEmpty()) {
            String[] lines = selectedPlayerStats.split("\n");
            for (String line : lines) {
                guiGraphics.drawString(this.font,
                    Component.literal(line),
                    layout.getX(3), statsY, 0xFFFFFF, false);
                statsY += 14;
            }
        } else {
            guiGraphics.drawString(this.font,
                Component.literal("§7Введите имя игрока и нажмите 'Показать статистику'"),
                layout.getX(3), statsY, 0xCCCCCC, false);
        }
    }
    
    public void updatePlayerStats(String stats) {
        this.selectedPlayerStats = stats;
    }
    
    public void updateReplaysList(net.minecraft.network.FriendlyByteBuf data) {
        replayIds.clear();
        replayPlayer1Names.clear();
        replayPlayer2Names.clear();
        
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            String replayId = data.readUtf();
            String player1Name = data.readUtf();
            String player2Name = data.readUtf();
            data.readUtf();
            data.readInt();
            data.readInt();
            data.readLong();
            
            replayIds.add(replayId);
            replayPlayer1Names.add(player1Name);
            replayPlayer2Names.add(player2Name);
        }
        
        if (currentTab == 3 && this.minecraft != null && this.minecraft.screen == this) {
            this.init();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Сначала проверяем клики по виджетам (EditBox, Button и т.д.)
        // Это важно, чтобы клики по полям ввода и кнопкам обрабатывались правильно
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true; // Клик обработан виджетом
        }
        
        // Только если клик не обработан виджетами, проверяем клики по списку карт
        if (button == 0 && currentTab == 0) {
            // Используем те же координаты, что и в renderCardsTab
            int maxElementY = playerNameBox.getY() + playerNameBox.getHeight();
            if (cardSearchBox != null) {
                maxElementY = Math.max(maxElementY, cardSearchBox.getY() + cardSearchBox.getHeight());
            }
            if (cardIdBox != null) {
                maxElementY = Math.max(maxElementY, cardIdBox.getY() + cardIdBox.getHeight());
            }
            // Проверяем кнопки вкладки
            for (Button btn : buttons) {
                if (btn != null) {
                    String btnText = btn.getMessage().getString();
                    if (btnText.contains("Выдать карту") || btnText.contains("Разблокировать")) {
                        maxElementY = Math.max(maxElementY, btn.getY() + btn.getHeight());
                    }
                }
            }
            
            int listStartY = maxElementY + layout.getSpacing() * 2;
            int listEndY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 2;
            int listHeight = listEndY - listStartY;
            int listX = layout.getX(3);
            int listWidth = layout.getWidth(45);
            
            // Проверяем, что список не выходит за границы
            if (listStartY >= listEndY || listHeight < 50) {
                listStartY = layout.getY(32);
                listEndY = layout.getY(88);
                listHeight = listEndY - listStartY;
            }
            
            // Проверяем, что клик находится внутри области списка
            if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listStartY && mouseY < listEndY) {
                int itemHeight = 22;
                int maxVisibleItems = listHeight / itemHeight;
                int startIndex = cardScrollOffset;
                int endIndex = Math.min(startIndex + maxVisibleItems, filteredCards.size());
                
                for (int i = startIndex; i < endIndex; i++) {
                    Card card = filteredCards.get(i);
                    int y = listStartY + (i - startIndex) * itemHeight;
                    
                    if (mouseY >= y && mouseY < y + itemHeight) {
                        selectedCard = card;
                        if (cardIdBox != null) {
                            cardIdBox.setValue(card.getId());
                        }
                        return true;
                    }
                }
            }
        }
        
        return false; // Клик не обработан
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Сначала проверяем прокрутку виджетов (если они поддерживают прокрутку)
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        // Только если прокрутка не обработана виджетами, проверяем прокрутку списка карт
        if (currentTab == 0) {
            // Используем те же координаты, что и в renderCardsTab
            int maxElementY = playerNameBox.getY() + playerNameBox.getHeight();
            if (cardSearchBox != null) {
                maxElementY = Math.max(maxElementY, cardSearchBox.getY() + cardSearchBox.getHeight());
            }
            if (cardIdBox != null) {
                maxElementY = Math.max(maxElementY, cardIdBox.getY() + cardIdBox.getHeight());
            }
            // Проверяем кнопки вкладки
            for (Button btn : buttons) {
                if (btn != null) {
                    String btnText = btn.getMessage().getString();
                    if (btnText.contains("Выдать карту") || btnText.contains("Разблокировать")) {
                        maxElementY = Math.max(maxElementY, btn.getY() + btn.getHeight());
                    }
                }
            }
            
            int listStartY = maxElementY + layout.getSpacing() * 2;
            int listEndY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 2;
            int listHeight = listEndY - listStartY;
            int listX = layout.getX(3);
            int listWidth = layout.getWidth(45);
            
            // Проверяем, что список не выходит за границы
            if (listStartY >= listEndY || listHeight < 50) {
                listStartY = layout.getY(32);
                listEndY = layout.getY(88);
                listHeight = listEndY - listStartY;
            }
            
            // Проверяем, что мышь находится внутри области списка
            if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listStartY && mouseY < listEndY) {
                int itemHeight = 22;
                int maxVisibleItems = listHeight / itemHeight;
                int maxScroll = Math.max(0, filteredCards.size() - maxVisibleItems);
                
                if (delta > 0 && cardScrollOffset > 0) {
                    cardScrollOffset = Math.max(0, cardScrollOffset - 1);
                    return true;
                } else if (delta < 0 && cardScrollOffset < maxScroll) {
                    cardScrollOffset = Math.min(maxScroll, cardScrollOffset + 1);
                    return true;
                }
            }
        }
        
        return false; // Прокрутка не обработана
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    // ========== НОВЫЕ ВКЛАДКИ ==========
    
    private void initReplaysTab() {
        int buttonWidth = layout.getWidth(17);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        // Проверяем границы для кнопок
        int buttonX1 = layout.getX(3);
        int buttonX2 = buttonX1 + buttonWidth + spacing;
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - spacing;
        if (buttonX2 + buttonWidth > maxButtonX) {
            buttonX2 = maxButtonX;
        }
        
        Button refreshBtn = createStyledButton(buttonX1, contentY, buttonWidth, buttonHeight,
            Component.literal("§eОбновить список"),
            (btn) -> NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket()));
        this.addRenderableWidget(refreshBtn);
        buttons.add(refreshBtn);
        
        Button deleteAllBtn = createStyledButton(buttonX2, contentY, buttonWidth, buttonHeight,
            Component.literal("§cУдалить все"),
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.AdminDeleteAllReplaysPacket());
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
            });
        this.addRenderableWidget(deleteAllBtn);
        buttons.add(deleteAllBtn);
        
        // Список позиционируется после кнопок
        int listStartY = contentY + buttonHeight + spacing * 2;
        int listEndY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 2;
        int listHeight = listEndY - listStartY;
        int itemHeight = 30;
        int maxVisibleItems = listHeight / itemHeight;
        int startIndex = replayScrollOffset;
        int endIndex = Math.min(startIndex + maxVisibleItems, replayIds.size());
        
        for (Button btn : replayDeleteButtons) {
            this.removeWidget(btn);
        }
        replayDeleteButtons.clear();
        
        int listX = layout.getX(3);
        int listWidth = layout.getWidth(94);
        for (int i = startIndex; i < endIndex; i++) {
            int y = listStartY + (i - startIndex) * itemHeight;
            String finalReplayId = replayIds.get(i);
            int deleteButtonWidth = layout.getWidth(10);
            Button deleteBtn = createStyledButton(listX + listWidth - deleteButtonWidth - spacing, y + 2, deleteButtonWidth, itemHeight - 4,
                Component.literal("§cУдалить"),
                (btn) -> {
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.AdminDeleteReplayPacket(finalReplayId));
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestReplaysPacket());
                });
            this.addRenderableWidget(deleteBtn);
            buttons.add(deleteBtn);
            replayDeleteButtons.add(deleteBtn);
        }
    }
    
    private void initEventsTab() {
        int buttonWidth = layout.getWidth(17);
        int buttonHeight = layout.getHeight(4);
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + layout.getSpacing();
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        int buttonX = layout.getX(3);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - layout.getSpacing();
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        Button refreshBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§eОбновить список"),
            (btn) -> NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestEventsPacket()));
        this.addRenderableWidget(refreshBtn);
        buttons.add(refreshBtn);
    }
    
    private void initSeasonsTab() {
        int buttonWidth = layout.getWidth(22);
        int buttonHeight = layout.getHeight(4);
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + layout.getSpacing();
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - layout.getSpacing() * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        int buttonX = layout.getX(3);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - layout.getSpacing();
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        Button refreshBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§eОбновить информацию"),
            (btn) -> NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestSeasonPacket()));
        this.addRenderableWidget(refreshBtn);
        buttons.add(refreshBtn);
    }
    
    private void initAchievementsTab() {
        int inputWidth = layout.getWidth(28);
        int inputHeight = layout.getHeight(4);
        int buttonWidth = layout.getWidth(20);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        this.achievementIdBox = new EditBox(this.font, layout.getX(3), contentY, inputWidth, inputHeight,
            Component.literal("ID достижения"));
        this.achievementIdBox.setMaxLength(64);
        this.achievementIdBox.setHint(Component.literal("Введите ID достижения"));
        this.addRenderableWidget(this.achievementIdBox);
        
        // Кнопка справа, проверяем границы
        int buttonX = layout.getX(32);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - spacing;
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        Button giveAchievementBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§dВыдать достижение"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                String achievementId = achievementIdBox.getValue();
                if (!playerName.isEmpty() && !achievementId.isEmpty()) {
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.AdminGiveAchievementPacket(playerName, achievementId));
                }
            });
        this.addRenderableWidget(giveAchievementBtn);
        buttons.add(giveAchievementBtn);
    }
    
    private void initRatingTab() {
        int buttonWidth = layout.getWidth(20);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        // Проверяем границы для кнопок
        int buttonX1 = layout.getX(3);
        int buttonX2 = buttonX1 + buttonWidth + spacing;
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - spacing;
        if (buttonX2 + buttonWidth > maxButtonX) {
            buttonX2 = maxButtonX;
        }
        
        Button refreshBtn = createStyledButton(buttonX1, contentY, buttonWidth, buttonHeight,
            Component.literal("§eОбновить рейтинг"),
            (btn) -> {});
        this.addRenderableWidget(refreshBtn);
        buttons.add(refreshBtn);
        
        Button resetRatingBtn = createStyledButton(buttonX2, contentY, buttonWidth, buttonHeight,
            Component.literal("§cСбросить рейтинг"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                if (!playerName.isEmpty()) {}
            });
        this.addRenderableWidget(resetRatingBtn);
        buttons.add(resetRatingBtn);
    }
    
    private void initCoinsTab() {
        int inputWidth = layout.getWidth(28);
        int inputHeight = layout.getHeight(4);
        int buttonWidth = layout.getWidth(17);
        int buttonHeight = layout.getHeight(4);
        int spacing = layout.getSpacing();
        
        // Позиционируем после поля имени игрока
        int contentY = playerNameBox.getY() + playerNameBox.getHeight() + spacing;
        int maxContentY = layout.getGuiY() + layout.getGuiHeight() - layout.getHeight(5) - spacing * 3;
        if (contentY > maxContentY) {
            contentY = maxContentY;
        }
        
        this.coinsAmountBox = new EditBox(this.font, layout.getX(3), contentY, inputWidth, inputHeight,
            Component.literal("Количество монет"));
        this.coinsAmountBox.setMaxLength(10);
        this.coinsAmountBox.setFilter(s -> s.matches("\\d*"));
        this.coinsAmountBox.setHint(Component.literal("Число"));
        this.addRenderableWidget(this.coinsAmountBox);
        
        // Кнопка справа, проверяем границы
        int buttonX = layout.getX(32);
        int maxButtonX = layout.getGuiX() + layout.getGuiWidth() - buttonWidth - spacing;
        if (buttonX + buttonWidth > maxButtonX) {
            buttonX = maxButtonX;
        }
        
        Button giveCoinsBtn = createStyledButton(buttonX, contentY, buttonWidth, buttonHeight,
            Component.literal("§aВыдать монеты"),
            (btn) -> {
                String playerName = playerNameBox.getValue();
                String coinsStr = coinsAmountBox.getValue();
                if (!playerName.isEmpty() && !coinsStr.isEmpty()) {
                    try {
                        int coins = Integer.parseInt(coinsStr);
                        NetworkHandler.INSTANCE.sendToServer(
                            new NetworkHandler.AdminGiveCoinsPacket(playerName, coins));
                    } catch (NumberFormatException e) {}
                }
            });
        this.addRenderableWidget(giveCoinsBtn);
        buttons.add(giveCoinsBtn);
    }
    
    private void renderReplaysTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listStartY = layout.getY(32);
        int listEndY = layout.getY(88);
        int listHeight = listEndY - listStartY;
        int listX = layout.getX(3);
        int listWidth = layout.getWidth(94);
        
        // ПЕРЕПИСАНО: Фон для списка в скевоморфном стиле
        GuiUtils.drawLeatherElement(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25);
        guiGraphics.fill(listX - 5, listStartY - 20, listX - 5 + listWidth + 10, listStartY - 20 + listHeight + 25, 0x40000000);
        GuiUtils.drawMetalFrame(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25, 2, false);
        
        guiGraphics.drawString(this.font,
            Component.literal("§6§lСписок реплеев (§f" + replayIds.size() + "§6§l):"),
            listX, listStartY - 15, 0xFFFFFF, false);
        
        if (replayIds.isEmpty()) {
            guiGraphics.drawString(this.font,
                Component.literal("§7Нет сохранённых реплеев"),
                listX, listStartY + 20, 0xCCCCCC, false);
        } else {
            // Заголовок колонки
            int headerY = listStartY - 2;
            guiGraphics.drawString(this.font,
                Component.literal("§7§lИгроки"),
                listX + 5, headerY, 0xAAAAAA, false);
            guiGraphics.fill(listX, listStartY - 1, listX + listWidth, listStartY, 0x66FFFFFF);
            
            int itemHeight = 30;
            int maxVisibleItems = listHeight / itemHeight;
            int startIndex = replayScrollOffset;
            int endIndex = Math.min(startIndex + maxVisibleItems, replayIds.size());
            
            guiGraphics.enableScissor(listX, listStartY, listX + listWidth, listEndY);
            
            for (int i = startIndex; i < endIndex; i++) {
                int y = listStartY + (i - startIndex) * itemHeight;
                String player1 = i < replayPlayer1Names.size() ? replayPlayer1Names.get(i) : "Unknown";
                String player2 = i < replayPlayer2Names.size() ? replayPlayer2Names.get(i) : "Unknown";
                
                // Разделительная линия между строками
                if (i < endIndex - 1) {
                    guiGraphics.fill(listX + 5, y + itemHeight - 1, listX + listWidth - 5, y + itemHeight, 0x33FFFFFF);
                }
                
                if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + itemHeight) {
                    // ПЕРЕПИСАНО: Hover эффект
                    guiGraphics.fill(listX + 2, y, listX + listWidth - 2, y + itemHeight, 0x44FFFFFF);
                }
                
                guiGraphics.drawString(this.font,
                    Component.literal("§7" + player1 + " §fvs §7" + player2),
                    listX + 5, y + 8, 0xFFFFFF, false);
            }
            
            guiGraphics.disableScissor();
        }
    }
    
    private void renderEventsTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listStartY = layout.getY(32);
        int listEndY = layout.getY(88);
        int listHeight = listEndY - listStartY;
        int listX = layout.getX(3);
        int listWidth = layout.getWidth(94);
        
        // ПЕРЕПИСАНО: Фон для списка в скевоморфном стиле
        GuiUtils.drawLeatherElement(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25);
        guiGraphics.fill(listX - 5, listStartY - 20, listX - 5 + listWidth + 10, listStartY - 20 + listHeight + 25, 0x40000000);
        GuiUtils.drawMetalFrame(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25, 2, false);
        
        guiGraphics.drawString(this.font,
            Component.literal("§6§lАктивные события (§f" + eventIds.size() + "§6§l):"),
            listX, listStartY - 15, 0xFFFFFF, false);
        
        if (eventIds.isEmpty()) {
            guiGraphics.drawString(this.font,
                Component.literal("§7Нет активных событий"),
                listX, listStartY + 20, 0xCCCCCC, false);
        } else {
            // Заголовок колонки
            int headerY = listStartY - 2;
            guiGraphics.drawString(this.font,
                Component.literal("§7§lСобытие"),
                listX + 5, headerY, 0xAAAAAA, false);
            guiGraphics.fill(listX, listStartY - 1, listX + listWidth, listStartY, 0x66FFFFFF);
            
            int itemHeight = 30;
            int maxVisibleItems = listHeight / itemHeight;
            int startIndex = eventScrollOffset;
            int endIndex = Math.min(startIndex + maxVisibleItems, eventIds.size());
            
            guiGraphics.enableScissor(listX, listStartY, listX + listWidth, listEndY);
            
            for (int i = startIndex; i < endIndex; i++) {
                int y = listStartY + (i - startIndex) * itemHeight;
                String eventName = i < eventNames.size() ? eventNames.get(i) : "Unknown";
                long endTime = i < eventEndTimes.size() ? eventEndTimes.get(i) : 0;
                long remaining = Math.max(0, endTime - System.currentTimeMillis());
                int hours = (int)(remaining / 3600000);
                int minutes = (int)((remaining % 3600000) / 60000);
                
                // Разделительная линия между строками
                if (i < endIndex - 1) {
                    guiGraphics.fill(listX + 5, y + itemHeight - 1, listX + listWidth - 5, y + itemHeight, 0x33FFFFFF);
                }
                
                if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + itemHeight) {
                    // ПЕРЕПИСАНО: Hover эффект
                    guiGraphics.fill(listX + 2, y, listX + listWidth - 2, y + itemHeight, 0x44FFFFFF);
                }
                
                guiGraphics.drawString(this.font,
                    Component.literal("§6" + eventName + " §7(осталось: §f" + hours + "ч " + minutes + "м§7)"),
                    listX + 5, y + 8, 0xFFFFFF, false);
            }
            
            guiGraphics.disableScissor();
        }
    }
    
    private void renderSeasonsTab(GuiGraphics guiGraphics) {
        int infoY = layout.getY(32);
        guiGraphics.drawString(this.font,
            Component.literal("§6§lИнформация о текущем сезоне"),
            layout.getX(3), infoY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
            Component.literal("§7Используйте кнопку 'Обновить информацию' для получения данных"),
            layout.getX(3), infoY + 20, 0xCCCCCC, false);
    }
    
    private void renderAchievementsTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listStartY = layout.getY(32);
        guiGraphics.drawString(this.font,
            Component.literal("§6§lДоступные достижения"),
            layout.getX(3), listStartY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
            Component.literal("§7Введите ID достижения и имя игрока для выдачи"),
            layout.getX(3), listStartY + 20, 0xCCCCCC, false);
    }
    
    private void renderRatingTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listStartY = layout.getY(32);
        int listEndY = layout.getY(88);
        int listHeight = listEndY - listStartY;
        int listX = layout.getX(3);
        int listWidth = layout.getWidth(94);
        
        // ПЕРЕПИСАНО: Фон для списка в скевоморфном стиле
        GuiUtils.drawLeatherElement(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25);
        guiGraphics.fill(listX - 5, listStartY - 20, listX - 5 + listWidth + 10, listStartY - 20 + listHeight + 25, 0x40000000);
        GuiUtils.drawMetalFrame(guiGraphics, listX - 5, listStartY - 20, listWidth + 10, listHeight + 25, 2, false);
        
        guiGraphics.drawString(this.font,
            Component.literal("§6§lРейтинг игроков (§f" + ratingPlayerNames.size() + "§6§l):"),
            listX, listStartY - 15, 0xFFFFFF, false);
        
        if (ratingPlayerNames.isEmpty()) {
            guiGraphics.drawString(this.font,
                Component.literal("§7Нет данных о рейтинге"),
                listX, listStartY + 20, 0xCCCCCC, false);
        } else {
            // Заголовки колонок
            int headerY = listStartY - 2;
            int columnDividerX = listX + listWidth / 2;
            guiGraphics.drawString(this.font,
                Component.literal("§7§lМесто | Игрок"),
                listX + 5, headerY, 0xAAAAAA, false);
            guiGraphics.drawString(this.font,
                Component.literal("§e§lРейтинг"),
                columnDividerX + 5, headerY, 0xCCAA88, false);
            guiGraphics.fill(listX, listStartY - 1, listX + listWidth, listStartY, 0x66FFFFFF);
            
            int itemHeight = 22;
            int maxVisibleItems = listHeight / itemHeight;
            int startIndex = ratingScrollOffset;
            int endIndex = Math.min(startIndex + maxVisibleItems, ratingPlayerNames.size());
            
            guiGraphics.enableScissor(listX, listStartY, listX + listWidth, listEndY);
            
            for (int i = startIndex; i < endIndex; i++) {
                int y = listStartY + (i - startIndex) * itemHeight;
                String playerName = ratingPlayerNames.get(i);
                int score = i < ratingScores.size() ? ratingScores.get(i) : 0;
                
                // Разделительная линия между строками
                if (i < endIndex - 1) {
                    guiGraphics.fill(listX + 5, y + itemHeight - 1, listX + listWidth - 5, y + itemHeight, 0x33FFFFFF);
                }
                
                // Вертикальный разделитель между колонками
                guiGraphics.fill(columnDividerX, y, columnDividerX + 1, y + itemHeight, 0x44FFFFFF);
                
                if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + itemHeight) {
                    // ПЕРЕПИСАНО: Hover эффект
                    guiGraphics.fill(listX + 2, y, listX + listWidth - 2, y + itemHeight, 0x44FFFFFF);
                }
                
                guiGraphics.drawString(this.font,
                    Component.literal("§7" + (i + 1) + ". §f" + playerName),
                    listX + 5, y + 5, 0xFFFFFF, false);
                guiGraphics.drawString(this.font,
                    Component.literal("§e" + score),
                    columnDividerX + 5, y + 5, 0xFFFFAA, false);
            }
            
            guiGraphics.disableScissor();
        }
    }
    
    private void renderCoinsTab(GuiGraphics guiGraphics) {
        int hintY = layout.getY(32);
        guiGraphics.drawString(this.font,
            Component.literal("§7Введите имя игрока и количество монет для выдачи"),
            layout.getX(3), hintY, 0xCCCCCC, false);
    }
    
    public static boolean hasAdminPermissions(Player player) {
        if (player.level().isClientSide) {
            if (net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer()) {
                var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    return server.getPlayerList().isOp(player.getGameProfile());
                }
            }
            return false;
        } else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return serverPlayer.hasPermissions(2);
        }
        return false;
    }
}
