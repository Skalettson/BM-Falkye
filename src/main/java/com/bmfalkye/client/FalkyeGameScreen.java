package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.CardTextures;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.client.gui.ActionLogRenderer;
import com.bmfalkye.client.gui.WeatherRenderer;
import com.bmfalkye.client.gui.GameTextures;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.ClientFalkyeGameSession;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * Улучшенный GUI для игры Falkye
 * Реализовано в стиле BM Characters с использованием стандартных компонентов Minecraft
 */
public class FalkyeGameScreen extends Screen {
    // Адаптивные размеры (процент от размера экрана, но с ограничениями)
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    private ClientFalkyeGameSession session;
    private int guiX;
    private int guiY;
    
    // Анимации карт
    private final java.util.Map<String, com.bmfalkye.client.animations.CardAnimation> cardAnimations = new java.util.HashMap<>();
    private int selectedCardIndex = -1;
    
    // Менеджер эффектов для GUI
    private final com.bmfalkye.client.effects.GuiEffectManager effectManager = new com.bmfalkye.client.effects.GuiEffectManager();
    
    // Менеджер анимаций сброса карт
    private final com.bmfalkye.client.effects.CardPlayAnimationManager animationManager = new com.bmfalkye.client.effects.CardPlayAnimationManager();
    
    // Предыдущее состояние сессии для отслеживания изменений
    private ClientFalkyeGameSession previousSession = null;

    // Выбор карты и ряда
    private Card selectedCard = null;
    private FalkyeGameSession.CardRow selectedRow = null;
    
    // Кнопки выбора ряда (создаются динамически при выборе карты)
    private final java.util.List<Button> rowSelectionButtons = new java.util.ArrayList<>();
    
    // Список кнопок для кастомного рендеринга
    private final java.util.List<Button> buttons = new java.util.ArrayList<>();
    
    // Ссылки на основные кнопки для стилизации
    private Button rulesButton;
    private Button surrenderButton;
    private Button leaderButton;
    private Button passButton;
    
    // Лог действий
    private final java.util.List<String> actionLog = new java.util.ArrayList<>();
    private static final int MAX_LOG_ENTRIES = 50; // Увеличено для автоматической прокрутки
    private int logScrollOffset = 0; // Смещение для автоматической прокрутки
    
    // Таймер для закрытия экрана после окончания игры
    private long gameEndTime = -1;
    private static final long GAME_END_CLOSE_DELAY = 3000; // 3 секунды в миллисекундах
    
    // Локальное обновление таймера на клиенте (для предотвращения зависаний)
    private int localRemainingTime = 0;
    private long lastTimerUpdate = 0;
    private UUID lastCurrentPlayerUUID = null;
    
    // ОПТИМИЗАЦИЯ: Throttling для обновления анимаций
    private long lastAnimationUpdateTime = 0;

    public FalkyeGameScreen(ClientFalkyeGameSession session) {
        super(Component.translatable("screen.bm_falkye.game_title"));
        this.session = session;
    }
    
    public FalkyeGameScreen(FalkyeGameSession session) {
        super(Component.translatable("screen.bm_falkye.game_title"));
        // Конвертируем FalkyeGameSession в ClientFalkyeGameSession (для обратной совместимости)
        this.session = convertToClientSession(session);
    }
    
    private ClientFalkyeGameSession convertToClientSession(FalkyeGameSession serverSession) {
        int remainingTime = com.bmfalkye.game.TurnTimer.getRemainingTime(serverSession);
        UUID currentPlayerUUID = serverSession.getCurrentPlayerUUID();
        int timeoutCount = currentPlayerUUID != null ? 
            com.bmfalkye.game.TurnTimer.getTimeOutCount(currentPlayerUUID) : 0;
        
        // Получаем UUID второго игрока (для villager используем UUID жителя)
        UUID player2UUID = serverSession.getPlayer2() != null ? serverSession.getPlayer2().getUUID() : 
            (serverSession.isPlayingWithVillager() && serverSession.getVillagerOpponent() != null ? 
                serverSession.getVillagerOpponent().getUUID() : java.util.UUID.randomUUID());
        
        // Получаем руки игроков (для villager используем null)
        java.util.List<String> hand1Ids = serverSession.getHand(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> hand2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getHand(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getHand(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        // Получаем ряды игроков (для villager используем null)
        java.util.List<String> melee2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getMeleeRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getMeleeRow(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        java.util.List<String> ranged2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getRangedRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getRangedRow(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        java.util.List<String> siege2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getSiegeRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getSiegeRow(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        return new ClientFalkyeGameSession(
            serverSession.getPlayer1().getUUID(),
            player2UUID,
            serverSession.getRoundScore(serverSession.getPlayer1()),
            serverSession.getRoundScore(serverSession.getPlayer2()),
            serverSession.getCurrentRound(),
            serverSession.getCurrentPlayerUUID(),
            hand1Ids,
            hand2Ids,
            serverSession.getMeleeRow(serverSession.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            serverSession.getRangedRow(serverSession.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            serverSession.getSiegeRow(serverSession.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            melee2Ids,
            ranged2Ids,
            siege2Ids,
            new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            serverSession.getWeather(),
            serverSession.getLeader(serverSession.getPlayer1()) != null ? serverSession.getLeader(serverSession.getPlayer1()).getId() : "",
            serverSession.getLeader(serverSession.getPlayer2()) != null ? serverSession.getLeader(serverSession.getPlayer2()).getId() : "",
            serverSession.getRoundsWon(serverSession.getPlayer1()),
            serverSession.getRoundsWon(serverSession.getPlayer2()),
            serverSession.hasPassed(serverSession.getPlayer1()),
            serverSession.hasPassed(serverSession.getPlayer2()),
            serverSession.isPlayingWithVillager(),
            remainingTime,
            timeoutCount,
            serverSession.getPowerModifiers(),
            new java.util.ArrayList<>(), // Показанные карты не используются в этом конструкторе (для обратной совместимости)
            serverSession.getLocationType(), // Тип локации
            serverSession.getMatchConfig() != null ? serverSession.getMatchConfig().getGameMode() : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D // Режим игры
        );
    }

    @Override
    protected void init() {
        super.init();
        
        // Полностью адаптивные размеры: используем процент от размера экрана с ограничениями
        // Расширяем окно: 75-80% по ширине и 85-90% по высоте для большего пространства
        int baseWidth = (int)(this.width * 0.78); // 78% ширины экрана
        int baseHeight = (int)(this.height * 0.88); // 88% высоты экрана
        
        // Ограничения для разных разрешений
        int minWidth = 600;
        int maxWidth = 1000;
        int minHeight = 500;
        int maxHeight = 800;
        
        // Используем оптимизатор для маленьких экранов
        if (com.bmfalkye.client.gui.SmallScreenOptimizer.isSmallScreen(this)) {
            // Для маленьких экранов используем оптимизатор
            minWidth = 400; // Снижаем минимальную ширину для очень маленьких экранов
            minHeight = 350; // Снижаем минимальную высоту для очень маленьких экранов
            GUI_WIDTH = com.bmfalkye.client.gui.SmallScreenOptimizer.getGuiWidth(this, baseWidth, minWidth);
            GUI_HEIGHT = com.bmfalkye.client.gui.SmallScreenOptimizer.getGuiHeight(this, baseHeight, minHeight);
        } else {
            // Для больших экранов используем адаптивные размеры
            GUI_WIDTH = Math.max(minWidth, Math.min(maxWidth, baseWidth));
            GUI_HEIGHT = Math.max(minHeight, Math.min(maxHeight, baseHeight));
        }
        
        // Смещаем окно влево, влево-вверх и влево-вниз
        // Расширяем влево: смещаем на 80-100 пикселей влево (в зависимости от размера экрана)
        int leftOffset = Math.max(60, Math.min(100, (int)(this.width * 0.08))); // 8% экрана, но не менее 60 и не более 100
        this.guiX = (this.width - GUI_WIDTH) / 2 - leftOffset;
        
        // Расширяем вверх и вниз: смещаем немного вверх для большего пространства
        int verticalOffset = Math.max(20, Math.min(40, (int)(this.height * 0.03))); // 3% экрана, но не менее 20 и не более 40
        this.guiY = (this.height - GUI_HEIGHT) / 2 - verticalOffset;
        
        // Ограничиваем позицию, чтобы окно не выходило за границы экрана
        this.guiX = Math.max(10, Math.min(this.guiX, this.width - GUI_WIDTH - 200)); // Оставляем место справа для лога
        this.guiY = Math.max(10, Math.min(this.guiY, this.height - GUI_HEIGHT - 10));
        
        // Инициализируем координаты лога действий (адаптивно)
        // Используем оптимизатор для маленьких экранов
        int baseLogWidth = Math.max(160, Math.min(200, (int)(this.width * 0.12))); // 12% ширины экрана
        logWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getLogWidth(this, baseLogWidth);
        logX = guiX + GUI_WIDTH + 10;
        int infoPanelHeight = Math.max(90, Math.min(110, (int)(GUI_HEIGHT * 0.12))); // 12% высоты окна
        logY = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.05)) + infoPanelHeight + 10; // 5% от высоты окна + высота панели
        int baseLogHeight = Math.max(180, Math.min(250, (int)(GUI_HEIGHT * 0.35))); // 35% высоты окна
        logHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getLogHeight(this, baseLogHeight);
        
        // Очищаем кнопки выбора ряда и основные кнопки
        clearRowSelectionButtons();
        buttons.clear();
        
        // Кнопки (адаптивные размеры и позиции)
        // Используем оптимизатор для маленьких экранов
        int buttonY = this.guiY + GUI_HEIGHT - Math.max(25, Math.min(35, (int)(GUI_HEIGHT * 0.05))); // 5% от высоты окна
        int baseButtonHeight = Math.max(18, Math.min(22, (int)(GUI_HEIGHT * 0.03))); // 3% от высоты окна
        int baseButtonWidth = Math.max(60, Math.min(75, (int)(GUI_WIDTH * 0.09))); // 9% от ширины окна
        int buttonHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonHeight(this, baseButtonHeight);
        int buttonWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonWidth(this, baseButtonWidth);
        
        // Кнопки (адаптивное расположение)
        int buttonSpacing = Math.max(4, Math.min(6, (int)(GUI_WIDTH * 0.008))); // 0.8% от ширины окна
        int buttonStartX = this.guiX + Math.max(8, (int)(GUI_WIDTH * 0.015)); // 1.5% от ширины окна
        
        // Кнопка "Правила" - используем CreativeCore если доступен
        rulesButton = GuiUtils.createStyledButton(
            buttonStartX, buttonY, buttonWidth, buttonHeight,
            Component.translatable("button.bm_falkye.rules"),
            (button) -> {
                // Используем Patchouli если доступен, иначе обычный экран
                boolean bookOpened = false;
                if (com.bmfalkye.integration.LibraryIntegration.isPatchouliLoaded()) {
                    bookOpened = com.bmfalkye.integration.PatchouliIntegration.openBook(
                        new net.minecraft.resources.ResourceLocation("bm_falkye", "tutorial"));
                }
                // Если Patchouli не загружен или открытие книги не удалось, открываем обычный экран
                if (!bookOpened) {
                    // Сохраняем текущий экран и открываем правила
                    FalkyeGameScreen gameScreen = this;
                    this.minecraft.setScreen(new FalkyeTutorialScreen() {
                        @Override
                        public void onClose() {
                            // Возвращаемся к игровому экрану
                            this.minecraft.setScreen(gameScreen);
                        }
                    });
                }
            }
        );
        this.addRenderableWidget(rulesButton);
        buttons.add(rulesButton);
        
        // Кнопка "Сдаться" - используем CreativeCore если доступен
        surrenderButton = GuiUtils.createStyledButton(
            buttonStartX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight,
            Component.translatable("button.bm_falkye.surrender"),
            (button) -> {
                // Воспроизводим звук сдачи
                com.bmfalkye.client.sounds.SoundEffectManager.playSurrenderSound();
                // Проверяем, что игра не закончилась
                if (session != null && !session.isGameEnded()) {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.SurrenderPacket());
                    // Устанавливаем таймер для закрытия экрана сразу после отправки пакета
                    // (на случай, если обновление состояния задержится)
                    gameEndTime = System.currentTimeMillis() + 2000; // 2 секунды задержка
            }
            }
        );
        this.addRenderableWidget(surrenderButton);
        buttons.add(surrenderButton);
        
        // Кнопка использования лидера - используем CreativeCore если доступен
        leaderButton = GuiUtils.createStyledButton(
            buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight,
            Component.translatable("button.bm_falkye.use_leader"),
            (button) -> {
                UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
                    net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
                boolean isMyTurn = currentPlayerUUID != null && 
                    currentPlayerUUID.equals(session.getCurrentPlayerUUID());
                if (isMyTurn) {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.UseLeaderPacket());
            }
            }
        );
        this.addRenderableWidget(leaderButton);
        buttons.add(leaderButton);
        
        // Кнопка паса - используем CreativeCore если доступен
        passButton = GuiUtils.createStyledButton(
            buttonStartX + (buttonWidth + buttonSpacing) * 3, buttonY, buttonWidth, buttonHeight,
            Component.translatable("button.bm_falkye.pass"),
            (button) -> {
                UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
                    net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
                boolean isMyTurn = currentPlayerUUID != null && 
                    currentPlayerUUID.equals(session.getCurrentPlayerUUID());
                if (isMyTurn) {
                    // Воспроизводим звук паса
                    com.bmfalkye.client.sounds.SoundEffectManager.playPassSound();
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.PassPacket());
            }
            }
        );
        this.addRenderableWidget(passButton);
        buttons.add(passButton);
    }
    
    /**
     * Создаёт кнопки выбора ряда при выборе карты
     */
    private void initRowSelectionButtons() {
        clearRowSelectionButtons();
        
        if (selectedCard == null) return;
        
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        UUID sessionCurrentPlayer = session.getCurrentPlayerUUID();
        boolean isMyTurn = currentPlayerUUID != null && sessionCurrentPlayer != null &&
            currentPlayerUUID.equals(sessionCurrentPlayer);
        
        if (!isMyTurn) return;
        
        // Позиция кнопок - в центре экрана (адаптивно)
        // Используем оптимизатор для маленьких экранов
        int baseButtonWidth = Math.max(140, Math.min(180, (int)(GUI_WIDTH * 0.22))); // 22% от ширины окна
        int baseButtonHeight = Math.max(18, Math.min(22, (int)(GUI_HEIGHT * 0.03))); // 3% от высоты окна
        int buttonWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonWidth(this, baseButtonWidth);
        int buttonHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonHeight(this, baseButtonHeight);
        int buttonSpacing = Math.max(4, Math.min(6, (int)(GUI_HEIGHT * 0.008))); // 0.8% от высоты окна
        int totalHeight = buttonHeight * 4 + buttonSpacing * 3; // 4 кнопки + 3 отступа
        int buttonX = guiX + GUI_WIDTH / 2 - buttonWidth / 2; // Центрируем адаптивно
        int buttonY = guiY + GUI_HEIGHT / 2 - totalHeight / 2; // Центрируем адаптивно
        
        // Кнопка "Ближний бой" - используем CreativeCore если доступен
        Button meleeBtn = GuiUtils.createStyledButton(
            buttonX, buttonY, buttonWidth, buttonHeight,
            Component.literal("§6⚔ Ближний бой"),
            (btn) -> {
                playCard(selectedCard, FalkyeGameSession.CardRow.MELEE);
                selectedCard = null;
                selectedCardIndex = -1;
                clearRowSelectionButtons();
            }
        );
        this.addRenderableWidget(meleeBtn);
        rowSelectionButtons.add(meleeBtn);
        
        // Кнопка "Дальний бой" - используем CreativeCore если доступен
        Button rangedBtn = GuiUtils.createStyledButton(
            buttonX, buttonY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
            Component.literal("§e🏹 Дальний бой"),
            (btn) -> {
                playCard(selectedCard, FalkyeGameSession.CardRow.RANGED);
                selectedCard = null;
                selectedCardIndex = -1;
                clearRowSelectionButtons();
            }
        );
        this.addRenderableWidget(rangedBtn);
        rowSelectionButtons.add(rangedBtn);
        
        // Кнопка "Осада" - используем CreativeCore если доступен
        Button siegeBtn = GuiUtils.createStyledButton(
            buttonX, buttonY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight,
            Component.literal("§c🏰 Осада"),
            (btn) -> {
                playCard(selectedCard, FalkyeGameSession.CardRow.SIEGE);
                selectedCard = null;
                selectedCardIndex = -1;
                clearRowSelectionButtons();
            }
        );
        this.addRenderableWidget(siegeBtn);
        rowSelectionButtons.add(siegeBtn);
        
        // Кнопка "Отмена" - используем CreativeCore если доступен
        Button cancelBtn = GuiUtils.createStyledButton(
            buttonX, buttonY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight,
            Component.literal("§7Отмена"),
            (btn) -> {
                selectedCard = null;
                selectedCardIndex = -1;
                clearRowSelectionButtons();
            }
        );
        this.addRenderableWidget(cancelBtn);
        rowSelectionButtons.add(cancelBtn);
    }
    
    /**
     * Удаляет кнопки выбора ряда
     */
    private void clearRowSelectionButtons() {
        for (Button btn : rowSelectionButtons) {
            this.removeWidget(btn);
        }
        rowSelectionButtons.clear();
    }
    
    /**
     * Проверяет, требует ли карта способности выбора ряда
     * Некоторые способности работают на все карты и не требуют выбора ряда
     */
    private boolean requiresRowSelection(Card card) {
        if (card == null) return true;
        
        // ИСПРАВЛЕНО: Обычные карты (CREATURE) ВСЕГДА требуют выбора ряда
        if (card.getType() == Card.CardType.CREATURE) {
            return true; // Обычные карты всегда требуют выбора ряда
        }
        
        // Для карт способностей (SPELL/SPECIAL) проверяем, требуют ли они выбора ряда
        String cardId = card.getId();
        String description = card.getDescription().toLowerCase();
        
        // Карты способностей, которые работают на все карты (не требуют выбора ряда)
        if (cardId.equals("nature_heal") || description.contains("усиливает все ваши карты") || 
            description.contains("лечит все ваши карты")) {
            return false; // Не требует выбора ряда
        }
        
        if (cardId.equals("solar_beam") || (description.contains("солнечный луч") && description.contains("усиливает все ваши карты"))) {
            return false;
        }
        
        if (cardId.equals("grove_song") || (description.contains("песнь рощи") && description.contains("усиливает все ваши карты"))) {
            return false;
        }
        
        if (cardId.equals("ancient_tree") || (description.contains("древнее дерево") && description.contains("лечит"))) {
            return false;
        }
        
        // Погодные карты не требуют выбора ряда
        if (cardId.equals("weather_frost") || cardId.equals("weather_fog") || cardId.equals("weather_rain") ||
            description.contains("мороз") || description.contains("туман") || description.contains("дождь")) {
            return false;
        }
        
        // Карты, которые работают на карты противника, не требуют выбора ряда
        if (cardId.equals("flame_storm") || description.contains("огненная буря") ||
            cardId.equals("entropy_whisper") || description.contains("шёпот энтропии") ||
            cardId.equals("glacis") || description.contains("глацис")) {
            return false;
        }
        
        // Остальные способности требуют выбора ряда
        return true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Сначала проверяем клики по кнопкам выбора ряда (они должны обрабатываться в любом случае)
        for (Button btn : rowSelectionButtons) {
            if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                btn.onPress();
                return true;
            }
        }
        
        // Проверяем клики по основным кнопкам
        for (Button btn : buttons) {
            if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                btn.onPress();
                return true;
            }
        }
        
        if (button == 0 && session != null) { // ЛКМ
            UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
                net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
            
            // Проверяем, наш ли ход
            UUID sessionCurrentPlayer = session.getCurrentPlayerUUID();
            boolean isMyTurn = currentPlayerUUID != null && sessionCurrentPlayer != null &&
                currentPlayerUUID.equals(sessionCurrentPlayer);
            
            if (!isMyTurn) {
                // Если не наш ход, всё равно обрабатываем клики по кнопкам
                // Но не позволяем выбирать карты
                return super.mouseClicked(mouseX, mouseY, button);
            }
            
            // Проверяем клик по карте в руке (компактно, как в renderHand)
            // Получаем руку текущего игрока - ClientFalkyeGameSession.getHand(null) автоматически определяет игрока по UUID
            List<Card> hand = session.getHand(null);
            
            // Используем те же координаты, что и в renderHand (адаптивно)
            // Позиция руки: справа в нижнем углу ЭКРАНА КЛИЕНТА (не окна игры)
            // Позиция справа: от правого края ЭКРАНА с отступом
            // Рассчитываем размеры карт для расчета размеров окна (адаптивно под размер экрана)
            int baseCardWidth = 60;
            int baseCardHeight = 90;
            // Используем оптимизатор для маленьких экранов
            float baseScale = Math.max(0.8f, Math.min(1.2f, (float)this.width / 1000.0f));
            float scale = (float)(baseScale * com.bmfalkye.client.gui.SmallScreenOptimizer.getScale(this));
            int cardWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardWidth(this, (int)(baseCardWidth * scale));
            int cardHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardHeight(this, (int)(baseCardHeight * scale));
            int cardSpacing = cardWidth + 15; // Отступ между картами
            
            // Размеры окна колоды (совпадают с renderHand, адаптивно под разрешение экрана)
            int deckWindowPadding = Math.max(10, Math.min(20, (int)(this.width * 0.015))); // 1.5% от ширины экрана, но не менее 10 и не более 20
            int headerHeight = Math.max(18, Math.min(24, (int)(this.height * 0.025))); // 2.5% от высоты экрана, но не менее 18 и не более 24
            int bottomPadding = Math.max(8, Math.min(12, (int)(this.height * 0.012))); // 1.2% от высоты экрана, но не менее 8 и не более 12
            
            // Высота окна колоды: заголовок + карты + отступы (адаптивно, но с учетом реальных размеров карт)
            int deckWindowHeight = deckWindowPadding + headerHeight + cardHeight + bottomPadding + deckWindowPadding;
            
            // Позиция руки: справа внизу, с минимальным отступом от нижнего края ЭКРАНА (опускаем руку ниже)
            // Отступ от нижнего края: минимум 3 пикселя
            int bottomGap = Math.max(3, Math.min(5, (int)(this.height * 0.003))); // 0.3% от высоты экрана, но не менее 3 и не более 5
            int handY = this.height - deckWindowHeight - bottomGap;
            
            // Рассчитываем необходимую ширину для 5 карт с отступами (убираем лишний запас справа)
            int requiredWidth = (5 * cardSpacing) + (deckWindowPadding * 2); // 5 карт + отступы слева и справа
            
            // Отступ от правого края ЭКРАНА (адаптивно, такой же как слева - deckWindowPadding)
            int rightOffset = deckWindowPadding; // Используем тот же отступ, что и слева
            
            // Ширина окна колоды должна быть достаточной для 5 карт, но не выходить за правый край ЭКРАНА
            int deckWindowWidth = Math.max(requiredWidth, Math.min(requiredWidth, this.width - rightOffset));
            
            // Позиция окна колоды: строго справа внизу ЭКРАНА КЛИЕНТА (от правого края экрана с отступом)
            int deckWindowX = this.width - deckWindowWidth - rightOffset;
            int deckWindowY = handY; // Окно начинается с handY, без смещения вверх
            int cardsStartX = deckWindowX + deckWindowPadding;
            int cardsStartY = deckWindowY + deckWindowPadding + headerHeight; // Ниже заголовка
            
            // Адаптивные размеры карт (совпадают с renderHand)
            // baseCardWidth, baseCardHeight, scale, cardWidth, cardHeight и cardSpacing уже объявлены выше
            
            // Максимум 5 карт на экране (совпадает с renderHand)
            int maxVisibleCards = 5;
            int startIndex = 0;
            if (hand.size() > maxVisibleCards) {
                // Если карт больше 5, сдвигаем влево, чтобы освободить место для 5-й карты
                if (selectedCardIndex >= 0 && selectedCardIndex < hand.size()) {
                    startIndex = Math.max(0, Math.min(selectedCardIndex, hand.size() - maxVisibleCards));
                } else {
                    startIndex = hand.size() - maxVisibleCards;
                }
            } else {
                // Если карт 5 или меньше, показываем все с начала
                startIndex = 0;
            }
            
            int endIndex = Math.min(startIndex + maxVisibleCards, hand.size());
            for (int i = startIndex; i < endIndex; i++) {
                Card card = hand.get(i);
                int displayIndex = i - startIndex; // Индекс для отображения (0-4)
                int cardX = cardsStartX + (displayIndex * cardSpacing);
                
                if (mouseX >= cardX && mouseX <= cardX + cardWidth && 
                    mouseY >= cardsStartY && mouseY <= cardsStartY + cardHeight) {
                    // Проверяем тип карты
                    boolean isAbilityCard = card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL;
                    
                    // Для карт способностей, которые работают на все карты, применяем сразу без выбора ряда
                    if (isAbilityCard && !requiresRowSelection(card)) {
                        // Применяем способность сразу на все карты (ряд не важен для таких способностей)
                        playCard(card, FalkyeGameSession.CardRow.MELEE); // Используем MELEE как значение по умолчанию, но способность будет применена ко всем картам
                        return true;
                    }
                    
                    // Для обычных карт и способностей, требующих выбор ряда - показываем кнопки
                    selectedCard = card;
                    selectedCardIndex = i; // Сохраняем реальный индекс карты
                    initRowSelectionButtons();
                    return true;
                }
            }
            
            // Если кликнули вне карты, сбрасываем выбор
            if (selectedCard != null && mouseY < cardsStartY - 20) {
                    selectedCard = null;
                    selectedCardIndex = -1;
                clearRowSelectionButtons();
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Обновляем координаты лога (на случай изменения размера окна) - адаптивно
        // Используем те же вычисления, что и в init()
        logWidth = Math.max(160, Math.min(200, (int)(this.width * 0.12))); // 12% ширины экрана
        logX = guiX + GUI_WIDTH + 10;
        int infoPanelHeight = Math.max(90, Math.min(110, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
        logY = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.05)) + infoPanelHeight + 10;
        logHeight = Math.max(180, Math.min(250, (int)(GUI_HEIGHT * 0.35))); // 35% от высоты окна
        
        // Проверяем, находится ли мышь над логом действий
        if (mouseX >= logX && mouseX <= logX + logWidth && 
            mouseY >= logY && mouseY <= logY + logHeight) {
            
            // Вычисляем максимальное смещение
            int maxVisibleEntries = Math.min(6, (logHeight - 30) / 12);
            int maxScrollOffset = Math.max(0, actionLog.size() - maxVisibleEntries);
            
            // Прокручиваем лог (delta > 0 - вверх, delta < 0 - вниз)
            if (delta > 0) {
                // Прокрутка вверх (к старым записям)
                logScrollOffset = Math.max(0, logScrollOffset - 1);
            } else if (delta < 0) {
                // Прокрутка вниз (к новым записям)
                logScrollOffset = Math.min(maxScrollOffset, logScrollOffset + 1);
            }
            
            return true; // Обработали прокрутку
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    private void playCard(Card card, FalkyeGameSession.CardRow row) {
        if (card == null) return;
        
        // Проверяем, что игра не закончилась и это наш ход
        if (session != null && session.isGameEnded()) {
            return; // Игра закончилась, не позволяем играть карты
        }
        
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        if (currentPlayerUUID == null || !currentPlayerUUID.equals(session.getCurrentPlayerUUID())) {
            return; // Не наш ход
        }
        
        int rowIndex = switch (row) {
            case MELEE -> 0;
            case RANGED -> 1;
            case SIEGE -> 2;
        };
        
        // Отправляем пакет на сервер
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.PlayCardPacket(card.getId(), rowIndex)
        );
        
        // Сбрасываем выбор карты сразу после отправки пакета
        selectedCard = null;
        selectedCardIndex = -1;
        clearRowSelectionButtons();
        
        // Определяем тип карты
        boolean isAbilityCard = card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL;
        
        // Добавляем в лог с правильным сообщением
        if (isAbilityCard) {
            // Для карт способностей показываем специальное сообщение
            addActionLog("§aВы использовали способность: §f" + card.getName());
        } else {
            // Для обычных карт показываем ряд
            addActionLog("Вы сыграли: " + card.getName() + " в " + 
                switch (row) {
                    case MELEE -> "ближний бой";
                    case RANGED -> "дальний бой";
                    case SIEGE -> "осаду";
                });
        }
        
        // Создаем визуальный эффект игры карты в GUI
        // Для карт способностей используем центр экрана, так как они не привязаны к ряду
        FalkyeGameSession.CardRow effectRow = isAbilityCard ? FalkyeGameSession.CardRow.RANGED : row;
        effectManager.playCardPlayEffect(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, card, effectRow);
        
        // Воспроизводим звук игры карты
        com.bmfalkye.client.sounds.SoundEffectManager.playCardPlaySound(card);
    }
    
    public void addActionLog(String action) {
        actionLog.add(action);
        if (actionLog.size() > MAX_LOG_ENTRIES) {
            actionLog.remove(0);
            // Уменьшаем смещение, если оно стало больше размера лога
            if (logScrollOffset > 0) {
                logScrollOffset = Math.max(0, logScrollOffset - 1);
            }
        }
        // Автоматически прокручиваем лог вниз при добавлении новой записи
        // Только если пользователь не прокручивал вручную (т.е. смещение уже внизу)
        int logHeight = Math.min(200, GUI_HEIGHT / 3);
        int maxVisibleEntries = Math.min(6, (logHeight - 30) / 12);
        int maxScrollOffset = Math.max(0, actionLog.size() - maxVisibleEntries);
        
        // Если пользователь уже прокрутил вверх, не меняем смещение
        // Если смещение внизу (или близко к низу), автоматически прокручиваем
        if (logScrollOffset >= maxScrollOffset - 1) {
            logScrollOffset = maxScrollOffset;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Проверяем, нужно ли закрыть экран после окончания игры
        if (gameEndTime > 0) {
            long currentTime = System.currentTimeMillis();
            // Если gameEndTime установлен в будущем (кнопка "Сдаться"), ждём до этого времени
            // Если gameEndTime в прошлом, проверяем задержку
            if (gameEndTime > currentTime) {
                // Ждём до установленного времени (кнопка "Сдаться" установила время в будущем)
            } else if (currentTime - gameEndTime >= GAME_END_CLOSE_DELAY) {
                if (this.minecraft != null && this.minecraft.screen == this) {
                    this.minecraft.setScreen(null);
                }
                gameEndTime = -1; // Сбрасываем таймер
                return; // Не рендерим, если закрываем экран
            }
        }
        
        // Обновляем анимации карт
        cardAnimations.values().removeIf(anim -> {
            anim.update();
            return anim.isFinished();
        });
        
        // Обновляем менеджер анимаций сброса карт
        // ОПТИМИЗАЦИЯ: Обновляем анимации с throttling (раз в 2 кадра вместо каждого кадра)
        long currentTime = System.currentTimeMillis();
        if (lastAnimationUpdateTime == 0 || currentTime - lastAnimationUpdateTime >= 33) { // ~30 FPS для анимаций
            animationManager.update();
            lastAnimationUpdateTime = currentTime;
        }
        
        // ОПТИМИЗАЦИЯ: Настройки производительности (не требуется)
        
        // Обновляем эффекты GUI
        effectManager.update();
        
        // detectAndAnimateCardPlays вызывается в updateGameSession, где есть доступ к старой и новой сессии
        
        // Обновляем время анимации (не требуется для SimpleCardRenderer)
        
        // Проверяем, закончилась ли игра - если да, не рендерим элементы интерфейса
        boolean gameEnded = session != null && session.isGameEnded();
        
        // Рендерим улучшенный фон игрового поля с анимациями
        this.renderBackground(guiGraphics);
        renderEnhancedGameFieldBackground(guiGraphics, partialTick);
        
        if (gameEnded) {
            // Затемняем фон игрового поля, когда игра закончилась
            GuiUtils.drawRoundedRect(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 0x80000000);
        }
        
        if (session != null && !gameEnded) {
            renderGameInfo(guiGraphics);
            
            // ВАЖНО: Рендерим анимации сброса ДО рендеринга карт на поле, чтобы они были видны
            renderDropAnimations(guiGraphics);
            
            renderRows(guiGraphics, mouseX, mouseY);
            renderHand(guiGraphics, mouseX, mouseY);
            renderLeader(guiGraphics);
            renderActionLog(guiGraphics);
            renderWeatherEffects(guiGraphics);
            
            // Рендерим анимации карт способности (движение в центр и исчезновение)
            renderAbilityCardAnimations(guiGraphics);
            
            // Рендерим анимации сброса карт (карты, которые исчезли с поля)
            renderDiscardAnimations(guiGraphics);
            
            // Рендерим эффекты GUI (поверх всего остального)
            effectManager.render(guiGraphics);
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они будут отрисованы кастомно)
        // Не рендерим виджеты, если игра закончилась
        if (!gameEnded) {
            for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
                if (!(renderable instanceof Button)) {
                    renderable.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
        }
        
        // Кастомный рендеринг окна выбора ряда с фоном (рендерим ПЕРЕД кнопками, чтобы фон был под ними)
        // Не рендерим, если игра закончилась
        if (!rowSelectionButtons.isEmpty() && selectedCard != null && !gameEnded) {
            // Позиция кнопок (адаптивно, должна совпадать с initRowSelectionButtons)
            // Используем оптимизатор для маленьких экранов
            int baseButtonWidth = Math.max(140, Math.min(180, (int)(GUI_WIDTH * 0.22))); // 22% от ширины окна
            int baseButtonHeight = Math.max(18, Math.min(22, (int)(GUI_HEIGHT * 0.03))); // 3% от высоты окна
            int buttonWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonWidth(this, baseButtonWidth);
            int buttonHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getButtonHeight(this, baseButtonHeight);
            int buttonSpacing = Math.max(4, Math.min(6, (int)(GUI_HEIGHT * 0.008))); // 0.8% от высоты окна
            int totalHeight = buttonHeight * 4 + buttonSpacing * 3; // 4 кнопки + 3 отступа
            int buttonX = guiX + GUI_WIDTH / 2 - buttonWidth / 2; // Центрируем адаптивно
            int buttonY = guiY + GUI_HEIGHT / 2 - totalHeight / 2; // Центрируем адаптивно
            
            // Полупрозрачный темный фон с отступами
            int panelX = buttonX - 10;
            int panelY = buttonY - 35;
            int panelWidth = buttonWidth + 20;
            int panelHeight = totalHeight + 60; // Дополнительное место для информации о карте
            
            // Фон панели (рисуем ПЕРЕД кнопками, чтобы избежать наложения)
            GuiUtils.drawRoundedRect(guiGraphics, panelX, panelY, panelWidth, panelHeight, 0xE0000000);
            GuiUtils.drawRoundedBorder(guiGraphics, panelX, panelY, panelWidth, panelHeight, 0xFF8B7355, 2);
            
            // Информация о выбранной карте (над кнопками)
            String cardName = selectedCard.getName();
            if (cardName.length() > 25) {
                cardName = cardName.substring(0, 22) + "...";
            }
            String rarityColorCode = getRarityColorCode(selectedCard.getRarity());
            guiGraphics.drawCenteredString(this.font, 
                Component.literal(rarityColorCode + cardName), 
                buttonX + buttonWidth / 2, buttonY - 30, 0xFFFFFF);
            
            // Информация о силе (редкость убрана, так как визуально видна по цвету рамки)
            String cardInfo = "§bСила: §f" + selectedCard.getPower();
            guiGraphics.drawCenteredString(this.font, 
                Component.literal(cardInfo), 
                buttonX + buttonWidth / 2, buttonY - 20, 0xFFFFFF);
            
            // Подсказка
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7Выберите ряд для игры"), 
                buttonX + buttonWidth / 2, buttonY - 10, 0xCCCCCC);
            }
        
        // Кастомный рендеринг кнопок выбора ряда (поверх фона)
        // Не рендерим, если игра закончилась
        if (!gameEnded) {
            for (Button button : rowSelectionButtons) {
                if (button != null && button.visible) {
                    GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
                }
            }
            
            // Кастомный рендеринг основных кнопок со скруглёнными углами
            for (Button button : buttons) {
                if (button != null && button.visible) {
                    GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
                }
            }
        }
        
        // Рендерим уведомления о случайных событиях
        com.bmfalkye.client.gui.RandomEventNotification.render(guiGraphics, this.font, this.width, this.height);
        
        // Рендерим окно победы/поражения ПОСЛЕДНИМ, чтобы оно было поверх всего
        if (gameEnded) {
            UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
                net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
            renderGameEndInfo(guiGraphics, currentPlayerUUID);
        }
    }
    
    // Координаты лога действий для проверки прокрутки
    private int logX, logY, logWidth, logHeight;
    
    /**
     * Рендерит лог действий игры.
     * 
     * <p>Использует {@link ActionLogRenderer} для отображения лога действий игроков.
     * Лог отображается справа от игрового поля, под информационной панелью.
     */
    private void renderActionLog(GuiGraphics guiGraphics) {
        // Рендерим лог действий справа ЗА полем (не внутри окна) - адаптивно
        // Лог опущен ниже, под информационной панелью
        // logWidth, logX, logY, logHeight уже вычислены в init() адаптивно
        ActionLogRenderer.render(guiGraphics, this.font, actionLog, 
                                 logX, logY, logWidth, logHeight, logScrollOffset);
    }
    
    /**
     * Рендерит улучшенные погодные эффекты на поле с визуальными эффектами.
     * 
     * <p>Использует {@link WeatherRenderer} для отображения текущей погоды
     * и её влияния на силу карт в различных рядах.
     */
    private void renderWeatherEffects(GuiGraphics guiGraphics) {
        if (session.getWeather() == FalkyeGameSession.WeatherType.NONE) {
            return;
        }
        
        // Показываем влияние погоды на карты (адаптивно, справа ЗА полем, под лидером)
        int infoPanelHeight = Math.max(90, Math.min(110, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
        int leaderHeight = Math.max(45, Math.min(55, (int)(GUI_HEIGHT * 0.07))); // 7% от высоты окна
        int weatherX = guiX + GUI_WIDTH + 10; // Справа от игрового окна (адаптивно)
        int weatherY = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.05)) + infoPanelHeight + 10 + logHeight + Math.max(8, (int)(GUI_HEIGHT * 0.01)) + leaderHeight + Math.max(8, (int)(GUI_HEIGHT * 0.01)); // Под лидером (адаптивно)
        int weatherWidth = logWidth; // Используем ширину лога
        int weatherHeight = Math.max(40, Math.min(50, (int)(GUI_HEIGHT * 0.06))); // 6% от высоты окна
        
        WeatherRenderer.render(guiGraphics, this.font, session.getWeather(), 
                               weatherX, weatherY, weatherWidth, weatherHeight);
        
        // Добавляем визуальные эффекты погоды на поле (частицы)
        renderWeatherParticles(guiGraphics, session.getWeather());
        
        // Рендерим эффекты локации
        renderLocationEffects(guiGraphics);
    }
    
    /**
     * Рендерит эффекты окружения (локации)
     */
    private void renderLocationEffects(GuiGraphics guiGraphics) {
        if (session == null || net.minecraft.client.Minecraft.getInstance().player == null) {
            return;
        }
        
        // Получаем тип локации из сессии
        com.bmfalkye.game.LocationEffect.LocationType location = session.getLocationType();
        if (location != null && location != com.bmfalkye.game.LocationEffect.LocationType.NONE && 
            location != com.bmfalkye.game.LocationEffect.LocationType.PLAINS) {
            int locationX = guiX + GUI_WIDTH + 10;
            int locationY = guiY + 10;
            int locationWidth = 150;
            int locationHeight = 50;
            com.bmfalkye.client.gui.LocationEffectRenderer.renderLocationEffect(
                guiGraphics, this.font, locationX, locationY, locationWidth, locationHeight, location);
        }
    }
    
    // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Кэш для частиц погоды
    private long lastWeatherParticleTime = 0;
    private static final long WEATHER_PARTICLE_INTERVAL = 200; // Создаём частицы раз в 200мс
    
    /**
     * Рендерит частицы погодных эффектов на игровом поле
     * АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Ограничено создание частиц
     */
    private void renderWeatherParticles(GuiGraphics guiGraphics, FalkyeGameSession.WeatherType weather) {
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Создаём частицы не каждый кадр, а с интервалом
        long time = System.currentTimeMillis();
        if (time - lastWeatherParticleTime < WEATHER_PARTICLE_INTERVAL) {
            return; // Пропускаем создание частиц
        }
        lastWeatherParticleTime = time;
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Уменьшено количество частиц
        int particleCount = 2; // Было 5, стало 2
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Ограничиваем общее количество частиц
        if (effectManager.particles.size() >= 20) {
            return; // Не создаём новые, если уже много частиц
        }
        
        for (int i = 0; i < particleCount; i++) {
            float x = guiX + (float) (Math.random() * GUI_WIDTH);
            float y = guiY + (float) (Math.random() * GUI_HEIGHT);
            
            int color = switch (weather) {
                case FROST -> 0xFF44AAFF; // Голубой
                case FOG -> 0xFF888888; // Серый
                case RAIN -> 0xFF4488FF; // Синий
                default -> 0xFFFFFFFF;
            };
            
            float vx = switch (weather) {
                case FROST -> (float) ((Math.random() - 0.5) * 0.1f);
                case FOG -> (float) ((Math.random() - 0.5) * 0.05f);
                case RAIN -> 0.0f;
                default -> 0.0f;
            };
            
            float vy = switch (weather) {
                case FROST -> (float) ((Math.random() - 0.5) * 0.1f);
                case FOG -> (float) ((Math.random() - 0.5) * 0.05f);
                case RAIN -> 0.2f; // Дождь падает вниз
                default -> 0.0f;
            };
            
            effectManager.particles.add(effectManager.getParticleFromPool(
                x, y, color, 1.5f, 30, vx, vy
            ));
        }
    }
    
    private void renderSelectedCardInfo(GuiGraphics guiGraphics) {
        // Показываем информацию о выбранной карте (адаптивно под разрешение)
        if (selectedCard == null) return;
        
        // Адаптивные размеры панели информации
        int infoWidth = Math.max(180, Math.min(250, (int)(GUI_WIDTH * 0.3))); // 30% от ширины окна
        int infoHeight = Math.max(70, Math.min(100, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
        int infoX = guiX + GUI_WIDTH / 2 - infoWidth / 2; // Центрируем
        int infoY = guiY + GUI_HEIGHT / 2 - infoHeight / 2; // Центрируем
        
        // Фон
        GuiUtils.drawRoundedRect(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xE0000000);
        GuiUtils.drawRoundedBorder(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xFF8B7355, 2);
        
        // Адаптивные отступы внутри панели
        int padding = Math.max(8, Math.min(12, (int)(infoWidth * 0.05))); // 5% от ширины панели
        int lineHeight = Math.max(12, Math.min(16, (int)(infoHeight * 0.15))); // 15% от высоты панели
        
        // Название карты
        String rarityColorCode = getRarityColorCode(selectedCard.getRarity());
        int titleY = infoY + padding;
        guiGraphics.drawCenteredString(this.font, 
            Component.literal(rarityColorCode + selectedCard.getName()), 
            infoX + infoWidth / 2, titleY, 0xFFFFFF);
        
        // Описание
        String description = selectedCard.getDescription();
        int descMaxWidth = infoWidth - padding * 2;
        if (this.font.width(description) > descMaxWidth) {
            description = this.font.plainSubstrByWidth(description, descMaxWidth - 5) + "...";
        }
        int descY = titleY + lineHeight;
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§7" + description), 
            infoX + infoWidth / 2, descY, 0xCCCCCC);
        
        // Сила (редкость убрана, так как визуально видна по цвету рамки)
        int powerY = descY + lineHeight;
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§eСила: §f" + selectedCard.getPower()), 
            infoX + infoWidth / 2, powerY, 0xFFFFFF);
        
        // Подсказка
        int hintY = powerY + lineHeight;
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§7Выберите ряд для игры"), 
            infoX + infoWidth / 2, hintY, 0xCCCCCC);
    }

    private void renderGameInfo(GuiGraphics guiGraphics) {
        // Заголовок (адаптивно)
        Component titleComponent = Component.translatable("screen.bm_falkye.game_title");
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = guiY + Math.max(12, (int)(GUI_HEIGHT * 0.03)); // 3% от высоты
        // Тень заголовка (черная)
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, titleY, 0xFFFFFF, false);
        
        // Информация о раундах
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        boolean isPlayer1 = currentPlayerUUID != null && currentPlayerUUID.equals(session.getPlayer1UUID());
        
        int roundsWon1 = session.getRoundsWon(null); // Используем null, так как ClientFalkyeGameSession определяет по UUID
        int roundsWon2 = session.getOpponentRoundsWon(); // Получаем очки противника правильно
        int currentRound = session.getCurrentRound();
        
        // Информационная панель справа ЗА полем, над логом действий (адаптивно)
        int infoX = guiX + GUI_WIDTH + 10;
        int infoY = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.05)); // 5% от высоты окна
        int infoWidth = logWidth; // Используем ширину лога
        int infoHeight = Math.max(90, Math.min(110, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
        
        // Используем текстуру информации об игре, если доступна
        if (CardTextures.textureExists(GameTextures.GAME_INFO)) {
            guiGraphics.blit(GameTextures.GAME_INFO, infoX, infoY, 0, 0, infoWidth, infoHeight, infoWidth, infoHeight);
        } else {
            // Fallback: простой фон
            GuiUtils.drawRoundedRect(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xE0101010);
            GuiUtils.drawRoundedBorder(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xFF8B7355, 2);
        }
        
        // Компактная статистика - рендерится ПОВЕРХ текстуры
        int lineHeight = 12; // Увеличен отступ для лучшей читаемости
        // Позиционируем текст так, чтобы он был виден на текстуре
        int currentY = infoY + Math.max(12, (int)(infoHeight * 0.12)); // 12% от высоты панели
        int textX = infoX + Math.max(12, (int)(infoWidth * 0.08)); // 8% от ширины панели
        
        // Раунд
        guiGraphics.drawString(this.font, 
            Component.translatable("screen.bm_falkye.round", currentRound), 
            textX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Очки текущего раунда
        // На сервере очки уже учитывают модификаторы, поэтому получаем их напрямую
        int score1 = session.getRoundScore(null);
        int score2 = session.getOpponentRoundScore();
        
        // Вычисляем базовые очки БЕЗ модификаторов (только базовая сила карт)
        int baseScore1 = calculateBaseScore(session, null);
        int baseScore2 = calculateBaseOpponentScore(session);
        
        // Отображаем очки с изменёнными в скобках (если они отличаются от базовых)
        String score1Text = "§aВы: §f" + score1;
        if (score1 != baseScore1) {
            int diff = score1 - baseScore1;
            score1Text += " §7(" + (diff > 0 ? "§a+" : "§c") + diff + "§7)";
        }
        guiGraphics.drawString(this.font, 
            Component.literal(score1Text), 
            textX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        String score2Text = "§cОпп: §f" + score2;
        if (score2 != baseScore2) {
            int diff = score2 - baseScore2;
            score2Text += " §7(" + (diff > 0 ? "§a+" : "§c") + diff + "§7)";
        }
        guiGraphics.drawString(this.font, 
            Component.literal(score2Text), 
            textX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Победные раунды
        guiGraphics.drawString(this.font, 
            Component.literal("§7§a" + roundsWon1 + "§7/§c" + roundsWon2), 
            textX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Погода (компактно)
        if (session.getWeather() != FalkyeGameSession.WeatherType.NONE) {
            String weatherText = switch (session.getWeather()) {
                case FROST -> "§b❄";
                case FOG -> "§7☁";
                case RAIN -> "§9🌧";
                default -> "";
            };
            guiGraphics.drawString(this.font, 
                Component.literal(weatherText), 
                textX, currentY, 0xFFFFFF, false);
            currentY += lineHeight;
        }
        
        // Таймер хода (компактно) - с улучшенным локальным обновлением для предотвращения зависаний
        UUID currentTurnPlayerUUID = session.getCurrentPlayerUUID();
        boolean isMyTurn = currentPlayerUUID != null && 
            currentPlayerUUID.equals(currentTurnPlayerUUID);
        
        // Обновляем локальный таймер каждый кадр для плавного отображения
        long currentTime = System.currentTimeMillis();
        if (currentTurnPlayerUUID != null) {
            if (!currentTurnPlayerUUID.equals(lastCurrentPlayerUUID)) {
                // Сменился игрок - сбрасываем таймер
                localRemainingTime = session.getRemainingTime();
                lastTimerUpdate = currentTime;
                lastCurrentPlayerUUID = currentTurnPlayerUUID;
            } else {
                // Тот же игрок - обновляем таймер локально
                if (lastTimerUpdate > 0) {
                    long elapsed = (currentTime - lastTimerUpdate) / 1000;
                    if (elapsed > 0) {
                        localRemainingTime = Math.max(0, localRemainingTime - (int)elapsed);
                        lastTimerUpdate = currentTime;
                    }
                } else {
                    // Первая инициализация
                    localRemainingTime = session.getRemainingTime();
                    lastTimerUpdate = currentTime;
                }
            }
        } else {
            // Нет текущего игрока - используем значение с сервера
            localRemainingTime = session.getRemainingTime();
            lastTimerUpdate = currentTime;
        }
        
        // Используем локальный таймер для отображения
        int displayTime = localRemainingTime;
        if (isMyTurn && displayTime > 0) {
            int color = displayTime <= 10 ? 0xFFFF0000 : (displayTime <= 20 ? 0xFFFFFF00 : 0xFFFFFFFF);
            guiGraphics.drawString(this.font, 
                Component.literal("§e⏱ " + displayTime + "с"), 
                textX, currentY, color, false);
            currentY += lineHeight; // Добавляем отступ, чтобы не перекрывалось
        } else if (!isMyTurn) {
            guiGraphics.drawString(this.font, 
                Component.literal("§7⏳"), 
                textX, currentY, 0xCCCCCC, false);
            currentY += lineHeight; // Добавляем отступ
        }
    }

    private void renderRows(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Определяем, какой игрок мы
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        boolean isPlayer1 = currentPlayerUUID != null && currentPlayerUUID.equals(session.getPlayer1UUID());
        
        // Рендерим ряды противника (адаптивно)
        int opponentY = guiY + Math.max(70, (int)(GUI_HEIGHT * 0.12)); // 12% от высоты окна
        
        // Разделитель между игроками (адаптивно)
        int dividerY = guiY + Math.max(65, (int)(GUI_HEIGHT * 0.10)); // 10% от высоты окна
        int dividerMargin = Math.max(10, (int)(GUI_WIDTH * 0.015)); // 1.5% от ширины окна
        guiGraphics.fill(guiX + dividerMargin, dividerY, guiX + GUI_WIDTH - dividerMargin, dividerY + 1, 0xFF4A3A2A);
        // Метка "Противник" - убрана, чтобы не перекрывалась с другими элементами
        
        // Получаем ряды противника (используем методы для получения рядов противника)
        List<Card> opponentMelee = session.getOpponentMeleeRow();
        List<Card> opponentRanged = session.getOpponentRangedRow();
        List<Card> opponentSiege = session.getOpponentSiegeRow();
        
        // Адаптивные ряды (отступы между рядами зависят от высоты окна) - увеличены для корректного наложения
        int rowSpacing = Math.max(50, Math.min(65, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна (увеличено с 8%)
        int infoWidth = Math.max(120, Math.min(180, (int)(GUI_WIDTH * 0.18))); // 18% от ширины окна
        int rowStartX = guiX + Math.max(50, infoWidth + Math.max(15, (int)(GUI_WIDTH * 0.02))); // 2% от ширины окна
        
        // Рендерим ряды в правильном порядке для наложения: сначала ближние, потом дальние, потом осадные
        // (осадные будут поверх дальних, дальние поверх ближних)
        // Порядок рендеринга определяет, что будет сверху при визуальном наложении
        // Добавляем небольшое смещение по Y для визуального наложения (ближние ниже, дальние выше, осадные ещё выше)
        int overlapOffset = Math.max(8, Math.min(12, (int)(GUI_HEIGHT * 0.015))); // 1.5% от высоты окна для наложения
        renderRow(guiGraphics, opponentMelee, 
            rowStartX, opponentY, mouseX, mouseY, "§6⚔");
        renderRow(guiGraphics, opponentRanged, 
            rowStartX, opponentY + rowSpacing - overlapOffset, mouseX, mouseY, "§e🏹");
        renderRow(guiGraphics, opponentSiege, 
            rowStartX, opponentY + rowSpacing * 2 - overlapOffset * 2, mouseX, mouseY, "§c🏰");
        
        // Разделитель (адаптивно)
        int middleY = guiY + GUI_HEIGHT / 2;
        int middleDividerMargin = Math.max(10, (int)(GUI_WIDTH * 0.015)); // 1.5% от ширины окна
        guiGraphics.fill(guiX + middleDividerMargin, middleY - 1, guiX + GUI_WIDTH - middleDividerMargin, middleY, 0xFF4A3A2A);
        
        // Рендерим свои ряды (адаптивно)
        int myY = middleY + Math.max(3, (int)(GUI_HEIGHT * 0.008)); // 0.8% от высоты окна
        
        // Метка "Вы" - убрана, чтобы не перекрывалась с другими элементами
        
        List<Card> myMelee = session.getMeleeRow(null);
        List<Card> myRanged = session.getRangedRow(null);
        List<Card> mySiege = session.getSiegeRow(null);
        
        // Рендерим ряды в правильном порядке для наложения: сначала ближние, потом дальние, потом осадные
        // (осадные будут поверх дальних, дальние поверх ближних)
        // Порядок рендеринга определяет, что будет сверху при визуальном наложении
        // Используем уже определенный overlapOffset для визуального наложения
        renderRow(guiGraphics, myMelee, 
            rowStartX, myY, mouseX, mouseY, "§6⚔");
        renderRow(guiGraphics, myRanged, 
            rowStartX, myY + rowSpacing - overlapOffset, mouseX, mouseY, "§e🏹");
        renderRow(guiGraphics, mySiege, 
            rowStartX, myY + rowSpacing * 2 - overlapOffset * 2, mouseX, mouseY, "§c🏰");
    }

    private void renderRow(GuiGraphics guiGraphics, List<Card> cards, int x, int y, int mouseX, int mouseY, String label) {
        // Подсчёт силы ряда (используем эффективную силу с учётом модификаторов)
        int rowPower = 0;
        for (Card card : cards) {
            // Используем эффективную силу из сессии
            Integer effectivePower = session != null ? session.getEffectivePower(card) : null;
            rowPower += effectivePower != null ? effectivePower : card.getPower();
        }
        
        // Рендерим счёт ряда влево от иконки (адаптивно)
        int powerTextWidth = this.font.width("§7" + rowPower);
        int powerX = x - 25 - powerTextWidth - Math.max(5, (int)(GUI_WIDTH * 0.008)); // Отступ между счётом и иконкой (0.8% от ширины)
        guiGraphics.drawString(this.font, 
            Component.literal("§7" + rowPower), 
            powerX, y + 12, 0xFFFFFF, false);
        
        // Рендерим метку ряда (иконку) - компактно
        guiGraphics.drawString(this.font, Component.literal(label), x - 25, y + 12, 0xFFFFFF, false);
        
        // Рендерим карты в ряду (адаптивный отступ)
        // Базовый размер карты на поле увеличен, поэтому увеличиваем отступ
        int baseCardWidth = 48;
        float fieldScale = Math.max(0.7f, Math.min(1.1f, (float)this.width / 1000.0f));
        int cardWidth = (int)(baseCardWidth * fieldScale);
        int cardHeight = (int)(68 * fieldScale);
        int cardSpacing = cardWidth + 25; // Увеличенный отступ между картами для предотвращения наложения
        
        // Определяем область для обрезки (чтобы карты не выходили за границы) - адаптивно
        int rowAreaX = x;
        int rowAreaY = y;
        int rightMargin = Math.max(180, Math.min(220, (int)(GUI_WIDTH * 0.25))); // 25% от ширины окна для информации справа
        int rowAreaWidth = GUI_WIDTH - (x - guiX) - rightMargin;
        int rowAreaHeight = cardHeight + Math.max(8, (int)(GUI_HEIGHT * 0.012)); // 1.2% от высоты окна
        
        // Рендерим фон ряда, если доступна текстура
        // Позиционируем текстуру так, чтобы она не заезжала на разделительную линию
        // Разделительная линия находится на middleY, поэтому текстура должна быть строго в пределах ряда
        if (CardTextures.textureExists(GameTextures.GAME_CARD_ROW)) {
            // Позиционируем текстуру строго в пределах ряда, не заезжая на разделитель
            // Используем позицию y без смещения вверх/вниз, чтобы не заезжать на разделитель
            guiGraphics.blit(GameTextures.GAME_CARD_ROW, x - 15, y, 0, 0, rowAreaWidth + 15, rowAreaHeight, rowAreaWidth + 15, rowAreaHeight);
        }
        
        // Устанавливаем область обрезки для ряда
        guiGraphics.enableScissor(rowAreaX, rowAreaY, rowAreaX + rowAreaWidth, rowAreaY + rowAreaHeight);
        
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cardX = x + (i * cardSpacing);
            
            // Проверяем, не выходит ли карта за границы области
            if (cardX + cardWidth > rowAreaX + rowAreaWidth) {
                break; // Прекращаем рендеринг, если карты выходят за границы
            }
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверка видимости карты перед рендерингом (используем базовую позицию)
            if (cardX + cardWidth < 0 || cardX > this.width || 
                y + cardHeight < 0 || y > this.height) {
                continue; // Карта не видна - пропускаем
            }
            
            // Проверяем, есть ли анимация для этой карты
            String cardKey = card.getId() + "_" + cardX + "_" + y;
            com.bmfalkye.client.animations.CardAnimation anim = cardAnimations.get(cardKey);
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Упрощённый поиск анимаций (только для видимых карт)
            // Кэшируем результат поиска анимаций для производительности
            com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation dropAnim = null;
            // ОПТИМИЗАЦИЯ: Проверяем только активные анимации (не все)
            java.util.Map<String, com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation> activeDrops = 
                animationManager.getAllDropAnimations();
            if (!activeDrops.isEmpty()) {
                // Быстрая проверка по ID карты (без перебора всех анимаций)
                String cardId = card.getId();
                for (com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation dropAnimation : 
                     activeDrops.values()) {
                    if (dropAnimation.card.getId().equals(cardId) && 
                        !dropAnimation.animation.isFinished()) {
                        // Упрощённая проверка расстояния (без sqrt для производительности)
                        float dx = dropAnimation.targetX - cardX;
                        float dy = dropAnimation.targetY - y;
                        float distanceSq = dx * dx + dy * dy; // Квадрат расстояния (быстрее чем sqrt)
                        if (distanceSq < 900.0f) { // 30^2 = 900
                            dropAnim = dropAnimation;
                            break;
                        }
                    }
                }
            }
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверяем постоянную анимацию только если нет dropAnim
            com.bmfalkye.client.effects.CardPlayAnimationManager.FieldCardAnimation fieldAnim = null;
            if (dropAnim == null) {
                fieldAnim = animationManager.getFieldAnimation(card, cardX, y);
            }
            
            float finalX = cardX;
            float finalY = y;
            float finalScale = 1.0f;
            float finalAlpha = 1.0f;
            
            if (dropAnim != null && !dropAnim.animation.isFinished()) {
                // Используем анимацию сброса - ВСЕГДА применяем анимацию, если она активна
                finalX = dropAnim.animation.getX();
                finalY = dropAnim.animation.getY();
                finalScale = dropAnim.animation.getScale();
                finalAlpha = dropAnim.animation.getAlpha();
                
                // Проигрываем эффекты один раз при достижении целевой позиции
                if (!dropAnim.effectsPlayed) {
                    float distanceToTarget = (float) Math.sqrt(
                        Math.pow(finalX - dropAnim.targetX, 2) + 
                        Math.pow(finalY - dropAnim.targetY, 2)
                    );
                    // Проигрываем эффекты когда карта близко к цели
                    if (distanceToTarget < 20.0f) {
                        com.bmfalkye.client.effects.AdvancedVisualEffects.createCardDropEffect(
                            effectManager, dropAnim.targetX, dropAnim.targetY, card, dropAnim.row
                        );
                        dropAnim.effectsPlayed = true;
                    }
                }
            } else if (anim != null && !anim.isFinished()) {
                // Используем стандартную анимацию
                finalX = anim.getX();
                finalY = anim.getY();
                finalScale = anim.getScale();
                finalAlpha = anim.getAlpha();
            } else if (fieldAnim != null) {
                // Используем постоянную анимацию
                finalX = fieldAnim.getX();
                finalY = fieldAnim.getY();
                finalScale = fieldAnim.getScale();
            }
            
            // Базовые размеры карты на поле (адаптивные, используем уже определенный fieldScale)
            int baseWidth = 48;
            int baseHeight = 68;
            int originalWidth = (int)(baseWidth * fieldScale);
            int originalHeight = (int)(baseHeight * fieldScale);
            
            // Применяем масштаб анимации
            int scaledWidth = (int)(originalWidth * finalScale);
            int scaledHeight = (int)(originalHeight * finalScale);
            
            // Центрируем карту при масштабировании
            int offsetX = (originalWidth - scaledWidth) / 2;
            int offsetY = (originalHeight - scaledHeight) / 2;
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Рендерим карту только если она видима
            // Проверяем, наведена ли мышь на карту (только для видимых карт)
            boolean isHovered = mouseX >= (int)finalX + offsetX && mouseX <= (int)finalX + offsetX + scaledWidth &&
                               mouseY >= (int)finalY + offsetY && mouseY <= (int)finalY + offsetY + scaledHeight;
            
            // ВАЖНО: Показываем tooltip при наведении, если карта не в анимации сброса или анимация завершена
            boolean showTooltipForField = (dropAnim == null || dropAnim.animation.isFinished()) && isHovered;
            renderCardOnFieldAnimated(guiGraphics, card, (int)finalX + offsetX, (int)finalY + offsetY, 
                                     scaledWidth, scaledHeight, mouseX, mouseY, finalAlpha, showTooltipForField);
        }
        
        // Отключаем обрезку
        guiGraphics.disableScissor();
        
        // Счёт ряда уже отрендерен в начале метода, перед иконкой
    }

    private void renderHand(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Рендерим карты в руке (компактнее, но адаптивно)
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        List<Card> hand = session.getHand(null); // ClientFalkyeGameSession определит по UUID
        
        // Позиция руки: справа в нижнем углу ЭКРАНА КЛИЕНТА (не окна игры)
        // Позиция справа: от правого края ЭКРАНА с отступом
        // Рассчитываем размеры карт для расчета размеров окна (адаптивно под размер экрана)
        int baseCardWidth = 60;
        int baseCardHeight = 90;
        float scale = Math.max(0.8f, Math.min(1.2f, (float)this.width / 1000.0f));
        int cardWidth = (int)(baseCardWidth * scale);
        int cardHeight = (int)(baseCardHeight * scale);
        int cardSpacing = cardWidth + 15; // Отступ между картами
        
        // Размеры окна колоды (адаптивно под разрешение экрана)
        int deckWindowPadding = Math.max(10, Math.min(20, (int)(this.width * 0.015))); // 1.5% от ширины экрана, но не менее 10 и не более 20
        int headerHeight = Math.max(18, Math.min(24, (int)(this.height * 0.025))); // 2.5% от высоты экрана, но не менее 18 и не более 24
        int bottomPadding = Math.max(8, Math.min(12, (int)(this.height * 0.012))); // 1.2% от высоты экрана, но не менее 8 и не более 12
        
        // Высота окна колоды: заголовок + карты + отступы (адаптивно, но с учетом реальных размеров карт)
        int deckWindowHeight = deckWindowPadding + headerHeight + cardHeight + bottomPadding + deckWindowPadding;
        
        // Позиция руки: справа внизу, с минимальным отступом от нижнего края ЭКРАНА (опускаем руку ниже)
        // Отступ от нижнего края: минимум 3 пикселя
        int bottomGap = Math.max(3, Math.min(5, (int)(this.height * 0.003))); // 0.3% от высоты экрана, но не менее 3 и не более 5
        int handY = this.height - deckWindowHeight - bottomGap;
        
        // Рассчитываем необходимую ширину для 5 карт с отступами (убираем лишний запас справа)
        int requiredWidth = (5 * cardSpacing) + (deckWindowPadding * 2); // 5 карт + отступы слева и справа
        
        // Отступ от правого края ЭКРАНА (адаптивно, такой же как слева - deckWindowPadding)
        int rightOffset = deckWindowPadding; // Используем тот же отступ, что и слева
        
        // Ширина окна колоды должна быть достаточной для 5 карт, но не выходить за правый край ЭКРАНА
        int deckWindowWidth = Math.max(requiredWidth, Math.min(requiredWidth, this.width - rightOffset));
        
        // Позиция окна колоды: строго справа внизу ЭКРАНА КЛИЕНТА (от правого края экрана с отступом)
        int deckWindowX = this.width - deckWindowWidth - rightOffset;
        int deckWindowY = handY; // Окно начинается с handY, без смещения вверх
        
        // Начальная позиция карт внутри окна
        int cardsStartX = deckWindowX + deckWindowPadding;
        
        // Рисуем окно колоды с текстурой, если доступна
        if (CardTextures.textureExists(GameTextures.PLAYER_DECK)) {
            guiGraphics.blit(GameTextures.PLAYER_DECK, deckWindowX, deckWindowY, 0, 0, deckWindowWidth, deckWindowHeight, deckWindowWidth, deckWindowHeight);
        } else {
            // Fallback: простой фон
            int deckWindowBgColor = 0xD0080808;
            GuiUtils.drawRoundedRect(guiGraphics, deckWindowX, deckWindowY, deckWindowWidth, deckWindowHeight, deckWindowBgColor);
            GuiUtils.drawRoundedBorder(guiGraphics, deckWindowX, deckWindowY, deckWindowWidth, deckWindowHeight, 0xFF8B7355, 2);
        }
        
        // Заголовок руки с количеством карт (внутри окна)
        guiGraphics.drawString(this.font, 
            Component.literal("§eРука: §f" + hand.size()), 
            deckWindowX + deckWindowPadding, deckWindowY + deckWindowPadding, 0xFFFFFF, false);
        
        // Позиция карт внутри окна (ниже заголовка)
        int cardsStartY = deckWindowY + deckWindowPadding + headerHeight; // Ниже заголовка
        
        // Максимум 5 карт на экране
        int maxVisibleCards = 5;
        int startIndex = 0;
        
        // Логика сдвижения: если карт больше 5, сдвигаем влево, чтобы освободить место для 5-й карты
        if (hand.size() > maxVisibleCards) {
            // Если есть выбранная карта, стараемся показать её и следующие карты
            if (selectedCardIndex >= 0 && selectedCardIndex < hand.size()) {
                // Показываем выбранную карту и следующие, но не больше 5
                // Сдвигаем влево, чтобы показать выбранную карту и следующие
                startIndex = Math.max(0, Math.min(selectedCardIndex, hand.size() - maxVisibleCards));
            } else {
                // Показываем последние 5 карт (сдвигаем влево, чтобы освободить место для 5-й карты)
                startIndex = hand.size() - maxVisibleCards;
            }
        } else {
            // Если карт 5 или меньше, показываем все с начала
            startIndex = 0;
        }
        
        // Отображаем максимум 5 карт (с обрезкой по окну)
        int endIndex = Math.min(startIndex + maxVisibleCards, hand.size());
        
        // Включаем обрезку (scissor) для окна колоды
        int scissorX = deckWindowX;
        int scissorY = deckWindowY;
        int scissorWidth = deckWindowWidth;
        int scissorHeight = deckWindowHeight;
        
        // Устанавливаем область обрезки
        guiGraphics.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Определяем наведённую карту за один проход
        int hoveredCardIndex = -1;
        int hoverOffset = Math.max(6, (int)(GUI_HEIGHT * 0.015));
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Объединяем поиск наведённой карты и рендеринг в один цикл
        for (int i = startIndex; i < endIndex; i++) {
            Card card = hand.get(i);
            int displayIndex = i - startIndex;
            int cardX = cardsStartX + (displayIndex * cardSpacing);
            int cardY = cardsStartY;
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверка видимости карты
            if (cardX + cardWidth < 0 || cardX > this.width || 
                cardY + cardHeight < 0 || cardY > this.height) {
                continue; // Карта не видна - пропускаем
            }
            
            // Проверяем наведение (только для видимых карт)
            int adjustedCardY = cardY - hoverOffset;
            boolean isHovered = (hoveredCardIndex == -1) && // Проверяем только если ещё не нашли
                              mouseX >= cardX && mouseX <= cardX + cardWidth && 
                              mouseY >= adjustedCardY && mouseY <= adjustedCardY + cardHeight;
            
            if (isHovered) {
                hoveredCardIndex = i;
                selectedCardIndex = i; // Сохраняем реальный индекс карты только при наведении
                // Воспроизводим звук наведения для редких карт (только один раз)
                com.bmfalkye.client.sounds.SoundEffectManager.playCardHoverSound(card.getRarity());
            }
            
            // Анимация при наведении (адаптивно)
            float offsetY = isHovered ? -hoverOffset : 0;
            int finalCardY = (int)(cardY + offsetY);
            
            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Рендерим карту с tooltip только для наведённой
            Integer effectivePower = session != null ? session.getEffectivePower(card) : null;
            SimpleCardRenderer.renderCard(guiGraphics, this.font, card, cardX, finalCardY, cardWidth, cardHeight,
                isHovered ? mouseX : -1, isHovered ? mouseY : -1, 
                i == selectedCardIndex, isHovered, effectivePower); // showTooltip только для наведённой
        }
        
        // Сбрасываем selectedCardIndex, если курсор не наведен на карту
        if (hoveredCardIndex == -1) {
            selectedCardIndex = -1;
        }
        
        // Отключаем обрезку
        guiGraphics.disableScissor();
        
        // Показываем руку противника (рубашками или показанными картами) вверху, смещена правее (адаптивно)
        int opponentHandSize = session.getOpponentHandSize();
        if (opponentHandSize > 0) {
            int opponentHandY = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.06)); // 6% от высоты окна
            // Смещаем правее, чтобы не заступать на поле (адаптивно)
            int opponentHandStartX = guiX + GUI_WIDTH - Math.max(180, Math.min(220, (int)(GUI_WIDTH * 0.25))); // 25% от ширины окна
            
            // Используем текстуру колоды оппонента, если доступна
            int opponentDeckWidth = Math.max(200, Math.min(250, (int)(GUI_WIDTH * 0.3))); // 30% от ширины окна
            int opponentDeckHeight = Math.max(60, Math.min(80, (int)(GUI_HEIGHT * 0.1))); // 10% от высоты окна
            boolean hasOpponentDeckTexture = CardTextures.textureExists(GameTextures.OPPONENT_DECK);
            
            if (hasOpponentDeckTexture) {
                // Если есть текстура, рендерим только её (без отдельных карт и рубашек)
                guiGraphics.blit(GameTextures.OPPONENT_DECK, opponentHandStartX, opponentHandY, 0, 0, opponentDeckWidth, opponentDeckHeight, opponentDeckWidth, opponentDeckHeight);
                
                // Текст с количеством карт поверх текстуры
                String handText = "§cРука опп: §f" + opponentHandSize;
                guiGraphics.drawString(this.font, 
                    Component.literal(handText), 
                    opponentHandStartX + (opponentDeckWidth / 2) - (this.font.width(handText) / 2), 
                    opponentHandY + (opponentDeckHeight / 2) - 4, 0xFFFFFF, false);
            } else {
                // Если текстуры нет, рендерим карты и рубашки как обычно
                String handText = "§cРука опп: §f" + opponentHandSize;
                guiGraphics.drawString(this.font, 
                    Component.literal(handText), 
                    opponentHandStartX, opponentHandY - 15, 0xFFFFFF, false);
            
                // Получаем показанные карты оппонента
                List<String> revealedCards = session.getRevealedCards();
                List<String> opponentHandIds = session.getOpponentHandIds();
                
                // Рендерим карты противника (показанные карты или рубашки)
                // Максимум 5 карт на экране (адаптивно для маленьких экранов)
                int baseMaxVisibleOpponentCards = 5;
                int maxVisibleOpponentCards = com.bmfalkye.client.gui.SmallScreenOptimizer.isVerySmallScreen(this) ? 3 : baseMaxVisibleOpponentCards;
                // Адаптивные размеры карт противника
                float baseOpponentCardScale = Math.max(0.6f, Math.min(0.9f, (float)this.width / 1200.0f));
                float opponentCardScale = (float)(baseOpponentCardScale * com.bmfalkye.client.gui.SmallScreenOptimizer.getScale(this));
                int baseOpponentCardWidth = (int)(32 * opponentCardScale);
                int baseOpponentCardHeight = (int)(48 * opponentCardScale);
                int opponentCardWidth = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardWidth(this, baseOpponentCardWidth);
                int opponentCardHeight = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardHeight(this, baseOpponentCardHeight);
                int opponentCardSpacing = opponentCardWidth + Math.max(3, Math.min(5, (int)(this.width * 0.005))); // Адаптивный отступ
                int opponentStartIndex = Math.max(0, opponentHandSize - maxVisibleOpponentCards); // Показываем последние 5
                
                // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Объединяем рендеринг показанных карт и рубашек
                int revealedCardCount = 0;
                for (String cardId : revealedCards) {
                    if (revealedCardCount >= maxVisibleOpponentCards) break;
                    if (opponentHandIds.contains(cardId)) {
                        int displayIndex = revealedCardCount;
                        int cardX = opponentHandStartX + (displayIndex * opponentCardSpacing);
                        
                        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверка видимости
                        if (cardX + opponentCardWidth < 0 || cardX > this.width) {
                            revealedCardCount++; // Считаем, но не рендерим
                            continue;
                        }
                        
                        Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                        if (card != null) {
                            Integer effectivePower = session.getEffectivePower(card);
                            // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Не показываем tooltip для карт противника (экономия производительности)
                            SimpleCardRenderer.renderCard(guiGraphics, this.font, card, 
                                cardX, opponentHandY, opponentCardWidth, opponentCardHeight, 
                                -1, -1, false, false, effectivePower); // showTooltip=false
                            revealedCardCount++;
                        }
                    }
                }
                
                // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Рендерим рубашки только для видимых слотов
                int remainingSlots = maxVisibleOpponentCards - revealedCardCount;
                for (int i = 0; i < remainingSlots && (opponentStartIndex + revealedCardCount + i) < opponentHandSize; i++) {
                    int displayIndex = revealedCardCount + i;
                    int cardX = opponentHandStartX + (displayIndex * opponentCardSpacing);
                    
                    // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверка видимости
                    if (cardX + opponentCardWidth < 0 || cardX > this.width) {
                        continue; // Рубашка не видна - пропускаем
                    }
                    
                    renderCardBack(guiGraphics, cardX, opponentHandY, opponentCardWidth, opponentCardHeight);
                }
            }
        }
        }
        
    /**
     * Рендерит рубашку карты (для карт противника) - пустой слот с вопросительным знаком
     */
    private void renderCardBack(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Пустой слот как на скриншоте 3: темно-серый фон, светло-коричневая рамка, белый вопросительный знак, горизонтальная линия
        
        // Темно-серый фон (как на скриншоте)
        guiGraphics.fill(x, y, x + width, y + height, 0xFF2A2A2A);
        
        // Светло-коричневая рамка
        int borderColor = 0xFF8B7355; // Светло-коричневый цвет
        int borderThickness = 1;
        guiGraphics.fill(x, y, x + width, y + borderThickness, borderColor);
        guiGraphics.fill(x, y + height - borderThickness, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + borderThickness, y + height, borderColor);
        guiGraphics.fill(x + width - borderThickness, y, x + width, y + height, borderColor);
        
        // Тонкая горизонтальная линия через середину (как на скриншоте)
        int middleY = y + height / 2;
        guiGraphics.fill(x + 2, middleY, x + width - 2, middleY + 1, borderColor);
        
        // Белый вопросительный знак по центру (пиксельный стиль)
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§f?"), 
            x + width / 2, middleY - 4, 0xFFFFFF);
    }

    private void renderCardInHand(GuiGraphics guiGraphics, Card card, int x, int y, int mouseX, int mouseY, boolean selected) {
        // Рендерим карту в руке в стиле редактора колод (адаптивный размер)
        // Базовый размер увеличен для размещения длинных названий
        int baseWidth = 60;
        int baseHeight = 90;
        
        // Адаптивное масштабирование в зависимости от размера экрана
        // Используем оптимизатор для маленьких экранов
        float baseScale = Math.max(0.8f, Math.min(1.2f, (float)this.width / 1000.0f));
        float scale = (float)(baseScale * com.bmfalkye.client.gui.SmallScreenOptimizer.getScale(this));
        int width = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardWidth(this, (int)(baseWidth * scale));
        int height = com.bmfalkye.client.gui.SmallScreenOptimizer.getCardHeight(this, (int)(baseHeight * scale));
        
        // Получаем эффективную силу карты из сессии
        Integer effectivePower = session != null ? session.getEffectivePower(card) : null;
        
        // Используем общий метод отрисовки карт
        // Если mouseX/mouseY = -1, тултип не показывается (будет показан отдельно)
        boolean showTooltip = mouseX >= 0 && mouseY >= 0;
        SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, width, height, 
                               mouseX, mouseY, selected, showTooltip, effectivePower);
    }
    

    private void renderCardOnField(GuiGraphics guiGraphics, Card card, int x, int y, int mouseX, int mouseY) {
        // Не показываем тултипы, если игра закончилась (окно победы/поражения должно быть поверх)
        boolean gameEnded = session != null && session.isGameEnded();
        
        // Рендерим карту на поле в стиле редактора колод (адаптивный размер)
        // Базовый размер увеличен для размещения длинных названий
        int baseWidth = 48;
        int baseHeight = 68;
        
        // Адаптивное масштабирование в зависимости от размера экрана
        float scale = Math.max(0.7f, Math.min(1.1f, (float)this.width / 1000.0f));
        int width = (int)(baseWidth * scale);
        int height = (int)(baseHeight * scale);
        
        // Получаем эффективную силу карты из сессии
        Integer effectivePower = session != null ? session.getEffectivePower(card) : null;
        
        // Используем общий метод отрисовки карт
        // Не показываем тултип, если игра закончилась
        SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, width, height, 
                               mouseX, mouseY, false, !gameEnded, effectivePower);
    }
    
    /**
     * Рендерит карту на поле с анимацией (с учетом альфа-канала и масштаба)
     * @param showTooltip показывать ли tooltip при наведении (не показываем во время активной анимации)
     */
    private void renderCardOnFieldAnimated(GuiGraphics guiGraphics, Card card, int x, int y, 
                                          int width, int height, int mouseX, int mouseY, float alpha, boolean showTooltip) {
        // Рендерим карту с учетом альфа-канала
        // CardRenderer не поддерживает альфа напрямую, поэтому используем обходной путь
        // через временное изменение цвета рендеринга
        
        // Сохраняем текущий цвет
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        
        // Получаем эффективную силу карты из сессии
        Integer effectivePower = session != null ? session.getEffectivePower(card) : null;
        
        // Не показываем tooltip, если игра закончилась (окно победы/поражения должно быть поверх)
        boolean gameEnded = session != null && session.isGameEnded();
        boolean shouldShowTooltip = showTooltip && !gameEnded;
        
        // Рендерим карту с tooltip при наведении
        SimpleCardRenderer.renderCard(guiGraphics, this.font, card, x, y, width, height, 
                               mouseX, mouseY, false, shouldShowTooltip, effectivePower);
        
        // Восстанавливаем цвет
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderLeader(GuiGraphics guiGraphics) {
        // Рендерим карту лидера (полностью адаптивно)
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        boolean isPlayer1 = currentPlayerUUID != null && currentPlayerUUID.equals(session.getPlayer1UUID());
        var leader = session.getLeader(null); // ClientFalkyeGameSession определит по UUID
        
        if (leader != null) {
            // Лидер справа ЗА полем, под информационной панелью и логом (адаптивно)
            int infoPanelHeight = Math.max(90, Math.min(110, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
            int x = guiX + GUI_WIDTH + 10; // Справа от игрового окна (адаптивно)
            int y = guiY + Math.max(40, (int)(GUI_HEIGHT * 0.05)) + infoPanelHeight + 10 + logHeight + Math.max(8, (int)(GUI_HEIGHT * 0.01)); // Под логом действий (адаптивно)
            
            // Фон карты лидера (адаптивно)
            int leaderWidth = logWidth; // Используем ширину лога
            int leaderHeight = Math.max(45, Math.min(55, (int)(GUI_HEIGHT * 0.07))); // 7% от высоты окна
            int leaderColor = leader.isUsed() ? 0xFF666666 : 0xFF8B7355;
            GuiUtils.drawRoundedRect(guiGraphics, x, y, leaderWidth, leaderHeight, 0xE0101010);
            GuiUtils.drawRoundedBorder(guiGraphics, x, y, leaderWidth, leaderHeight, leaderColor, 2);
            
            // Внутренний фон
            guiGraphics.fill(x + 2, y + 2, x + leaderWidth - 2, y + leaderHeight - 2, 0xFF4A3A2A);
            
            // Название лидера (компактно)
            String leaderName = leader.getName();
            if (this.font.width(leaderName) > leaderWidth - 20) {
                leaderName = this.font.plainSubstrByWidth(leaderName, leaderWidth - 25) + "...";
            }
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§6" + leaderName), 
                x + leaderWidth / 2, y + 8, leader.isUsed() ? 0xFF888888 : 0xFFFFFF);
            
            // Описание способности (компактно)
            String ability = leader.getAbility();
            if (this.font.width(ability) > leaderWidth - 20) {
                ability = this.font.plainSubstrByWidth(ability, leaderWidth - 25) + "...";
            }
            guiGraphics.drawCenteredString(this.font, 
                Component.literal("§7" + ability), 
                x + leaderWidth / 2, y + 20, 0xFFFFFF);
            
            // Статус (компактно)
            String status = leader.isUsed() ? "§c✗" : "§a✓";
            guiGraphics.drawCenteredString(this.font, 
                Component.literal(status), 
                x + leaderWidth / 2, y + 35, 0xFFFFFF);
        }
    }

    private boolean isCurrentPlayer(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || minecraft == null || minecraft.player == null) {
            return false;
        }
        return player.getUUID().equals(minecraft.player.getUUID());
    }

    public void updateGameState(ClientFalkyeGameSession newSession) {
        // Обновляем лог действий при изменении состояния
        if (this.session != null && newSession != null) {
            UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
                net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
            
            // Проверяем, изменился ли ход
            UUID oldCurrentPlayer = this.session.getCurrentPlayerUUID();
            UUID newCurrentPlayer = newSession.getCurrentPlayerUUID();
            if (oldCurrentPlayer != null && newCurrentPlayer != null && !oldCurrentPlayer.equals(newCurrentPlayer)) {
                if (currentPlayerUUID != null && newCurrentPlayer.equals(currentPlayerUUID)) {
                    addActionLog("§aВаш ход!");
                    com.bmfalkye.client.sounds.SoundEffectManager.playTurnStartSound();
                } else {
                    addActionLog("§cХод противника");
                }
            }
            
            // Проверяем новые карты на поле (для анимации)
            checkNewCardsOnField(this.session, newSession);
            
            // Обнаруживаем сброс карт и использование карт способности
            // ВАЖНО: Вызываем ДО обновления session, чтобы previousSession был правильным
            detectAndAnimateCardPlaysForSession(this.session, newSession);
            
            // Проверяем изменения силы карт (для визуальных эффектов способностей)
            checkPowerChanges(this.session, newSession);
            
            // Проверяем комбо-эффекты (большие изменения силы могут указывать на комбо)
            checkComboEffects(this.session, newSession);
            
            // Синхронизируем визуальные эффекты через менеджер синхронизации
            com.bmfalkye.client.network.VisualEffectSyncManager.getInstance()
                .syncGameStateEffects(this.session, newSession, effectManager, guiX, guiY, GUI_WIDTH, GUI_HEIGHT);
            
            // Проверяем изменение погоды
            if (this.session.getWeather() != newSession.getWeather()) {
                com.bmfalkye.client.network.VisualEffectSyncManager.getInstance()
                    .syncWeatherEffects(this.session.getWeather(), newSession.getWeather());
            }
        }
        
        // Проверяем, закончилась ли игра - если да, устанавливаем таймер закрытия
        if (newSession.isGameEnded() && !this.session.isGameEnded()) {
            // Игра только что закончилась - устанавливаем таймер закрытия GUI
            gameEndTime = System.currentTimeMillis();
        }
        
        // Сохраняем предыдущее состояние ПЕРЕД обновлением для отслеживания изменений
        if (this.session != null) {
            previousSession = this.session; // Используем текущую сессию как предыдущую
        }
        
        this.session = newSession;
        
        // Синхронизируем локальный таймер с сервером при обновлении состояния игры
        // Это критично для предотвращения зависаний и рассинхронизации
        int serverRemainingTime = newSession.getRemainingTime();
        UUID newCurrentPlayerUUID = newSession.getCurrentPlayerUUID();
        
        // Всегда синхронизируем с сервером при обновлении состояния
        // Это гарантирует, что таймер не будет зависать
        if (newCurrentPlayerUUID != null) {
            if (newCurrentPlayerUUID.equals(lastCurrentPlayerUUID)) {
                // Тот же игрок - синхронизируем таймер с сервером (исправляем возможную рассинхронизацию)
                localRemainingTime = serverRemainingTime;
                lastTimerUpdate = System.currentTimeMillis();
            } else {
                // Сменился игрок - сбрасываем таймер
                localRemainingTime = serverRemainingTime;
                lastTimerUpdate = System.currentTimeMillis();
                lastCurrentPlayerUUID = newCurrentPlayerUUID;
            }
        } else {
            // Нет текущего игрока - сбрасываем
            localRemainingTime = 0;
            lastTimerUpdate = System.currentTimeMillis();
            lastCurrentPlayerUUID = null;
        }
        
        selectedCard = null; // Сбрасываем выбор при обновлении
        selectedCardIndex = -1;
    }
    
    /**
     * Проверяет новые карты на поле и создает для них анимации
     */
    private void checkNewCardsOnField(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        if (newSession == null) return;
        
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        if (currentPlayerUUID == null) return;
        
        // Проверяем карты игрока
        List<Card> oldMelee = oldSession != null ? oldSession.getMeleeRow(null) : new java.util.ArrayList<>();
        List<Card> newMelee = newSession.getMeleeRow(null);
        List<Card> oldRanged = oldSession != null ? oldSession.getRangedRow(null) : new java.util.ArrayList<>();
        List<Card> newRanged = newSession.getRangedRow(null);
        List<Card> oldSiege = oldSession != null ? oldSession.getSiegeRow(null) : new java.util.ArrayList<>();
        List<Card> newSiege = newSession.getSiegeRow(null);
        
        // Находим новые карты и создаем анимации для игрока
        createAnimationsForNewCards(oldMelee, newMelee, "melee", false);
        createAnimationsForNewCards(oldRanged, newRanged, "ranged", false);
        createAnimationsForNewCards(oldSiege, newSiege, "siege", false);
        
        // Проверяем карты оппонента - ВАЖНО: проверяем всегда
        List<Card> oldOpponentMelee = oldSession != null ? oldSession.getOpponentMeleeRow() : new java.util.ArrayList<>();
        List<Card> newOpponentMelee = newSession.getOpponentMeleeRow();
        List<Card> oldOpponentRanged = oldSession != null ? oldSession.getOpponentRangedRow() : new java.util.ArrayList<>();
        List<Card> newOpponentRanged = newSession.getOpponentRangedRow();
        List<Card> oldOpponentSiege = oldSession != null ? oldSession.getOpponentSiegeRow() : new java.util.ArrayList<>();
        List<Card> newOpponentSiege = newSession.getOpponentSiegeRow();
        
        // Находим новые карты и создаем анимации для оппонента
        createAnimationsForNewCards(oldOpponentMelee, newOpponentMelee, "opponent_melee", true);
        createAnimationsForNewCards(oldOpponentRanged, newOpponentRanged, "opponent_ranged", true);
        createAnimationsForNewCards(oldOpponentSiege, newOpponentSiege, "opponent_siege", true);
    }
    
    /**
     * Проверяет изменения силы карт и создает визуальные эффекты
     * ВАЖНО: Проверяет изменения для ВСЕХ карт на поле, чтобы оба игрока видели изменения
     */
    private void checkPowerChanges(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        if (oldSession == null || newSession == null) return;
        
        // Проверяем все ряды игрока (чтобы видеть изменения своих карт)
        checkPowerChangesInRow(oldSession.getMeleeRow(null), newSession.getMeleeRow(null), "melee", oldSession, newSession);
        checkPowerChangesInRow(oldSession.getRangedRow(null), newSession.getRangedRow(null), "ranged", oldSession, newSession);
        checkPowerChangesInRow(oldSession.getSiegeRow(null), newSession.getSiegeRow(null), "siege", oldSession, newSession);
        
        // Проверяем ряды противника (чтобы видеть изменения карт противника)
        checkPowerChangesInRow(oldSession.getOpponentMeleeRow(), newSession.getOpponentMeleeRow(), "opponent_melee", oldSession, newSession);
        checkPowerChangesInRow(oldSession.getOpponentRangedRow(), newSession.getOpponentRangedRow(), "opponent_ranged", oldSession, newSession);
        checkPowerChangesInRow(oldSession.getOpponentSiegeRow(), newSession.getOpponentSiegeRow(), "opponent_siege", oldSession, newSession);
    }
    
    /**
     * Проверяет изменения силы карт в ряду и создает визуальные эффекты
     */
    private void checkPowerChangesInRow(List<Card> oldCards, List<Card> newCards, String rowType, 
                                       ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        // Создаем карту для быстрого поиска старых карт
        java.util.Map<String, Card> oldCardsMap = new java.util.HashMap<>();
        for (Card card : oldCards) {
            oldCardsMap.put(card.getId(), card);
        }
        
        // Проверяем каждую карту в новом списке
        for (Card newCard : newCards) {
            Card oldCard = oldCardsMap.get(newCard.getId());
            if (oldCard != null) {
                // Карта уже была на поле, проверяем изменение силы
                Integer oldEffectivePower = oldSession.getEffectivePower(oldCard);
                int oldPower = oldEffectivePower != null ? oldEffectivePower : oldCard.getPower();
                
                Integer newEffectivePower = newSession.getEffectivePower(newCard);
                int newPower = newEffectivePower != null ? newEffectivePower : newCard.getPower();
                
                if (oldPower != newPower) {
                    // Сила изменилась - создаем визуальный эффект
                    createPowerChangeEffect(newCard, rowType, oldPower, newPower, newSession);
                }
            }
        }
    }
    
    /**
     * Проверяет комбо-эффекты и создает визуальные эффекты
     * ВАЖНО: Проверяет комбо для всех карт на поле, чтобы оба игрока видели эффекты
     */
    private void checkComboEffects(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        if (oldSession == null || newSession == null) return;
        
        // Проверяем все ряды игрока на большие изменения силы (может быть комбо)
        // ВАЖНО: Учитываем все карты на поле (игрока и противника) с эффективной силой
        int totalOldPower = calculateTotalPower(oldSession, null) + calculateOpponentTotalPower(oldSession);
        int totalNewPower = calculateTotalPower(newSession, null) + calculateOpponentTotalPower(newSession);
        
        int powerDiff = totalNewPower - totalOldPower;
        
        // Если изменение силы больше 5, это может быть комбо
        if (powerDiff > 5) {
            // Определяем тип комбо по количеству карт
            // ВАЖНО: Проверяем все карты на поле (игрока и противника), чтобы видеть комбо обоих
            List<Card> allCards = new java.util.ArrayList<>();
            allCards.addAll(newSession.getMeleeRow(null));
            allCards.addAll(newSession.getRangedRow(null));
            allCards.addAll(newSession.getSiegeRow(null));
            allCards.addAll(newSession.getOpponentMeleeRow());
            allCards.addAll(newSession.getOpponentRangedRow());
            allCards.addAll(newSession.getOpponentSiegeRow());
            
            // Проверяем комбо фракции
            java.util.Map<String, Integer> factionCount = new java.util.HashMap<>();
            for (Card card : allCards) {
                factionCount.put(card.getFaction(), 
                    factionCount.getOrDefault(card.getFaction(), 0) + 1);
            }
            
            String comboType = "power";
            int comboLevel = Math.min(powerDiff / 5, 5);
            
            for (java.util.Map.Entry<String, Integer> entry : factionCount.entrySet()) {
                if (entry.getValue() >= 3) {
                    comboType = "faction";
                    comboLevel = entry.getValue();
                    break;
                }
            }
            
            // Проверяем легендарные карты
            long legendaryCount = allCards.stream()
                .filter(c -> c.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY)
                .count();
            if (legendaryCount >= 2) {
                comboType = "legendary";
                comboLevel = (int) legendaryCount;
            }
            
            // Создаем визуальный эффект комбо (отображается обоим игрокам)
            float centerX = guiX + GUI_WIDTH / 2.0f;
            float centerY = guiY + GUI_HEIGHT / 2.0f;
            effectManager.playComboEffect(centerX, centerY, comboType, comboLevel);
            
            // Воспроизводим звук комбо
            com.bmfalkye.client.sounds.SoundEffectManager.playComboSound(comboType, comboLevel);
        }
    }
    
    /**
     * Вычисляет общую силу всех карт игрока
     */
    private int calculateTotalPower(ClientFalkyeGameSession session, net.minecraft.server.level.ServerPlayer player) {
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        int total = 0;
        for (Card card : melee) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (Card card : ranged) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (Card card : siege) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        return total;
    }
    
    /**
     * Создает визуальный эффект изменения силы карты
     */
    private void createPowerChangeEffect(Card card, String rowType, int oldPower, int newPower, ClientFalkyeGameSession session) {
        // Воспроизводим звук изменения силы
        com.bmfalkye.client.sounds.SoundEffectManager.playPowerChangeSound(oldPower, newPower);
        // Определяем позицию карты на поле (адаптивно)
        int infoWidth = Math.max(120, Math.min(180, (int)(GUI_WIDTH * 0.18))); // 18% от ширины окна
        int rowStartX = guiX + Math.max(50, infoWidth + Math.max(15, (int)(GUI_WIDTH * 0.02))); // 2% от ширины окна
        int myY = guiY + GUI_HEIGHT / 2 + Math.max(3, (int)(GUI_HEIGHT * 0.008)); // 0.8% от высоты окна
        int opponentY = guiY + Math.max(70, (int)(GUI_HEIGHT * 0.12)); // 12% от высоты окна
        int rowSpacing = Math.max(50, Math.min(65, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна (увеличено с 8%)
        int overlapOffset = Math.max(8, Math.min(12, (int)(GUI_HEIGHT * 0.015))); // 1.5% от высоты окна для наложения
        
        int y = switch (rowType) {
            case "melee" -> myY;
            case "ranged" -> myY + rowSpacing - overlapOffset;
            case "siege" -> myY + rowSpacing * 2 - overlapOffset * 2;
            case "opponent_melee" -> opponentY;
            case "opponent_ranged" -> opponentY + rowSpacing - overlapOffset;
            case "opponent_siege" -> opponentY + rowSpacing * 2 - overlapOffset * 2;
            default -> myY;
        };
        
        // Находим индекс карты в ряду
        List<Card> rowCards = switch (rowType) {
            case "melee" -> session.getMeleeRow(null);
            case "ranged" -> session.getRangedRow(null);
            case "siege" -> session.getSiegeRow(null);
            case "opponent_melee" -> session.getOpponentMeleeRow();
            case "opponent_ranged" -> session.getOpponentRangedRow();
            case "opponent_siege" -> session.getOpponentSiegeRow();
            default -> new java.util.ArrayList<>();
        };
        
        int cardIndex = -1;
        for (int i = 0; i < rowCards.size(); i++) {
            if (rowCards.get(i).getId().equals(card.getId())) {
                cardIndex = i;
                break;
            }
        }
        
        if (cardIndex >= 0) {
            // Вычисляем позицию карты
            int baseCardWidth = 48;
            float fieldScale = Math.max(0.7f, Math.min(1.1f, (float)this.width / 1000.0f));
            int cardWidth = (int)(baseCardWidth * fieldScale);
            int cardSpacing = cardWidth + 25;
            int cardX = rowStartX + (cardIndex * cardSpacing);
            int cardY = y;
            
            // Создаем эффект изменения силы
            float effectX = cardX + cardWidth / 2.0f;
            float effectY = cardY + 20.0f; // Позиция силы на карте
            
            // Создаем эффект изменения силы через GuiEffectManager
            effectManager.playPowerChangeEffect(effectX, effectY, oldPower, newPower);
        }
    }
    
    /**
     * Создает анимации для новых карт в ряду
     */
    private void createAnimationsForNewCards(List<Card> oldCards, List<Card> newCards, String rowType, boolean isOpponent) {
        // Создаем множества для быстрого поиска
        java.util.Set<String> oldCardIds = new java.util.HashSet<>();
        for (Card oldCard : oldCards) {
            oldCardIds.add(oldCard.getId());
        }
        
        // Находим карты, которые есть в новом списке, но нет в старом
        // ВАЖНО: сравниваем количество карт с одинаковым ID, а не просто наличие
        java.util.Map<String, Integer> oldCardCounts = new java.util.HashMap<>();
        for (Card oldCard : oldCards) {
            oldCardCounts.put(oldCard.getId(), oldCardCounts.getOrDefault(oldCard.getId(), 0) + 1);
        }
        
        java.util.Map<String, Integer> newCardCounts = new java.util.HashMap<>();
        for (Card newCard : newCards) {
            newCardCounts.put(newCard.getId(), newCardCounts.getOrDefault(newCard.getId(), 0) + 1);
        }
        
        // Находим новые карты (когда количество увеличилось)
        for (int i = 0; i < newCards.size(); i++) {
            Card newCard = newCards.get(i);
            int oldCount = oldCardCounts.getOrDefault(newCard.getId(), 0);
            int newCount = newCardCounts.get(newCard.getId());
            
            // Если количество карт с таким ID увеличилось, это новая карта
            // Но нужно определить, какая именно карта новая (берем первую, которой нет в старом списке)
            boolean isNew = false;
            if (newCount > oldCount) {
                // Подсчитываем, сколько карт с таким ID уже было обработано
                int processedCount = 0;
                for (int j = 0; j < i; j++) {
                    if (newCards.get(j).getId().equals(newCard.getId())) {
                        processedCount++;
                    }
                }
                // Если обработано меньше, чем было в старом списке, это не новая карта
                // Если обработано >= старого количества, это новая карта
                isNew = processedCount >= oldCount;
            } else if (!oldCardIds.contains(newCard.getId())) {
                // Если карты с таким ID вообще не было, это новая карта
                isNew = true;
            }
            
            if (isNew) {
                int cardIndex = i; // Используем текущий индекс вместо indexOf
                
                // Вычисляем позицию карты на поле (должно совпадать с renderRow)
                int rowStartX = guiX + Math.max(50, 150 + 20);
                int myY = guiY + GUI_HEIGHT / 2 + 5;
                int opponentY = guiY + Math.max(70, (int)(GUI_HEIGHT * 0.12)); // 12% от высоты окна
                int rowSpacing = Math.max(50, Math.min(65, (int)(GUI_HEIGHT * 0.12))); // 12% от высоты окна
                int overlapOffset = Math.max(8, Math.min(12, (int)(GUI_HEIGHT * 0.015))); // 1.5% от высоты окна для наложения
                
                int y;
                if (isOpponent) {
                    // Позиции для карт оппонента
                    y = switch (rowType) {
                        case "opponent_melee" -> opponentY;
                        case "opponent_ranged" -> opponentY + rowSpacing - overlapOffset;
                        case "opponent_siege" -> opponentY + rowSpacing * 2 - overlapOffset * 2;
                        default -> opponentY;
                    };
                } else {
                    // Позиции для карт игрока
                    y = switch (rowType) {
                        case "melee" -> myY;
                        case "ranged" -> myY + rowSpacing - overlapOffset;
                        case "siege" -> myY + rowSpacing * 2 - overlapOffset * 2;
                        default -> myY;
                    };
                }
                // Адаптивный отступ между картами (совпадает с renderRow)
                int baseCardWidth = 48;
                float fieldScale = Math.max(0.7f, Math.min(1.1f, (float)this.width / 1000.0f));
                int cardWidth = (int)(baseCardWidth * fieldScale);
                int cardSpacing = cardWidth + 25; // Увеличенный отступ для предотвращения наложения
                int cardX = rowStartX + (cardIndex * cardSpacing);
                
                // Определяем тип ряда для анимации
                FalkyeGameSession.CardRow row = switch (rowType) {
                    case "melee", "opponent_melee" -> FalkyeGameSession.CardRow.MELEE;
                    case "ranged", "opponent_ranged" -> FalkyeGameSession.CardRow.RANGED;
                    case "siege", "opponent_siege" -> FalkyeGameSession.CardRow.SIEGE;
                    default -> FalkyeGameSession.CardRow.MELEE;
                };
                
                // Вычисляем позицию руки (примерная позиция, откуда карта сбрасывается)
                // Для оппонента рука находится в верхней части экрана
                int handX = guiX + GUI_WIDTH / 2;
                int handY = isOpponent ? guiY + 100 : guiY + GUI_HEIGHT - 100;
                
                // Создаем анимацию сброса карты с эффектами
                animationManager.createDropAnimation(newCard, handX, handY, cardX, y, row);
                
                // Создаем визуальные эффекты сброса
                com.bmfalkye.client.effects.AdvancedVisualEffects.createCardDropEffect(
                    effectManager, cardX, y, newCard, row
                );
                
                // Создаем ключ для анимации (используем cardX и y для совпадения с renderRow)
                String cardKey = newCard.getId() + "_" + cardX + "_" + y;
                
                // Проверяем, нет ли уже анимации для этой карты
                if (cardAnimations.containsKey(cardKey)) {
                    continue; // Пропускаем, если анимация уже есть
                }
                
                // Анимация: карта появляется сверху с плавным увеличением и появлением
                com.bmfalkye.client.animations.CardAnimation anim = 
                    new com.bmfalkye.client.animations.CardAnimation(
                        cardX, y - 60, // Начальная позиция (выше)
                        cardX, y,      // Конечная позиция
                        com.bmfalkye.client.animations.CardAnimation.AnimationType.FADE_IN,
                        60 // Длительность 60 тиков (3 секунды) - увеличено для более плавной анимации
                    );
                cardAnimations.put(cardKey, anim);
                
                // Для карт со способностями добавляем постоянную анимацию
                if (newCard.getType() == Card.CardType.SPECIAL) {
                    com.bmfalkye.client.effects.CardPlayAnimationManager.FieldCardAnimation.AnimationStyle style = 
                        com.bmfalkye.client.effects.CardPlayAnimationManager.getAnimationStyleForCard(newCard);
                    animationManager.addFieldAnimation(newCard, cardX, y, style);
                }
            }
        }
    }
    
    /**
     * Получает цветовой код редкости для форматирования текста
     */
    private String getRarityColorCode(com.bmfalkye.cards.CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7";      // Серый
            case RARE -> "§b";        // Голубой
            case EPIC -> "§5";         // Фиолетовый
            case LEGENDARY -> "§6";    // Золотой
        };
    }
    
    /**
     * Рисует маленькую иконку редкости в углу карты
     */
    private void renderRarityIcon(GuiGraphics guiGraphics, com.bmfalkye.cards.CardRarity rarity, int x, int y, int size) {
        // Используем текстуру карты редкости как иконку (маленькая версия)
        if (CardTextures.hasCardTexture(rarity)) {
            ResourceLocation rarityTexture = CardTextures.getCardTextureByRarity(rarity);
            guiGraphics.blit(rarityTexture, x, y, 0, 0, size, size, size, size);
        } else {
            // Если текстуры нет, рисуем цветной квадрат
            int rarityColor = rarity.getColor();
            guiGraphics.fill(x, y, x + size, y + size, rarityColor);
            guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF000000);
        }
    }
    
    /**
     * Рендерит информацию об окончании игры (почему проиграл/выиграл)
     */
    private void renderGameEndInfo(GuiGraphics guiGraphics, UUID currentPlayerUUID) {
        if (currentPlayerUUID == null) return;
        
        UUID winnerUUID = session.getWinnerUUID();
        if (winnerUUID == null) return;
        
        boolean playerWon = winnerUUID.equals(currentPlayerUUID);
        int roundsWon1 = session.getRoundsWon(null);
        int roundsWon2 = session.getOpponentRoundsWon(); // Получаем очки противника правильно
        
        // Большое заметное окно с результатом игры (по центру экрана, адаптивно)
        int infoWidth = Math.min(500, (int)(this.width * 0.6));
        int infoHeight = Math.min(240, (int)(this.height * 0.4));
        int infoX = (this.width - infoWidth) / 2;
        int infoY = (this.height - infoHeight) / 2;
        
        // Полупрозрачный тёмный фон для затемнения экрана (более непрозрачный для лучшей видимости)
        // Рендерим несколько слоёв для полного затемнения
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000); // Более непрозрачный фон
        
        // Фон окна (более непрозрачный для лучшей видимости)
        GuiUtils.drawRoundedRect(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xFF1A1A1A);
        GuiUtils.drawRoundedBorder(guiGraphics, infoX, infoY, infoWidth, infoHeight, 
            playerWon ? 0xFF00FF00 : 0xFFFF0000, 4);
        
        // Внутренняя рамка для дополнительного выделения
        GuiUtils.drawRoundedBorder(guiGraphics, infoX + 2, infoY + 2, infoWidth - 4, infoHeight - 4, 
            playerWon ? 0xFF88FF88 : 0xFFFF8888, 2);
        
        // Адаптивные отступы внутри окна
        int padding = Math.max(20, Math.min(40, (int)(infoHeight * 0.15))); // 15% от высоты окна
        int lineHeight = Math.max(20, Math.min(30, (int)(infoHeight * 0.12))); // 12% от высоты окна
        
        // Заголовок (крупный текст) - адаптивно
        Component titleComponent = playerWon ? 
            Component.translatable("screen.bm_falkye.victory_title") : 
            Component.translatable("screen.bm_falkye.defeat_title");
        int titleY = infoY + padding;
        int titleX = infoX + infoWidth / 2;
        // Рисуем заголовок
        guiGraphics.drawCenteredString(this.font, titleComponent, titleX, titleY, 
            playerWon ? 0xFFFF00 : 0xFF0000);
        
        // Причина победы/поражения - адаптивно
        Component reasonComponent;
        if (playerWon) {
            reasonComponent = Component.translatable("screen.bm_falkye.victory_reason", roundsWon1);
        } else {
            reasonComponent = Component.translatable("screen.bm_falkye.defeat_reason", roundsWon2);
        }
        int reasonY = titleY + lineHeight;
        guiGraphics.drawCenteredString(this.font, reasonComponent, 
            infoX + infoWidth / 2, reasonY, 0xFFFFFF);
        
        // Счёт - адаптивно
        Component scoreComponent = Component.translatable("screen.bm_falkye.final_score", roundsWon1, roundsWon2);
        int scoreY = reasonY + lineHeight;
        guiGraphics.drawCenteredString(this.font, scoreComponent, 
            infoX + infoWidth / 2, scoreY, 0xFFFFFF);
        
        // Дополнительная информация - адаптивно
        Component infoComponent = Component.translatable("screen.bm_falkye.check_action_log");
        int infoYPos = scoreY + lineHeight;
        guiGraphics.drawCenteredString(this.font, infoComponent, 
            infoX + infoWidth / 2, infoYPos, 0xCCCCCC);
        
        // Сообщение о закрытии - адаптивно
        Component closeMsgComponent = Component.translatable("screen.bm_falkye.window_close_message");
        int closeY = infoYPos + lineHeight;
        guiGraphics.drawCenteredString(this.font, closeMsgComponent, 
            infoX + infoWidth / 2, closeY, 0xCCCCCC);
    }
    
    /**
     * Вычисляет базовые очки игрока БЕЗ модификаторов (только базовая сила карт)
     * Используется для отображения разницы между базовыми и изменёнными очками
     */
    private int calculateBaseScore(ClientFalkyeGameSession session, net.minecraft.server.level.ServerPlayer player) {
        List<Card> melee = session.getMeleeRow(player);
        List<Card> ranged = session.getRangedRow(player);
        List<Card> siege = session.getSiegeRow(player);
        
        int score = 0;
        
        // Применяем эффекты погоды (как на сервере), но используем базовую силу карт
        if (session.getWeather() == FalkyeGameSession.WeatherType.FROST) {
            score += melee.size(); // Все ближние карты считаются как 1
            score += calculateRowBasePower(ranged);
            score += calculateRowBasePower(siege);
        } else if (session.getWeather() == FalkyeGameSession.WeatherType.FOG) {
            score += calculateRowBasePower(melee);
            score += ranged.size(); // Все дальние карты считаются как 1
            score += calculateRowBasePower(siege);
        } else if (session.getWeather() == FalkyeGameSession.WeatherType.RAIN) {
            score += calculateRowBasePower(melee);
            score += calculateRowBasePower(ranged);
            score += siege.size(); // Все осадные карты считаются как 1
        } else {
            score += calculateRowBasePower(melee);
            score += calculateRowBasePower(ranged);
            score += calculateRowBasePower(siege);
        }
        
        return score;
    }
    
    /**
     * Вычисляет базовые очки противника БЕЗ модификаторов (только базовая сила карт)
     * ВАЖНО: Используется только для отображения разницы между базовыми и измененными очками
     */
    private int calculateBaseOpponentScore(ClientFalkyeGameSession session) {
        List<Card> melee = session.getOpponentMeleeRow();
        List<Card> ranged = session.getOpponentRangedRow();
        List<Card> siege = session.getOpponentSiegeRow();
        
        int score = 0;
        
        // Применяем эффекты погоды (как на сервере), но используем базовую силу карт
        if (session.getWeather() == FalkyeGameSession.WeatherType.FROST) {
            score += melee.size(); // Все ближние карты считаются как 1
            score += calculateRowBasePower(ranged);
            score += calculateRowBasePower(siege);
        } else if (session.getWeather() == FalkyeGameSession.WeatherType.FOG) {
            score += calculateRowBasePower(melee);
            score += ranged.size(); // Все дальние карты считаются как 1
            score += calculateRowBasePower(siege);
        } else if (session.getWeather() == FalkyeGameSession.WeatherType.RAIN) {
            score += calculateRowBasePower(melee);
            score += calculateRowBasePower(ranged);
            score += siege.size(); // Все осадные карты считаются как 1
        } else {
            score += calculateRowBasePower(melee);
            score += calculateRowBasePower(ranged);
            score += calculateRowBasePower(siege);
        }
        
        return score;
    }
    
    /**
     * Вычисляет базовую силу ряда БЕЗ модификаторов (только базовая сила карт)
     * ВАЖНО: Используется только для отображения разницы между базовыми и измененными очками
     */
    private int calculateRowBasePower(List<Card> row) {
        int power = 0;
        for (Card card : row) {
            power += card.getPower(); // Используем базовую силу, без модификаторов
        }
        return power;
    }
    
    /**
     * Вычисляет общую силу всех карт противника с учетом модификаторов
     * ВАЖНО: Использует эффективную силу для правильного отображения изменений
     */
    private int calculateOpponentTotalPower(ClientFalkyeGameSession session) {
        List<Card> melee = session.getOpponentMeleeRow();
        List<Card> ranged = session.getOpponentRangedRow();
        List<Card> siege = session.getOpponentSiegeRow();
        
        int total = 0;
        for (Card card : melee) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (Card card : ranged) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        for (Card card : siege) {
            Integer power = session.getEffectivePower(card);
            total += power != null ? power : card.getPower();
        }
        return total;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Не паузим игру при открытии экрана
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Запрещаем выход по Esc во время матча
        if (keyCode == 256) { // ESC key
            return true; // Блокируем выход
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Рендерит улучшенный фон игрового поля с анимированными эффектами и градиентами
     */
    // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Кэш для проверки легендарных карт
    private boolean cachedHasLegendary = false;
    private long lastLegendaryCheck = 0;
    private static final long LEGENDARY_CHECK_INTERVAL = 500; // Проверяем раз в 500мс
    
    private void renderEnhancedGameFieldBackground(GuiGraphics guiGraphics, float partialTick) {
        // Используем текстуру игрового GUI, если доступна
        if (CardTextures.textureExists(GameTextures.GAME_GUI)) {
            guiGraphics.blit(GameTextures.GAME_GUI, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            // Fallback: простой фон
            int tableColor = 0xFF3D2817;
            guiGraphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, tableColor);
            
            // Простая рамка
            int frameColor = 0xFFCD7F32;
            int frameThickness = 3;
            guiGraphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + frameThickness, frameColor);
            guiGraphics.fill(guiX, guiY + GUI_HEIGHT - frameThickness, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, frameColor);
            guiGraphics.fill(guiX, guiY, guiX + frameThickness, guiY + GUI_HEIGHT, frameColor);
            guiGraphics.fill(guiX + GUI_WIDTH - frameThickness, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, frameColor);
        }
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Свечение только при наличии легендарных карт (с кэшированием)
        if (session != null) {
            long time = System.currentTimeMillis();
            if (time - lastLegendaryCheck > LEGENDARY_CHECK_INTERVAL) {
                cachedHasLegendary = checkHasLegendaryCards();
                lastLegendaryCheck = time;
            }
            
            if (cachedHasLegendary) {
                renderFieldGlowEffects(guiGraphics, time);
            }
        }
        
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Декоративные элементы отключены для производительности
        // renderCornerDecorations(guiGraphics, time); // ОТКЛЮЧЕНО
    }
    
    /**
     * Проверяет наличие легендарных карт на поле (оптимизированная версия)
     */
    private boolean checkHasLegendaryCards() {
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Проверяем без создания списков
        for (Card card : session.getMeleeRow(null)) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        for (Card card : session.getRangedRow(null)) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        for (Card card : session.getSiegeRow(null)) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        for (Card card : session.getOpponentMeleeRow()) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        for (Card card : session.getOpponentRangedRow()) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        for (Card card : session.getOpponentSiegeRow()) {
            if (card.getRarity() == com.bmfalkye.cards.CardRarity.LEGENDARY) return true;
        }
        return false;
    }
    
    /**
     * Рендерит эффекты свечения для карт на поле
     * АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Упрощённое свечение
     */
    private void renderFieldGlowEffects(GuiGraphics guiGraphics, long time) {
        // АГРЕССИВНАЯ ОПТИМИЗАЦИЯ: Упрощённое свечение (без пульсации)
        int glowColor = 0x40FFD700; // Статичное золотое свечение (полупрозрачное)
        
        // Верх и низ (упрощённые линии)
        guiGraphics.fill(guiX + 5, guiY + 2, guiX + GUI_WIDTH - 5, guiY + 3, glowColor);
        guiGraphics.fill(guiX + 5, guiY + GUI_HEIGHT - 3, guiX + GUI_WIDTH - 5, guiY + GUI_HEIGHT - 2, glowColor);
        
        // Лево и право (упрощённые линии)
        guiGraphics.fill(guiX + 2, guiY + 5, guiX + 3, guiY + GUI_HEIGHT - 5, glowColor);
        guiGraphics.fill(guiX + GUI_WIDTH - 3, guiY + 5, guiX + GUI_WIDTH - 2, guiY + GUI_HEIGHT - 5, glowColor);
    }
    
    /**
     * Рендерит декоративные элементы по углам поля
     */
    private void renderCornerDecorations(GuiGraphics guiGraphics, long time) {
        float rotation = (float) ((time / 5000.0f) % (Math.PI * 2));
        int cornerSize = 20;
        int cornerColor = 0x44FFFFFF;
        
        // Углы с вращающимися элементами
        for (int i = 0; i < 4; i++) {
            int cornerX = (i % 2 == 0) ? guiX : guiX + GUI_WIDTH - cornerSize;
            int cornerY = (i < 2) ? guiY : guiY + GUI_HEIGHT - cornerSize;
            
            // Декоративные линии
            guiGraphics.fill(cornerX, cornerY, cornerX + cornerSize, cornerY + 2, cornerColor);
            guiGraphics.fill(cornerX, cornerY, cornerX + 2, cornerY + cornerSize, cornerColor);
        }
    }
    
    /**
     * Обнаруживает сброс карт и создает анимации для текущей сессии
     */
    private void detectAndAnimateCardPlaysForSession(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        if (oldSession == null || newSession == null) {
            return;
        }
        
        UUID currentPlayerUUID = net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        if (currentPlayerUUID == null) {
            return;
        }
        
        // Проверяем карты способности (SPELL/SPECIAL) игрока - они исчезают из руки
        List<Card> oldHand = oldSession.getHand(null);
        List<Card> newHand = newSession.getHand(null);
        
        // Находим карты, которые были в руке игрока, но теперь их нет
        for (Card oldCard : oldHand) {
            boolean stillInHand = false;
            for (Card newCard : newHand) {
                if (oldCard.getId().equals(newCard.getId())) {
                    stillInHand = true;
                    break;
                }
            }
            
            // Карта исчезла из руки - это карта способности
            if (!stillInHand && (oldCard.getType() == Card.CardType.SPELL || oldCard.getType() == Card.CardType.SPECIAL)) {
                // Проверяем, не появилась ли карта на поле (если появилась, это не карта способности)
                boolean appearedOnField = false;
                List<Card> allNewFieldCards = new java.util.ArrayList<>();
                allNewFieldCards.addAll(newSession.getMeleeRow(null));
                allNewFieldCards.addAll(newSession.getRangedRow(null));
                allNewFieldCards.addAll(newSession.getSiegeRow(null));
                for (Card fieldCard : allNewFieldCards) {
                    if (fieldCard.getId().equals(oldCard.getId())) {
                        appearedOnField = true;
                        break;
                    }
                }
                
                if (!appearedOnField) {
                    // Карта способности использована игроком
                    float centerX = guiX + GUI_WIDTH / 2.0f;
                    float centerY = guiY + GUI_HEIGHT / 2.0f;
                    float handX = guiX + GUI_WIDTH / 2.0f;
                    float handY = guiY + GUI_HEIGHT - 100;
                    
                    animationManager.createAbilityAnimation(oldCard, handX, handY, centerX, centerY);
                    com.bmfalkye.client.effects.AdvancedVisualEffects.createAbilityCardEffect(
                        effectManager, centerX, centerY, oldCard
                    );
                }
            }
        }
        
        // Проверяем использование карт способности оппонентом
        // Сравниваем количество карт в руке оппонента
        java.util.List<String> oldOpponentHandIds = oldSession.getOpponentHandIds();
        java.util.List<String> newOpponentHandIds = newSession.getOpponentHandIds();
        
        // Если количество карт в руке оппонента уменьшилось
        if (oldOpponentHandIds.size() > newOpponentHandIds.size()) {
            // Проверяем, появились ли новые карты на поле оппонента
            List<Card> oldOpponentMelee = oldSession.getOpponentMeleeRow();
            List<Card> newOpponentMelee = newSession.getOpponentMeleeRow();
            List<Card> oldOpponentRanged = oldSession.getOpponentRangedRow();
            List<Card> newOpponentRanged = newSession.getOpponentRangedRow();
            List<Card> oldOpponentSiege = oldSession.getOpponentSiegeRow();
            List<Card> newOpponentSiege = newSession.getOpponentSiegeRow();
            
            // Собираем все старые карты оппонента на поле
            java.util.Set<String> oldOpponentFieldCardIds = new java.util.HashSet<>();
            for (Card card : oldOpponentMelee) oldOpponentFieldCardIds.add(card.getId());
            for (Card card : oldOpponentRanged) oldOpponentFieldCardIds.add(card.getId());
            for (Card card : oldOpponentSiege) oldOpponentFieldCardIds.add(card.getId());
            
            // Собираем все новые карты оппонента на поле
            java.util.Set<String> newOpponentFieldCardIds = new java.util.HashSet<>();
            for (Card card : newOpponentMelee) newOpponentFieldCardIds.add(card.getId());
            for (Card card : newOpponentRanged) newOpponentFieldCardIds.add(card.getId());
            for (Card card : newOpponentSiege) newOpponentFieldCardIds.add(card.getId());
            
            // Если количество карт на поле не изменилось, но карта исчезла из руки,
            // это означает использование карты способности оппонентом
            if (oldOpponentFieldCardIds.size() == newOpponentFieldCardIds.size()) {
                // Находим карту, которая исчезла из руки оппонента
                for (String oldHandId : oldOpponentHandIds) {
                    if (!newOpponentHandIds.contains(oldHandId)) {
                        // Эта карта исчезла из руки оппонента
                        Card card = com.bmfalkye.cards.CardRegistry.getCard(oldHandId);
                        if (card != null && (card.getType() == Card.CardType.SPELL || card.getType() == Card.CardType.SPECIAL)) {
                            // Это карта способности - создаем анимацию
                            float centerX = guiX + GUI_WIDTH / 2.0f;
                            float centerY = guiY + GUI_HEIGHT / 2.0f;
                            float handX = guiX + GUI_WIDTH / 2.0f;
                            float handY = guiY + 100; // Рука оппонента вверху
                            
                            animationManager.createAbilityAnimation(card, handX, handY, centerX, centerY);
                            com.bmfalkye.client.effects.AdvancedVisualEffects.createAbilityCardEffect(
                                effectManager, centerX, centerY, card
                            );
                            break; // Создаем анимацию только для одной карты за раз
                        }
                    }
                }
            }
        }
        
        // Обнаруживаем карты, которые исчезли с поля (сброшены)
        detectAndAnimateDiscardedCards(oldSession, newSession);
    }
    
    /**
     * Обнаруживает карты, которые были сброшены с поля, и создает для них анимации
     */
    private void detectAndAnimateDiscardedCards(ClientFalkyeGameSession oldSession, ClientFalkyeGameSession newSession) {
        if (oldSession == null || newSession == null) {
            return;
        }
        
        // Проверяем карты игрока на поле
        detectDiscardedCardsFromRow(oldSession.getMeleeRow(null), newSession.getMeleeRow(null), 
            "melee", false);
        detectDiscardedCardsFromRow(oldSession.getRangedRow(null), newSession.getRangedRow(null), 
            "ranged", false);
        detectDiscardedCardsFromRow(oldSession.getSiegeRow(null), newSession.getSiegeRow(null), 
            "siege", false);
        
        // Проверяем карты оппонента на поле
        detectDiscardedCardsFromRow(oldSession.getOpponentMeleeRow(), newSession.getOpponentMeleeRow(), 
            "opponent_melee", true);
        detectDiscardedCardsFromRow(oldSession.getOpponentRangedRow(), newSession.getOpponentRangedRow(), 
            "opponent_ranged", true);
        detectDiscardedCardsFromRow(oldSession.getOpponentSiegeRow(), newSession.getOpponentSiegeRow(), 
            "opponent_siege", true);
    }
    
    /**
     * Обнаруживает карты, которые исчезли из ряда, и создает для них анимацию сброса
     */
    private void detectDiscardedCardsFromRow(List<Card> oldRow, List<Card> newRow, 
                                            String rowType, boolean isOpponent) {
        // Находим карты, которые были в старом ряду, но теперь их нет
        for (Card oldCard : oldRow) {
            boolean stillInRow = false;
            for (Card newCard : newRow) {
                if (oldCard.getId().equals(newCard.getId())) {
                    stillInRow = true;
                    break;
                }
            }
            
            // Карта исчезла из ряда - создаем анимацию сброса
            if (!stillInRow) {
                // Вычисляем позицию карты на поле (используем ту же логику, что и в checkNewCardsOnField)
                int rowStartX = guiX + Math.max(50, 150 + 20);
                int myY = guiY + GUI_HEIGHT / 2 + 5;
                int opponentY = guiY + Math.max(70, (int)(GUI_HEIGHT * 0.12));
                int rowSpacing = Math.max(50, Math.min(65, (int)(GUI_HEIGHT * 0.12)));
                int overlapOffset = Math.max(8, Math.min(12, (int)(GUI_HEIGHT * 0.015)));
                
                int y;
                if (isOpponent) {
                    y = switch (rowType) {
                        case "opponent_melee" -> opponentY;
                        case "opponent_ranged" -> opponentY + rowSpacing - overlapOffset;
                        case "opponent_siege" -> opponentY + rowSpacing * 2 - overlapOffset * 2;
                        default -> opponentY;
                    };
                } else {
                    y = switch (rowType) {
                        case "melee" -> myY;
                        case "ranged" -> myY + rowSpacing - overlapOffset;
                        case "siege" -> myY + rowSpacing * 2 - overlapOffset * 2;
                        default -> myY;
                    };
                }
                
                // Находим индекс карты в старом ряду для вычисления позиции
                int cardIndex = oldRow.indexOf(oldCard);
                int baseCardWidth = 48;
                float fieldScale = Math.max(0.7f, Math.min(1.1f, (float)this.width / 1000.0f));
                int cardWidth = (int)(baseCardWidth * fieldScale);
                int cardSpacing = cardWidth + 25;
                int cardX = rowStartX + (cardIndex * cardSpacing);
                
                // Определяем тип ряда для анимации
                FalkyeGameSession.CardRow row = switch (rowType) {
                    case "melee", "opponent_melee" -> FalkyeGameSession.CardRow.MELEE;
                    case "ranged", "opponent_ranged" -> FalkyeGameSession.CardRow.RANGED;
                    case "siege", "opponent_siege" -> FalkyeGameSession.CardRow.SIEGE;
                    default -> FalkyeGameSession.CardRow.MELEE;
                };
                
                // Позиция сброса (внизу экрана)
                int discardX = guiX + GUI_WIDTH / 2;
                int discardY = guiY + GUI_HEIGHT + 50; // Внизу экрана, за пределами видимости
                
                // Создаем анимацию сброса (карта движется вниз и исчезает)
                animationManager.createDropAnimation(oldCard, cardX, y, discardX, discardY, row);
                
                // Создаем визуальные эффекты сброса
                com.bmfalkye.client.effects.AdvancedVisualEffects.createCardDropEffect(
                    effectManager, cardX, y, oldCard, row
                );
            }
        }
    }
    
    /**
     * Создает копию сессии для отслеживания изменений
     */
    private ClientFalkyeGameSession createSessionCopy(ClientFalkyeGameSession session) {
        // Создаем новую сессию с теми же данными
        // Это упрощенная версия - в реальности нужно копировать все поля
        return session; // Для простоты возвращаем ту же сессию
        // В реальной реализации нужно создать глубокую копию
    }
    
    /**
     * Рендерит анимации сброса карт на поле (карты, которые появляются на поле)
     * ВАЖНО: Рендерим анимацию, если она активна и карта еще не достигла целевой позиции
     */
    private void renderDropAnimations(GuiGraphics guiGraphics) {
        // ОПТИМИЗАЦИЯ: Ограничиваем количество одновременно рендерящихся анимаций
        java.util.Map<String, com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation> dropAnims = 
            animationManager.getAllDropAnimations();
        
        int maxAnimations = 5; // Максимум 5 анимаций одновременно
        int renderedCount = 0;
        
        for (com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation dropAnim : dropAnims.values()) {
            if (renderedCount >= maxAnimations) break; // ОПТИМИЗАЦИЯ: Прерываем, если достигли лимита
            
            if (dropAnim == null || dropAnim.card == null || dropAnim.animation.isFinished()) {
                continue;
            }
            
            // Проверяем, достигла ли карта целевой позиции
            float currentX = dropAnim.animation.getX();
            float currentY = dropAnim.animation.getY();
            float distanceToTarget = (float) Math.sqrt(
                Math.pow(currentX - dropAnim.targetX, 2) + 
                Math.pow(currentY - dropAnim.targetY, 2)
            );
            
            // Рендерим анимацию, если карта еще не достигла целевой позиции
            boolean shouldRender = distanceToTarget > 5.0f;
            
            if (shouldRender) {
                float x = dropAnim.animation.getX();
                float y = dropAnim.animation.getY();
                float scale = dropAnim.animation.getScale();
                
                // ОПТИМИЗАЦИЯ: Упрощенные размеры карты
                int cardWidth = 48;
                int cardHeight = 68;
                int scaledWidth = (int)(cardWidth * scale);
                int scaledHeight = (int)(cardHeight * scale);
                
                // ОПТИМИЗАЦИЯ: Упрощенный рендеринг без альфа-канала
                renderCardOnFieldAnimated(guiGraphics, dropAnim.card,
                    (int)x - scaledWidth / 2, (int)y - scaledHeight / 2,
                    scaledWidth, scaledHeight,
                    (int)x, (int)y, 1.0f, false);
                renderedCount++;
            }
        }
    }
    
    /**
     * Рендерит анимации сброса карт (карты, которые исчезли с поля)
     */
    private void renderDiscardAnimations(GuiGraphics guiGraphics) {
        // Получаем все активные анимации сброса
        java.util.Map<String, com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation> dropAnims = 
            animationManager.getAllDropAnimations();
        
        for (com.bmfalkye.client.effects.CardPlayAnimationManager.CardDropAnimation dropAnim : dropAnims.values()) {
            if (dropAnim == null || dropAnim.card == null || dropAnim.animation.isFinished()) {
                continue;
            }
            
            // Проверяем, находится ли карта еще на поле
            // Если карта больше не на поле, рендерим анимацию сброса
            boolean cardStillOnField = false;
            if (session != null) {
                List<Card> allFieldCards = new java.util.ArrayList<>();
                allFieldCards.addAll(session.getMeleeRow(null));
                allFieldCards.addAll(session.getRangedRow(null));
                allFieldCards.addAll(session.getSiegeRow(null));
                allFieldCards.addAll(session.getOpponentMeleeRow());
                allFieldCards.addAll(session.getOpponentRangedRow());
                allFieldCards.addAll(session.getOpponentSiegeRow());
                
                for (Card fieldCard : allFieldCards) {
                    if (fieldCard.getId().equals(dropAnim.card.getId())) {
                        cardStillOnField = true;
                        break;
                    }
                }
            }
            
            // Если карта больше не на поле, рендерим анимацию сброса
            if (!cardStillOnField) {
                float x = dropAnim.animation.getX();
                float y = dropAnim.animation.getY();
                float scale = dropAnim.animation.getScale();
                float alpha = dropAnim.animation.getAlpha();
                
                // Размеры карты для анимации
                int cardWidth = 80;
                int cardHeight = 120;
                
                // Рендерим карту с анимацией
                int scaledWidth = (int)(cardWidth * scale);
                int scaledHeight = (int)(cardHeight * scale);
                
                // Рендерим карту с учетом альфа-канала
                com.bmfalkye.client.gui.SimpleCardRenderer.renderCard(
                    guiGraphics, this.font, dropAnim.card,
                    (int)x - scaledWidth / 2, (int)y - scaledHeight / 2,
                    scaledWidth, scaledHeight,
                    (int)x, (int)y, false, false
                );
            }
        }
    }
    
    /**
     * Рендерит анимации карт способности (движение в центр и исчезновение)
     */
    private void renderAbilityCardAnimations(GuiGraphics guiGraphics) {
        // ОПТИМИЗАЦИЯ: Ограничиваем количество одновременно рендерящихся анимаций
        java.util.Map<String, com.bmfalkye.client.effects.CardPlayAnimationManager.AbilityCardAnimation> anims = 
            animationManager.getAbilityAnimations();
        
        int maxAnimations = 3; // Максимум 3 анимации способностей одновременно
        int renderedCount = 0;
        
        for (com.bmfalkye.client.effects.CardPlayAnimationManager.AbilityCardAnimation abilityAnim : anims.values()) {
            if (renderedCount >= maxAnimations) break; // ОПТИМИЗАЦИЯ: Прерываем, если достигли лимита
            
            if (abilityAnim == null || abilityAnim.card == null) {
                continue;
            }
            
            float x, y;
            float scale = 1.0f;
            
            if (!abilityAnim.moveComplete) {
                x = abilityAnim.moveAnimation.getX();
                y = abilityAnim.moveAnimation.getY();
                scale = abilityAnim.moveAnimation.getScale();
            } else if (!abilityAnim.fadeComplete) {
                x = abilityAnim.centerX;
                y = abilityAnim.centerY;
                scale = abilityAnim.fadeAnimation.getScale();
            } else {
                continue;
            }
            
            // ОПТИМИЗАЦИЯ: Упрощенные размеры
            int cardWidth = 80;
            int cardHeight = 120;
            int scaledWidth = (int)(cardWidth * scale);
            int scaledHeight = (int)(cardHeight * scale);
            
            // ОПТИМИЗАЦИЯ: Упрощенный рендеринг
            com.bmfalkye.client.gui.SimpleCardRenderer.renderCard(
                guiGraphics, this.font, abilityAnim.card,
                (int)x - scaledWidth / 2, (int)y - scaledHeight / 2,
                scaledWidth, scaledHeight,
                (int)x, (int)y, false, false // ОПТИМИЗАЦИЯ: Отключаем tooltip для анимаций
            );
            renderedCount++;
        }
    }
    
    /**
     * Смешивает два цвета
     */
    private int blendColor(int color1, int color2, float t) {
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
}
