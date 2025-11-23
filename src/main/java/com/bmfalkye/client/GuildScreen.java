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
 * Экран управления гильдией
 */
public class GuildScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 500;
    private static final int BASE_GUI_HEIGHT = 400;
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private com.bmfalkye.network.GuildPackets.SendGuildInfoPacket guildInfo = null;
    
    public GuildScreen(Screen parent) {
        super(Component.literal("Гильдия"));
        this.parentScreen = parent;
    }
    
    public void updateGuildInfo(com.bmfalkye.network.GuildPackets.SendGuildInfoPacket info) {
        this.guildInfo = info;
        this.init(); // Переинициализируем экран
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 0.85, 450, 350);
        
        // Кнопка "Назад" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(90), layout.getWidth(15), 20,
            Component.literal("Назад"), b -> onClose()));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопка "Обновить" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(25), layout.getY(10), layout.getWidth(20), 20,
            Component.literal("Обновить"), b -> {
            requestGuildInfo();
        }));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        if (guildInfo == null || guildInfo.getGuildId().isEmpty()) {
            // Игрок не в гильдии - скевоморфный стиль
            buttons.add(GuiUtils.createStyledButton(layout.getX(50), layout.getY(10), layout.getWidth(30), 20,
                Component.literal("Создать гильдию"), b -> {
                openCreateGuildDialog();
            }));
            addRenderableWidget(buttons.get(buttons.size() - 1));
        } else {
            // Игрок в гильдии - кнопка "Покинуть гильдию" - скевоморфный стиль
            buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(85), layout.getWidth(30), 20,
                Component.literal("Покинуть гильдию"), b -> {
                leaveGuild();
            }));
            addRenderableWidget(buttons.get(buttons.size() - 1));
        }
        
        // Запрашиваем информацию о гильдии при открытии
        if (guildInfo == null) {
            requestGuildInfo();
        }
    }
    
    private void requestGuildInfo() {
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.GuildPackets.RequestGuildInfoPacket());
    }
    
    private void openCreateGuildDialog() {
        // Создаём диалог с EditBox для ввода имени и описания гильдии
        net.minecraft.client.gui.components.EditBox nameBox = new net.minecraft.client.gui.components.EditBox(
            font, layout.getX(30), layout.getY(50), layout.getWidth(40), 20, 
            Component.literal("Название гильдии"));
        nameBox.setMaxLength(30);
        nameBox.setValue("");
        
        net.minecraft.client.gui.components.EditBox descBox = new net.minecraft.client.gui.components.EditBox(
            font, layout.getX(30), layout.getY(70), layout.getWidth(40), 20, 
            Component.literal("Описание"));
        descBox.setMaxLength(100);
        descBox.setValue("");
        
        // Создаём временный экран-диалог
        net.minecraft.client.gui.screens.Screen dialog = new net.minecraft.client.gui.screens.Screen(Component.literal("Создать гильдию")) {
            @Override
            protected void init() {
                super.init();
                clearWidgets();
                
                // EditBox для ввода имени
                nameBox.setX(layout.getX(30));
                nameBox.setY(layout.getY(50));
                nameBox.setWidth(layout.getWidth(40));
                addRenderableWidget(nameBox);
                setInitialFocus(nameBox);
                
                // EditBox для ввода описания
                descBox.setX(layout.getX(30));
                descBox.setY(layout.getY(70));
                descBox.setWidth(layout.getWidth(40));
                addRenderableWidget(descBox);
                
                // Кнопка "Создать"
                addRenderableWidget(GuiUtils.createStyledButton(
                    layout.getX(30), layout.getY(95), layout.getWidth(18), 20,
                    Component.literal("Создать"), b -> {
                        String guildName = nameBox.getValue().trim();
                        String guildDesc = descBox.getValue().trim();
                        if (!guildName.isEmpty()) {
                            // Отправляем пакет на сервер для создания гильдии
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.GuildPackets.CreateGuildPacket(guildName, guildDesc));
                            minecraft.setScreen(GuildScreen.this);
                            // Запрашиваем обновлённую информацию о гильдии
                            requestGuildInfo();
                        }
                    }));
                
                // Кнопка "Отмена"
                addRenderableWidget(GuiUtils.createStyledButton(
                    layout.getX(52), layout.getY(95), layout.getWidth(18), 20,
                    Component.literal("Отмена"), b -> {
                        minecraft.setScreen(GuildScreen.this);
                    }));
            }
            
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // Полупрозрачный фон
                guiGraphics.fill(0, 0, width, height, 0x80000000);
                
                // Рендерим родительский экран
                GuildScreen.this.render(guiGraphics, -1, -1, partialTick);
                
                // Рендерим диалог
                GuiUtils.drawWoodenPanel(guiGraphics, layout.getX(25), layout.getY(40), 
                    layout.getWidth(50), layout.getHeight(60), true);
                GuiUtils.drawMetalFrame(guiGraphics, layout.getX(25), layout.getY(40), 
                    layout.getWidth(50), layout.getHeight(60), 3, false);
                
                guiGraphics.drawString(font, "Создание гильдии", 
                    layout.getX(30), layout.getY(42), 0xFFFFFF);
                guiGraphics.drawString(font, "Название:", 
                    layout.getX(30), layout.getY(45), 0xCCCCCC);
                guiGraphics.drawString(font, "Описание:", 
                    layout.getX(30), layout.getY(65), 0xCCCCCC);
                
                super.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 256) { // ESC
                    minecraft.setScreen(GuildScreen.this);
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) { // Enter
                    String guildName = nameBox.getValue().trim();
                    String guildDesc = descBox.getValue().trim();
                    if (!guildName.isEmpty()) {
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                            new com.bmfalkye.network.GuildPackets.CreateGuildPacket(guildName, guildDesc));
                        minecraft.setScreen(GuildScreen.this);
                        requestGuildInfo();
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        
        minecraft.setScreen(dialog);
    }
    
    private void leaveGuild() {
        // Отправляем пакет на сервер для покидания гильдии
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.GuildPackets.LeaveGuildPacket());
        // Запрашиваем обновлённую информацию (должна быть пустой)
        requestGuildInfo();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), true);
        GuiUtils.drawMetalFrame(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), 3, false);
        
        guiGraphics.drawString(font, "Гильдия", layout.getX(5), layout.getY(5), 0xFFFFFF);
        
        if (guildInfo != null && !guildInfo.getGuildId().isEmpty()) {
            int yPos = layout.getY(35);
            guiGraphics.drawString(font, "Гильдия: " + guildInfo.getName(), 
                layout.getX(5), yPos, 0xFFFFFF);
            yPos += 15;
            guiGraphics.drawString(font, "Описание: " + guildInfo.getDescription(), 
                layout.getX(5), yPos, 0xCCCCCC);
            yPos += 15;
            guiGraphics.drawString(font, "Уровень: " + guildInfo.getLevel() + 
                " (Опыт: " + guildInfo.getXp() + ")", layout.getX(5), yPos, 0xCCCCCC);
            yPos += 15;
            guiGraphics.drawString(font, "Участников: " + guildInfo.getMemberIds().size() + 
                "/" + guildInfo.getMaxMembers(), layout.getX(5), yPos, 0xCCCCCC);
            
            // Список участников
            yPos += 20;
            guiGraphics.drawString(font, "Участники:", layout.getX(5), yPos, 0xFFFFFF);
            yPos += 15;
            int memberCount = Math.min(guildInfo.getMemberIds().size(), 10);
            for (int i = 0; i < memberCount; i++) {
                String memberName = i < guildInfo.getMemberNames().size() ? 
                    guildInfo.getMemberNames().get(i) : "Неизвестный";
                boolean isLeader = guildInfo.getLeader() != null && 
                    guildInfo.getLeader().equals(guildInfo.getMemberIds().get(i));
                guiGraphics.drawString(font, (isLeader ? "★ " : "  ") + memberName, 
                    layout.getX(10), yPos, isLeader ? 0xFFFF00 : 0xCCCCCC);
                yPos += 12;
            }
        } else {
            guiGraphics.drawString(font, "Вы не состоите в гильдии", 
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

