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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Предматчевое меню для настройки игры
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class PreMatchScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 420;
    private static final int BASE_GUI_HEIGHT = 520;
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
    private Button gameModeButton;
    
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
        
        // Адаптивные размеры элементов
        int elementHeight = Math.max(18, layout.getHeight(4));
        int spacing = layout.getSpacing() * 2;
        int fullWidth = layout.getWidth(90);
        int halfWidth = layout.getWidth(43);
        
        int currentY = layout.getY(10);
        
        // Кнопка выбора сложности (только для NPC)
        if (isNPC) {
            currentY += spacing;
            MutableComponent difficultyText = Component.translatable("screen.bm_falkye.difficulty", 
                Component.literal(config.getDifficulty().getDisplayName())
                    .withStyle(ChatFormatting.YELLOW));
            this.difficultyButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, fullWidth, elementHeight,
                difficultyText,
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
                    MutableComponent newText = Component.translatable("screen.bm_falkye.difficulty", 
                        Component.literal(config.getDifficulty().getDisplayName())
                            .withStyle(ChatFormatting.YELLOW));
                    button.setMessage(newText);
                }
            );
            this.addRenderableWidget(this.difficultyButton);
            buttons.add(this.difficultyButton);
            currentY += elementHeight + spacing;
        }
        
        // Кнопка выбора колоды
        currentY += spacing;
        Component deckButtonText = getDeckButtonTextComponent();
        this.deckSelectButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            deckButtonText,
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
                    button.setMessage(getDeckButtonTextComponent());
                }
            }
        );
        this.addRenderableWidget(this.deckSelectButton);
        buttons.add(this.deckSelectButton);
        currentY += elementHeight + spacing;
        
        // Кнопка выбора режима игры (2D/3D)
        currentY += spacing;
        boolean is3DMode = config.getGameMode() == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D;
        MutableComponent gameModeText = Component.literal("Режим: ")
            .append(Component.literal(is3DMode ? "3D" : "2D")
                .withStyle(is3DMode ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        this.gameModeButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            gameModeText,
            (button) -> {
                com.bmfalkye.settings.GameModeSettings.GameMode currentMode = config.getGameMode();
                com.bmfalkye.settings.GameModeSettings.GameMode newMode = 
                    currentMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D ?
                        com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D :
                        com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
                config.setGameMode(newMode);
                boolean newIs3D = newMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D;
                MutableComponent newGameModeText = Component.literal("Режим: ")
                    .append(Component.literal(newIs3D ? "3D" : "2D")
                        .withStyle(newIs3D ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                button.setMessage(newGameModeText);
            }
        );
        this.addRenderableWidget(this.gameModeButton);
        buttons.add(this.gameModeButton);
        currentY += elementHeight + spacing;
        
        // Кнопка дружеской игры (только для игроков, не для NPC)
        if (!isNPC) {
            currentY += spacing;
            boolean isFriendly = config.isFriendlyMatch();
            MutableComponent friendlyText = Component.literal("Дружеская игра: ")
                .append(Component.literal(isFriendly ? "Включено" : "Выключено")
                    .withStyle(isFriendly ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            this.friendlyMatchButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, fullWidth, elementHeight,
                friendlyText,
                (button) -> {
                    config.setFriendlyMatch(!config.isFriendlyMatch());
                    if (config.isFriendlyMatch()) {
                        // В дружеской игре ставка = 0
                        if (betAmountField != null) {
                            betAmountField.setValue("0");
                        }
                        config.setBetAmount(0);
                    }
                    boolean newIsFriendly = config.isFriendlyMatch();
                    MutableComponent newFriendlyText = Component.literal("Дружеская игра: ")
                        .append(Component.literal(newIsFriendly ? "Включено" : "Выключено")
                            .withStyle(newIsFriendly ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                    button.setMessage(newFriendlyText);
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
        MutableComponent advancedButtonText = showAdvancedSettings ? 
            Component.literal("Скрыть настройки").withStyle(ChatFormatting.GRAY) :
            Component.literal("Расширенные настройки").withStyle(ChatFormatting.YELLOW);
        this.advancedButton = GuiUtils.createStyledButton(
            layout.getX(5), currentY, fullWidth, elementHeight,
            advancedButtonText,
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
            boolean allowLeader = config.isAllowLeader();
            MutableComponent leaderText = Component.literal("Лидеры: ")
                .append(Component.literal(allowLeader ? "Включено" : "Выключено")
                    .withStyle(allowLeader ? ChatFormatting.GREEN : ChatFormatting.RED));
            this.allowLeaderButton = GuiUtils.createStyledButton(
                layout.getX(5), currentY, halfWidth, elementHeight,
                leaderText,
                (button) -> {
                    config.setAllowLeader(!config.isAllowLeader());
                    boolean newAllowLeader = config.isAllowLeader();
                    MutableComponent newLeaderText = Component.literal("Лидеры: ")
                        .append(Component.literal(newAllowLeader ? "Включено" : "Выключено")
                            .withStyle(newAllowLeader ? ChatFormatting.GREEN : ChatFormatting.RED));
                    button.setMessage(newLeaderText);
                }
            );
            this.addRenderableWidget(this.allowLeaderButton);
            buttons.add(this.allowLeaderButton);
            
            // Кнопка "Разрешить погоду"
            boolean allowWeather = config.isAllowWeather();
            MutableComponent weatherText = Component.literal("Погода: ")
                .append(Component.literal(allowWeather ? "Включено" : "Выключено")
                    .withStyle(allowWeather ? ChatFormatting.GREEN : ChatFormatting.RED));
            this.allowWeatherButton = GuiUtils.createStyledButton(
                layout.getX(52), currentY, halfWidth, elementHeight,
                weatherText,
                (button) -> {
                    config.setAllowWeather(!config.isAllowWeather());
                    boolean newAllowWeather = config.isAllowWeather();
                    MutableComponent newWeatherText = Component.literal("Погода: ")
                        .append(Component.literal(newAllowWeather ? "Включено" : "Выключено")
                            .withStyle(newAllowWeather ? ChatFormatting.GREEN : ChatFormatting.RED));
                    button.setMessage(newWeatherText);
                }
            );
            this.addRenderableWidget(this.allowWeatherButton);
            buttons.add(this.allowWeatherButton);
            currentY += elementHeight + spacing * 2;
            
            // Поле для максимального количества раундов
            this.maxRoundsField = new EditBox(this.font, layout.getX(5), currentY, halfWidth, elementHeight,
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
            this.turnTimeLimitField = new EditBox(this.font, layout.getX(52), currentY, halfWidth, elementHeight,
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
                (button) -> parseAndStartMatch()
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
            this.minecraft.setScreen(new FalkyeTutorialScreen(this));
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                       MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        // Золотая металлическая рамка
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        int currentY = layout.getY(4);
        
        // Красивый заголовок с тенью
        MutableComponent titleComponent = Component.literal("⚔ Настройка матча ⚔")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GOLD)
                .withBold(true));
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        // Тень заголовка
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, currentY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, currentY, 0xFFFFFF, false);
        currentY += layout.getSpacing() * 3;
        
        // Имя противника
        Component opponentComponent = Component.translatable("screen.bm_falkye.opponent", opponentName)
            .withStyle(ChatFormatting.WHITE);
        guiGraphics.drawString(this.font, opponentComponent,
            layout.getX(5), currentY, 0xFFFFFF, false);
        currentY += layout.getSpacing() * 2;
        
        // Монеты жителя (если это NPC)
        if (isNPC && villagerCoins >= 0) {
            Component coinsComponent = Component.translatable("screen.bm_falkye.villager_coins", villagerCoins)
                .withStyle(ChatFormatting.YELLOW);
            guiGraphics.drawString(this.font, coinsComponent,
                layout.getX(5), currentY, 0xFFFF00, false);
            currentY += layout.getSpacing() * 3;
        } else {
            currentY += layout.getSpacing();
        }
        
        // Разделитель
        guiGraphics.fill(layout.getX(5), currentY, layout.getX(95), currentY + 1, 0xFF8B7355);
        currentY += layout.getSpacing() * 2;
        
        // Информация о ставке и награде - адаптивное позиционирование
        int infoY = layout.getY(75);
        if (config.isFriendlyMatch()) {
            Component friendlyInfo = Component.literal("Дружеская игра - без ставки и наград")
                .withStyle(ChatFormatting.GREEN);
            guiGraphics.drawString(this.font, friendlyInfo,
                layout.getX(5), infoY, 0x00FF00, false);
            infoY += layout.getSpacing() * 2;
        } else if (config.getBetAmount() > 0) {
            Component betInfo = Component.translatable("screen.bm_falkye.bet_info", config.getBetAmount())
                .withStyle(ChatFormatting.YELLOW);
            guiGraphics.drawString(this.font, betInfo,
                layout.getX(5), infoY, 0xFFFF00, false);
            infoY += layout.getSpacing() * 2;
        }
        
        // Информация о награде за сложность (только для NPC)
        if (isNPC) {
            float multiplier = config.getDifficulty().getAIMultiplier();
            int baseXP = 50;
            int xpReward = (int)(baseXP * multiplier);
            Component xpInfo = Component.translatable("screen.bm_falkye.xp_reward", xpReward)
                .withStyle(ChatFormatting.GREEN);
            guiGraphics.drawString(this.font, xpInfo,
                layout.getX(5), infoY, 0x00FF00, false);
            
            infoY += layout.getSpacing() * 2;
            Component difficultyInfo = Component.literal(getDifficultyInfo(config.getDifficulty()))
                .withStyle(ChatFormatting.GRAY);
            guiGraphics.drawString(this.font, difficultyInfo,
                layout.getX(5), infoY, 0xAAAAAA, false);
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Кастомный рендеринг кнопок
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    /**
     * Получает описание сложности
     */
    private String getDifficultyInfo(MatchConfig.Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "Легкий уровень - подходит для новичков";
            case NORMAL -> "Нормальный уровень - стандартная сложность";
            case HARD -> "Сложный уровень - требует опыта";
            case EXPERT -> "Экспертный уровень - для мастеров";
        };
    }
    
    /**
     * Получает текст для кнопки выбора колоды (использует Component вместо строк)
     */
    private Component getDeckButtonTextComponent() {
        if (savedDecks.isEmpty()) {
            return Component.literal("Колода: ")
                .append(Component.literal("Создать колоду").withStyle(ChatFormatting.YELLOW));
        }
        if (selectedDeckIndex == -1) {
            return Component.literal("Колода: ")
                .append(Component.literal("Сохранённая колода").withStyle(ChatFormatting.YELLOW));
        }
        if (selectedDeckIndex >= 0 && selectedDeckIndex < savedDecks.size()) {
            String deckName = savedDecks.get(selectedDeckIndex).getDeckName();
            if (deckName.length() > 20) {
                deckName = deckName.substring(0, 17) + "...";
            }
            return Component.literal("Колода: ")
                .append(Component.literal(deckName).withStyle(ChatFormatting.YELLOW));
        }
        return Component.literal("Колода: ")
            .append(Component.literal("Сохранённая колода").withStyle(ChatFormatting.YELLOW));
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
            deckSelectButton.setMessage(getDeckButtonTextComponent());
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
