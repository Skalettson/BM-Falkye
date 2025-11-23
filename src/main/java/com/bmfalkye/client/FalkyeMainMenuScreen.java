package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.AdaptiveLayout;
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
 * Главное меню Фальки с кнопками для всех систем
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class FalkyeMainMenuScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 440;
    private static final int BASE_GUI_HEIGHT = 420;
    private static final int MIN_GUI_WIDTH = 360;
    private static final int MIN_GUI_HEIGHT = 320;
    private static final double MAX_SCREEN_RATIO = 0.85;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    // Параметры для прокрутки
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int visibleAreaHeight = 0;
    private static final int BUTTON_HEIGHT = 28;
    private static final int BUTTON_SPACING = 6;
    private static final int HEADER_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 50;
    
    private final List<Button> buttons = new ArrayList<>();
    
    public FalkyeMainMenuScreen() {
        super(Component.empty()); // Заголовок рисуется вручную в render()
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Очищаем все виджеты
        this.clearWidgets();
        buttons.clear();
        
        // Инициализируем систему автоматической адаптации
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Вычисляем доступную высоту для кнопок
        visibleAreaHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT;
        
        int buttonWidth = Math.min(300, layout.getWidth(85));
        int startX = layout.getCenteredX(buttonWidth);
        int startY = layout.getY(15);
        
        // Список всех кнопок меню
        List<MenuButtonInfo> menuButtons = new ArrayList<>();
        Screen mainMenu = this;
        
        menuButtons.add(new MenuButtonInfo("Коллекция карт", ChatFormatting.YELLOW, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new CardCollectionScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Редактор колод", ChatFormatting.YELLOW, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.OpenDeckEditorPacket());
            com.bmfalkye.client.ClientPacketHandler.setMainMenuParent(mainMenu);
        }));
        menuButtons.add(new MenuButtonInfo("Энциклопедия", ChatFormatting.YELLOW, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.OpenEncyclopediaPacket());
            com.bmfalkye.client.ClientPacketHandler.setMainMenuParent(mainMenu);
        }));
        menuButtons.add(new MenuButtonInfo("Эволюция карт", ChatFormatting.GOLD, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new CardEvolutionScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Квесты", ChatFormatting.DARK_PURPLE, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new QuestScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Великий Турнир", ChatFormatting.GOLD, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.StartDraftPacket(false));
        }));
        menuButtons.add(new MenuButtonInfo("Пользовательские Турниры", ChatFormatting.LIGHT_PURPLE, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new CustomTournamentScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Зал Славы", ChatFormatting.GOLD, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new HallOfFameScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Настройки", ChatFormatting.YELLOW, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new com.bmfalkye.client.settings.GameSettingsScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Друзья", ChatFormatting.AQUA, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new FriendsScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Гильдия", ChatFormatting.AQUA, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new GuildScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Боссы", ChatFormatting.GOLD, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new BossScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Магазин карт", ChatFormatting.GREEN, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new CardShopScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Статистика", ChatFormatting.AQUA, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new StatisticsScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Турниры", ChatFormatting.GOLD, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new TournamentScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Сезон", ChatFormatting.GREEN, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new SeasonScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Реплеи", ChatFormatting.GRAY, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new ReplayScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Ежедневные награды", ChatFormatting.LIGHT_PURPLE, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new DailyRewardsScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("События", ChatFormatting.DARK_PURPLE, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            if (minecraft != null) {
                minecraft.setScreen(new EventsScreen(mainMenu));
            }
        }));
        menuButtons.add(new MenuButtonInfo("Правила/Руководство", ChatFormatting.YELLOW, () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            boolean bookOpened = false;
            if (com.bmfalkye.integration.LibraryIntegration.isPatchouliLoaded()) {
                bookOpened = com.bmfalkye.integration.PatchouliIntegration.openBook(
                    new net.minecraft.resources.ResourceLocation("bm_falkye", "tutorial"));
            }
            if (!bookOpened && minecraft != null) {
                minecraft.setScreen(new FalkyeTutorialScreen(mainMenu));
            }
        }));
        
        // Вычисляем общую высоту контента
        contentHeight = menuButtons.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        
        // Ограничиваем скролл
        int maxScroll = Math.max(0, contentHeight - visibleAreaHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Создаём кнопки с учётом прокрутки
        int currentY = startY - scrollOffset;
        int guiY = layout.getGuiY();
        int contentStartY = guiY + HEADER_HEIGHT;
        int contentEndY = guiY + GUI_HEIGHT - FOOTER_HEIGHT;
        
        for (MenuButtonInfo buttonInfo : menuButtons) {
            // Проверяем, видна ли кнопка
            if (currentY + BUTTON_HEIGHT >= contentStartY && currentY <= contentEndY) {
                MutableComponent buttonText = Component.literal(buttonInfo.text)
                    .withStyle(Style.EMPTY.withColor(buttonInfo.color));
                
                Button button = GuiUtils.createStyledButton(
                    startX, currentY, buttonWidth, BUTTON_HEIGHT,
                    buttonText,
                    (btn) -> buttonInfo.action.run()
                );
                this.addRenderableWidget(button);
                buttons.add(button);
            }
            
            currentY += BUTTON_HEIGHT + BUTTON_SPACING;
        }
        
        // Кнопка "Назад" (всегда внизу)
        int backButtonY = layout.getBottomY(BUTTON_HEIGHT, 5);
        Button backButton = GuiUtils.createStyledButton(
            startX, backButtonY, buttonWidth, BUTTON_HEIGHT,
            Component.literal("Назад").withStyle(ChatFormatting.GRAY),
            (btn) -> {
                com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
                this.onClose();
            }
        );
        this.addRenderableWidget(backButton);
        buttons.add(backButton);
    }
    
    /**
     * Информация о кнопке меню
     */
    private static class MenuButtonInfo {
        final String text;
        final ChatFormatting color;
        final Runnable action;
        
        MenuButtonInfo(String text, ChatFormatting color, Runnable action) {
            this.text = text;
            this.color = color;
            this.action = action;
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
            visibleAreaHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT;
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        // Золотая металлическая рамка
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, true);
        
        // Заголовок с тенью
        MutableComponent title = Component.literal("ФАЛЬКИ")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GOLD)
                .withBold(true));
        MutableComponent subtitle = Component.literal("Главное меню")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.YELLOW));
        
        int titleWidth = this.font.width(title);
        int titleX = layout.getCenteredX(titleWidth);
        int titleY = layout.getY(4);
        
        // Тень заголовка
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        int subtitleWidth = this.font.width(subtitle);
        int subtitleX = layout.getCenteredX(subtitleWidth);
        int subtitleY = titleY + this.font.lineHeight + 4;
        guiGraphics.drawString(this.font, subtitle, subtitleX, subtitleY, 0xFFFF00, false);
        
        // Область обрезки для кнопок
        int contentStartY = guiY + HEADER_HEIGHT;
        int contentEndY = guiY + GUI_HEIGHT - FOOTER_HEIGHT;
        guiGraphics.enableScissor(guiX + 10, contentStartY, guiX + GUI_WIDTH - 10, contentEndY);
        
        // Рендерим кнопки
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
        
        // Отключаем обрезку
        guiGraphics.disableScissor();
        
        // Индикаторы прокрутки
        if (contentHeight > visibleAreaHeight) {
            // Полоса прокрутки
            int scrollBarX = guiX + GUI_WIDTH - 12;
            int scrollBarY = contentStartY;
            int scrollBarHeight = visibleAreaHeight;
            int scrollBarWidth = 4;
            
            // Фон полосы прокрутки
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, 
                scrollBarY + scrollBarHeight, 0x66000000);
            
            // Ползунок
            int sliderHeight = Math.max(20, (int)((double)visibleAreaHeight / contentHeight * scrollBarHeight));
            int maxScroll = Math.max(1, contentHeight - visibleAreaHeight);
            int sliderY = scrollBarY + (int)((double)scrollOffset / maxScroll * (scrollBarHeight - sliderHeight));
            guiGraphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
            
            // Стрелки прокрутки
            if (scrollOffset > 0) {
                guiGraphics.drawString(this.font, Component.literal("▲"), 
                    scrollBarX - 8, contentStartY, 0xFFFFFF, false);
            }
            if (scrollOffset < maxScroll) {
                guiGraphics.drawString(this.font, Component.literal("▼"), 
                    scrollBarX - 8, contentEndY - 10, 0xFFFFFF, false);
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null) return super.mouseScrolled(mouseX, mouseY, delta);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int contentStartY = guiY + HEADER_HEIGHT;
        int contentEndY = guiY + GUI_HEIGHT - FOOTER_HEIGHT;
        
        // Проверяем, находится ли мышь в области контента
        if (mouseX >= guiX && mouseX <= guiX + GUI_WIDTH &&
            mouseY >= contentStartY && mouseY <= contentEndY) {
            
            int maxScroll = Math.max(0, contentHeight - visibleAreaHeight);
            int scrollStep = BUTTON_HEIGHT + BUTTON_SPACING;
            
            if (delta < 0 && scrollOffset < maxScroll) {
                scrollOffset = Math.min(maxScroll, scrollOffset + scrollStep);
                this.init(); // Пересоздаём кнопки с новым смещением
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset = Math.max(0, scrollOffset - scrollStep);
                this.init(); // Пересоздаём кнопки с новым смещением
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
