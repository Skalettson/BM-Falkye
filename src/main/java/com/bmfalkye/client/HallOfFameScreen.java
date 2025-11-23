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
 * Экран Зала Славы с визуальными портретами топ-игроков
 */
public class HallOfFameScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 900;
    private static final int BASE_GUI_HEIGHT = 700;
    private static final int MIN_GUI_WIDTH = 800;
    private static final int MIN_GUI_HEIGHT = 600;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    private AdaptiveLayout layout;
    
    private final Screen parentScreen;
    
    private List<HallOfFameEntry> hallOfFame = new ArrayList<>();
    private List<LeaderboardEntry> weeklyLeaderboard = new ArrayList<>();
    
    private Tab currentTab = Tab.HALL_OF_FAME;
    
    public HallOfFameScreen(Screen parentScreen) {
        super(Component.literal("§6§lЗАЛ СЛАВЫ"));
        this.parentScreen = parentScreen;
        requestData();
    }
    
    private void requestData() {
        NetworkHandler.INSTANCE.sendToServer(new NetworkHandler.RequestLeaderboardPacket());
    }
    
    public void updateHallOfFame(List<HallOfFameEntry> entries) {
        this.hallOfFame = new ArrayList<>(entries);
    }
    
    public void updateWeeklyLeaderboard(List<LeaderboardEntry> entries) {
        this.weeklyLeaderboard = new ArrayList<>(entries);
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
        
        // Вкладки
        Button hallOfFameTab = createStyledButton(
            layout.getX(20), layout.getY(2), layout.getWidth(25), layout.getHeight(5),
            Component.literal(currentTab == Tab.HALL_OF_FAME ? 
                "§6§lЗал Славы" : "§7Зал Славы"),
            (btn) -> {
                currentTab = Tab.HALL_OF_FAME;
                init();
            }
        );
        this.addRenderableWidget(hallOfFameTab);
        
        Button weeklyTab = createStyledButton(
            layout.getX(47), layout.getY(2), layout.getWidth(25), layout.getHeight(5),
            Component.literal(currentTab == Tab.WEEKLY ? 
                "§6§lЕженедельный" : "§7Еженедельный"),
            (btn) -> {
                currentTab = Tab.WEEKLY;
                init();
            }
        );
        this.addRenderableWidget(weeklyTab);
        
        // Кнопка "Обновить"
        Button refreshButton = createStyledButton(
            layout.getX(80), layout.getY(2), layout.getWidth(15), layout.getHeight(5),
            Component.literal("§eОбновить"),
            (btn) -> requestData()
        );
        this.addRenderableWidget(refreshButton);
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
        graphics.drawString(font, "§6§lЗАЛ СЛАВЫ", guiX + 20, guiY + 15, 0xFFFFFF, false);
        
        if (currentTab == Tab.HALL_OF_FAME) {
            renderHallOfFame(graphics, guiX, guiY);
        } else if (currentTab == Tab.WEEKLY) {
            renderWeeklyLeaderboard(graphics, guiX, guiY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderHallOfFame(GuiGraphics graphics, int guiX, int guiY) {
        graphics.drawString(font, "§eЛегенды всех сезонов:", guiX + 20, guiY + 50, 0xFFFFFF, false);
        
        int y = guiY + 70;
        for (int i = 0; i < Math.min(hallOfFame.size(), 20); i++) {
            HallOfFameEntry entry = hallOfFame.get(i);
            
            // Портрет (заглушка - можно заменить на реальный портрет)
            graphics.fill(guiX + 20, y, guiX + 50, y + 30, 0xFF444444);
            graphics.drawString(font, "§7" + (i + 1), guiX + 25, y + 10, 0xFFFFFF, false);
            
            // Имя и сезон
            graphics.drawString(font, "§f" + entry.playerName, guiX + 60, y + 5, 0xFFFFFF, false);
            graphics.drawString(font, "§7Сезон " + entry.season, guiX + 60, y + 18, 0x888888, false);
            
            y += 35;
        }
        
        if (hallOfFame.isEmpty()) {
            graphics.drawString(font, "§7Пока нет легенд", guiX + 20, guiY + 100, 0x888888, false);
        }
    }
    
    private void renderWeeklyLeaderboard(GuiGraphics graphics, int guiX, int guiY) {
        graphics.drawString(font, "§eЕженедельный рейтинг (Топ-50):", guiX + 20, guiY + 50, 0xFFFFFF, false);
        
        int y = guiY + 70;
        for (int i = 0; i < Math.min(weeklyLeaderboard.size(), 20); i++) {
            LeaderboardEntry entry = weeklyLeaderboard.get(i);
            
            // Место
            String positionColor = i < 3 ? "§6" : "§7";
            graphics.drawString(font, positionColor + (i + 1) + ".", guiX + 20, y, 0xFFFFFF, false);
            
            // Имя
            graphics.drawString(font, "§f" + entry.playerName, guiX + 50, y, 0xFFFFFF, false);
            
            // Рейтинг
            graphics.drawString(font, "§e" + entry.rating, guiX + GUI_WIDTH - 100, y, 0xFFFFFF, false);
            
            y += 20;
        }
        
        if (weeklyLeaderboard.isEmpty()) {
            graphics.drawString(font, "§7Рейтинг пуст", guiX + 20, guiY + 100, 0x888888, false);
        }
    }
    
    private Button createStyledButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress)
            .bounds(x, y, width, height)
            .build();
    }
    
    private enum Tab {
        HALL_OF_FAME, WEEKLY
    }
    
    /**
     * Запись в Зале Славы
     */
    public static class HallOfFameEntry {
        public final UUID playerUUID;
        public final String playerName;
        public final int season;
        
        public HallOfFameEntry(UUID playerUUID, String playerName, int season) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.season = season;
        }
    }
    
    /**
     * Запись в еженедельном рейтинге
     */
    public static class LeaderboardEntry {
        public final UUID playerUUID;
        public final String playerName;
        public final int rating;
        
        public LeaderboardEntry(UUID playerUUID, String playerName, int rating) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.rating = rating;
        }
    }
}

