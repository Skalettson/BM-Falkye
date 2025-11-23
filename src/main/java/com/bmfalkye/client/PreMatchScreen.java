package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.game.MatchConfig;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Предматчевое меню для настройки игры
 * Полностью переработанный интерфейс с улучшенным дизайном и адаптивным layout
 */
public class PreMatchScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 400;
    private static final int BASE_GUI_HEIGHT = 500;
    private static final int MIN_GUI_WIDTH = 350;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.85;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Список кнопок для кастомного рендеринга
    private final List<Button> buttons = new ArrayList<>();
    
    private final UUID opponentUUID;
    private final String opponentName;
    private final boolean isNPC;
    private final int villagerCoins;
    private final boolean isChallenged;
    
    private MatchConfig config;
    private EditBox betAmountField;
    private EditBox maxRoundsField;
    private EditBox turnTimeLimitField;
    private Button difficultyButton;
    private Button allowLeaderButton;
    private Button allowWeatherButton;
    private Button startButton;
    private Button acceptButton;
    private Button denyButton;
    private Button cancelButton;
    private Button rulesButton;
    private Button advancedButton;
    private boolean showAdvancedSettings = false;
    
    // Выбор колоды
    private Button deckSelectButton;
    private java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> savedDecks = new java.util.ArrayList<>();
    private int selectedDeckIndex = -1; // -1 = использовать сохранённую колоду
    
    // Дружеская игра
    private Button friendlyMatchButton;
    
    public PreMatchScreen(UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins) {
        this(opponentUUID, opponentName, isNPC, villagerCoins, false);
    }
    
    public PreMatchScreen(UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins, boolean isChallenged) {
        super(Component.translatable("screen.bm_falkye.prematch_title"));
        this.opponentUUID = opponentUUID;
        this.opponentName = opponentName;
        this.isNPC = isNPC;
        this.villagerCoins = villagerCoins;
        this.isChallenged = isChallenged;
        this.config = new MatchConfig();
        
        // Запрашиваем список колод с сервера
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDecksPacket());
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
        
        // Базовые размеры элементов
        int elementHeight = layout.getHeight(4);
        int spacing = layout.getSpacing() * 2;
        int buttonWidth = layout.getWidth(45);
        int fullWidth = layout.getWidth(90);
        
        int currentY = layout.getY(12); // Начальная позиция после заголовка
        
        // Кнопка выбора сложности (только для NPC)
        if (isNPC) {
            currentY += spacing;
            this.difficultyButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, fullWidth, elementHeight,
                Component.translatable("screen.bm_falkye.difficulty", config.getDifficulty().getDisplayName()),
                (button) -> {
                    MatchConfig.Difficulty[] difficulties = MatchConfig.Difficulty.values();
                    int currentIndex = 0;
                    for (int i = 0; i < difficulties.length; i++) {
                        if (difficulties[i] == config.getDifficulty()) {
                            currentIndex = i;
                            break;
                        }
                    }
                    int nextIndex = (currentIndex + 1) % difficulties.length;
                    config.setDifficulty(difficulties[nextIndex]);
                    button.setMessage(Component.translatable("screen.bm_falkye.difficulty", config.getDifficulty().getDisplayName()));
                }
            );
            this.addRenderableWidget(this.difficultyButton);
            buttons.add(this.difficultyButton);
            currentY += elementHeight + spacing;
        }
        
        // Кнопка выбора колоды
        currentY += spacing;
        String deckButtonText = getDeckButtonText();
        this.deckSelectButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            Component.literal(deckButtonText),
            (button) -> {
                if (savedDecks.isEmpty()) {
                    // Если колод нет, открываем редактор колод
                    this.minecraft.setScreen(new DeckEditorScreen(this));
                } else {
                    // Переключаем между колодами
                    selectedDeckIndex = (selectedDeckIndex + 1) % (savedDecks.size() + 1);
                    if (selectedDeckIndex == savedDecks.size()) {
                        selectedDeckIndex = -1; // Использовать сохранённую колоду
                        config.setSelectedDeckName(null);
                    } else {
                        config.setSelectedDeckName(savedDecks.get(selectedDeckIndex).getDeckName());
                    }
                    button.setMessage(Component.literal(getDeckButtonText()));
                }
            }
        );
        this.addRenderableWidget(this.deckSelectButton);
        buttons.add(this.deckSelectButton);
        currentY += elementHeight + spacing;
        
        // Кнопка выбора режима игры (2D/3D)
        currentY += spacing;
        String gameModeText = config.getGameMode() == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D ? 
            "§aРежим: 3D" : "§7Режим: 2D";
        Button gameModeButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            Component.literal(gameModeText),
            (button) -> {
                com.bmfalkye.settings.GameModeSettings.GameMode currentMode = config.getGameMode();
                com.bmfalkye.settings.GameModeSettings.GameMode newMode = 
                    currentMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D ?
                        com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D :
                        com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
                config.setGameMode(newMode);
                button.setMessage(Component.literal(newMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D ? 
                    "§aРежим: 3D" : "§7Режим: 2D"));
            }
        );
        this.addRenderableWidget(gameModeButton);
        buttons.add(gameModeButton);
        currentY += elementHeight + spacing;
        
        // Кнопка дружеской игры (только для игроков, не для NPC)
        if (!isNPC) {
            this.friendlyMatchButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, fullWidth, elementHeight,
                Component.literal(config.isFriendlyMatch() ? "§aДружеская игра: Включено" : "§7Дружеская игра: Выключено"),
                (button) -> {
                    config.setFriendlyMatch(!config.isFriendlyMatch());
                    if (config.isFriendlyMatch()) {
                        // В дружеской игре ставка = 0
                        betAmountField.setValue("0");
                        config.setBetAmount(0);
                    }
                    button.setMessage(Component.literal(config.isFriendlyMatch() ? "§aДружеская игра: Включено" : "§7Дружеская игра: Выключено"));
                }
            );
            this.addRenderableWidget(this.friendlyMatchButton);
            buttons.add(this.friendlyMatchButton);
            currentY += elementHeight + spacing;
        }
        
        // Поле для ставки
        currentY += spacing;
        this.betAmountField = new EditBox(this.font, layout.getX(5), currentY, fullWidth, elementHeight,
            Component.translatable("screen.bm_falkye.bet_amount"));
        this.betAmountField.setValue("0");
        this.betAmountField.setFilter((text) -> text.matches("\\d*"));
        this.betAmountField.setMaxLength(10);
        this.betAmountField.setHint(Component.translatable("screen.bm_falkye.bet_amount_hint"));
        this.betAmountField.setEditable(!config.isFriendlyMatch()); // В дружеской игре ставка недоступна
        this.addRenderableWidget(this.betAmountField);
        currentY += elementHeight + spacing;
        
        // Кнопка "Расширенные настройки" / "Скрыть настройки"
        String advancedButtonText = showAdvancedSettings ? "§7Скрыть настройки" : "§eРасширенные настройки";
        this.advancedButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            Component.literal(advancedButtonText),
            (button) -> {
                showAdvancedSettings = !showAdvancedSettings;
                this.clearWidgets();
                buttons.clear();
                this.init();
            }
        );
        this.addRenderableWidget(this.advancedButton);
        buttons.add(this.advancedButton);
        currentY += elementHeight + spacing;
        
        // Расширенные настройки (только если включены)
        if (showAdvancedSettings) {
            currentY += spacing;
            
            // Кнопка "Разрешить лидеров"
            this.allowLeaderButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, buttonWidth, elementHeight,
                Component.literal(config.isAllowLeader() ? "§aЛидеры: Включено" : "§cЛидеры: Выключено"),
                (button) -> {
                    config.setAllowLeader(!config.isAllowLeader());
                    button.setMessage(Component.literal(config.isAllowLeader() ? "§aЛидеры: Включено" : "§cЛидеры: Выключено"));
                }
            );
            this.addRenderableWidget(this.allowLeaderButton);
            buttons.add(this.allowLeaderButton);
            
            // Кнопка "Разрешить погоду"
            this.allowWeatherButton = GuiUtils.createStyledButton(
                layout.getX(50), currentY, buttonWidth, elementHeight,
                Component.literal(config.isAllowWeather() ? "§aПогода: Включено" : "§cПогода: Выключено"),
                (button) -> {
                    config.setAllowWeather(!config.isAllowWeather());
                    button.setMessage(Component.literal(config.isAllowWeather() ? "§aПогода: Включено" : "§cПогода: Выключено"));
                }
            );
            this.addRenderableWidget(this.allowWeatherButton);
            buttons.add(this.allowWeatherButton);
            currentY += elementHeight + spacing * 2; // Больше отступ для подписей
            
            // Поле для максимального количества раундов
            this.maxRoundsField = new EditBox(this.font, layout.getX(5), currentY, buttonWidth, elementHeight,
                Component.literal("Раунды"));
            this.maxRoundsField.setValue(String.valueOf(config.getMaxRounds()));
            this.maxRoundsField.setFilter((text) -> {
                if (text.isEmpty()) return true;
                try {
                    int val = Integer.parseInt(text);
                    return val >= 1 && val <= 5;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
            this.maxRoundsField.setMaxLength(1);
            this.maxRoundsField.setHint(Component.literal("1-5"));
            this.addRenderableWidget(this.maxRoundsField);
            
            // Поле для лимита времени на ход
            this.turnTimeLimitField = new EditBox(this.font, layout.getX(50), currentY, buttonWidth, elementHeight,
                Component.literal("Время"));
            this.turnTimeLimitField.setValue(String.valueOf(config.getTurnTimeLimit()));
            this.turnTimeLimitField.setFilter((text) -> text.matches("\\d*"));
            this.turnTimeLimitField.setMaxLength(3);
            this.turnTimeLimitField.setHint(Component.literal("30-300 сек"));
            this.addRenderableWidget(this.turnTimeLimitField);
            currentY += elementHeight + spacing;
        } else {
            this.allowLeaderButton = null;
            this.allowWeatherButton = null;
            this.maxRoundsField = null;
            this.turnTimeLimitField = null;
        }
        
        // Кнопки внизу - адаптивное позиционирование
        int bottomY = layout.getY(90);
        int bottomButtonWidth = layout.getWidth(28);
        
        // Если игрок тот, на кого вызвали (target), показываем кнопки "Принять/Отказать"
        if (isChallenged && !isNPC) {
            // Кнопка "Принять"
            this.acceptButton = GuiUtils.createStyledButton(
                layout.getX(5), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.accept"),
                (button) -> {
                    parseAndStartMatch();
                }
            );
            this.addRenderableWidget(this.acceptButton);
            buttons.add(this.acceptButton);
            
            // Кнопка "Отказать"
            this.denyButton = GuiUtils.createStyledButton(
                layout.getX(36), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.deny"),
                (button) -> {
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.DenyChallengePacket());
                    this.minecraft.setScreen(null);
                }
            );
            this.addRenderableWidget(this.denyButton);
            buttons.add(this.denyButton);
            
            // Кнопка "Правила"
            this.rulesButton = GuiUtils.createStyledButton(
                layout.getX(67), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.rules"),
                (button) -> openRulesScreen()
            );
            this.addRenderableWidget(this.rulesButton);
            buttons.add(this.rulesButton);
        } else if (!isNPC) {
            // Если это challenger (бросивший вызов), показываем только "Отмена" и "Правила"
            this.cancelButton = GuiUtils.createStyledButton(
                layout.getX(5), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.cancel"),
                (button) -> {
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.CancelChallengePacket());
                    this.minecraft.setScreen(null);
                }
            );
            this.addRenderableWidget(this.cancelButton);
            buttons.add(this.cancelButton);
            
            // Кнопка "Правила"
            this.rulesButton = GuiUtils.createStyledButton(
                layout.getX(36), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.rules"),
                (button) -> openRulesScreen()
            );
            this.addRenderableWidget(this.rulesButton);
            buttons.add(this.rulesButton);
        } else {
            // Для NPC показываем "Начать игру", "Отмена" и "Правила"
            this.startButton = GuiUtils.createStyledButton(
                layout.getX(5), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.start_match"),
                (button) -> parseAndStartMatch()
            );
            this.addRenderableWidget(this.startButton);
            buttons.add(this.startButton);
            
            this.cancelButton = GuiUtils.createStyledButton(
                layout.getX(36), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.cancel"),
                (button) -> this.minecraft.setScreen(null)
            );
            this.addRenderableWidget(this.cancelButton);
            buttons.add(this.cancelButton);
            
            // Кнопка "Правила"
            this.rulesButton = GuiUtils.createStyledButton(
                layout.getX(67), bottomY, bottomButtonWidth, elementHeight,
                Component.translatable("button.bm_falkye.rules"),
                (button) -> openRulesScreen()
            );
            this.addRenderableWidget(this.rulesButton);
            buttons.add(this.rulesButton);
        }
    }
    
    /**
     * Парсит все поля и запускает матч
     */
    private void parseAndStartMatch() {
        try {
            int bet = Integer.parseInt(betAmountField.getValue());
            config.setBetAmount(bet);
        } catch (NumberFormatException e) {
            config.setBetAmount(0);
        }
        
        if (maxRoundsField != null) {
            try {
                int rounds = Integer.parseInt(maxRoundsField.getValue());
                config.setMaxRounds(rounds);
            } catch (NumberFormatException e) {
                config.setMaxRounds(3);
            }
        }
        
        if (turnTimeLimitField != null) {
            try {
                int timeLimit = Integer.parseInt(turnTimeLimitField.getValue());
                config.setTurnTimeLimit(timeLimit);
            } catch (NumberFormatException e) {
                config.setTurnTimeLimit(90);
            }
        }
        
        // В дружеской игре ставка всегда 0
        if (config.isFriendlyMatch()) {
            config.setBetAmount(0);
        }
        
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.StartMatchPacket(opponentUUID, config));
    }
    
    /**
     * Открывает экран с правилами игры
     */
    private void openRulesScreen() {
        boolean bookOpened = false;
        if (com.bmfalkye.integration.LibraryIntegration.isPatchouliLoaded()) {
            bookOpened = com.bmfalkye.integration.PatchouliIntegration.openBook(
                new net.minecraft.resources.ResourceLocation("bm_falkye", "tutorial"));
        }
        // Если Patchouli не загружен или открытие книги не удалось, открываем обычный экран
        if (!bookOpened) {
            // Открываем экран с правилами, передавая текущий экран как родительский
            this.minecraft.setScreen(new FalkyeTutorialScreen(this));
        }
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
        
        // ПЕРЕПИСАНО: Красивый фон окна в скевоморфном стиле (деревянная панель)
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        // Золотая металлическая рамка
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        int currentY = layout.getY(4);
        
        // Красивый заголовок
        Component titleComponent = Component.literal("§6§l⚔ §e§lНастройка матча §6§l⚔");
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        // Тень заголовка (черная) - создаем версию без цветовых кодов для тени
        String titleText = titleComponent.getString();
        String shadowText = titleText.replaceAll("§[0-9a-fk-or]", "");
        Component shadowComponent = Component.literal(shadowText);
        guiGraphics.drawString(this.font, shadowComponent, titleX + 2, currentY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, currentY, 0xFFFFFF, false);
        currentY += layout.getSpacing() * 3;
        
        // Имя противника
        Component opponentComponent = Component.translatable("screen.bm_falkye.opponent", opponentName);
        guiGraphics.drawString(this.font, opponentComponent,
            layout.getX(5), currentY, 0xFFFFFF, false);
        currentY += layout.getSpacing() * 2;
        
        // Монеты жителя (если это NPC)
        if (isNPC && villagerCoins >= 0) {
            Component coinsComponent = Component.translatable("screen.bm_falkye.villager_coins", villagerCoins);
            guiGraphics.drawString(this.font, coinsComponent,
                layout.getX(5), currentY, 0xFFFF00, false);
            currentY += layout.getSpacing() * 3;
        } else {
            currentY += layout.getSpacing();
        }
        
        // Разделитель
        guiGraphics.fill(layout.getX(5), currentY, layout.getX(95), currentY + 1, 0xFF8B7355);
        currentY += layout.getSpacing() * 2;
        
        // Подпись для сложности (только для NPC) - позиционируется ПЕРЕД кнопкой
        if (isNPC) {
            Component difficultyLabel = Component.translatable("screen.bm_falkye.difficulty_label");
            guiGraphics.drawString(this.font, difficultyLabel,
                layout.getX(5), currentY, 0xCCCCCC, false);
            currentY += layout.getSpacing() * 3; // Больше отступ, чтобы текст не перекрывался с кнопкой
        }
        
        // Подпись для выбора колоды - позиционируется ПЕРЕД кнопкой
        Component deckLabel = Component.literal("§eВыбор колоды:");
        guiGraphics.drawString(this.font, deckLabel,
            layout.getX(5), currentY, 0xCCCCCC, false);
        currentY += layout.getSpacing() * 3;
        
        // Подпись для дружеской игры (если не NPC)
        if (!isNPC) {
            currentY += layout.getSpacing() * 2;
        }
        
        // Подпись для ставки - позиционируется ПЕРЕД полем
        Component betLabel = Component.translatable("screen.bm_falkye.bet_amount_label");
        guiGraphics.drawString(this.font, betLabel,
            layout.getX(5), currentY, 0xCCCCCC, false);
        currentY += layout.getSpacing() * 3;
        
        // Подписи для расширенных настроек (если включены) - позиционируются ПЕРЕД полями
        if (showAdvancedSettings && maxRoundsField != null && turnTimeLimitField != null) {
            int advancedY = maxRoundsField.getY() - layout.getSpacing() * 2;
            guiGraphics.drawString(this.font, Component.literal("§7Макс. раундов (1-5):"),
                layout.getX(5), advancedY, 0xCCCCCC, false);
            guiGraphics.drawString(this.font, Component.literal("§7Лимит времени (30-300 сек):"),
                layout.getX(50), advancedY, 0xCCCCCC, false);
        }
        
        // Информация о ставке и награде - адаптивное позиционирование
        int infoY = layout.getY(78);
        if (config.isFriendlyMatch()) {
            Component friendlyInfo = Component.literal("§aДружеская игра - без ставки и наград");
            guiGraphics.drawString(this.font, friendlyInfo,
                layout.getX(5), infoY, 0x00FF00, false);
            infoY += layout.getSpacing() * 2;
        } else if (config.getBetAmount() > 0) {
            Component betInfo = Component.translatable("screen.bm_falkye.bet_info", config.getBetAmount());
            guiGraphics.drawString(this.font, betInfo,
                layout.getX(5), infoY, 0xFFFF00, false);
            infoY += layout.getSpacing() * 2;
        }
        
        // Информация о награде за сложность (только для NPC)
        if (isNPC) {
            float multiplier = config.getDifficulty().getAIMultiplier();
            int baseXP = 50;
            int xpReward = (int)(baseXP * multiplier);
            Component xpInfo = Component.translatable("screen.bm_falkye.xp_reward", xpReward);
            guiGraphics.drawString(this.font, xpInfo,
                layout.getX(5), infoY, 0x00FF00, false);
            
            infoY += layout.getSpacing() * 2;
            String difficultyInfo = getDifficultyInfo(config.getDifficulty());
            guiGraphics.drawString(this.font, Component.literal(difficultyInfo),
                layout.getX(5), infoY, 0xAAAAAA, false);
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они будут отрисованы кастомно)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок со скруглёнными углами
        for (Button button : buttons) {
            if (button != null) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    /**
     * Получает описание сложности
     */
    private String getDifficultyInfo(MatchConfig.Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "§7Легкий уровень - подходит для новичков";
            case NORMAL -> "§7Нормальный уровень - стандартная сложность";
            case HARD -> "§7Сложный уровень - требует опыта";
            case EXPERT -> "§7Экспертный уровень - для мастеров";
        };
    }
    
    /**
     * Получает текст для кнопки выбора колоды
     */
    private String getDeckButtonText() {
        if (savedDecks.isEmpty()) {
            return "§7Колода: §eСоздать колоду";
        }
        if (selectedDeckIndex == -1) {
            return "§7Колода: §eСохранённая колода";
        }
        if (selectedDeckIndex >= 0 && selectedDeckIndex < savedDecks.size()) {
            String deckName = savedDecks.get(selectedDeckIndex).getDeckName();
            if (deckName.length() > 20) {
                deckName = deckName.substring(0, 17) + "...";
            }
            return "§7Колода: §e" + deckName;
        }
        return "§7Колода: §eСохранённая колода";
    }
    
    /**
     * Обновляет список колод (вызывается из ClientPacketHandler)
     */
    public void updateDecksList(java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks) {
        this.savedDecks = new java.util.ArrayList<>(decks);
        // Если выбранная колода больше не существует, сбрасываем выбор
        if (selectedDeckIndex >= savedDecks.size()) {
            selectedDeckIndex = -1;
            config.setSelectedDeckName(null);
        }
        // Обновляем текст кнопки, если она существует
        if (deckSelectButton != null) {
            deckSelectButton.setMessage(Component.literal(getDeckButtonText()));
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
