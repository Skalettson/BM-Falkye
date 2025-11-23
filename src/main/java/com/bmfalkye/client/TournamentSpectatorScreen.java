package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Экран для просмотра матчей турнира в качестве зрителя
 */
public class TournamentSpectatorScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 800;
    private static final int BASE_GUI_HEIGHT = 600;
    private static final int MIN_GUI_WIDTH = 700;
    private static final int MIN_GUI_HEIGHT = 500;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private final Screen parentScreen;
    private final String tournamentId;
    private final String tournamentName;
    
    private List<MatchInfo> activeMatches = new ArrayList<>();
    
    public TournamentSpectatorScreen(Screen parentScreen, String tournamentId, String tournamentName) {
        super(Component.literal("§6§lТРАНСЛЯЦИЯ ТУРНИРА"));
        this.parentScreen = parentScreen;
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        requestMatches();
    }
    
    private void requestMatches() {
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.RequestTournamentMatchesPacket(tournamentId));
    }
    
    public void updateMatches(List<MatchInfo> matches) {
        this.activeMatches = new ArrayList<>(matches);
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
                }
            }
        );
        this.addRenderableWidget(backButton);
        
        // Кнопка "Обновить"
        Button refreshButton = createStyledButton(
            layout.getX(80), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§eОбновить"),
            (btn) -> requestMatches()
        );
        this.addRenderableWidget(refreshButton);
        
        // Кнопки для каждого матча
        int matchY = layout.getY(15);
        for (int i = 0; i < activeMatches.size() && i < 5; i++) {
            MatchInfo match = activeMatches.get(i);
            
            Button watchButton = createStyledButton(
                layout.getX(5), matchY, layout.getWidth(40), layout.getHeight(8),
                Component.literal("§aСмотреть: §f" + match.player1Name + " §7vs §f" + match.player2Name),
                (btn) -> watchMatch(match)
            );
            this.addRenderableWidget(watchButton);
            
            matchY += layout.getHeight(10);
        }
    }
    
    private void watchMatch(MatchInfo match) {
        // Отправляем запрос на просмотр матча
        NetworkHandler.INSTANCE.sendToServer(
            new NetworkHandler.WatchTournamentMatchPacket(tournamentId, match.player1UUID, match.player2UUID));
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
        graphics.drawString(font, "§6§lТРАНСЛЯЦИЯ ТУРНИРА", guiX + 20, guiY + 15, 0xFFFFFF, false);
        graphics.drawString(font, "§7" + tournamentName, guiX + 20, guiY + 30, 0x888888, false);
        
        // Список активных матчей
        if (activeMatches.isEmpty()) {
            graphics.drawString(font, "§7Нет активных матчей", 
                guiX + 20, guiY + 100, 0x888888, false);
        } else {
            graphics.drawString(font, "§eАктивные матчи:", guiX + 20, guiY + 60, 0xFFFFFF, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
    
    /**
     * Информация о матче для отображения
     */
    public static class MatchInfo {
        public final String tournamentId;
        public final int round;
        public final UUID player1UUID;
        public final String player1Name;
        public final UUID player2UUID;
        public final String player2Name;
        public final int currentGameRound;
        public final int roundsWon1;
        public final int roundsWon2;
        
        public MatchInfo(String tournamentId, int round, UUID player1UUID, String player1Name,
                        UUID player2UUID, String player2Name, int currentGameRound,
                        int roundsWon1, int roundsWon2) {
            this.tournamentId = tournamentId;
            this.round = round;
            this.player1UUID = player1UUID;
            this.player1Name = player1Name;
            this.player2UUID = player2UUID;
            this.player2Name = player2Name;
            this.currentGameRound = currentGameRound;
            this.roundsWon1 = roundsWon1;
            this.roundsWon2 = roundsWon2;
        }
    }
}

