package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.ClientFalkyeGameSession;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.*;

/**
 * 3D экран игры Falkye
 * Использует стандартные Minecraft модели для отображения юнитов
 */
public class Falkye3DGameScreen extends Screen {
    private ClientFalkyeGameSession session;
    
    // 3D позиции для юнитов на поле
    private final Map<String, CardEntity3D> cardEntities = new HashMap<>();
    
    // Камера для 3D вида
    private float cameraRotationY = 0.0f;
    private float cameraRotationX = 30.0f;
    private float cameraDistance = 10.0f;
    
    // Выбранная карта
    private int selectedCardIndex = -1;
    
    // Кнопки управления
    private Button passButton;
    private Button leaderButton;
    
    public Falkye3DGameScreen(ClientFalkyeGameSession session) {
        super(Component.translatable("screen.bm_falkye.game_title_3d"));
        this.session = session;
        initializeCardEntities();
    }
    
    public Falkye3DGameScreen(FalkyeGameSession session) {
        super(Component.translatable("screen.bm_falkye.game_title_3d"));
        // Конвертируем в ClientFalkyeGameSession
        this.session = convertToClientSession(session);
        initializeCardEntities();
    }
    
    private ClientFalkyeGameSession convertToClientSession(FalkyeGameSession serverSession) {
        // Используем ту же логику конвертации, что и в FalkyeGameScreen
        int remainingTime = com.bmfalkye.game.TurnTimer.getRemainingTime(serverSession);
        UUID currentPlayerUUID = serverSession.getCurrentPlayerUUID();
        int timeoutCount = currentPlayerUUID != null ? 
            com.bmfalkye.game.TurnTimer.getTimeOutCount(currentPlayerUUID) : 0;
        
        UUID player2UUID = serverSession.getPlayer2() != null ? serverSession.getPlayer2().getUUID() : 
            (serverSession.isPlayingWithVillager() && serverSession.getVillagerOpponent() != null ? 
                java.util.UUID.randomUUID() : null);
        
        java.util.List<String> hand1Ids = serverSession.getHand(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> hand2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getHand(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getHand(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        java.util.List<String> melee1Ids = serverSession.getMeleeRow(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> ranged1Ids = serverSession.getRangedRow(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> siege1Ids = serverSession.getSiegeRow(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        
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
        
        java.util.List<String> graveyard1Ids = serverSession.getGraveyard(serverSession.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> graveyard2Ids = serverSession.isPlayingWithVillager() ? 
            serverSession.getGraveyard(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (serverSession.getPlayer2() != null ? 
                serverSession.getGraveyard(serverSession.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        String leader1Id = serverSession.getLeader(serverSession.getPlayer1()) != null ? 
            serverSession.getLeader(serverSession.getPlayer1()).getId() : null;
        String leader2Id = serverSession.isPlayingWithVillager() ? 
            (serverSession.getLeader(null) != null ? serverSession.getLeader(null).getId() : null) :
            (serverSession.getPlayer2() != null && serverSession.getLeader(serverSession.getPlayer2()) != null ? 
                serverSession.getLeader(serverSession.getPlayer2()).getId() : null);
        
        return new ClientFalkyeGameSession(
            serverSession.getPlayer1() != null ? serverSession.getPlayer1().getUUID() : null,
            player2UUID,
            serverSession.getRoundScore(serverSession.getPlayer1()),
            serverSession.getRoundScore(serverSession.getPlayer2() != null ? serverSession.getPlayer2() : null),
            serverSession.getCurrentRound(),
            currentPlayerUUID,
            hand1Ids,
            hand2Ids,
            melee1Ids,
            ranged1Ids,
            siege1Ids,
            melee2Ids,
            ranged2Ids,
            siege2Ids,
            graveyard1Ids,
            graveyard2Ids,
            serverSession.getWeather(),
            leader1Id,
            leader2Id,
            serverSession.getRoundsWon(serverSession.getPlayer1()),
            serverSession.getRoundsWon(serverSession.getPlayer2() != null ? serverSession.getPlayer2() : null),
            false, // player1Passed - будет получено из ClientFalkyeGameSession
            false, // player2Passed - будет получено из ClientFalkyeGameSession
            serverSession.isPlayingWithVillager(),
            remainingTime,
            timeoutCount,
            new java.util.HashMap<>(),
            new java.util.ArrayList<>(),
            serverSession.getLocationType(),
            serverSession.getMatchConfig() != null ? serverSession.getMatchConfig().getGameMode() : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D // Режим игры
        );
    }
    
    /**
     * Инициализирует 3D сущности для карт
     */
    private void initializeCardEntities() {
        cardEntities.clear();
        
        // Создаём позиции для карт на поле
        // Ряд 1 (ближний бой) - позиции слева направо
        createCardPositions("melee1", -2.0, 0.0, -1.5, 5);
        createCardPositions("ranged1", -2.0, 0.0, 0.0, 5);
        createCardPositions("siege1", -2.0, 0.0, 1.5, 5);
        
        // Ряд 2 (противник) - позиции справа налево
        createCardPositions("melee2", 2.0, 0.0, -1.5, 5);
        createCardPositions("ranged2", 2.0, 0.0, 0.0, 5);
        createCardPositions("siege2", 2.0, 0.0, 1.5, 5);
    }
    
    /**
     * Создаёт позиции для карт в ряду
     */
    private void createCardPositions(String rowId, double baseX, double baseY, double baseZ, int maxCards) {
        double spacing = 1.2;
        double startX = baseX - (maxCards - 1) * spacing / 2.0;
        
        for (int i = 0; i < maxCards; i++) {
            String cardId = rowId + "_slot_" + i;
            cardEntities.put(cardId, new CardEntity3D(
                startX + i * spacing,
                baseY,
                baseZ,
                rowId
            ));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Кнопка "Пас"
        passButton = Button.builder(
            Component.literal("§7Пас"),
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.PassPacket());
            })
            .bounds(width / 2 - 100, height - 50, 80, 20)
            .build();
        this.addRenderableWidget(passButton);
        
        // Кнопка "Лидер"
        leaderButton = Button.builder(
            Component.literal("§6Лидер"),
            (btn) -> {
                NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.UseLeaderPacket());
            })
            .bounds(width / 2 + 20, height - 50, 80, 20)
            .build();
        this.addRenderableWidget(leaderButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        // Рисуем 3D поле
        render3DField(graphics, partialTick);
        
        // Рисуем UI поверх 3D (рука, информация)
        renderUI(graphics, mouseX, mouseY);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * Рендерит 3D поле с юнитами
     */
    private void render3DField(GuiGraphics graphics, float partialTick) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        
        // Настраиваем камеру
        int centerX = width / 2;
        int centerY = height / 2;
        
        poseStack.translate(centerX, centerY, 0);
        poseStack.scale(50.0f, 50.0f, 50.0f); // Масштаб для 3D
        
        // Ротация камеры
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(cameraRotationY));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(cameraRotationX));
        poseStack.translate(0, -cameraDistance, 0);
        
        // Рисуем поле (плоскость)
        renderGameBoard(poseStack, graphics);
        
        // Рисуем карты как 3D сущности
        renderCardEntities(poseStack, graphics, partialTick);
        
        poseStack.popPose();
    }
    
    /**
     * Рисует игровое поле (плоскость)
     */
    private void renderGameBoard(PoseStack poseStack, GuiGraphics graphics) {
        // Рисуем простую плоскость для поля
        // TODO: Использовать текстуру игрового поля из косметики
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        
        // Простая плоскость для визуализации
        // В будущем можно заменить на текстурированное поле
    }
    
    /**
     * Рисует карты как 3D сущности
     */
    private void renderCardEntities(PoseStack poseStack, GuiGraphics graphics, float partialTick) {
        // Используем прямые методы для получения ID карт
        List<Card> melee1 = getCardsFromRow(session.getMelee1Ids());
        List<Card> ranged1 = getCardsFromRow(session.getRanged1Ids());
        List<Card> siege1 = getCardsFromRow(session.getSiege1Ids());
        List<Card> melee2 = getCardsFromRow(session.getMelee2Ids());
        List<Card> ranged2 = getCardsFromRow(session.getRanged2Ids());
        List<Card> siege2 = getCardsFromRow(session.getSiege2Ids());
        
        // Рисуем карты игрока 1
        renderRowCards(poseStack, graphics, melee1, "melee1", partialTick);
        renderRowCards(poseStack, graphics, ranged1, "ranged1", partialTick);
        renderRowCards(poseStack, graphics, siege1, "siege1", partialTick);
        
        // Рисуем карты игрока 2
        renderRowCards(poseStack, graphics, melee2, "melee2", partialTick);
        renderRowCards(poseStack, graphics, ranged2, "ranged2", partialTick);
        renderRowCards(poseStack, graphics, siege2, "siege2", partialTick);
    }
    
    /**
     * Рисует карты в ряду
     */
    private void renderRowCards(PoseStack poseStack, GuiGraphics graphics, List<Card> cards, 
                                String rowId, float partialTick) {
        for (int i = 0; i < cards.size() && i < 5; i++) {
            Card card = cards.get(i);
            String slotId = rowId + "_slot_" + i;
            CardEntity3D entity = cardEntities.get(slotId);
            
            if (entity != null) {
                renderCardAsEntity(poseStack, graphics, card, entity, partialTick);
            }
        }
    }
    
    /**
     * Рисует карту как 3D сущность (используя стандартную модель Minecraft)
     */
    private void renderCardAsEntity(PoseStack poseStack, GuiGraphics graphics, Card card, 
                                    CardEntity3D entity, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(entity.x, entity.y, entity.z);
        
        // Используем ArmorStand как базу для отображения карты
        // Можно использовать ItemFrame или создать кастомную модель
        // Для простоты используем ItemStack с иконкой карты
        
        ItemStack displayItem = new ItemStack(Items.PAPER); // Заглушка
        // TODO: Создать кастомный Item для карт или использовать существующий CardItem
        
        // Рендерим ItemStack как 3D объект
        // Используем ItemRenderer для отображения
        net.minecraft.client.renderer.entity.ItemRenderer itemRenderer = 
            Minecraft.getInstance().getItemRenderer();
        
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        
        // Рисуем ItemStack (упрощённая версия для 3D)
        // TODO: Реализовать полноценный 3D рендеринг карт
        // Пока используем простую визуализацию
        bufferSource.endBatch();
        
        // Рисуем силу карты над сущностью
        poseStack.translate(0, 0.5, 0);
        poseStack.scale(0.02f, 0.02f, 0.02f);
        graphics.drawString(font, String.valueOf(card.getPower()), 0, 0, 0xFFFFFF, false);
        
        poseStack.popPose();
    }
    
    /**
     * Получает карты из списка ID
     */
    private List<Card> getCardsFromRow(List<String> cardIds) {
        List<Card> cards = new ArrayList<>();
        if (cardIds != null) {
            for (String cardId : cardIds) {
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(cardId);
                if (card != null) {
                    cards.add(card);
                }
            }
        }
        return cards;
    }
    
    /**
     * Получает UUID локального игрока
     */
    private UUID getLocalPlayerUUID() {
        try {
            return net.minecraft.client.Minecraft.getInstance().player != null ?
                net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Рисует UI поверх 3D (рука, информация)
     */
    private void renderUI(GuiGraphics graphics, int mouseX, int mouseY) {
        // Рисуем руку внизу экрана
        renderHand(graphics);
        
        // Рисуем информацию о раунде
        renderRoundInfo(graphics);
        
        // Рисуем таймер
        renderTimer(graphics);
    }
    
    /**
     * Рисует руку игрока
     */
    private void renderHand(GuiGraphics graphics) {
        List<Card> hand = session.getHand(null);
        
        int handY = height - 120;
        int startX = width / 2 - (hand.size() * 60) / 2;
        
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            int cardX = startX + i * 60;
            
            // Рисуем карту как 2D спрайт в руке
            graphics.fill(cardX, handY, cardX + 50, handY + 80, 0xFF333333);
            graphics.drawString(font, card.getName(), cardX + 5, handY + 5, 0xFFFFFF, false);
            graphics.drawString(font, String.valueOf(card.getPower()), cardX + 5, handY + 70, 0xFFFFFF, false);
        }
    }
    
    /**
     * Рисует информацию о раунде
     */
    private void renderRoundInfo(GuiGraphics graphics) {
        graphics.drawString(font, "§eРаунд: " + session.getCurrentRound(), 10, 10, 0xFFFFFF, false);
        graphics.drawString(font, "§aПобеды: " + session.getRoundsWon1() + " - " + session.getRoundsWon2(), 10, 25, 0xFFFFFF, false);
    }
    
    /**
     * Рисует таймер
     */
    private void renderTimer(GuiGraphics graphics) {
        int remainingTime = session.getRemainingTime();
        if (remainingTime > 0) {
            graphics.drawString(font, "§cВремя: " + remainingTime, width - 100, 10, 0xFFFFFF, false);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка кликов по картам в руке
        List<Card> hand = session.getHand(null);
        int handY = height - 120;
        int startX = width / 2 - (hand.size() * 60) / 2;
        
        for (int i = 0; i < hand.size(); i++) {
            int cardX = startX + i * 60;
            if (mouseX >= cardX && mouseX <= cardX + 50 && 
                mouseY >= handY && mouseY <= handY + 80) {
                selectedCardIndex = i;
                // Открываем меню выбора ряда
                openRowSelectionMenu(hand.get(i));
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Открывает меню выбора ряда для карты
     */
    private void openRowSelectionMenu(Card card) {
        // TODO: Реализовать меню выбора ряда
        // Можно использовать существующую логику из FalkyeGameScreen
    }
    
    /**
     * Обновляет состояние игры
     */
    public void updateGameState(ClientFalkyeGameSession newSession) {
        this.session = newSession;
        // Обновляем 3D сущности при необходимости
    }
    
    /**
     * 3D сущность карты
     */
    private static class CardEntity3D {
        public final double x;
        public final double y;
        public final double z;
        public final String rowId;
        
        public CardEntity3D(double x, double y, double z, String rowId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rowId = rowId;
        }
    }
}

