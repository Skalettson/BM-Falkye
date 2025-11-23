package com.bmfalkye.replay;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система реплеев игр - сохранение и воспроизведение игр
 */
public class ReplaySystem {
    private static final Map<String, GameReplay> savedReplays = new HashMap<>();
    private static final int MAX_REPLAYS = 100; // Максимум сохранённых реплеев
    
    /**
     * Сохраняет реплей игры
     */
    public static String saveReplay(FalkyeGameSession session) {
        if (session == null || !session.isGameEnded()) {
            return null;
        }
        
        GameReplay replay = new GameReplay(session);
        String replayId = UUID.randomUUID().toString();
        savedReplays.put(replayId, replay);
        
        // Ограничиваем количество реплеев
        if (savedReplays.size() > MAX_REPLAYS) {
            // Удаляем самый старый реплей
            String oldestId = savedReplays.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().getTimestamp()))
                .map(Map.Entry::getKey)
                .orElse(null);
            if (oldestId != null) {
                savedReplays.remove(oldestId);
            }
        }
        
        return replayId;
    }
    
    /**
     * Получает реплей по ID
     */
    public static GameReplay getReplay(String replayId) {
        return savedReplays.get(replayId);
    }
    
    /**
     * Получает список всех сохранённых реплеев
     */
    public static List<ReplayInfo> getAllReplays() {
        List<ReplayInfo> replays = new ArrayList<>();
        for (Map.Entry<String, GameReplay> entry : savedReplays.entrySet()) {
            replays.add(new ReplayInfo(entry.getKey(), entry.getValue()));
        }
        replays.sort(Comparator.comparing(ReplayInfo::getTimestamp).reversed());
        return replays;
    }
    
    /**
     * Воспроизводит реплей для игрока
     */
    public static void playReplay(ServerPlayer player, String replayId) {
        GameReplay replay = savedReplays.get(replayId);
        if (replay == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cРеплей не найден!"));
            return;
        }
        
        // Отправляем информацию о реплее
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l=== РЕПЛЕЙ ИГРЫ ==="));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Игрок 1: §f" + replay.getPlayer1Name()));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Игрок 2: §f" + replay.getPlayer2Name()));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Победитель: §f" + replay.getWinnerName()));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Дата: §f" + new java.util.Date(replay.getTimestamp())));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Длительность: §f" + replay.getDuration() + " секунд"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§7Ходов: §f" + replay.getMoves().size()));
        
        // Воспроизводим ходы
        playReplayMoves(player, replay);
    }
    
    /**
     * Воспроизводит ходы реплея для игрока
     */
    private static void playReplayMoves(ServerPlayer player, GameReplay replay) {
        if (replay.getMoves().isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Нет записанных ходов"));
            return;
        }
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l=== ХОДЫ ИГРЫ ==="));
        
        int currentRound = 0;
        for (ReplayMove move : replay.getMoves()) {
            // Показываем смену раунда
            if (move.getRound() != currentRound) {
                currentRound = move.getRound();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§e§l--- Раунд " + currentRound + " ---"));
            }
            
            // Форматируем действие
            String actionText = formatMoveAction(move);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7[" + formatTimestamp(move.getTimestamp() - replay.getTimestamp()) + "] §f" + 
                move.getPlayerName() + " §7- " + actionText));
        }
    }
    
    /**
     * Форматирует действие хода
     */
    private static String formatMoveAction(ReplayMove move) {
        return switch (move.getAction()) {
            case "play_card" -> {
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(move.getCardId());
                yield "сыграл карту: §b" + (card != null ? card.getName() : move.getCardId());
            }
            case "use_ability" -> {
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(move.getCardId());
                yield "использовал способность: §d" + (card != null ? card.getName() : move.getCardId());
            }
            case "use_leader" -> {
                com.bmfalkye.cards.LeaderCard leader = com.bmfalkye.cards.LeaderRegistry.getLeader(move.getCardId());
                yield "использовал лидера: §6" + (leader != null ? leader.getName() : move.getCardId());
            }
            case "pass" -> "пасовал";
            default -> move.getAction();
        };
    }
    
    /**
     * Форматирует время в секундах в читаемый формат
     */
    private static String formatTimestamp(long milliseconds) {
        int seconds = (int)(milliseconds / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        }
        return seconds + "с";
    }
    
    /**
     * Удаляет реплей
     */
    public static boolean deleteReplay(String replayId) {
        return savedReplays.remove(replayId) != null;
    }
    
    /**
     * Класс реплея игры
     */
    public static class GameReplay {
        private final String player1Name;
        private final String player2Name;
        private final String winnerName;
        private final int roundsWon1;
        private final int roundsWon2;
        private final long timestamp;
        private final int duration;
        private final List<ReplayMove> moves = new ArrayList<>();
        
        public GameReplay(FalkyeGameSession session) {
            this.player1Name = session.getPlayer1() != null ? 
                session.getPlayer1().getName().getString() : "Unknown";
            this.player2Name = session.getPlayer2() != null ? 
                session.getPlayer2().getName().getString() : 
                (session.isPlayingWithVillager() ? "Villager" : "Unknown");
            this.winnerName = session.getWinner() != null ? 
                session.getWinner().getName().getString() : "Draw";
            this.roundsWon1 = session.getRoundsWon(session.getPlayer1());
            this.roundsWon2 = session.getRoundsWon(session.getPlayer2());
            this.timestamp = System.currentTimeMillis();
            this.duration = 0; // TODO: Вычислять из сессии
            
            // TODO: Сохранять все ходы из сессии
        }
        
        public String getPlayer1Name() { return player1Name; }
        public String getPlayer2Name() { return player2Name; }
        public String getWinnerName() { return winnerName; }
        public int getRoundsWon1() { return roundsWon1; }
        public int getRoundsWon2() { return roundsWon2; }
        public long getTimestamp() { return timestamp; }
        public int getDuration() { return duration; }
        public List<ReplayMove> getMoves() { return moves; }
    }
    
    /**
     * Ход в реплее
     */
    public static class ReplayMove {
        private final String playerName;
        private final String action; // "play_card", "pass", "use_leader"
        private final String cardId;
        private final int round;
        private final long timestamp;
        
        public ReplayMove(String playerName, String action, String cardId, int round) {
            this.playerName = playerName;
            this.action = action;
            this.cardId = cardId;
            this.round = round;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getPlayerName() { return playerName; }
        public String getAction() { return action; }
        public String getCardId() { return cardId; }
        public int getRound() { return round; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Информация о реплее
     */
    public static class ReplayInfo {
        private final String replayId;
        private final GameReplay replay;
        
        public ReplayInfo(String replayId, GameReplay replay) {
            this.replayId = replayId;
            this.replay = replay;
        }
        
        public String getReplayId() { return replayId; }
        public GameReplay getReplay() { return replay; }
        public long getTimestamp() { return replay.getTimestamp(); }
    }
}

