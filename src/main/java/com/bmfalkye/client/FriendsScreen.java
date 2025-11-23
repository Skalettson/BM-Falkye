package com.bmfalkye.client;

import com.bmfalkye.client.gui.AdaptiveLayout;
import com.bmfalkye.client.gui.GuiUtils;
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
import java.util.UUID;

/**
 * Экран управления друзьями
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class FriendsScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 600;
    private static final int BASE_GUI_HEIGHT = 500;
    private static final int MIN_GUI_WIDTH = 500;
    private static final int MIN_GUI_HEIGHT = 400;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private java.util.List<UUID> friendIds = new java.util.ArrayList<>();
    private java.util.List<String> friendNames = new java.util.ArrayList<>();
    private int scrollOffset = 0;
    
    private static final int FRIEND_ENTRY_HEIGHT = 40;
    private static final int FRIEND_ENTRY_SPACING = 5;
    
    public FriendsScreen(Screen parent) {
        super(Component.translatable("screen.bm_falkye.friends_title"));
        this.parentScreen = parent;
    }
    
    public void updateFriendsList(java.util.List<UUID> ids, java.util.List<String> names) {
        this.friendIds = new java.util.ArrayList<>(ids);
        this.friendNames = new java.util.ArrayList<>(names);
        this.scrollOffset = 0;
        this.init();
    }
    
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buttons.clear();
        
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        
        // Кнопка "Назад"
        Button backButton = GuiUtils.createStyledButton(
            layout.getX(2), layout.getBottomY(layout.getHeight(5), 2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.back").withStyle(ChatFormatting.GRAY),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                this.onClose();
            });
        buttons.add(backButton);
        addRenderableWidget(backButton);
        
        // Кнопка "Добавить друга"
        Button addFriendButton = GuiUtils.createStyledButton(
            layout.getX(25), layout.getY(2), layout.getWidth(25), layout.getHeight(5),
            Component.translatable("screen.bm_falkye.add_friend").withStyle(ChatFormatting.GREEN),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                openAddFriendDialog();
            });
        buttons.add(addFriendButton);
        addRenderableWidget(addFriendButton);
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                requestFriendsList();
            });
        buttons.add(refreshButton);
        addRenderableWidget(refreshButton);
        
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
        if (minecraft == null) return;
        
        EditBox nameBox = new EditBox(
            this.font, 0, 0, 200, 20, 
            Component.translatable("screen.bm_falkye.player_name"));
        nameBox.setMaxLength(16);
        nameBox.setValue("");
        
        Screen dialog = new Screen(Component.translatable("screen.bm_falkye.add_friend")) {
            private AdaptiveLayout dialogLayout;
            
            @Override
            protected void init() {
                super.init();
                this.clearWidgets();
                
                if (layout != null) {
                    this.dialogLayout = new AdaptiveLayout(this, 
                        BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                        MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
                } else {
                    this.dialogLayout = new AdaptiveLayout(this, 400, 200, 0.9, 350, 150);
                }
                
                // EditBox для ввода имени
                nameBox.setX(dialogLayout.getX(25));
                nameBox.setY(dialogLayout.getY(40));
                nameBox.setWidth(dialogLayout.getWidth(50));
                this.addRenderableWidget(nameBox);
                this.setInitialFocus(nameBox);
                
                // Кнопка "Добавить"
                Button addButton = GuiUtils.createStyledButton(
                    dialogLayout.getX(25), dialogLayout.getY(70), 
                    dialogLayout.getWidth(20), dialogLayout.getHeight(8),
                    Component.translatable("screen.bm_falkye.add").withStyle(ChatFormatting.GREEN),
                    b -> {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        String friendName = nameBox.getValue().trim();
                        if (!friendName.isEmpty() && minecraft != null) {
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket(friendName));
                            minecraft.setScreen(FriendsScreen.this);
                        }
                    });
                this.addRenderableWidget(addButton);
                
                // Кнопка "Отмена"
                Button cancelButton = GuiUtils.createStyledButton(
                    dialogLayout.getX(55), dialogLayout.getY(70), 
                    dialogLayout.getWidth(20), dialogLayout.getHeight(8),
                    Component.translatable("gui.cancel").withStyle(ChatFormatting.GRAY),
                    b -> {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        if (minecraft != null) {
                            minecraft.setScreen(FriendsScreen.this);
                        }
                    });
                this.addRenderableWidget(cancelButton);
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // Полупрозрачный фон
                guiGraphics.fill(0, 0, width, height, 0x80000000);
                
                // Рендерим родительский экран
                FriendsScreen.this.render(guiGraphics, -1, -1, partialTick);
                
                // Рендерим диалог
                int dialogX = dialogLayout.getX(20);
                int dialogY = dialogLayout.getY(25);
                int dialogWidth = dialogLayout.getWidth(60);
                int dialogHeight = dialogLayout.getHeight(50);
                
                GuiUtils.drawWoodenPanel(guiGraphics, dialogX, dialogY, dialogWidth, dialogHeight, true);
                GuiUtils.drawMetalFrame(guiGraphics, dialogX, dialogY, dialogWidth, dialogHeight, 3, false);
                
                // Заголовок диалога
                MutableComponent title = Component.translatable("screen.bm_falkye.add_friend")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                int titleX = dialogLayout.getCenteredX(this.font.width(title));
                guiGraphics.drawString(this.font, title, titleX, dialogY + 10, 0xFFFFFF, false);
                
                // Подсказка
                MutableComponent hint = Component.translatable("screen.bm_falkye.enter_player_name")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                guiGraphics.drawString(this.font, hint, dialogX + 10, dialogY + 30, 0xFFFFFF, false);
                
                super.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 256) { // ESC
                    if (minecraft != null) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        minecraft.setScreen(FriendsScreen.this);
                    }
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) { // Enter
                    String friendName = nameBox.getValue().trim();
                    if (!friendName.isEmpty() && minecraft != null) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
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
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int GUI_WIDTH = layout.getGuiWidth();
        int GUI_HEIGHT = layout.getGuiHeight();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.translatable("screen.bm_falkye.friends_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        MutableComponent countText = Component.literal(" (" + friendIds.size() + ")")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        MutableComponent fullTitle = Component.empty().append(title).append(countText);
        
        int titleWidth = this.font.width(fullTitle);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, fullTitle, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, fullTitle, titleX, titleY, 0xFFFFFF, false);
        
        // Область списка друзей
        int listY = layout.getY(10);
        int listHeight = GUI_HEIGHT - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = GUI_WIDTH - listX * 2;
        
        // Обрезка списка
        guiGraphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        
        if (friendIds.isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_friends")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), listY + listHeight / 2, 0xAAAAAA);
        } else {
            renderFriendsList(guiGraphics, listX, listY, listWidth, listHeight, mouseX, mouseY);
        }
        
        guiGraphics.disableScissor();
        
        // Рендерим кнопки
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderFriendsList(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                     int mouseX, int mouseY) {
        int visibleCount = height / (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
        int startIndex = scrollOffset / (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, friendIds.size());
        
        int friendY = y - (scrollOffset % (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= friendIds.size()) break;
            
            UUID friendId = friendIds.get(i);
            String friendName = i < friendNames.size() ? friendNames.get(i) : 
                Component.translatable("screen.bm_falkye.unknown").getString();
            
            boolean isHovered = mouseX >= x && mouseX <= x + width && 
                               mouseY >= friendY && mouseY <= friendY + FRIEND_ENTRY_HEIGHT;
            
            // Фон записи друга
            int bgColor = isHovered ? 0xAAFFFFFF : 0x66000000;
            GuiUtils.drawRoundedRect(guiGraphics, x, friendY, width, FRIEND_ENTRY_HEIGHT, bgColor);
            
            // Рамка
            if (isHovered) {
                GuiUtils.drawRoundedBorder(guiGraphics, x, friendY, width, FRIEND_ENTRY_HEIGHT, 
                    0xFFFFFFFF, 1);
            }
            
            // Имя друга
            MutableComponent name = Component.literal(friendName)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            guiGraphics.drawString(this.font, name, x + 10, friendY + 5, 0xFFFFFF, false);
            
            // Статус (онлайн/оффлайн) - заглушка
            MutableComponent status = Component.translatable("screen.bm_falkye.status_unknown")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int statusX = x + width - this.font.width(status) - 80;
            guiGraphics.drawString(this.font, status, statusX, friendY + 18, 0x888888, false);
            
            // Кнопка "Дуэль"
            int duelButtonX = x + width - 70;
            int duelButtonY = friendY + 5;
            int duelButtonWidth = 60;
            int duelButtonHeight = 30;
            
            boolean duelHovered = mouseX >= duelButtonX && mouseX <= duelButtonX + duelButtonWidth &&
                                 mouseY >= duelButtonY && mouseY <= duelButtonY + duelButtonHeight;
            
            int duelColor = duelHovered ? 0xAAFFAA00 : 0xAA00AA00;
            GuiUtils.drawRoundedRect(guiGraphics, duelButtonX, duelButtonY, 
                duelButtonWidth, duelButtonHeight, duelColor);
            GuiUtils.drawRoundedBorder(guiGraphics, duelButtonX, duelButtonY, 
                duelButtonWidth, duelButtonHeight, 0xFF00AA00, 1);
            
            MutableComponent duelText = Component.translatable("screen.bm_falkye.duel")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            int duelTextX = duelButtonX + (duelButtonWidth - this.font.width(duelText)) / 2;
            guiGraphics.drawString(this.font, duelText, duelTextX, duelButtonY + 10, 0xFFFFFF, false);
            
            friendY += FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !friendIds.isEmpty()) {
            int listY = layout.getY(10);
            int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
            int listX = layout.getX(3);
            int listWidth = layout.getGuiWidth() - listX * 2;
            
            int visibleCount = listHeight / (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
            int startIndex = scrollOffset / (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
            int endIndex = Math.min(startIndex + visibleCount + 1, friendIds.size());
            
            int friendY = listY - (scrollOffset % (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING));
            
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= friendIds.size()) break;
                
                UUID friendId = friendIds.get(i);
                int duelButtonX = listX + listWidth - 70;
                int duelButtonY = friendY + 5;
                int duelButtonWidth = 60;
                int duelButtonHeight = 30;
                
                // Проверяем клик по кнопке "Дуэль"
                if (mouseX >= duelButtonX && mouseX <= duelButtonX + duelButtonWidth &&
                    mouseY >= duelButtonY && mouseY <= duelButtonY + duelButtonHeight) {
                    
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    if (minecraft != null && minecraft.player != null) {
                        MutableComponent message = Component.translatable("screen.bm_falkye.use_challenge_command")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                        minecraft.player.sendSystemMessage(message);
                    }
                    return true;
                }
                
                // Проверяем клик по записи друга для удаления (правой кнопкой)
                if (mouseX >= listX && mouseX <= listX + listWidth &&
                    mouseY >= friendY && mouseY <= friendY + FRIEND_ENTRY_HEIGHT &&
                    button == 1) {
                    
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                        new com.bmfalkye.network.FriendsPackets.RemoveFriendPacket(friendId));
                    return true;
                }
                
                friendY += FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || friendIds.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int listY = layout.getY(10);
        int listHeight = layout.getGuiHeight() - listY - layout.getHeight(10);
        int listX = layout.getX(3);
        int listWidth = layout.getGuiWidth() - listX * 2;
        
        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int visibleCount = listHeight / (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
            int totalCount = friendIds.size();
            int maxScroll = Math.max(0, totalCount - visibleCount) * (FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING);
            int scrollStep = FRIEND_ENTRY_HEIGHT + FRIEND_ENTRY_SPACING;
            
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
    public void onClose() {
        if (parentScreen != null && minecraft != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
