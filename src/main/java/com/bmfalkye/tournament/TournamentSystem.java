package com.bmfalkye.tournament;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.storage.PlayerProgressStorage;
import com.bmfalkye.player.PlayerProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Система турниров - организованные соревнования с призами
 * Улучшенная версия с поддержкой призовых мест, истории и автоматического создания
 */
public class TournamentSystem {
    private static final Map<String, Tournament> activeTournaments = new HashMap<>();
    private static final Map<String, TournamentHistory> tournamentHistory = new HashMap<>();
    private static final Map<UUID, TournamentParticipation> playerParticipations = new HashMap<>();
    private static final Map<UUID, List<TournamentHistory>> playerTournamentHistory = new HashMap<>();
    
    // Автоматическое создание турниров
    private static final long AUTO_TOURNAMENT_INTERVAL = 86400000L; // 24 часа в миллисекундах
    private static long lastAutoTournamentTime = System.currentTimeMillis();
    
    // Счётчик для коротких ID
    private static int tournamentIdCounter = 1;
    
    /**
     * Генерирует короткий ID для турнира
     */
    private static String generateShortId() {
        return "T" + tournamentIdCounter++;
    }
    
    /**
     * Создаёт новый турнир
     */
    public static Tournament createTournament(String name, TournamentType type, int maxParticipants, 
                                              int entryFee, List<Card> prizes) {
        Tournament tournament = new Tournament(name, type, maxParticipants, entryFee, prizes);
        activeTournaments.put(tournament.getId(), tournament);
        return tournament;
    }
    
    /**
     * Регистрирует игрока на турнир
     */
    public static boolean registerPlayer(ServerPlayer player, String tournamentId) {
        Tournament tournament = activeTournaments.get(tournamentId);
        if (tournament == null) {
            player.sendSystemMessage(Component.literal("§cТурнир не найден!"));
            return false;
        }
        
        if (tournament.isStarted()) {
            player.sendSystemMessage(Component.literal("§cТурнир уже начался!"));
            return false;
        }
        
        if (tournament.getParticipants().size() >= tournament.getMaxParticipants()) {
            player.sendSystemMessage(Component.literal("§cТурнир переполнен!"));
            return false;
        }
        
        // Проверяем входной взнос
        if (tournament.getEntryFee() > 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
            if (currency.getCoins(player) < tournament.getEntryFee()) {
                player.sendSystemMessage(Component.literal("§cНедостаточно монет для участия!"));
                return false;
            }
            currency.removeCoins(player, tournament.getEntryFee());
        }
        
        tournament.addParticipant(player);
        playerParticipations.put(player.getUUID(), new TournamentParticipation(tournamentId));
        
        player.sendSystemMessage(Component.literal("§aВы зарегистрированы на турнир: §f" + tournament.getName()));
        broadcastTournamentMessage(tournament, "§7Игрок " + player.getName().getString() + " присоединился к турниру!");
        
        // Автоматически начинаем турнир, если набралось достаточно участников
        if (tournament.getParticipants().size() >= tournament.getMinParticipants()) {
            startTournament(tournamentId);
        }
        
        return true;
    }
    
    /**
     * Начинает турнир
     */
    public static void startTournament(String tournamentId) {
        Tournament tournament = activeTournaments.get(tournamentId);
        if (tournament == null || tournament.isStarted()) {
            return;
        }
        
        if (tournament.getParticipants().size() < tournament.getMinParticipants()) {
            broadcastTournamentMessage(tournament, "§cНедостаточно участников для начала турнира!");
            return;
        }
        
        tournament.start();
        broadcastTournamentMessage(tournament, "§a§lТУРНИР НАЧАЛСЯ! Удачи всем участникам!");
        
        // Генерируем сетку турнира
        generateTournamentBracket(tournament);
        
        // Начинаем первый раунд
        startNextRound(tournament);
    }
    
    /**
     * Генерирует сетку турнира
     */
    private static void generateTournamentBracket(Tournament tournament) {
        List<ServerPlayer> participants = new ArrayList<>(tournament.getParticipants());
        Collections.shuffle(participants);
        
        List<TournamentMatch> matches = new ArrayList<>();
        for (int i = 0; i < participants.size(); i += 2) {
            if (i + 1 < participants.size()) {
                TournamentMatch match = new TournamentMatch(participants.get(i), participants.get(i + 1));
                match.setRound(1);
                matches.add(match);
            } else {
                // Нечётное количество участников - один проходит автоматически
                tournament.addBye(participants.get(i));
            }
        }
        
        tournament.setCurrentRoundMatches(matches);
    }
    
    /**
     * Начинает следующий раунд турнира
     */
    private static void startNextRound(Tournament tournament) {
        List<TournamentMatch> currentMatches = tournament.getCurrentRoundMatches();
        if (currentMatches == null || currentMatches.isEmpty()) {
            // Турнир завершён
            finishTournament(tournament);
            return;
        }
        
        broadcastTournamentMessage(tournament, "§6Раунд " + tournament.getCurrentRound() + " начался!");
        
        // Уведомляем участников о матчах
        for (TournamentMatch match : currentMatches) {
            if (match.getPlayer1() != null && match.getPlayer2() != null) {
                match.getPlayer1().sendSystemMessage(Component.literal(
                    "§aВаш матч против " + match.getPlayer2().getName().getString()));
                match.getPlayer2().sendSystemMessage(Component.literal(
                    "§aВаш матч против " + match.getPlayer1().getName().getString()));
            }
        }
    }
    
    /**
     * Обрабатывает результат матча в турнире
     */
    public static void handleMatchResult(String tournamentId, ServerPlayer winner, ServerPlayer loser) {
        Tournament tournament = activeTournaments.get(tournamentId);
        if (tournament == null) {
            return;
        }
        
        // Находим матч
        TournamentMatch match = tournament.findMatch(winner, loser);
        if (match == null || match.isCompleted()) {
            return;
        }
        
        match.setWinner(winner);
        match.setCompleted(true);
        
        // Добавляем победителя в список победителей раунда
        tournament.addWinner(winner);
        
        broadcastTournamentMessage(tournament, 
            "§a" + winner.getName().getString() + " победил " + loser.getName().getString());
        
        // Проверяем, все ли матчи завершены
        if (tournament.allMatchesCompleted()) {
            // Если это последний раунд, завершаем турнир
            if (tournament.getCurrentRound() >= tournament.getTotalRounds()) {
                finishTournament(tournament);
            } else {
                tournament.nextRound();
                startNextRound(tournament);
            }
        }
    }
    
    /**
     * Завершает турнир и выдаёт призы
     */
    private static void finishTournament(Tournament tournament) {
        if (tournament.isFinished()) {
            return;
        }
        
        tournament.finish();
        
        // Определяем призовые места
        determineTournamentPlaces(tournament);
        
        ServerPlayer winner = tournament.getFirstPlace();
        if (winner == null) {
            return;
        }
        
        broadcastTournamentMessage(tournament, 
            "§6§l══════ ТУРНИР ЗАВЕРШЁН ══════");
        broadcastTournamentMessage(tournament, 
            "§6§l1 МЕСТО: §f" + (winner != null ? winner.getName().getString() : "Не определено"));
        broadcastTournamentMessage(tournament, 
            "§e§l2 МЕСТО: §f" + (tournament.getSecondPlace() != null ? tournament.getSecondPlace().getName().getString() : "Не определено"));
        broadcastTournamentMessage(tournament, 
            "§c§l3 МЕСТО: §f" + (tournament.getThirdPlace() != null ? tournament.getThirdPlace().getName().getString() : "Не определено"));
        
        // Выдаём призы за 1 место
        if (winner != null) {
            giveTournamentPrizes(winner, tournament, 1);
        }
        
        // Выдаём призы за 2 место
        if (tournament.getSecondPlace() != null) {
            giveTournamentPrizes(tournament.getSecondPlace(), tournament, 2);
        }
        
        // Выдаём призы за 3 место
        if (tournament.getThirdPlace() != null) {
            giveTournamentPrizes(tournament.getThirdPlace(), tournament, 3);
        }
        
        // Сохраняем историю турнира
        saveTournamentHistory(tournament);
        
        // Обновляем статистику
        updateTournamentStatistics(tournament);
        
        // Удаляем турнир из активных, но сохраняем в истории
        activeTournaments.remove(tournament.getId());
        
        // Если это пользовательский турнир, помечаем его как завершённый
        if (tournament.getCustomTournamentId() != null && winner != null && winner.level() instanceof ServerLevel serverLevel) {
            com.bmfalkye.tournament.CustomTournamentStorage storage = 
                com.bmfalkye.tournament.CustomTournamentStorage.get(serverLevel);
            com.bmfalkye.tournament.CustomTournament customTournament = 
                storage.getTournament(tournament.getCustomTournamentId());
            if (customTournament != null) {
                customTournament.setFinished(true);
                storage.setDirty();
            }
        }
    }
    
    /**
     * Определяет призовые места в турнире
     */
    private static void determineTournamentPlaces(Tournament tournament) {
        // 1 место - победитель финала
        ServerPlayer winner = tournament.getWinner();
        if (winner != null) {
            tournament.setFirstPlace(winner);
        }
        
        // Для определения 2 и 3 места используем проигравших в полуфинале и финале
        List<TournamentMatch> allMatches = tournament.getAllMatches();
        if (allMatches.isEmpty()) {
            return;
        }
        
        // Находим финальный матч
        TournamentMatch finalMatch = null;
        for (TournamentMatch match : allMatches) {
            if (match.getRound() == tournament.getTotalRounds()) {
                finalMatch = match;
                break;
            }
        }
        
        if (finalMatch != null && finalMatch.isCompleted()) {
            // 2 место - проигравший в финале
            ServerPlayer loser = finalMatch.getPlayer1().equals(winner) ? 
                finalMatch.getPlayer2() : finalMatch.getPlayer1();
            tournament.setSecondPlace(loser);
            
            // 3 место - проигравшие в полуфинале (если есть)
            int semifinalRound = tournament.getTotalRounds() - 1;
            for (TournamentMatch match : allMatches) {
                if (match.getRound() == semifinalRound && match.isCompleted()) {
                    ServerPlayer semifinalLoser = match.getLoser();
                    if (semifinalLoser != null && !semifinalLoser.equals(loser)) {
                        tournament.setThirdPlace(semifinalLoser);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Сохраняет историю турнира
     */
    private static void saveTournamentHistory(Tournament tournament) {
        TournamentHistory history = new TournamentHistory(
            tournament.getId(),
            tournament.getName(),
            tournament.getType(),
            tournament.getParticipants().size(),
            tournament.getFirstPlace() != null ? tournament.getFirstPlace().getUUID() : null,
            tournament.getSecondPlace() != null ? tournament.getSecondPlace().getUUID() : null,
            tournament.getThirdPlace() != null ? tournament.getThirdPlace().getUUID() : null,
            System.currentTimeMillis()
        );
        
        tournamentHistory.put(tournament.getId(), history);
        
        // Сохраняем историю для каждого игрока
        if (tournament.getFirstPlace() != null) {
            playerTournamentHistory.computeIfAbsent(tournament.getFirstPlace().getUUID(), k -> new ArrayList<>()).add(history);
        }
        if (tournament.getSecondPlace() != null) {
            playerTournamentHistory.computeIfAbsent(tournament.getSecondPlace().getUUID(), k -> new ArrayList<>()).add(history);
        }
        if (tournament.getThirdPlace() != null) {
            playerTournamentHistory.computeIfAbsent(tournament.getThirdPlace().getUUID(), k -> new ArrayList<>()).add(history);
        }
    }
    
    /**
     * Обновляет статистику участников турнира
     */
    private static void updateTournamentStatistics(Tournament tournament) {
        for (ServerPlayer participant : tournament.getParticipants()) {
            if (!(participant.level() instanceof ServerLevel)) continue;
            
            PlayerProgressStorage storage = PlayerProgressStorage.get((ServerLevel) participant.level());
            PlayerProgress progress = storage.getPlayerProgress(participant);
            
            // Обновляем статистику участия
            progress.getStatistics().put("tournaments_participated", 
                progress.getStatistics().getOrDefault("tournaments_participated", 0) + 1);
            
            // Обновляем статистику побед
            if (participant.equals(tournament.getFirstPlace())) {
                progress.getStatistics().put("tournaments_won", 
                    progress.getStatistics().getOrDefault("tournaments_won", 0) + 1);
            } else if (participant.equals(tournament.getSecondPlace())) {
                progress.getStatistics().put("tournaments_second_place", 
                    progress.getStatistics().getOrDefault("tournaments_second_place", 0) + 1);
            } else if (participant.equals(tournament.getThirdPlace())) {
                progress.getStatistics().put("tournaments_third_place", 
                    progress.getStatistics().getOrDefault("tournaments_third_place", 0) + 1);
            }
            
            storage.setPlayerProgress(participant, progress);
        }
    }
    
    /**
     * Выдаёт призы за место в турнире (1, 2 или 3)
     */
    private static void giveTournamentPrizes(ServerPlayer player, Tournament tournament, int place) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        PlayerProgressStorage storage = PlayerProgressStorage.get(serverLevel);
        PlayerProgress progress = storage.getPlayerProgress(player);
        com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
        
        // Используем призовой фонд пользовательского турнира, если есть
        int prizePool = tournament.getCustomTournamentPrizePool();
        int coinReward = 0;
        int xpReward = 0;
        
        switch (place) {
            case 1 -> {
                // 1 место: 50% призового фонда + все карты
                coinReward = (int)(prizePool * 0.5);
                xpReward = 1000;
                
                // Выдаём карты из призов
                for (Card prize : tournament.getPrizes()) {
                    progress.unlockCard(prize.getId());
                    player.sendSystemMessage(Component.literal("§6§lПРИЗ 1 МЕСТА: §f" + prize.getName()));
                }
                
                // Дополнительная редкая карта за 1 место
                Card bonusCard = getRandomCardByRarity(CardRarity.EPIC);
                if (bonusCard != null) {
                    progress.unlockCard(bonusCard.getId());
                    player.sendSystemMessage(Component.literal("§6§lБОНУС: §f" + bonusCard.getName()));
                }
            }
            case 2 -> {
                // 2 место: 30% призового фонда + редкая карта
                coinReward = (int)(prizePool * 0.3);
                xpReward = 500;
                
                Card rewardCard = getRandomCardByRarity(CardRarity.RARE);
                if (rewardCard != null) {
                    progress.unlockCard(rewardCard.getId());
                    player.sendSystemMessage(Component.literal("§e§lПРИЗ 2 МЕСТА: §f" + rewardCard.getName()));
                }
            }
            case 3 -> {
                // 3 место: 20% призового фонда + обычная карта
                coinReward = (int)(prizePool * 0.2);
                xpReward = 250;
                
                Card rewardCard = getRandomCardByRarity(CardRarity.COMMON);
                if (rewardCard != null) {
                    progress.unlockCard(rewardCard.getId());
                    player.sendSystemMessage(Component.literal("§c§lПРИЗ 3 МЕСТА: §f" + rewardCard.getName()));
                }
            }
        }
        
        // Выдаём монеты и опыт
        if (coinReward > 0) {
            currency.addCoins(player, coinReward);
            player.sendSystemMessage(Component.literal("§aПолучено монет: §e" + coinReward));
        }
        
        if (xpReward > 0) {
            progress.addExperience(xpReward);
            player.sendSystemMessage(Component.literal("§bПолучено опыта: " + xpReward));
        }
        
        storage.setPlayerProgress(player, progress);
    }
    
    /**
     * Получает случайную карту по редкости
     */
    private static Card getRandomCardByRarity(CardRarity rarity) {
        List<Card> cards = CardRegistry.getCardsByRarity(rarity);
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(new Random().nextInt(cards.size()));
    }
    
    /**
     * Отправляет сообщение всем участникам турнира
     */
    private static void broadcastTournamentMessage(Tournament tournament, String message) {
        for (ServerPlayer participant : tournament.getParticipants()) {
            participant.sendSystemMessage(Component.literal("§6[Турнир] §r" + message));
        }
    }
    
    /**
     * Получает список активных турниров
     */
    public static List<Tournament> getActiveTournaments() {
        return new ArrayList<>(activeTournaments.values());
    }
    
    /**
     * Тип турнира
     */
    public enum TournamentType {
        SINGLE_ELIMINATION("Олимпийская система"),
        DOUBLE_ELIMINATION("Двойная олимпийская система"),
        ROUND_ROBIN("Круговой турнир"),
        SWISS("Швейцарская система");
        
        private final String displayName;
        
        TournamentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Класс турнира
     */
    public static class Tournament {
        private final String id;
        private final String name;
        private final TournamentType type;
        private final int maxParticipants;
        private final int minParticipants;
        private final int entryFee;
        private final List<Card> prizes;
        private final List<ServerPlayer> participants = new ArrayList<>();
        private final List<ServerPlayer> winners = new ArrayList<>();
        private final List<ServerPlayer> byes = new ArrayList<>();
        private List<TournamentMatch> currentRoundMatches = new ArrayList<>();
        private List<TournamentMatch> previousRoundMatches = new ArrayList<>();
        private int currentRound = 1;
        private boolean started = false;
        private boolean finished = false;
        private ServerPlayer firstPlace = null;
        private ServerPlayer secondPlace = null;
        private ServerPlayer thirdPlace = null;
        private final long startTime;
        private final long endTime;
        
        public Tournament(String name, TournamentType type, int maxParticipants, int entryFee, List<Card> prizes) {
            this.id = generateShortId();
            this.name = name;
            this.type = type;
            this.maxParticipants = maxParticipants;
            this.minParticipants = Math.max(2, maxParticipants / 2);
            this.entryFee = entryFee;
            this.prizes = prizes;
            this.startTime = System.currentTimeMillis();
            this.endTime = startTime + 604800000L; // 7 дней по умолчанию
        }
        
        public void addParticipant(ServerPlayer player) {
            if (!participants.contains(player)) {
                participants.add(player);
            }
        }
        
        public void addWinner(ServerPlayer player) {
            winners.add(player);
        }
        
        public void addBye(ServerPlayer player) {
            byes.add(player);
        }
        
        public void nextRound() {
            // ИСПРАВЛЕНО: Сохраняем победителей перед очисткой
            List<ServerPlayer> roundWinners = new ArrayList<>(winners);
            List<ServerPlayer> roundByes = new ArrayList<>(byes);
            
            // Сохраняем текущие матчи в историю
            previousRoundMatches.addAll(currentRoundMatches);
            
            currentRound++;
            
            // Очищаем для нового раунда
            winners.clear();
            currentRoundMatches.clear();
            byes.clear();
            
            // Генерируем новые матчи для следующего раунда из победителей предыдущего раунда
            List<ServerPlayer> nextRoundPlayers = new ArrayList<>(roundWinners);
            nextRoundPlayers.addAll(roundByes);
            
            for (int i = 0; i < nextRoundPlayers.size(); i += 2) {
                if (i + 1 < nextRoundPlayers.size()) {
                    TournamentMatch match = new TournamentMatch(nextRoundPlayers.get(i), nextRoundPlayers.get(i + 1));
                    match.setRound(currentRound);
                    currentRoundMatches.add(match);
                } else {
                    byes.add(nextRoundPlayers.get(i));
                }
            }
        }
        
        public TournamentMatch findMatch(ServerPlayer player1, ServerPlayer player2) {
            for (TournamentMatch match : currentRoundMatches) {
                if ((match.getPlayer1().equals(player1) && match.getPlayer2().equals(player2)) ||
                    (match.getPlayer1().equals(player2) && match.getPlayer2().equals(player1))) {
                    return match;
                }
            }
            return null;
        }
        
        public boolean allMatchesCompleted() {
            for (TournamentMatch match : currentRoundMatches) {
                if (!match.isCompleted()) {
                    return false;
                }
            }
            return true;
        }
        
        public void start() {
            this.started = true;
        }
        
        public ServerPlayer getWinner() {
            return winners.size() == 1 ? winners.get(0) : null;
        }
        
        public void finish() {
            this.finished = true;
        }
        
        public boolean isFinished() {
            return finished;
        }
        
        public int getTotalRounds() {
            return (int) Math.ceil(Math.log(participants.size()) / Math.log(2));
        }
        
        public List<TournamentMatch> getAllMatches() {
            List<TournamentMatch> allMatches = new ArrayList<>();
            allMatches.addAll(currentRoundMatches);
            if (previousRoundMatches != null) {
                allMatches.addAll(previousRoundMatches);
            }
            return allMatches;
        }
        
        // Геттеры и сеттеры для призовых мест
        public ServerPlayer getFirstPlace() { return firstPlace; }
        public void setFirstPlace(ServerPlayer player) { this.firstPlace = player; }
        public ServerPlayer getSecondPlace() { return secondPlace; }
        public void setSecondPlace(ServerPlayer player) { this.secondPlace = player; }
        public ServerPlayer getThirdPlace() { return thirdPlace; }
        public void setThirdPlace(ServerPlayer player) { this.thirdPlace = player; }
        
        // Геттеры
        public String getId() { return id; }
        public String getName() { return name; }
        public TournamentType getType() { return type; }
        public int getMaxParticipants() { return maxParticipants; }
        public int getMinParticipants() { return minParticipants; }
        public int getEntryFee() { return entryFee; }
        public List<Card> getPrizes() { return prizes; }
        public List<ServerPlayer> getParticipants() { return participants; }
        public List<TournamentMatch> getCurrentRoundMatches() { return currentRoundMatches; }
        public void setCurrentRoundMatches(List<TournamentMatch> matches) { this.currentRoundMatches = matches; }
        public int getCurrentRound() { return currentRound; }
        public boolean isStarted() { return started; }
        
        public int getPrizePool() {
            return entryFee * participants.size();
        }
        
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        
        // Для пользовательских турниров
        private int customTournamentPrizePool = 0;
        private String customTournamentId = null;
        
        public void setCustomTournamentPrizePool(int prizePool) {
            this.customTournamentPrizePool = prizePool;
        }
        
        public int getCustomTournamentPrizePool() {
            return customTournamentPrizePool > 0 ? customTournamentPrizePool : getPrizePool();
        }
        
        public void setCustomTournamentId(String id) {
            this.customTournamentId = id;
        }
        
        public String getCustomTournamentId() {
            return customTournamentId;
        }
    }
    
    /**
     * Получает турнир по ID
     */
    public static Tournament getTournament(String tournamentId) {
        return activeTournaments.get(tournamentId);
    }
    
    /**
     * Матч в турнире
     */
    public static class TournamentMatch {
        private final ServerPlayer player1;
        private final ServerPlayer player2;
        private ServerPlayer winner;
        private int round;
        private boolean completed = false;
        
        public TournamentMatch(ServerPlayer player1, ServerPlayer player2) {
            this.player1 = player1;
            this.player2 = player2;
        }
        
        public ServerPlayer getPlayer1() { return player1; }
        public ServerPlayer getPlayer2() { return player2; }
        public ServerPlayer getWinner() { return winner; }
        public void setWinner(ServerPlayer winner) { 
            this.winner = winner; 
        }
        public ServerPlayer getLoser() {
            if (winner == null) return null;
            return winner.equals(player1) ? player2 : player1;
        }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public int getRound() { return round; }
        public void setRound(int round) { this.round = round; }
    }
    
    /**
     * История турнира
     */
    public static class TournamentHistory {
        private final String tournamentId;
        private final String tournamentName;
        private final TournamentType type;
        private final int participantCount;
        private final UUID firstPlace;
        private final UUID secondPlace;
        private final UUID thirdPlace;
        private final long endTime;
        
        public TournamentHistory(String tournamentId, String tournamentName, TournamentType type,
                                int participantCount, UUID firstPlace, UUID secondPlace, UUID thirdPlace,
                                long endTime) {
            this.tournamentId = tournamentId;
            this.tournamentName = tournamentName;
            this.type = type;
            this.participantCount = participantCount;
            this.firstPlace = firstPlace;
            this.secondPlace = secondPlace;
            this.thirdPlace = thirdPlace;
            this.endTime = endTime;
        }
        
        public String getTournamentId() { return tournamentId; }
        public String getTournamentName() { return tournamentName; }
        public TournamentType getType() { return type; }
        public int getParticipantCount() { return participantCount; }
        public UUID getFirstPlace() { return firstPlace; }
        public UUID getSecondPlace() { return secondPlace; }
        public UUID getThirdPlace() { return thirdPlace; }
        public long getEndTime() { return endTime; }
    }
    
    /**
     * Получает историю турниров игрока
     */
    public static List<TournamentHistory> getPlayerTournamentHistory(UUID playerUUID) {
        return playerTournamentHistory.getOrDefault(playerUUID, new ArrayList<>());
    }
    
    /**
     * Получает историю конкретного турнира
     */
    public static TournamentHistory getTournamentHistory(String tournamentId) {
        return tournamentHistory.get(tournamentId);
    }
    
    /**
     * Автоматически создаёт новый турнир при необходимости
     */
    public static void checkAndCreateAutoTournament(ServerLevel level) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoTournamentTime < AUTO_TOURNAMENT_INTERVAL) {
            return;
        }
        
        // Проверяем, есть ли уже активные турниры
        if (!activeTournaments.isEmpty()) {
            return;
        }
        
        // Создаём автоматический турнир
        createAutoTournament(level);
        lastAutoTournamentTime = currentTime;
    }
    
    /**
     * Создаёт автоматический турнир
     */
    private static void createAutoTournament(ServerLevel level) {
        Random random = new Random();
        String[] tournamentNames = {
            "Еженедельный турнир",
            "Турнир мастеров",
            "Кубок чемпионов",
            "Ежедневный турнир",
            "Турнир новичков"
        };
        
        String name = tournamentNames[random.nextInt(tournamentNames.length)];
        TournamentType type = TournamentType.SINGLE_ELIMINATION;
        int maxParticipants = 8 + random.nextInt(9); // 8-16 участников
        int entryFee = 50 + random.nextInt(100); // 50-150 монет
        
        // Генерируем призы
        List<Card> prizes = new ArrayList<>();
        prizes.add(getRandomCardByRarity(CardRarity.EPIC));
        if (random.nextBoolean()) {
            prizes.add(getRandomCardByRarity(CardRarity.RARE));
        }
        
        Tournament tournament = createTournament(name, type, maxParticipants, entryFee, prizes);
        
        // Уведомляем всех игроков
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                "§6§l══════ НОВЫЙ ТУРНИР! ══════"));
            player.sendSystemMessage(Component.literal(
                "§6Название: §f" + tournament.getName()));
            player.sendSystemMessage(Component.literal(
                "§6Участников: §f" + tournament.getMaxParticipants()));
            player.sendSystemMessage(Component.literal(
                "§6Взнос: §e" + tournament.getEntryFee() + " монет"));
            player.sendSystemMessage(Component.literal(
                "§6Призовой фонд: §e" + tournament.getPrizePool() + " монет"));
            player.sendSystemMessage(Component.literal(
                "§7Используйте команду для регистрации"));
        }
    }
    
    /**
     * Участие игрока в турнире
     */
    private static class TournamentParticipation {
        private final String tournamentId;
        private int wins = 0;
        private int losses = 0;
        
        public TournamentParticipation(String tournamentId) {
            this.tournamentId = tournamentId;
        }
    }
}

