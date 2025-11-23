package com.bmfalkye.client;

import com.bmfalkye.client.gui.GuiUtils;
import com.bmfalkye.client.gui.StyledCardCollectionButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Экран просмотра реплея игры
 */
public class ReplayViewerScreen extends Screen {
    private static final int GUI_WIDTH = 800;
    private static final int GUI_HEIGHT = 500;
    
    private int guiX;
    private int guiY;
    private int scrollOffset = 0;
    
    private String replayId;
    private String player1Name;
    private String player2Name;
    private String winnerName;
    private int roundsWon1;
    private int roundsWon2;
    private long timestamp;
    private int duration;
    
    private static class ReplayMove {
        String playerName;
        String action;
        String cardId;
        int round;
        long timestamp;
    }
    
    private List<ReplayMove> moves = new ArrayList<>();
    private final Screen parentScreen;
    
    public ReplayViewerScreen(FriendlyByteBuf data) {
        this(data, null);
    }
    
    public ReplayViewerScreen(FriendlyByteBuf data, Screen parentScreen) {
        super(Component.literal("§7§lПРОСМОТР РЕПЛЕЯ"));
        this.parentScreen = parentScreen;
        
        // Читаем данные реплея
        replayId = data.readUtf();
        player1Name = data.readUtf();
        player2Name = data.readUtf();
        winnerName = data.readUtf();
        roundsWon1 = data.readInt();
        roundsWon2 = data.readInt();
        timestamp = data.readLong();
        duration = data.readInt();
        
        int moveCount = data.readInt();
        for (int i = 0; i < moveCount; i++) {
            ReplayMove move = new ReplayMove();
            move.playerName = data.readUtf();
            move.action = data.readUtf();
            move.cardId = data.readUtf();
            move.round = data.readInt();
            move.timestamp = data.readLong();
            moves.add(move);
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        // Кнопка "Назад"
        Button backButton = new StyledCardCollectionButton(
            guiX + 20, guiY + GUI_HEIGHT - 35, 100, 20,
            Component.literal("§7Назад"),
            (btn) -> {
                // Возвращаемся к списку реплеев или родительскому экрану
                if (parentScreen != null) {
                    this.minecraft.setScreen(parentScreen);
                } else {
                    this.minecraft.setScreen(new ReplayScreen(null));
                }
            }
        );
        this.addRenderableWidget(backButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // ПЕРЕПИСАНО: Фон в скевоморфном стиле
        GuiUtils.drawWoodenPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, true);
        GuiUtils.drawMetalFrame(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 2, false);
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("§7§l══════ ПРОСМОТР РЕПЛЕЯ ══════"),
            guiX + GUI_WIDTH / 2, guiY + 15, 0xFFFFFF);
        
        // Разделитель
        guiGraphics.fill(guiX + 20, guiY + 40, guiX + GUI_WIDTH - 20, guiY + 42, 0xFF808080);
        
        int y = guiY + 55;
        
        // Информация о реплее
        guiGraphics.drawString(this.font, Component.literal("§eИгрок 1: §f" + player1Name), 
            guiX + 30, y, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.literal("§eИгрок 2: §f" + player2Name), 
            guiX + 30, y + 15, 0xFFFFFF, false);
        
        String score = roundsWon1 + " - " + roundsWon2;
        guiGraphics.drawString(this.font, Component.literal("§7Счёт: §f" + score), 
            guiX + 30, y + 30, 0xFFFFFF, false);
        
        guiGraphics.drawString(this.font, Component.literal("§aПобедитель: §f" + winnerName), 
            guiX + 30, y + 45, 0xFFFFFF, false);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStr = sdf.format(new Date(timestamp));
        guiGraphics.drawString(this.font, Component.literal("§7Дата: §f" + dateStr), 
            guiX + GUI_WIDTH - 250, y, 0xFFFFFF, false);
        
        if (duration > 0) {
            guiGraphics.drawString(this.font, Component.literal("§7Длительность: §f" + duration + " сек"), 
                guiX + GUI_WIDTH - 250, y + 15, 0xFFFFFF, false);
        }
        
        guiGraphics.drawString(this.font, Component.literal("§7Ходов: §f" + moves.size()), 
            guiX + GUI_WIDTH - 250, y + 30, 0xFFFFFF, false);
        
        // Разделитель перед списком ходов
        int movesStartY = y + 70;
        guiGraphics.fill(guiX + 20, movesStartY, guiX + GUI_WIDTH - 20, movesStartY + 1, 0xFF808080);
        
        // Заголовок списка ходов
        guiGraphics.drawString(this.font, Component.literal("§6§lХоды игры:"), 
            guiX + 30, movesStartY + 10, 0xFFFFFF, false);
        
        // Список ходов
        int movesY = movesStartY + 30;
        int visibleAreaHeight = GUI_HEIGHT - (movesY - guiY) - 50;
        int moveHeight = 25;
        int maxVisibleMoves = visibleAreaHeight / moveHeight;
        
        if (moves.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("§7Нет сохранённых ходов"), 
                guiX + 30, movesY, 0xFFFFFF, false);
        } else {
            int startIndex = scrollOffset / moveHeight;
            int visibleCount = Math.min(moves.size() - startIndex, maxVisibleMoves);
            
            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                if (index >= moves.size()) break;
                
                ReplayMove move = moves.get(index);
                int moveY = movesY + i * moveHeight;
                
                // ПЕРЕПИСАНО: Фон хода в скевоморфном стиле
                GuiUtils.drawLeatherElement(guiGraphics, guiX + 30, moveY, GUI_WIDTH - 60, moveHeight - 2);
                
                // Информация о ходе
                String moveText = "§7Раунд " + move.round + ": §f" + move.playerName + " §7- ";
                switch (move.action) {
                    case "play_card":
                        moveText += "§aСыграл карту";
                        if (!move.cardId.isEmpty()) {
                            com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(move.cardId);
                            if (card != null) {
                                moveText += " §f" + card.getName();
                            }
                        }
                        break;
                    case "pass":
                        moveText += "§eПас";
                        break;
                    case "use_leader":
                        moveText += "§6Использовал лидера";
                        break;
                    default:
                        moveText += move.action;
                }
                
                guiGraphics.drawString(this.font, Component.literal(moveText), 
                    guiX + 40, moveY + 5, 0xFFFFFF, false);
            }
            
            // Скроллбар
            if (moves.size() > maxVisibleMoves) {
                int scrollbarHeight = visibleAreaHeight;
                int scrollbarY = movesY;
                int scrollbarX = guiX + GUI_WIDTH - 25;
                int thumbHeight = Math.max(20, scrollbarHeight * maxVisibleMoves / moves.size());
                int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                    Math.max(1, (moves.size() - maxVisibleMoves) * moveHeight);
                
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight, 0xFF404040);
                guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF808080);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int movesStartY = guiY + 55 + 70 + 30;
        int visibleAreaHeight = GUI_HEIGHT - (movesStartY - guiY) - 50;
        int moveHeight = 25;
        int maxVisibleMoves = visibleAreaHeight / moveHeight;
        
        if (mouseX >= guiX && mouseX <= guiX + GUI_WIDTH &&
            mouseY >= movesStartY && mouseY <= movesStartY + visibleAreaHeight) {
            if (moves.size() > maxVisibleMoves) {
                int oldScroll = scrollOffset;
                scrollOffset = (int) Math.max(0, Math.min((moves.size() - maxVisibleMoves) * moveHeight, 
                    scrollOffset - delta * 20));
                return oldScroll != scrollOffset;
            }
        }
        return false;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

