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
 * Экран управления гильдией
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class GuildScreen extends Screen {
    private static final int BASE_GUI_WIDTH = 700;
    private static final int BASE_GUI_HEIGHT = 550;
    private static final int MIN_GUI_WIDTH = 600;
    private static final int MIN_GUI_HEIGHT = 450;
    private static final double MAX_SCREEN_RATIO = 0.9;
    
    private AdaptiveLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private Screen parentScreen;
    private com.bmfalkye.network.GuildPackets.SendGuildInfoPacket guildInfo = null;
    
    private int memberScrollOffset = 0;
    private static final int MEMBER_ENTRY_HEIGHT = 25;
    private static final int MEMBER_ENTRY_SPACING = 3;
    
    public GuildScreen(Screen parent) {
        super(Component.translatable("screen.bm_falkye.guild_title"));
        this.parentScreen = parent;
    }
    
    public void updateGuildInfo(com.bmfalkye.network.GuildPackets.SendGuildInfoPacket info) {
        this.guildInfo = info;
        this.memberScrollOffset = 0;
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
        
        // Кнопка "Обновить"
        Button refreshButton = GuiUtils.createStyledButton(
            layout.getRightX(layout.getWidth(15), 2), layout.getY(2), 
            layout.getWidth(15), layout.getHeight(5),
            Component.translatable("gui.refresh").withStyle(ChatFormatting.YELLOW),
            b -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                requestGuildInfo();
            });
        buttons.add(refreshButton);
        addRenderableWidget(refreshButton);
        
        if (guildInfo == null || (guildInfo.getGuildId() != null && guildInfo.getGuildId().isEmpty())) {
            // Игрок не в гильдии - кнопка "Создать гильдию"
            Button createButton = GuiUtils.createStyledButton(
                layout.getCenteredX(layout.getWidth(30)), layout.getY(40), 
                layout.getWidth(30), layout.getHeight(8),
                Component.translatable("screen.bm_falkye.create_guild").withStyle(ChatFormatting.GREEN),
                b -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    openCreateGuildDialog();
                });
            buttons.add(createButton);
            addRenderableWidget(createButton);
        } else {
            // Игрок в гильдии - кнопка "Покинуть гильдию"
            Button leaveButton = GuiUtils.createStyledButton(
                layout.getX(2), layout.getBottomY(layout.getHeight(5), 8), 
                layout.getWidth(25), layout.getHeight(5),
                Component.translatable("screen.bm_falkye.leave_guild").withStyle(ChatFormatting.RED),
                b -> {
                    com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                    leaveGuild();
                });
            buttons.add(leaveButton);
            addRenderableWidget(leaveButton);
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
        if (minecraft == null) return;
        
        EditBox nameBox = new EditBox(
            this.font, 0, 0, 200, 20, 
            Component.translatable("screen.bm_falkye.guild_name"));
        nameBox.setMaxLength(30);
        nameBox.setValue("");
        
        EditBox descBox = new EditBox(
            this.font, 0, 0, 200, 20, 
            Component.translatable("screen.bm_falkye.guild_description"));
        descBox.setMaxLength(100);
        descBox.setValue("");
        
        Screen dialog = new Screen(Component.translatable("screen.bm_falkye.create_guild")) {
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
                    this.dialogLayout = new AdaptiveLayout(this, 500, 300, 0.9, 450, 250);
                }
                
                // EditBox для ввода имени
                nameBox.setX(dialogLayout.getX(25));
                nameBox.setY(dialogLayout.getY(35));
                nameBox.setWidth(dialogLayout.getWidth(50));
                this.addRenderableWidget(nameBox);
                this.setInitialFocus(nameBox);
                
                // EditBox для ввода описания
                descBox.setX(dialogLayout.getX(25));
                descBox.setY(dialogLayout.getY(50));
                descBox.setWidth(dialogLayout.getWidth(50));
                this.addRenderableWidget(descBox);
                
                // Кнопка "Создать"
                Button createButton = GuiUtils.createStyledButton(
                    dialogLayout.getX(25), dialogLayout.getY(75), 
                    dialogLayout.getWidth(20), dialogLayout.getHeight(8),
                    Component.translatable("screen.bm_falkye.create").withStyle(ChatFormatting.GREEN),
                    b -> {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        String guildName = nameBox.getValue().trim();
                        String guildDesc = descBox.getValue().trim();
                        if (!guildName.isEmpty() && minecraft != null) {
                            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                                new com.bmfalkye.network.GuildPackets.CreateGuildPacket(guildName, guildDesc));
                            minecraft.setScreen(GuildScreen.this);
                            requestGuildInfo();
                        }
                    });
                this.addRenderableWidget(createButton);
                
                // Кнопка "Отмена"
                Button cancelButton = GuiUtils.createStyledButton(
                    dialogLayout.getX(55), dialogLayout.getY(75), 
                    dialogLayout.getWidth(20), dialogLayout.getHeight(8),
                    Component.translatable("gui.cancel").withStyle(ChatFormatting.GRAY),
                    b -> {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        if (minecraft != null) {
                            minecraft.setScreen(GuildScreen.this);
                        }
                    });
                this.addRenderableWidget(cancelButton);
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // Полупрозрачный фон
                guiGraphics.fill(0, 0, width, height, 0x80000000);
                
                // Рендерим родительский экран
                GuildScreen.this.render(guiGraphics, -1, -1, partialTick);
                
                // Рендерим диалог
                int dialogX = dialogLayout.getX(20);
                int dialogY = dialogLayout.getY(25);
                int dialogWidth = dialogLayout.getWidth(60);
                int dialogHeight = dialogLayout.getHeight(50);
                
                GuiUtils.drawWoodenPanel(guiGraphics, dialogX, dialogY, dialogWidth, dialogHeight, true);
                GuiUtils.drawMetalFrame(guiGraphics, dialogX, dialogY, dialogWidth, dialogHeight, 3, false);
                
                // Заголовок диалога
                MutableComponent title = Component.translatable("screen.bm_falkye.create_guild")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                int titleX = dialogLayout.getCenteredX(this.font.width(title));
                guiGraphics.drawString(this.font, title, titleX, dialogY + 10, 0xFFFFFF, false);
                
                // Подсказки
                MutableComponent nameHint = Component.translatable("screen.bm_falkye.guild_name")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                guiGraphics.drawString(this.font, nameHint, dialogX + 10, dialogY + 30, 0xFFFFFF, false);
                
                MutableComponent descHint = Component.translatable("screen.bm_falkye.guild_description")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                guiGraphics.drawString(this.font, descHint, dialogX + 10, dialogY + 45, 0xFFFFFF, false);
                
                super.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 256) { // ESC
                    if (minecraft != null) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                        minecraft.setScreen(GuildScreen.this);
                    }
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) { // Enter
                    String guildName = nameBox.getValue().trim();
                    String guildDesc = descBox.getValue().trim();
                    if (!guildName.isEmpty() && minecraft != null) {
                        com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
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
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
            new com.bmfalkye.network.GuildPackets.LeaveGuildPacket());
        requestGuildInfo();
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
        MutableComponent title = Component.translatable("screen.bm_falkye.guild_title")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(3);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        if (guildInfo != null && guildInfo.getGuildId() != null && !guildInfo.getGuildId().isEmpty()) {
            // Информация о гильдии
            int infoY = layout.getY(12);
            int infoX = layout.getX(5);
            int lineHeight = 16;
            
            // Название гильдии
            MutableComponent guildName = Component.literal(guildInfo.getName())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
            guiGraphics.drawString(this.font, guildName, infoX, infoY, 0xFFFFFF, false);
            infoY += lineHeight + 5;
            
            // Описание
            String description = guildInfo.getDescription();
            if (description != null && !description.isEmpty()) {
                MutableComponent desc = Component.literal(description)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(desc, layout.getWidth(90));
                for (net.minecraft.util.FormattedCharSequence line : descLines) {
                    guiGraphics.drawString(this.font, line, infoX, infoY, 0xCCCCCC, false);
                    infoY += lineHeight;
                    if (infoY > layout.getY(40)) break; // Ограничиваем высоту
                }
            }
            infoY += 5;
            
            // Разделитель
            guiGraphics.fill(infoX, infoY, layout.getRightX(layout.getWidth(90), 5), 
                infoY + 1, 0xFF8B7355);
            infoY += 10;
            
            // Уровень и опыт
            MutableComponent level = Component.translatable("screen.bm_falkye.guild_level", 
                guildInfo.getLevel())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
            guiGraphics.drawString(this.font, level, infoX, infoY, 0xFFFFFF, false);
            
            MutableComponent xp = Component.translatable("screen.bm_falkye.guild_experience", 
                guildInfo.getXp())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int xpX = layout.getRightX(this.font.width(xp), 5);
            guiGraphics.drawString(this.font, xp, xpX, infoY, 0xFFFFFF, false);
            infoY += lineHeight;
            
            // Участников
            int memberCount = guildInfo.getMemberIds() != null ? guildInfo.getMemberIds().size() : 0;
            int maxMembers = guildInfo.getMaxMembers();
            MutableComponent members = Component.translatable("screen.bm_falkye.guild_members", 
                memberCount, maxMembers)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
            guiGraphics.drawString(this.font, members, infoX, infoY, 0xFFFFFF, false);
            infoY += lineHeight + 10;
            
            // Разделитель
            guiGraphics.fill(infoX, infoY, layout.getRightX(layout.getWidth(90), 5), 
                infoY + 1, 0xFF8B7355);
            infoY += 10;
            
            // Список участников
            MutableComponent membersTitle = Component.translatable("screen.bm_falkye.guild_members_list")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true));
            guiGraphics.drawString(this.font, membersTitle, infoX, infoY, 0xFFFFFF, false);
            infoY += lineHeight + 5;
            
            // Область списка участников
            int membersListY = infoY;
            int membersListHeight = GUI_HEIGHT - membersListY - layout.getHeight(10);
            int membersListX = infoX;
            int membersListWidth = layout.getWidth(90);
            
            // Обрезка списка участников
            guiGraphics.enableScissor(membersListX, membersListY, 
                membersListX + membersListWidth, membersListY + membersListHeight);
            
            renderMembersList(guiGraphics, membersListX, membersListY, 
                membersListWidth, membersListHeight, guildInfo);
            
            guiGraphics.disableScissor();
        } else {
            // Игрок не в гильдии
            MutableComponent noGuild = Component.translatable("screen.bm_falkye.no_guild")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            int noGuildY = layout.getY(40);
            guiGraphics.drawCenteredString(this.font, noGuild, 
                layout.getCenteredX(this.font.width(noGuild)), noGuildY, 0xAAAAAA);
            
            MutableComponent hint = Component.translatable("screen.bm_falkye.create_guild_hint")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
            guiGraphics.drawCenteredString(this.font, hint, 
                layout.getCenteredX(this.font.width(hint)), noGuildY + 20, 0x888888);
        }
        
        // Рендерим кнопки
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderMembersList(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   com.bmfalkye.network.GuildPackets.SendGuildInfoPacket guildInfo) {
        if (guildInfo.getMemberIds() == null || guildInfo.getMemberIds().isEmpty()) {
            MutableComponent empty = Component.translatable("screen.bm_falkye.no_members")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            guiGraphics.drawCenteredString(this.font, empty, 
                layout.getCenteredX(this.font.width(empty)), y + height / 2, 0x888888);
            return;
        }
        
        int visibleCount = height / (MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING);
        int startIndex = memberScrollOffset / (MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING);
        int endIndex = Math.min(startIndex + visibleCount + 1, guildInfo.getMemberIds().size());
        
        int memberY = y - (memberScrollOffset % (MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING));
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= guildInfo.getMemberIds().size()) break;
            
            UUID memberId = guildInfo.getMemberIds().get(i);
            String memberName = i < guildInfo.getMemberNames().size() ? 
                guildInfo.getMemberNames().get(i) : 
                Component.translatable("screen.bm_falkye.unknown").getString();
            boolean isLeader = guildInfo.getLeader() != null && 
                guildInfo.getLeader().equals(memberId);
            
            // Фон записи участника
            int bgColor = isLeader ? 0x66FFAA00 : 0x33000000;
            GuiUtils.drawRoundedRect(guiGraphics, x, memberY, width, MEMBER_ENTRY_HEIGHT, bgColor);
            
            // Имя участника
            MutableComponent name = Component.literal((isLeader ? "★ " : "  ") + memberName)
                .withStyle(Style.EMPTY
                    .withColor(isLeader ? ChatFormatting.GOLD : ChatFormatting.WHITE)
                    .withBold(isLeader));
            guiGraphics.drawString(this.font, name, x + 5, memberY + 8, 0xFFFFFF, false);
            
            // Роль
            if (isLeader) {
                MutableComponent role = Component.translatable("screen.bm_falkye.guild_leader")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
                int roleX = x + width - this.font.width(role) - 5;
                guiGraphics.drawString(this.font, role, roleX, memberY + 8, 0xFFFFFF, false);
            }
            
            memberY += MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || guildInfo == null || 
            guildInfo.getMemberIds() == null || guildInfo.getMemberIds().isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        int membersListY = layout.getY(35);
        int membersListHeight = layout.getGuiHeight() - membersListY - layout.getHeight(10);
        int membersListX = layout.getX(5);
        int membersListWidth = layout.getWidth(90);
        
        if (mouseX >= membersListX && mouseX <= membersListX + membersListWidth &&
            mouseY >= membersListY && mouseY <= membersListY + membersListHeight) {
            
            int totalCount = guildInfo.getMemberIds().size();
            int visibleCount = membersListHeight / (MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING);
            int maxScroll = Math.max(0, totalCount - visibleCount) * 
                (MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING);
            int scrollStep = MEMBER_ENTRY_HEIGHT + MEMBER_ENTRY_SPACING;
            
            if (delta < 0 && memberScrollOffset < maxScroll) {
                memberScrollOffset = Math.min(maxScroll, memberScrollOffset + scrollStep);
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                return true;
            } else if (delta > 0 && memberScrollOffset > 0) {
                memberScrollOffset = Math.max(0, memberScrollOffset - scrollStep);
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
