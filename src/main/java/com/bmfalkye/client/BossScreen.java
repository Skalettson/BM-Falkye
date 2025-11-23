package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран выбора боссов
 */
public class BossScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 400;
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private int scrollOffset = 0;
    private com.bmfalkye.network.BossPackets.SendBossesPacket bossesData = null;
    private static final int BOSSES_PER_PAGE = 5;
    
    public BossScreen(Screen parent) {
        super(Component.literal("Боссы"));
        this.parentScreen = parent;
    }
    
    public void updateBossesList(com.bmfalkye.network.BossPackets.SendBossesPacket data) {
        this.bossesData = data;
        this.init(); // Переинициализируем экран
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 0.9, 550, 350);
        
        // Кнопка "Назад" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(90), layout.getWidth(15), 20,
            Component.literal("Назад"), b -> onClose()));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопка "Обновить" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(25), layout.getY(10), layout.getWidth(20), 20,
            Component.literal("Обновить"), b -> {
            requestBossesList();
        }));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        if (bossesData != null && !bossesData.getBossIds().isEmpty()) {
            int startY = layout.getY(35);
            int visibleCount = Math.min(bossesData.getBossIds().size() - scrollOffset, BOSSES_PER_PAGE);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset + i;
                if (index < bossesData.getBossIds().size()) {
                    final String bossId = bossesData.getBossIds().get(index);
                    String bossName = index < bossesData.getBossNames().size() ? 
                        bossesData.getBossNames().get(index) : "Неизвестный босс";
                    int difficulty = index < bossesData.getBossDifficulties().size() ? 
                        bossesData.getBossDifficulties().get(index) : 0;
                    boolean unlocked = index < bossesData.getBossUnlocked().size() ? 
                        bossesData.getBossUnlocked().get(index) : false;
                    
                    int yPos = startY + i * 60;
                    
                    // Кнопка вызова босса - скевоморфный стиль
                    buttons.add(GuiUtils.createStyledButton(layout.getX(5), yPos, layout.getWidth(25), 20,
                        Component.literal(unlocked ? "Вызвать" : "Заблокирован"), 
                        b -> {
                            if (unlocked) {
                                com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                    new com.bmfalkye.network.BossPackets.ChallengeBossPacket(bossId));
                            } else {
                                minecraft.player.sendSystemMessage(Component.literal(
                                    "§cБосс не разблокирован!"));
                            }
                        }));
                    addRenderableWidget(buttons.get(buttons.size() - 1));
                }
            }
            
            // Кнопки прокрутки - скевоморфный стиль
            if (scrollOffset > 0) {
                buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(85), layout.getWidth(10), 15,
                    Component.literal("↑"), b -> {
                    scrollOffset = Math.max(0, scrollOffset - BOSSES_PER_PAGE);
                    init();
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
            }
            
            if (scrollOffset + BOSSES_PER_PAGE < bossesData.getBossIds().size()) {
                buttons.add(GuiUtils.createStyledButton(layout.getX(20), layout.getY(85), layout.getWidth(10), 15,
                    Component.literal("↓"), b -> {
                    scrollOffset = Math.min(bossesData.getBossIds().size() - BOSSES_PER_PAGE, 
                        scrollOffset + BOSSES_PER_PAGE);
                    init();
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
            }
        }
        
        // Запрашиваем список боссов при открытии
        if (bossesData == null) {
            requestBossesList();
        }
    }
    
    private void requestBossesList() {
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.BossPackets.RequestBossesPacket());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), true);
        GuiUtils.drawMetalFrame(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), 3, false);
        
        guiGraphics.drawString(font, "Боссы", layout.getX(5), layout.getY(5), 0xFFFFFF);
        
        if (bossesData != null && !bossesData.getBossIds().isEmpty()) {
            int startY = layout.getY(35);
            int visibleCount = Math.min(bossesData.getBossIds().size() - scrollOffset, BOSSES_PER_PAGE);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset + i;
                if (index < bossesData.getBossIds().size()) {
                    String bossName = index < bossesData.getBossNames().size() ? 
                        bossesData.getBossNames().get(index) : "Неизвестный босс";
                    String description = index < bossesData.getBossDescriptions().size() ? 
                        bossesData.getBossDescriptions().get(index) : "";
                    int difficulty = index < bossesData.getBossDifficulties().size() ? 
                        bossesData.getBossDifficulties().get(index) : 0;
                    boolean unlocked = index < bossesData.getBossUnlocked().size() ? 
                        bossesData.getBossUnlocked().get(index) : false;
                    
                    int yPos = startY + i * 60;
                    int color = unlocked ? 0xFFFFFF : 0x888888;
                    
                    guiGraphics.drawString(font, bossName, layout.getX(35), yPos, color);
                    if (!description.isEmpty()) {
                        guiGraphics.drawString(font, description, layout.getX(35), yPos + 12, 
                            0xCCCCCC, false);
                    }
                    guiGraphics.drawString(font, "Сложность: " + difficulty + "/10", 
                        layout.getX(35), yPos + 24, 0xAAAAAA);
                }
            }
        } else {
            guiGraphics.drawString(font, "Нет доступных боссов", 
                layout.getX(5), layout.getY(50), 0xAAAAAA);
        }
        
        // Кастомный рендеринг кнопок в скевоморфном стиле
        for (Button button : buttons) {
            if (button != null) {
                GuiUtils.renderStyledButton(guiGraphics, font, button, mouseX, mouseY, false);
            }
        }
        
        // Рендерим виджеты (EditBox и т.д.), но НЕ кнопки (они уже отрисованы)
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    @Override
    public void onClose() {
        if (parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
}

