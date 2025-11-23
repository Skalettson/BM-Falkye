package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.tournament.CustomTournament;
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

/**
 * Экран для создания и управления пользовательскими турнирами
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class CustomTournamentScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 850;
    private static final int BASE_GUI_HEIGHT = 650;
    private static final int MIN_GUI_WIDTH = 750;
    private static final int MIN_GUI_HEIGHT = 550;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private final Screen parentScreen;
    
    // Режимы экрана
    private ScreenMode mode = ScreenMode.LIST;
    private List<CustomTournamentInfo> tournaments = new ArrayList<>();
    private int scrollOffset = 0;
    
    // Поля для создания турнира
    private EditBox nameField;
    private EditBox entryFeeField;
    private CustomTournament.TournamentRules selectedRules = CustomTournament.TournamentRules.STANDARD;
    private int selectedMaxParticipants = 8;
    
    private static final int TOURNAMENT_ENTRY_HEIGHT = 70;
    private static final int TOURNAMENT_ENTRY_SPACING = 5;
    
    public CustomTournamentScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.custom_tournaments_title"));
        this.parentScreen = parentScreen;
        requestTournaments();
    }
    
    private void requestTournaments() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCustomTournamentsPacket());
    }
    
    public void updateTournaments(List<CustomTournamentInfo> tournaments) {
        this.tournaments = new ArrayList<>(tournaments);
        scrollOffset = 0;
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
        
        if (mode == ScreenMode.LIST) {
            // Кнопка "Создать турнир"
            Button createButton = GuiUtils.createStyledButton(
                layout.getX(40), layout.getY(2), layout.getWidth(20), layout.getHeight(5),
                Component.translatable("screen.bm_falkye.create_tournament")
                    .withStyle(ChatFormatting.GREEN),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    mode = ScreenMode.CREATE;
                    init();
                }
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка "Обновить"
            Button refreshButton = GuiUtils.createStyledButton(
                layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
                layout.getWidth(15), layout.getHeight(5),
                Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    requestTournaments();
                }
            );
            this.addRenderableWidget(refreshButton);
        } else if (mode == ScreenMode.CREATE) {
            // Поля для создания турнира
            int fieldY = layout.getY(15);
            int fieldWidth = layout.getWidth(40);
            int fieldHeight = 20;
            
            // Поле названия
            nameField = new EditBox(this.font, layout.getX(30), fieldY, fieldWidth, fieldHeight, 
                Component.translatable("screen.bm_falkye.tournament_name"));
            nameField.setMaxLength(50);
            nameField.setValue("");
            this.addRenderableWidget(nameField);
            this.setInitialFocus(nameField);
            
            // Поле взноса
            entryFeeField = new EditBox(this.font, layout.getX(30), fieldY + fieldHeight + 10, 
                fieldWidth, fieldHeight, Component.translatable("screen.bm_falkye.entry_fee_coins"));
            entryFeeField.setMaxLength(10);
            entryFeeField.setValue("0");
            entryFeeField.setFilter(s -> s.matches("\\d*"));
            this.addRenderableWidget(entryFeeField);
            
            // Кнопки выбора правил
            int buttonY = fieldY + fieldHeight * 2 + 30;
            Button standardButton = GuiUtils.createStyledButton(
                layout.getX(30), buttonY, layout.getWidth(20), layout.getHeight(5),
                Component.translatable("screen.bm_falkye.rules_standard")
                    .withStyle(selectedRules == CustomTournament.TournamentRules.STANDARD ? 
                        ChatFormatting.GREEN : ChatFormatting.GRAY),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    selectedRules = CustomTournament.TournamentRules.STANDARD;
                    init();
                }
            );
            this.addRenderableWidget(standardButton);
            
            Button legacyButton = GuiUtils.createStyledButton(
                layout.getX(52), buttonY, layout.getWidth(20), layout.getHeight(5),
                Component.translatable("screen.bm_falkye.rules_legacy")
                    .withStyle(selectedRules == CustomTournament.TournamentRules.LEGACY ? 
                        ChatFormatting.GREEN : ChatFormatting.GRAY),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    selectedRules = CustomTournament.TournamentRules.LEGACY;
                    init();
                }
            );
            this.addRenderableWidget(legacyButton);
            
            // Кнопки выбора количества участников
            int participantsY = buttonY + layout.getHeight(7);
            for (int i = 0; i < 3; i++) {
                int participants = 8 * (int)Math.pow(2, i); // 8, 16, 32
                final int finalParticipants = participants;
                Button participantButton = GuiUtils.createStyledButton(
                    layout.getX(30 + i * 24), participantsY, layout.getWidth(20), layout.getHeight(5),
                    Component.translatable("screen.bm_falkye.participants_count", finalParticipants)
                        .withStyle(selectedMaxParticipants == finalParticipants ? 
                            ChatFormatting.GREEN : ChatFormatting.GRAY),
                    (btn) -> {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        selectedMaxParticipants = finalParticipants;
                        init();
                    }
                );
                this.addRenderableWidget(participantButton);
            }
            
            // Кнопка "Создать"
            Button createButton = GuiUtils.createStyledButton(
                layout.getCenteredX(layout.getWidth(25)), layout.getY(70), 
                layout.getWidth(25), layout.getHeight(8),
                Component.translatable("screen.bm_falkye.create_tournament")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    createTournament();
                }
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка "Отмена"
            Button cancelButton = GuiUtils.createStyledButton(
                layout.getCenteredX(layout.getWidth(25)), layout.getY(80), 
                layout.getWidth(25), layout.getHeight(6),
                Component.translatable("gui.cancel").withStyle(ChatFormatting.GRAY),
                (btn) -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    mode = ScreenMode.LIST;
                    init();
                }
            );
            this.addRenderableWidget(cancelButton);
        }
    }
    
    private void createTournament() {
        String name = nameField != null ? nameField.getValue() : "";
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        
        int entryFee = 0;
        try {
            if (entryFeeField != null && entryFeeField.getValue() != null && !entryFeeField.getValue().isEmpty()) {
                entryFee = Integer.parseInt(entryFeeField.getValue());
            }
        } catch (NumberFormatException e) {
            entryFee = 0;
        }
        
        // Отправляем запрос на создание турнира
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.CreateCustomTournamentPacket(
            name.trim(), selectedRules, selectedMaxParticipants, entryFee, 0));
        
        mode = ScreenMode.LIST;
        init();
        requestTournaments();
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
        MutableComponent title = Component.translatable(mode == ScreenMode.CREATE ? 
            "screen.bm_falkye.create_tournament" : "screen.bm_falkye.custom_tournaments_title")
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
        
        if (mode == ScreenMode.LIST) {
            renderTournamentList(graphics, mouseX, mouseY);
        } else if (mode == ScreenMode.CREATE) {
            renderCreateForm(graphics);
        }
        
        // Рендерим кнопки
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button btn && btn.visible) {
                GuiUtils.renderStyledButton(graphics, this.font, btn, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderTournamentList(GuiGraphics graphics, int mouseX, int mouseY) {
        int listY = layout.getY(11);
        int listHeight = layout.getGuiHeight() - listY - layout.getHeight(8);
        int listX = layout.getX(3);
        int listWidth = layout.getGuiWidth() - listX * 2;
        
        // Обрезка списка турниров
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        
        if (tournaments.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_custom_tournaments")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), listY + listHeight / 2, 0x888888);
        } else {
            int visibleCount = listHeight / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int startIndex = scrollOffset / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, tournaments.size());
            
            int tournamentY = listY - (scrollOffset % (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= tournaments.size()) break;
                
                CustomTournamentInfo info = tournaments.get(i);
                int entryWidth = listWidth - 10;
                
                boolean isHovered = mouseX >= listX && mouseX <= listX + entryWidth && 
                                   mouseY >= tournamentY && mouseY <= tournamentY + TOURNAMENT_ENTRY_HEIGHT;
                
                // Фон карточки турнира
                GuiUtils.drawLeatherElement(graphics, listX + 5, tournamentY, entryWidth, TOURNAMENT_ENTRY_HEIGHT);
                int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
                graphics.fill(listX + 7, tournamentY + 2, listX + entryWidth - 3, 
                    tournamentY + TOURNAMENT_ENTRY_HEIGHT - 2, bgColor);
                GuiUtils.drawMetalFrame(graphics, listX + 5, tournamentY, entryWidth, TOURNAMENT_ENTRY_HEIGHT, 1, false);
                
                int textX = listX + 15;
                int textY = tournamentY + 10;
                int lineHeight = 15;
                
                // Название турнира
                MutableComponent name = Component.literal(info.name)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                graphics.drawString(this.font, name, textX, textY, 0xFFFFFF, false);
                textY += lineHeight;
                
                // Участников
                MutableComponent participants = Component.translatable("screen.bm_falkye.participants", 
                    info.participants, info.maxParticipants)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                graphics.drawString(this.font, participants, textX, textY, 0xFFFFFF, false);
                textY += lineHeight;
                
                // Взнос
                MutableComponent entryFee = Component.translatable("screen.bm_falkye.entry_fee", info.entryFee)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                graphics.drawString(this.font, entryFee, textX, textY, 0xFFFFFF, false);
                
                // Кнопка "Смотреть" для начатых турниров (рендерится в init, но позиция здесь)
                if (info.started) {
                    MutableComponent watchText = Component.translatable("screen.bm_falkye.watch")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                    int watchX = listX + entryWidth - this.font.width(watchText) - 15;
                    graphics.drawString(this.font, watchText, watchX, tournamentY + 10, 0xFFFFFF, false);
                }
                
                tournamentY += TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING;
            }
        }
        
        graphics.disableScissor();
        
        // Индикатор прокрутки
        int visibleCount = listHeight / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
        if (tournaments.size() > visibleCount) {
            int scrollBarX = layout.getRightX(4, 2);
            int scrollBarY = listY;
            int scrollBarHeight = listHeight;
            int scrollBarWidth = 4;
            
            // Фон полосы прокрутки
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                scrollBarY + scrollBarHeight, 0x66000000);
            
            // Ползунок
            int maxScroll = Math.max(1, tournaments.size() - visibleCount);
            int sliderHeight = Math.max(20, (int)((double)visibleCount / tournaments.size() * scrollBarHeight));
            int scrollStep = TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING;
            int currentScrollIndex = scrollOffset / scrollStep;
            int sliderY = scrollBarY + (int)((double)currentScrollIndex / maxScroll * (scrollBarHeight - sliderHeight));
            graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
        }
    }
    
    private void renderCreateForm(GuiGraphics graphics) {
        int formX = layout.getX(25);
        int formY = layout.getY(12);
        
        // Подсказки для полей
        if (nameField != null) {
            MutableComponent nameHint = Component.translatable("screen.bm_falkye.tournament_name")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, nameHint, formX, formY, 0xFFFFFF, false);
        }
        
        if (entryFeeField != null) {
            MutableComponent feeHint = Component.translatable("screen.bm_falkye.entry_fee_coins")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.drawString(this.font, feeHint, formX, 
                layout.getY(15) + 30, 0xFFFFFF, false);
        }
        
        // Подсказка для правил
        MutableComponent rulesHint = Component.translatable("screen.bm_falkye.tournament_rules")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        graphics.drawString(this.font, rulesHint, formX, 
            layout.getY(15) + 70, 0xFFFFFF, false);
        
        // Подсказка для участников
        MutableComponent participantsHint = Component.translatable("screen.bm_falkye.max_participants")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        graphics.drawString(this.font, participantsHint, formX, 
            layout.getY(15) + 110, 0xFFFFFF, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mode == ScreenMode.LIST && !tournaments.isEmpty()) {
            int listY = layout.getY(11);
            int listHeight = layout.getGuiHeight() - listY - layout.getHeight(8);
            int listX = layout.getX(3);
            int listWidth = layout.getGuiWidth() - listX * 2;
            
            int visibleCount = listHeight / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int startIndex = scrollOffset / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, tournaments.size());
            
            int tournamentY = listY - (scrollOffset % (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= tournaments.size()) break;
                
                CustomTournamentInfo info = tournaments.get(i);
                int entryWidth = listWidth - 10;
                
                if (info.started && mouseX >= listX && mouseX <= listX + entryWidth &&
                    mouseY >= tournamentY && mouseY <= tournamentY + TOURNAMENT_ENTRY_HEIGHT) {
                    
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    NetworkHandler.INSTANCE.sendToServer(
                        new NetworkHandler.AddTournamentSpectatorPacket(info.id));
                    return true;
                }
                
                tournamentY += TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || mode != ScreenMode.LIST || tournaments.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int listY = layout.getY(11);
        int listHeight = layout.getGuiHeight() - listY - layout.getHeight(8);
        int listX = layout.getX(3);
        int listWidth = layout.getGuiWidth() - listX * 2;
        
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleCount = listHeight / (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int totalCount = tournaments.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING);
            int scrollStep = TOURNAMENT_ENTRY_HEIGHT + TOURNAMENT_ENTRY_SPACING;
            
            if (delta < 0 && scrollOffset < maxScroll) {
                scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep);
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset = Math.max(0, scrollOffset - scrollStep);
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            if (mode == ScreenMode.CREATE) {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                mode = ScreenMode.LIST;
                init();
                return true;
            }
        }
        if ((keyCode == 257 || keyCode == 335) && mode == ScreenMode.CREATE) { // Enter
            createTournament();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private enum ScreenMode {
        LIST, CREATE
    }
    
    /**
     * Информация о турнире для отображения
     */
    public static class CustomTournamentInfo {
        public final String id;
        public final String name;
        public final int participants;
        public final int maxParticipants;
        public final int entryFee;
        public final String rules;
        public final boolean started;
        
        public CustomTournamentInfo(String id, String name, int participants, 
                                   int maxParticipants, int entryFee, String rules, boolean started) {
            this.id = id;
            this.name = name;
            this.participants = participants;
            this.maxParticipants = maxParticipants;
            this.entryFee = entryFee;
            this.rules = rules;
            this.started = started;
        }
    }
}
