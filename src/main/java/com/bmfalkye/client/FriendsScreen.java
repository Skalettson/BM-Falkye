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
 * Экран управления друзьями
 */
public class FriendsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 400;
    private static final int BASE_GUI_HEIGHT = 300;
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private java.util.List<java.util.UUID> friendIds = new java.util.ArrayList<>();
    private java.util.List<String> friendNames = new java.util.ArrayList<>();
    private int scrollOffset = 0;
    private static final int FRIENDS_PER_PAGE = 8;
    
    public FriendsScreen(Screen parent) {
        super(Component.literal("Друзья"));
        this.parentScreen = parent;
    }
    
    public void updateFriendsList(java.util.List<java.util.UUID> ids, java.util.List<String> names) {
        this.friendIds = ids;
        this.friendNames = names;
        this.init(); // Переинициализируем экран для обновления кнопок
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 0.85, 350, 250);
        
        // Кнопка "Назад" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(85), layout.getWidth(20), 20,
            Component.literal("Назад"), b -> onClose()));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопка "Добавить друга" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(30), layout.getY(10), layout.getWidth(40), 20,
            Component.literal("Добавить друга"), b -> {
            openAddFriendDialog();
        }));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Кнопка "Обновить" - скевоморфный стиль
        buttons.add(GuiUtils.createStyledButton(layout.getX(75), layout.getY(10), layout.getWidth(20), 20,
            Component.literal("Обновить"), b -> {
            requestFriendsList();
        }));
        addRenderableWidget(buttons.get(buttons.size() - 1));
        
        // Отображаем список друзей
        int startY = layout.getY(35);
        int visibleCount = Math.min(friendIds.size() - scrollOffset, FRIENDS_PER_PAGE);
        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index < friendIds.size()) {
                final java.util.UUID friendId = friendIds.get(index);
                String friendName = index < friendNames.size() ? friendNames.get(index) : "Неизвестный";
                
                // Кнопка друга с действиями - скевоморфный стиль
                int yPos = startY + i * 25;
                buttons.add(GuiUtils.createStyledButton(layout.getX(5), yPos, layout.getWidth(60), 20,
                    Component.literal(friendName), b -> {
                    // Действие при клике на друга
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
                
                // Кнопка "Вызвать на дуэль" - скевоморфный стиль
                buttons.add(GuiUtils.createStyledButton(layout.getX(70), yPos, layout.getWidth(15), 20,
                    Component.literal("Дуэль"), b -> {
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                        new com.bmfalkye.network.NetworkHandler.PlayCardPacket("", 0)); // Временная заглушка
                    minecraft.player.sendSystemMessage(Component.literal("§eИспользуйте команду /falkye challenge для вызова"));
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
                
                // Кнопка "Удалить" - скевоморфный стиль
                buttons.add(GuiUtils.createStyledButton(layout.getX(90), yPos, layout.getWidth(5), 20,
                    Component.literal("✕"), b -> {
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                        new com.bmfalkye.network.FriendsPackets.RemoveFriendPacket(friendId));
                }));
                addRenderableWidget(buttons.get(buttons.size() - 1));
            }
        }
        
        // Кнопки прокрутки - скевоморфный стиль
        if (scrollOffset > 0) {
            buttons.add(GuiUtils.createStyledButton(layout.getX(5), layout.getY(80), layout.getWidth(10), 15,
                Component.literal("↑"), b -> {
                scrollOffset = Math.max(0, scrollOffset - FRIENDS_PER_PAGE);
                init();
            }));
            addRenderableWidget(buttons.get(buttons.size() - 1));
        }
        
        if (scrollOffset + FRIENDS_PER_PAGE < friendIds.size()) {
            buttons.add(GuiUtils.createStyledButton(layout.getX(20), layout.getY(80), layout.getWidth(10), 15,
                Component.literal("↓"), b -> {
                scrollOffset = Math.min(friendIds.size() - FRIENDS_PER_PAGE, scrollOffset + FRIENDS_PER_PAGE);
                init();
            }));
            addRenderableWidget(buttons.get(buttons.size() - 1));
        }
        
        // Запрашиваем список друзей при открытии экрана
        if (friendIds.isEmpty()) {
            requestFriendsList();
        }
    }
    
    private void requestFriendsList() {
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.FriendsPackets.RequestFriendsPacket());
    }
    
    private void openAddFriendDialog() {
        // Создаём диалог с EditBox для ввода имени друга
        net.minecraft.client.gui.components.EditBox nameBox = new net.minecraft.client.gui.components.EditBox(
            font, layout.getX(30), layout.getY(50), layout.getWidth(40), 20, 
            Component.literal("Имя игрока"));
        nameBox.setMaxLength(16);
        nameBox.setValue("");
        
        // Создаём временный экран-диалог
        net.minecraft.client.gui.screens.Screen dialog = new net.minecraft.client.gui.screens.Screen(Component.literal("Добавить друга")) {
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
                
                // Кнопка "Добавить"
                addRenderableWidget(GuiUtils.createStyledButton(
                    layout.getX(30), layout.getY(75), layout.getWidth(18), 20,
                    Component.literal("Добавить"), b -> {
                        String friendName = nameBox.getValue().trim();
                        if (!friendName.isEmpty()) {
                            // Отправляем пакет на сервер для добавления друга по имени
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket(friendName));
                            minecraft.setScreen(FriendsScreen.this);
                        }
                    }));
                
                // Кнопка "Отмена"
                addRenderableWidget(GuiUtils.createStyledButton(
                    layout.getX(52), layout.getY(75), layout.getWidth(18), 20,
                    Component.literal("Отмена"), b -> {
                        minecraft.setScreen(FriendsScreen.this);
                    }));
            }
            
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // Полупрозрачный фон
                guiGraphics.fill(0, 0, width, height, 0x80000000);
                
                // Рендерим родительский экран
                FriendsScreen.this.render(guiGraphics, -1, -1, partialTick);
                
                // Рендерим диалог
                GuiUtils.drawWoodenPanel(guiGraphics, layout.getX(25), layout.getY(40), 
                    layout.getWidth(50), layout.getHeight(40), true);
                GuiUtils.drawMetalFrame(guiGraphics, layout.getX(25), layout.getY(40), 
                    layout.getWidth(50), layout.getHeight(40), 3, false);
                
                guiGraphics.drawString(font, "Введите имя игрока:", 
                    layout.getX(30), layout.getY(42), 0xFFFFFF);
                
                super.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 256) { // ESC
                    minecraft.setScreen(FriendsScreen.this);
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) { // Enter
                    String friendName = nameBox.getValue().trim();
                    if (!friendName.isEmpty()) {
                        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                            new com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket(friendName));
                        minecraft.setScreen(FriendsScreen.this);
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        
        minecraft.setScreen(dialog);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Рендерим фон GUI
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), true);
        GuiUtils.drawMetalFrame(guiGraphics, layout.getGuiX(), layout.getGuiY(), 
            layout.getGuiWidth(), layout.getGuiHeight(), 3, false);
        
        // Заголовок
        guiGraphics.drawString(font, "Друзья (" + friendIds.size() + ")", layout.getX(5), layout.getY(5), 0xFFFFFF);
        
        // Информация, если нет друзей
        if (friendIds.isEmpty()) {
            guiGraphics.drawString(font, "У вас пока нет друзей", 
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

