package com.bmfalkye.client;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.SimpleCardRenderer;
import com.bmfalkye.network.NetworkHandler;
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
 * Экран драфта - выбор карт для "Великого Турнира"
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class DraftScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 1000;
    private static final int BASE_GUI_HEIGHT = 700;
    private static final int MIN_GUI_WIDTH = 850;
    private static final int MIN_GUI_HEIGHT = 600;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    
    // Данные драфта
    private List<Card> currentChoice = new ArrayList<>(); // Текущие 3 карты на выбор
    private int currentChoiceIndex = 0; // Текущий выбор (0-29)
    private int totalChoices = 30;
    private List<String> selectedCards = new ArrayList<>(); // Уже выбранные карты
    private boolean draftCompleted = false;
    
    private final Screen parentScreen;
    
    public DraftScreen() {
        this(null);
    }
    
    public DraftScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.draft_title"));
        this.parentScreen = parentScreen;
        requestDraftData();
    }
    
    private void requestDraftData() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestDraftDataPacket());
    }
    
    public void updateDraftData(List<String> cardIds1, List<String> cardIds2, List<String> cardIds3, 
                               int currentIndex, List<String> selected, boolean completed) {
        // Обновляем текущий выбор
        currentChoice.clear();
        if (cardIds1 != null && !cardIds1.isEmpty()) {
            Card card1 = CardRegistry.getCard(cardIds1.get(0));
            if (card1 != null) currentChoice.add(card1);
        }
        if (cardIds2 != null && !cardIds2.isEmpty()) {
            Card card2 = CardRegistry.getCard(cardIds2.get(0));
            if (card2 != null) currentChoice.add(card2);
        }
        if (cardIds3 != null && !cardIds3.isEmpty()) {
            Card card3 = CardRegistry.getCard(cardIds3.get(0));
            if (card3 != null) currentChoice.add(card3);
        }
        
        this.currentChoiceIndex = currentIndex;
        this.selectedCards = new ArrayList<>(selected);
        this.draftCompleted = completed;
        
        if (this.minecraft != null && this.minecraft.screen == this) {
            this.init();
        }
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                if (parentScreen != null && minecraft != null) {
                    minecraft.setScreen(parentScreen);
                } else if (minecraft != null) {
                    minecraft.setScreen(new FalkyeMainMenuScreen());
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        // Кнопка начала арены (если драфт завершён)
        if (draftCompleted) {
            Button startArenaButton = GuiUtils.createStyledButton(
                layout.getCenteredX(layout.getWidth(25)), layout.getY(75), 
                layout.getWidth(25), layout.getHeight(8),
                Component.translatable("screen.bm_falkye.start_arena")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.StartArenaPacket());
                }
            );
            this.addRenderableWidget(startArenaButton);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        }
        
        this.renderBackground(graphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int GUI_WIDTH = layout.getGuiWidth();
        int GUI_HEIGHT = layout.getGuiHeight();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(graphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable(draftCompleted ? 
            "screen.bm_falkye.draft_completed" : "screen.bm_falkye.draft_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        graphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        graphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Разделитель
        int dividerY = layout.getY(8);
        graphics.fill(guiX + layout.getWidth(5), dividerY, 
            guiX + GUI_WIDTH - layout.getWidth(5), dividerY + 2, 0xFFFFA500);
        
        if (draftCompleted) {
            // Драфт завершён
            MutableComponent completed = Component.translatable("screen.bm_falkye.draft_successfully_completed")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
            graphics.drawCenteredString(this.font, completed, 
                layout.getCenteredX(this.font.width(completed)), layout.getY(50), 0xFFFFFF);
            
            MutableComponent hint = Component.translatable("screen.bm_falkye.press_button_to_start_arena")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), layout.getY(60), 0x888888);
        } else {
            // Прогресс
            MutableComponent progress = Component.translatable("screen.bm_falkye.draft_progress", 
                currentChoiceIndex + 1, totalChoices)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            graphics.drawString(this.font, progress, layout.getX(5), layout.getY(12), 0xFFFFFF, false);
            
            // Выбранные карты
            MutableComponent selected = Component.translatable("screen.bm_falkye.cards_selected", 
                selectedCards.size(), totalChoices)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, selected, layout.getX(5), layout.getY(15), 0xFFFFFF, false);
            
            if (currentChoice.size() == 3) {
                // Рисуем 3 карты на выбор
                renderCardChoices(graphics, mouseX, mouseY);
                
                // Инструкция
                MutableComponent instruction = Component.translatable("screen.bm_falkye.select_one_of_three_cards")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                graphics.drawCenteredString(this.font, instruction, 
                    layout.getCenteredX(this.font.width(instruction)), 
                    layout.getBottomY(layout.getHeight(8), 2), 0x888888);
            } else {
                // Загрузка
                MutableComponent loading = Component.translatable("screen.bm_falkye.loading_card_choice")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                graphics.drawCenteredString(this.font, loading, 
                    layout.getCenteredX(this.font.width(loading)), 
                    layout.getY(50), 0x888888);
            }
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderCardChoices(GuiGraphics graphics, int mouseX, int mouseY) {
        int cardsAreaY = layout.getY(20);
        int cardsAreaHeight = layout.getGuiHeight() - cardsAreaY - layout.getHeight(15);
        int cardWidth = Math.min(layout.getWidth(28), 220);
        int cardHeight = (int)(cardWidth * 1.4f);
        int cardSpacing = layout.getWidth(3);
        int totalWidth = cardWidth * 3 + cardSpacing * 2;
        int startX = layout.getCenteredX(totalWidth);
        
        for (int i = 0; i < 3; i++) {
            Card card = currentChoice.get(i);
            if (card == null) continue;
            
            int cardX = startX + i * (cardWidth + cardSpacing);
            int cardY = cardsAreaY;
            
            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardWidth && 
                               mouseY >= cardY && mouseY <= cardY + cardHeight;
            
            // Рендерим карту
            SimpleCardRenderer.renderCard(graphics, this.font, card, cardX, cardY, 
                cardWidth, cardHeight, mouseX, mouseY, isHovered, false);
            
            // Кнопка выбора (невидимая, но кликабельная)
            // Клик обрабатывается в mouseClicked
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !draftCompleted && currentChoice.size() == 3) {
            int cardsAreaY = layout.getY(20);
            int cardWidth = Math.min(layout.getWidth(28), 220);
            int cardHeight = (int)(cardWidth * 1.4f);
            int cardSpacing = layout.getWidth(3);
            int totalWidth = cardWidth * 3 + cardSpacing * 2;
            int startX = layout.getCenteredX(totalWidth);
            
            for (int i = 0; i < 3; i++) {
                int cardX = startX + i * (cardWidth + cardSpacing);
                int cardY = cardsAreaY;
                
                if (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                    mouseY >= cardY && mouseY <= cardY + cardHeight) {
                    
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    selectCard(i);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void selectCard(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= currentChoice.size()) {
            return;
        }
        
        // Отправляем выбор на сервер
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.SelectDraftCardPacket(currentChoiceIndex, cardIndex));
        
        // Обновляем локально (сервер подтвердит)
        Card selectedCard = currentChoice.get(cardIndex);
        if (selectedCard != null) {
            selectedCards.add(selectedCard.getId());
            currentChoiceIndex++;
            
            if (currentChoiceIndex >= totalChoices) {
                draftCompleted = true;
            }
            
            // Пересоздаём экран для обновления
            init();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
