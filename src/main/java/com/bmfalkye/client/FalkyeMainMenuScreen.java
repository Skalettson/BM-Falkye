package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Главное меню Фальки с кнопками для всех систем
 * Адаптивный интерфейс с прокруткой
 */
public class FalkyeMainMenuScreen extends Screen {
    // Базовые размеры (адаптируются под разрешение)
    private static final int BASE_GUI_WIDTH = 420;
    private static final int BASE_GUI_HEIGHT = 400;
    private static final int MIN_GUI_WIDTH = 350;
    private static final int MIN_GUI_HEIGHT = 300;
    private static final double MAX_SCREEN_RATIO = 0.85; // Максимум 85% экрана
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    private int guiX;
    private int guiY;
    private final List<Button> buttons = new ArrayList<>();
    
    // Параметры для прокрутки
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int visibleAreaHeight = 0;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_SPACING = 8;
    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 50;
    
    public FalkyeMainMenuScreen() {
        super(Component.empty()); // Заголовок рисуется вручную в render()
    }
    
    /**
     * Вычисляет адаптивные размеры окна
     */
    private void calculateAdaptiveSize() {
        // Вычисляем максимальные размеры (85% экрана)
        int maxWidth = (int)(this.width * MAX_SCREEN_RATIO);
        int maxHeight = (int)(this.height * MAX_SCREEN_RATIO);
        
        // Используем базовые размеры, но не больше максимума и не меньше минимума
        GUI_WIDTH = Math.max(MIN_GUI_WIDTH, Math.min(BASE_GUI_WIDTH, maxWidth));
        GUI_HEIGHT = Math.max(MIN_GUI_HEIGHT, Math.min(BASE_GUI_HEIGHT, maxHeight));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Вычисляем адаптивные размеры
        calculateAdaptiveSize();
        
        // Очищаем все виджеты перед добавлением новых (предотвращает дублирование)
        this.clearWidgets();
        buttons.clear();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Вычисляем доступную высоту для кнопок
        visibleAreaHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT;
        
        int buttonWidth = Math.min(280, GUI_WIDTH - 40);
        int startX = guiX + (GUI_WIDTH - buttonWidth) / 2;
        int startY = guiY + HEADER_HEIGHT;
        
        // Список всех кнопок меню
        List<MenuButtonInfo> menuButtons = new ArrayList<>();
        // Сохраняем ссылку на главное меню для передачи в дочерние экраны
        Screen mainMenu = this;
        
        menuButtons.add(new MenuButtonInfo("§eКоллекция карт", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new CardCollectionScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§eРедактор колод", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.OpenDeckEditorPacket());
            // Сохраняем ссылку на главное меню для DeckEditor (будет использовано при открытии)
            com.bmfalkye.client.ClientPacketHandler.setMainMenuParent(mainMenu);
        }));
        menuButtons.add(new MenuButtonInfo("§eЭнциклопедия", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.OpenEncyclopediaPacket());
            // Сохраняем ссылку на главное меню для Encyclopedia (будет использовано при открытии)
            com.bmfalkye.client.ClientPacketHandler.setMainMenuParent(mainMenu);
        }));
        menuButtons.add(new MenuButtonInfo("§6Эволюция карт", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new CardEvolutionScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§5Квесты", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new QuestScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§6Великий Турнир", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            // Отправляем запрос на начало драфта
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendToServer(
                new com.bmfalkye.network.NetworkHandler.StartDraftPacket(false));
        }));
        menuButtons.add(new MenuButtonInfo("§dПользовательские Турниры", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new CustomTournamentScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§6Зал Славы", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new HallOfFameScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§eНастройки", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.bmfalkye.client.settings.GameSettingsScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§bДрузья", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new FriendsScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§bГильдия", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new GuildScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§6Боссы", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new BossScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§aМагазин карт", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new CardShopScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§bСтатистика", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new StatisticsScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§6Турниры", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new TournamentScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§aСезон", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new SeasonScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§7Реплеи", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new ReplayScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§dЕжедневные награды", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new DailyRewardsScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§5События", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            net.minecraft.client.Minecraft.getInstance().setScreen(new EventsScreen(mainMenu));
        }));
        menuButtons.add(new MenuButtonInfo("§eПравила/Руководство", () -> {
            com.bmfalkye.client.sounds.SoundEffectManager.playButtonClickSound();
            // Используем Patchouli если доступен, иначе обычный экран
            boolean bookOpened = false;
            if (com.bmfalkye.integration.LibraryIntegration.isPatchouliLoaded()) {
                bookOpened = com.bmfalkye.integration.PatchouliIntegration.openBook(
                    new net.minecraft.resources.ResourceLocation("bm_falkye", "tutorial"));
            }
            // Если Patchouli не загружен или открытие книги не удалось, открываем обычный экран
            if (!bookOpened) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new FalkyeTutorialScreen(mainMenu));
            }
        }));
        
        // Вычисляем общую высоту контента
        contentHeight = menuButtons.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        
        // Ограничиваем скролл
        int maxScroll = Math.max(0, contentHeight - visibleAreaHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Создаём кнопки с учётом прокрутки
        int currentY = startY - scrollOffset;
        for (MenuButtonInfo buttonInfo : menuButtons) {
            // Проверяем, видна ли кнопка
            if (currentY + BUTTON_HEIGHT >= guiY + HEADER_HEIGHT && 
                currentY <= guiY + GUI_HEIGHT - FOOTER_HEIGHT) {
                
                Button button = new com.bmfalkye.client.gui.StyledCardCollectionButton(
                    startX, currentY, buttonWidth, BUTTON_HEIGHT,
                    Component.literal(buttonInfo.text),
                    (btn) -> buttonInfo.action.run()
                );
                this.addRenderableWidget(button);
                buttons.add(button);
            }
            
            currentY += BUTTON_HEIGHT + BUTTON_SPACING;
        }
        
        // Кнопка "Назад" (всегда внизу)
        Button backButton = new com.bmfalkye.client.gui.StyledCardCollectionButton(
            startX, guiY + GUI_HEIGHT - FOOTER_HEIGHT + 10, buttonWidth, BUTTON_HEIGHT,
            Component.literal("§7Назад"),
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
        final Runnable action;
        
        MenuButtonInfo(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }
    }
    
    private static long animationTime = 0;
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        animationTime = System.currentTimeMillis();
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Улучшенный фон окна в скевоморфном стиле (деревянная панель)
        long time = System.currentTimeMillis();
        float pulse = (float) (0.5f + 0.2f * Math.sin(time / 2000.0f));
        
        // Деревянная панель
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        
        // Анимированная металлическая рамка (золотая при высоком pulse)
        boolean goldFrame = pulse > 0.6f;
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 3, goldFrame);
        
        // Внутреннее свечение
        int glowAlpha = (int) (pulse * 40);
        int glowColor = (0xFF8B7355 & 0x00FFFFFF) | (glowAlpha << 24);
        guiGraphics.fill(guiX + 3, guiY + 3, guiX + GUI_WIDTH - 3, guiY + 5, glowColor);
        guiGraphics.fill(guiX + 3, guiY + GUI_HEIGHT - 5, guiX + GUI_WIDTH - 3, guiY + GUI_HEIGHT - 3, glowColor);
        
        // Заголовок с эффектом (рисуем только один раз)
        Component title = Component.literal("§6§lФАЛЬКИ - ГЛАВНОЕ МЕНЮ");
        int titleWidth = this.font.width(title);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = guiY + 18;
        // Тень заголовка (черная)
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
        
        // Область обрезки для кнопок (чтобы они не вылезали за границы)
        int contentStartY = guiY + HEADER_HEIGHT;
        int contentEndY = guiY + GUI_HEIGHT - FOOTER_HEIGHT;
        guiGraphics.enableScissor(guiX + 10, contentStartY, guiX + GUI_WIDTH - 10, contentEndY);
        
        // Рендерим виджеты (кнопки) вручную, чтобы избежать автоматической отрисовки заголовка
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            if (renderable instanceof Button button) {
                com.bmfalkye.client.gui.GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            } else {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
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
            int sliderY = scrollBarY + (int)((double)scrollOffset / (contentHeight - visibleAreaHeight) * 
                (scrollBarHeight - sliderHeight));
            guiGraphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, 
                sliderY + sliderHeight, 0xFF8B7355);
            
            // Стрелки прокрутки
            if (scrollOffset > 0) {
                guiGraphics.drawString(this.font, "▲", scrollBarX - 8, contentStartY, 0xFFFFFF, false);
            }
            if (scrollOffset < contentHeight - visibleAreaHeight) {
                guiGraphics.drawString(this.font, "▼", scrollBarX - 8, contentEndY - 10, 0xFFFFFF, false);
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Проверяем, находится ли мышь в области контента
        if (mouseX >= guiX && mouseX <= guiX + GUI_WIDTH &&
            mouseY >= guiY + HEADER_HEIGHT && mouseY <= guiY + GUI_HEIGHT - FOOTER_HEIGHT) {
            
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
    
    /**
     * Смешивает два цвета
     */
    private static int blendColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        int a = (int) (a1 + (a2 - a1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

