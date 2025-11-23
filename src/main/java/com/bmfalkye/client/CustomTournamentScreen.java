package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.network.NetworkHandler;
import com.bmfalkye.tournament.CustomTournament;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран для создания и управления пользовательскими турнирами
 */
public class CustomTournamentScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private final Screen parentScreen;
    
    // Режимы экрана
    private ScreenMode mode = ScreenMode.LIST;
    private List<CustomTournamentInfo> tournaments = new ArrayList<>();
    
    // Поля для создания турнира
    private EditBox nameField;
    private EditBox entryFeeField;
    private CustomTournament.TournamentRules selectedRules = CustomTournament.TournamentRules.STANDARD;
    private int selectedMaxParticipants = 8;
    
    public CustomTournamentScreen(Screen parentScreen) {
        super(Component.literal("§6§lПОЛЬЗОВАТЕЛЬСКИЕ ТУРНИРЫ"));
        this.parentScreen = parentScreen;
        requestTournaments();
    }
    
    private void requestTournaments() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestCustomTournamentsPacket());
    }
    
    public void updateTournaments(List<CustomTournamentInfo> tournaments) {
        this.tournaments = new ArrayList<>(tournaments);
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Кнопка "Назад"
        Button backButton = createStyledButton(
            layout.getX(2), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§7Назад"),
            (btn) -> {
                if (parentScreen != null) {
                    minecraft.setScreen(parentScreen);
                } else {
                    minecraft.setScreen(new FalkyeMainMenuScreen());
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        if (mode == ScreenMode.LIST) {
            // Кнопка "Создать турнир"
            Button createButton = createStyledButton(
                layout.getX(40), layout.getY(2), layout.getWidth(20), layout.getHeight(5),
                Component.literal("§aСоздать турнир"),
                (btn) -> {
                    mode = ScreenMode.CREATE;
                    init();
                }
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка "Обновить"
            Button refreshButton = createStyledButton(
                layout.getX(65), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
                Component.literal("§eОбновить"),
                (btn) -> requestTournaments()
            );
            this.addRenderableWidget(refreshButton);
        } else if (mode == ScreenMode.CREATE) {
            // Поля для создания турнира
            int fieldY = layout.getY(15);
            int fieldWidth = layout.getWidth(40);
            int fieldHeight = layout.getHeight(5);
            
            // Поле названия
            nameField = new EditBox(font, layout.getX(30), fieldY, fieldWidth, fieldHeight, 
                Component.literal("Название турнира"));
            nameField.setMaxLength(50);
            this.addRenderableWidget(nameField);
            
            // Поле взноса
            entryFeeField = new EditBox(font, layout.getX(30), fieldY + fieldHeight + 10, 
                fieldWidth, fieldHeight, Component.literal("Взнос (монеты)"));
            entryFeeField.setMaxLength(10);
            entryFeeField.setFilter(s -> s.matches("\\d*"));
            this.addRenderableWidget(entryFeeField);
            
            // Кнопки выбора правил
            int buttonY = fieldY + fieldHeight * 2 + 20;
            Button standardButton = createStyledButton(
                layout.getX(30), buttonY, layout.getWidth(20), layout.getHeight(5),
                Component.literal(selectedRules == CustomTournament.TournamentRules.STANDARD ? 
                    "§a✓ Стандарт" : "§7Стандарт"),
                (btn) -> {
                    selectedRules = CustomTournament.TournamentRules.STANDARD;
                    init();
                }
            );
            this.addRenderableWidget(standardButton);
            
            Button legacyButton = createStyledButton(
                layout.getX(52), buttonY, layout.getWidth(20), layout.getHeight(5),
                Component.literal(selectedRules == CustomTournament.TournamentRules.LEGACY ? 
                    "§a✓ Наследие" : "§7Наследие"),
                (btn) -> {
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
                Button participantButton = createStyledButton(
                    layout.getX(30 + i * 24), participantsY, layout.getWidth(20), layout.getHeight(5),
                    Component.literal(selectedMaxParticipants == finalParticipants ? 
                        "§a✓ " + finalParticipants : "§7" + finalParticipants),
                    (btn) -> {
                        selectedMaxParticipants = finalParticipants;
                        init();
                    }
                );
                this.addRenderableWidget(participantButton);
            }
            
            // Кнопка "Создать"
            Button createButton = createStyledButton(
                layout.getX(40), layout.getY(70), layout.getWidth(20), layout.getHeight(6),
                Component.literal("§a§lСоздать турнир"),
                (btn) -> createTournament()
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка "Отмена"
            Button cancelButton = createStyledButton(
                layout.getX(40), layout.getY(78), layout.getWidth(20), layout.getHeight(5),
                Component.literal("§7Отмена"),
                (btn) -> {
                    mode = ScreenMode.LIST;
                    init();
                }
            );
            this.addRenderableWidget(cancelButton);
        }
    }
    
    private void createTournament() {
        String name = nameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        
        int entryFee = 0;
        try {
            entryFee = Integer.parseInt(entryFeeField.getValue());
        } catch (NumberFormatException e) {
            // Оставляем 0
        }
        
        // Отправляем запрос на создание турнира
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.CreateCustomTournamentPacket(
            name, selectedRules, selectedMaxParticipants, entryFee, 0)); // scheduledStartTime = 0 (сразу)
        
        mode = ScreenMode.LIST;
        init();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
        
        // Рисуем фон
        graphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xC0101010);
        graphics.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + GUI_HEIGHT - 1, 0xFF2C2C2C);
        
        // Заголовок
        graphics.drawString(font, "§6§lПОЛЬЗОВАТЕЛЬСКИЕ ТУРНИРЫ", 
            guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        if (mode == ScreenMode.LIST) {
            // Список турниров
            int listY = guiY + 40;
            for (int i = 0; i < Math.min(tournaments.size(), 10); i++) {
                CustomTournamentInfo info = tournaments.get(i);
                graphics.drawString(font, "§f" + info.name, guiX + 20, listY, 0xFFFFFF, false);
                graphics.drawString(font, "§7Участников: §f" + info.participants + "§7/§f" + info.maxParticipants, 
                    guiX + 20, listY + 12, 0x888888, false);
                graphics.drawString(font, "§7Взнос: §e" + info.entryFee + " монет", 
                    guiX + 20, listY + 24, 0x888888, false);
                
                // Кнопка "Смотреть" для начатых турниров
                if (info.started) {
                    Button watchButton = createStyledButton(
                        guiX + GUI_WIDTH - 120, listY, 100, 20,
                        Component.literal("§aСмотреть"),
                        (btn) -> {
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.NetworkHandler.AddTournamentSpectatorPacket(info.id));
                        }
                    );
                    this.addRenderableWidget(watchButton);
                }
                
                listY += 50;
            }
        } else if (mode == ScreenMode.CREATE) {
            // Форма создания
            graphics.drawString(font, "§6Создание турнира", guiX + 20, guiY + 40, 0xFFFFFF, false);
            graphics.drawString(font, "§7Название:", layout.getX(30), layout.getY(12), 0x888888, false);
            graphics.drawString(font, "§7Взнос (монеты):", layout.getX(30), layout.getY(20), 0x888888, false);
            graphics.drawString(font, "§7Правила:", layout.getX(30), layout.getY(28), 0x888888, false);
            graphics.drawString(font, "§7Максимум участников:", layout.getX(30), layout.getY(35), 0x888888, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
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

