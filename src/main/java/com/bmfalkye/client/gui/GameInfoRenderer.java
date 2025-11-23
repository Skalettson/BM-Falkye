package com.bmfalkye.client.gui;

import com.bmfalkye.game.ClientFalkyeGameSession;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Рендерер информации об игре Falkye.
 * 
 * <p>Отображает основную информацию о текущем состоянии игры:
 * <ul>
 *   <li>Текущий раунд</li>
 *   <li>Счёт раундов (сколько раундов выиграл каждый игрок)</li>
 *   <li>Счёт текущего раунда (сумма силы карт каждого игрока)</li>
 *   <li>Таймер хода</li>
 *   <li>Чей ход</li>
 * </ul>
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
public class GameInfoRenderer {
    
    /**
     * Рендерит информацию об игре.
     * 
     * @param guiGraphics графический контекст для рендеринга
     * @param font шрифт для отображения текста
     * @param session клиентская игровая сессия
     * @param guiX координата X левого верхнего угла игрового окна
     * @param guiY координата Y левого верхнего угла игрового окна
     * @param guiWidth ширина игрового окна
     * @param guiHeight высота игрового окна
     * @param currentPlayerUUID UUID текущего игрока (клиента)
     * @param localRemainingTime локальное оставшееся время хода (для предотвращения зависаний)
     */
    public static void render(GuiGraphics guiGraphics, Font font,
                             ClientFalkyeGameSession session,
                             int guiX, int guiY, int guiWidth, int guiHeight,
                             UUID currentPlayerUUID, int localRemainingTime) {
        // Адаптивные размеры панели информации
        int infoPanelWidth = Math.max(250, Math.min(350, (int)(guiWidth * 0.35))); // 35% от ширины окна
        int infoPanelHeight = Math.max(90, Math.min(110, (int)(guiHeight * 0.12))); // 12% от высоты окна
        int infoPanelX = guiX + Math.max(8, (int)(guiWidth * 0.015)); // 1.5% от ширины окна
        int infoPanelY = guiY + Math.max(8, (int)(guiHeight * 0.01)); // 1% от высоты окна
        
        // Используем текстуру информации об игре, если доступна
        if (CardTextures.textureExists(com.bmfalkye.client.gui.GameTextures.GAME_INFO)) {
            guiGraphics.blit(com.bmfalkye.client.gui.GameTextures.GAME_INFO, infoPanelX, infoPanelY, 0, 0, infoPanelWidth, infoPanelHeight, infoPanelWidth, infoPanelHeight);
        } else {
            // Fallback: простой фон
            GuiUtils.drawLeatherElement(guiGraphics, infoPanelX, infoPanelY, infoPanelWidth, infoPanelHeight);
            GuiUtils.drawMetalFrame(guiGraphics, infoPanelX, infoPanelY, infoPanelWidth, infoPanelHeight, 2, false);
        }
        
        // Адаптивные отступы внутри панели
        int padding = Math.max(8, Math.min(12, (int)(infoPanelWidth * 0.04))); // 4% от ширины панели
        int lineHeight = Math.max(12, Math.min(16, (int)(infoPanelHeight * 0.14))); // 14% от высоты панели
        
        int y = infoPanelY + padding;
        
        // Раунд
        String roundText = "§6Раунд: §f" + session.getCurrentRound() + "/3";
        guiGraphics.drawString(font, 
            Component.literal(roundText), 
            infoPanelX + padding, y, 0xFFFFFF, false);
        
        // Счёт раундов
        String roundsWonText = "§aВыиграно раундов: §f" + session.getRoundsWon1() + 
                              " §7vs §f" + session.getRoundsWon2();
        guiGraphics.drawString(font, 
            Component.literal(roundsWonText), 
            infoPanelX + padding, y + lineHeight, 0xFFFFFF, false);
        
        // Счёт текущего раунда
        String roundScoreText = "§bСчёт раунда: §f" + session.getRoundScore1() + 
                               " §7vs §f" + session.getRoundScore2();
        guiGraphics.drawString(font, 
            Component.literal(roundScoreText), 
            infoPanelX + padding, y + lineHeight * 2, 0xFFFFFF, false);
        
        // Таймер и чей ход
        boolean isMyTurn = currentPlayerUUID != null && 
                          currentPlayerUUID.equals(session.getCurrentPlayerUUID());
        
        // Используем локальное время, если доступно
        int remainingTime = localRemainingTime > 0 ? localRemainingTime : 
                           (session.getRemainingTime() > 0 ? session.getRemainingTime() : 0);
        
        String timeText = remainingTime > 0 ? 
                         "§eОсталось времени: §f" + remainingTime + " сек" : 
                         "§eОсталось времени: §c0 сек";
        
        String turnText = isMyTurn ? "§aВаш ход" : "§cХод противника";
        
        guiGraphics.drawString(font, 
            Component.literal(timeText + " | " + turnText), 
            infoPanelX + padding, y + lineHeight * 3, 0xFFFFFF, false);
    }
}

