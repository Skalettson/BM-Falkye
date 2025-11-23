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
 * Экран с руководством и правилами игры Falkye
 * Полностью переписан с нуля с исправлением всех визуальных багов и полной адаптивностью
 * Использует актуальное API Minecraft Forge 1.20.1
 * Дата: 23 ноября 2025
 */
public class FalkyeTutorialScreen extends Screen {
    // Базовые размеры для адаптации
    private static final int BASE_GUI_WIDTH = 620;
    private static final int BASE_GUI_HEIGHT = 420;
    private static final int MIN_GUI_WIDTH = 500;
    private static final int MIN_GUI_HEIGHT = 350;
    private static final double MAX_SCREEN_RATIO = 0.90;
    
    private int GUI_WIDTH;
    private int GUI_HEIGHT;
    
    // Система автоматической адаптации
    private AdaptiveLayout layout;
    
    private double scrollOffset = 0.0;
    private static final int LINE_HEIGHT = 12;
    private int contentHeight = 0;
    private Screen parentScreen; // Экран, из которого открыли правила
    
    // Кнопки
    private Button backButton;
    private final List<Button> buttons = new ArrayList<>();
    
    public FalkyeTutorialScreen() {
        this(null);
    }
    
    public FalkyeTutorialScreen(Screen parentScreen) {
        super(Component.translatable("screen.bm_falkye.tutorial_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        buttons.clear();
        
        // Инициализируем систему автоматической адаптации
        this.layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
        this.GUI_WIDTH = layout.getGuiWidth();
        this.GUI_HEIGHT = layout.getGuiHeight();
        
        // Вычисляем высоту контента
        contentHeight = getTotalLines() * LINE_HEIGHT;
        
        // Адаптивные размеры кнопки
        int buttonWidth = Math.max(100, layout.getWidth(30));
        int buttonHeight = Math.max(20, layout.getHeight(5));
        int buttonY = layout.getBottomY(buttonHeight, 5);
        
        // Кнопка "Назад"
        this.backButton = GuiUtils.createStyledButton(
            layout.getCenteredX(buttonWidth), buttonY, buttonWidth, buttonHeight,
            Component.translatable("button.bm_falkye.back"),
            (button) -> {
                if (parentScreen != null && minecraft != null) {
                    this.minecraft.setScreen(parentScreen);
                } else {
                    this.onClose();
                }
            }
        );
        this.addRenderableWidget(this.backButton);
        buttons.add(this.backButton);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null) return super.mouseScrolled(mouseX, mouseY, delta);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int textStartY = guiY + 50;
        int textEndY = guiY + GUI_HEIGHT - 60;
        int visibleHeight = textEndY - textStartY;
        
        // Проверяем, находится ли мышь в области контента
        if (mouseX >= guiX && mouseX <= guiX + GUI_WIDTH && 
            mouseY >= textStartY && mouseY <= textEndY) {
            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            if (maxScroll > 0) {
                double scrollSpeed = LINE_HEIGHT * 3;
                double newScrollOffset = scrollOffset - delta * scrollSpeed;
                scrollOffset = Math.max(0, Math.min(maxScroll, newScrollOffset));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пересчитываем layout при изменении размера экрана
        if (layout == null || layout.needsRecalculation()) {
            layout = new AdaptiveLayout(this, BASE_GUI_WIDTH, BASE_GUI_HEIGHT, 
                                         MAX_SCREEN_RATIO, MIN_GUI_WIDTH, MIN_GUI_HEIGHT);
            GUI_WIDTH = layout.getGuiWidth();
            GUI_HEIGHT = layout.getGuiHeight();
            contentHeight = getTotalLines() * LINE_HEIGHT;
        }
        
        this.renderBackground(guiGraphics);
        
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        
        // Красивый фон окна
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        // Металлическая рамка
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок с тенью
        MutableComponent titleComponent = Component.translatable("screen.bm_falkye.tutorial_title")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GOLD)
                .withBold(true));
        int titleWidth = this.font.width(titleComponent);
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = guiY + 15;
        // Тень заголовка
        guiGraphics.drawString(this.font, titleComponent, titleX + 2, titleY + 2, 0x000000, false);
        // Сам заголовок
        guiGraphics.drawString(this.font, titleComponent, titleX, titleY, 0xFFFFFF, false);
        
        // Разделитель
        int separatorY = guiY + 40;
        guiGraphics.fill(guiX + 20, separatorY, guiX + GUI_WIDTH - 20, separatorY + 1, 0xFF4A3A2A);
        
        // Рендерим текст руководства
        renderTutorialText(guiGraphics);
        
        // Рендерим кнопки
        for (Button button : buttons) {
            if (button != null && button.visible) {
                GuiUtils.renderStyledButton(guiGraphics, this.font, button, mouseX, mouseY, false);
            }
        }
    }
    
    private void renderTutorialText(GuiGraphics guiGraphics) {
        if (layout == null) return;
        
        List<String> lines = getTutorialLines();
        int guiX = layout.getGuiX();
        int guiY = layout.getGuiY();
        int textStartY = guiY + 50;
        int textEndY = guiY + GUI_HEIGHT - 60;
        int textX = guiX + 30;
        int visibleHeight = textEndY - textStartY;
        
        // Вычисляем начальную строку на основе scrollOffset
        int startLine = Math.max(0, (int)(scrollOffset / LINE_HEIGHT));
        int endLine = Math.min(startLine + (visibleHeight / LINE_HEIGHT) + 2, lines.size());
        
        for (int i = startLine; i < endLine; i++) {
            int lineY = textStartY + (i - startLine) * LINE_HEIGHT - (int)(scrollOffset % LINE_HEIGHT);
            if (lineY + LINE_HEIGHT > textEndY) break;
            if (lineY < textStartY) continue;
            
            String line = lines.get(i);
            Component textComponent = Component.literal(line);
            int color = getLineColor(line);
            guiGraphics.drawString(this.font, textComponent, textX, lineY, color, false);
        }
        
        // Рисуем scrollbar справа
        if (contentHeight > visibleHeight) {
            int scrollbarX = guiX + GUI_WIDTH - 20;
            int scrollbarY = textStartY;
            int scrollbarHeight = visibleHeight;
            int scrollbarWidth = 6;
            
            // Фон scrollbar
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x80000000);
            
            // Ползунок
            int maxScroll = contentHeight - visibleHeight;
            if (maxScroll > 0) {
                double scrollRatio = Math.max(0, Math.min(1, scrollOffset / maxScroll));
                int thumbHeight = Math.max(10, (int)(scrollbarHeight * ((double)visibleHeight / contentHeight)));
                int thumbY = scrollbarY + (int)(scrollRatio * (scrollbarHeight - thumbHeight));
                guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFF8B7355);
            }
        }
    }
    
    private int getLineColor(String line) {
        // Если строка содержит цветовые коды §, Component.literal() обработает их,
        // и параметр color в drawString() будет проигнорирован
        if (line.startsWith("§")) {
            return 0xFFFFFF; // Значение игнорируется, но нужно для совместимости
        }
        if (line.startsWith("  ")) {
            // Подпункт - серый цвет
            return 0xCCCCCC;
        }
        if (line.length() > 0 && Character.isUpperCase(line.charAt(0)) && line.length() < 50) {
            // Заголовок - жёлтый цвет
            return 0xFFFF00;
        }
        // Обычный текст - белый цвет
        return 0xFFFFFF;
    }
    
    private List<String> getTutorialLines() {
        List<String> lines = new ArrayList<>();
        
        // Определяем язык (упрощённо - проверяем локализацию)
        boolean isRussian = false;
        try {
            if (minecraft != null && minecraft.options != null) {
                isRussian = minecraft.options.languageCode != null && 
                           minecraft.options.languageCode.startsWith("ru");
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        
        if (isRussian) {
            lines.add("§6═══════════════════════════════════════");
            lines.add("§6  ПРАВИЛА ИГРЫ FALKYE");
            lines.add("§6═══════════════════════════════════════");
            lines.add("");
            lines.add("§eОСНОВЫ ИГРЫ:");
            lines.add("  • Falkye - карточная игра на 3 раунда");
            lines.add("  • Победитель - тот, кто выиграет 2 раунда");
            lines.add("  • В начале игры у каждого игрока 10 карт");
            lines.add("  • Каждый раунд нужно выиграть больше рядов");
            lines.add("  • Победитель раунда определяется по количеству");
            lines.add("    выигранных рядов (ближний, дальний, осада)");
            lines.add("  • При равном количестве рядов - по общему счёту");
            lines.add("");
            lines.add("§eИГРОВОЕ ПОЛЕ:");
            lines.add("  • §6⚔ Ближний бой - карты ближнего боя");
            lines.add("  • §e🏹 Дальний бой - карты дальнего боя");
            lines.add("  • §c🏰 Осада - осадные карты");
            lines.add("  • Очки = сумма силы всех карт в рядах");
            lines.add("  • Победитель раунда - кто выиграл больше рядов");
            lines.add("");
            lines.add("§eТИПЫ КАРТ:");
            lines.add("  • §a⚔ Существа - размещаются в ряды");
            lines.add("  • §b✨ Заклинания - имеют особые эффекты");
            lines.add("  • §d★ Специальные - уникальные способности");
            lines.add("");
            lines.add("§eХОД ИГРЫ:");
            lines.add("  • Игроки ходят по очереди");
            lines.add("  • За ход можно сыграть:");
            lines.add("    - 1 обычную карту (существо)");
            lines.add("    - + 1 карту способности (если есть в руке)");
            lines.add("  • Если нет карты способности - только 1 обычная");
            lines.add("  • Пас = отказ от карты способности (если уже");
            lines.add("    сыграна обычная карта) или пропуск хода");
            lines.add("  • На каждый ход даётся 1 минута 30 секунд");
            lines.add("  • Если не успели сходить - автоматический пас");
            lines.add("  • 3 пропуска по таймауту = проигрыш");
            lines.add("");
            lines.add("§eБИТВА КАРТ:");
            lines.add("  • Происходит, когда оба игрока пасуют");
            lines.add("    без игры карт в своих ходах");
            lines.add("  • Карты одного типа боя сражаются друг с другом");
            lines.add("  • Слабая карта проигрывает и исчезает");
            lines.add("  • У победившей карты вычитается сила проигравшей");
            lines.add("  • Раунд заканчивается после битвы карт");
            lines.add("  • Победитель раунда определяется по количеству");
            lines.add("    выигранных рядов после битвы");
            lines.add("");
            lines.add("§eВАЛЮТА И СТАВКИ:");
            lines.add("  • У каждого игрока есть монеты");
            lines.add("  • Начальный баланс: 800 монет");
            lines.add("  • Жители имеют от 100 до 10000 монет");
            lines.add("  • Перед игрой проверяется баланс для ставки");
            lines.add("  • Можно сдаться за 200 монет");
            lines.add("");
            lines.add("§eЛИДЕРЫ:");
            lines.add("  • У каждого игрока есть лидер");
            lines.add("  • Лидер имеет уникальную способность");
            lines.add("  • Способность можно использовать 1 раз за игру");
            lines.add("");
            lines.add("§eПОГОДА:");
            lines.add("  • §b❄ Мороз - ослабляет ближний бой противника");
            lines.add("    (сила карт противника = 1)");
            lines.add("  • §7☁ Туман - ослабляет дальний бой противника");
            lines.add("    (сила карт противника = 1)");
            lines.add("  • §9🌧 Дождь - ослабляет осаду противника");
            lines.add("    (сила карт противника = 1)");
            lines.add("  • Погода влияет только на карты противника");
            lines.add("  • Можно снять эффект картой 'Ясная погода'");
            lines.add("");
            lines.add("§eРЕДКОСТЬ КАРТ:");
            lines.add("  • §7Обычные - базовые карты");
            lines.add("  • §bРедкие - более сильные карты");
            lines.add("  • §dЭпические - очень сильные карты");
            lines.add("  • §6Легендарные - самые мощные карты");
            lines.add("");
            lines.add("§eСТРАТЕГИЯ:");
            lines.add("  • Следите за очками и выигранными рядами");
            lines.add("  • Используйте лидера в нужный момент");
            lines.add("  • Учитывайте погодные эффекты");
            lines.add("  • Не тратьте сильные карты в первом раунде");
            lines.add("  • Комбинируйте обычные карты и способности");
            lines.add("  • Используйте способности для усиления своих");
            lines.add("    карт или ослабления карт противника");
            lines.add("  • Следите за таймером хода!");
            lines.add("  • Помните: победа определяется по рядам!");
            lines.add("");
            lines.add("§eУПРАВЛЕНИЕ:");
            lines.add("  • ЛКМ по карте в руке - выбрать ряд");
            lines.add("  • ЛКМ по ряду - сыграть карту в этот ряд");
            lines.add("  • Кнопка 'Пас' - пропустить ход");
            lines.add("  • Кнопка 'Лидер' - использовать способность");
            lines.add("  • Кнопка 'Сдаться' - сдаться за 200 монет");
            lines.add("");
            lines.add("§6═══════════════════════════════════════");
        } else {
            lines.add("§6═══════════════════════════════════════");
            lines.add("§6  FALKYE GAME RULES");
            lines.add("§6═══════════════════════════════════════");
            lines.add("");
            lines.add("§eBASICS:");
            lines.add("  • Falkye is a card game with 3 rounds");
            lines.add("  • Winner is the one who wins 2 rounds");
            lines.add("  • Each player starts with 10 cards");
            lines.add("  • Each round requires winning more rows");
            lines.add("  • Round winner is determined by number of");
            lines.add("    won rows (melee, ranged, siege)");
            lines.add("  • If rows are equal - by total score");
            lines.add("");
            lines.add("§eGAME FIELD:");
            lines.add("  • §6⚔ Melee - melee combat cards");
            lines.add("  • §e🏹 Ranged - ranged combat cards");
            lines.add("  • §c🏰 Siege - siege cards");
            lines.add("  • Points = sum of all card power in rows");
            lines.add("  • Round winner - who won more rows");
            lines.add("");
            lines.add("§eCARD TYPES:");
            lines.add("  • §a⚔ Creatures - placed in rows");
            lines.add("  • §b✨ Spells - have special effects");
            lines.add("  • §d★ Special - unique abilities");
            lines.add("");
            lines.add("§eTURN ORDER:");
            lines.add("  • Players take turns");
            lines.add("  • Per turn you can play:");
            lines.add("    - 1 normal card (creature)");
            lines.add("    - + 1 ability card (if in hand)");
            lines.add("  • If no ability card - only 1 normal");
            lines.add("  • Pass = decline ability card (if normal");
            lines.add("    already played) or skip turn");
            lines.add("  • Each turn has 1 minute 30 seconds");
            lines.add("  • If time runs out - automatic pass");
            lines.add("  • 3 timeouts = defeat");
            lines.add("");
            lines.add("§eCARD BATTLE:");
            lines.add("  • Occurs when both players pass");
            lines.add("    without playing cards in their turns");
            lines.add("  • Cards of same combat type fight each other");
            lines.add("  • Weaker card loses and disappears");
            lines.add("  • Winner card loses loser's power");
            lines.add("  • Round ends after card battle");
            lines.add("  • Round winner determined by number of");
            lines.add("    won rows after battle");
            lines.add("");
            lines.add("§eCURRENCY AND BETS:");
            lines.add("  • Each player has coins");
            lines.add("  • Starting balance: 800 coins");
            lines.add("  • Villagers have 100-10000 coins");
            lines.add("  • Balance is checked before game for bet");
            lines.add("  • Can surrender for 200 coins");
            lines.add("");
            lines.add("§eLEADERS:");
            lines.add("  • Each player has a leader");
            lines.add("  • Leader has unique ability");
            lines.add("  • Ability can be used once per game");
            lines.add("");
            lines.add("§eWEATHER:");
            lines.add("  • §b❄ Frost - weakens opponent's melee");
            lines.add("    (opponent's card power = 1)");
            lines.add("  • §7☁ Fog - weakens opponent's ranged");
            lines.add("    (opponent's card power = 1)");
            lines.add("  • §9🌧 Rain - weakens opponent's siege");
            lines.add("    (opponent's card power = 1)");
            lines.add("  • Weather affects only opponent's cards");
            lines.add("  • Can be removed with 'Clear Weather' card");
            lines.add("");
            lines.add("§eCARD RARITY:");
            lines.add("  • §7Common - basic cards");
            lines.add("  • §bRare - stronger cards");
            lines.add("  • §dEpic - very strong cards");
            lines.add("  • §6Legendary - most powerful cards");
            lines.add("");
            lines.add("§eSTRATEGY:");
            lines.add("  • Watch opponent's points and won rows");
            lines.add("  • Use leader at the right moment");
            lines.add("  • Consider weather effects");
            lines.add("  • Don't waste strong cards in round 1");
            lines.add("  • Combine normal cards and abilities");
            lines.add("  • Use abilities to boost your cards or");
            lines.add("    weaken opponent's cards");
            lines.add("  • Watch the turn timer!");
            lines.add("  • Remember: victory is determined by rows!");
            lines.add("");
            lines.add("§eCONTROLS:");
            lines.add("  • LMB on card in hand - select row");
            lines.add("  • LMB on row - play card to that row");
            lines.add("  • 'Pass' button - skip turn");
            lines.add("  • 'Leader' button - use ability");
            lines.add("  • 'Surrender' button - surrender for 200 coins");
            lines.add("");
            lines.add("§6═══════════════════════════════════════");
        }
        
        return lines;
    }
    
    private int getTotalLines() {
        return getTutorialLines().size();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
