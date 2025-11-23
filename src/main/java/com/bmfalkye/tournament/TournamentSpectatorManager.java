package com.bmfalkye.tournament;

import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.GameManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Менеджер для управления зрителями турниров
 */
public class TournamentSpectatorManager {
    // Матч турнира -> список зрителей
    private static final Map<String, Set<UUID>> matchSpectators = new HashMap<>();
    
    // Зритель -> матч, который он смотрит
    private static final Map<UUID, String> spectatorToMatch = new HashMap<>();
    
    /**
     * Добавляет зрителя к матчу турнира
     */
    public static void addSpectatorToMatch(ServerPlayer spectator, String tournamentId, 
                                          ServerPlayer player1, ServerPlayer player2) {
        String matchKey = createMatchKey(tournamentId, player1, player2);
        
        matchSpectators.computeIfAbsent(matchKey, k -> new HashSet<>()).add(spectator.getUUID());
        spectatorToMatch.put(spectator.getUUID(), matchKey);
    }
    
    /**
     * Удаляет зрителя из матча
     */
    public static void removeSpectator(ServerPlayer spectator) {
        String matchKey = spectatorToMatch.remove(spectator.getUUID());
        if (matchKey != null) {
            Set<UUID> spectators = matchSpectators.get(matchKey);
            if (spectators != null) {
                spectators.remove(spectator.getUUID());
                if (spectators.isEmpty()) {
                    matchSpectators.remove(matchKey);
                }
            }
        }
    }
    
    /**
     * Получает список зрителей матча
     */
    public static Set<UUID> getMatchSpectators(String tournamentId, 
                                               ServerPlayer player1, ServerPlayer player2) {
        String matchKey = createMatchKey(tournamentId, player1, player2);
        return matchSpectators.getOrDefault(matchKey, new HashSet<>());
    }
    
    /**
     * Получает матч, который смотрит зритель
     */
    public static String getSpectatorMatch(UUID spectatorUUID) {
        return spectatorToMatch.get(spectatorUUID);
    }
    
    /**
     * Находит активную игровую сессию для матча турнира
     */
    public static FalkyeGameSession findTournamentMatch(ServerLevel level, 
                                                        ServerPlayer player1, 
                                                        ServerPlayer player2) {
        // Ищем активную игру между этими игроками
        for (FalkyeGameSession session : GameManager.getActiveGames().values()) {
            if (session.getPlayer1() != null && session.getPlayer2() != null) {
                UUID p1UUID = session.getPlayer1().getUUID();
                UUID p2UUID = session.getPlayer2().getUUID();
                
                UUID player1UUID = player1.getUUID();
                UUID player2UUID = player2.getUUID();
                
                if ((p1UUID.equals(player1UUID) && p2UUID.equals(player2UUID)) ||
                    (p1UUID.equals(player2UUID) && p2UUID.equals(player1UUID))) {
                    return session;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Получает список активных матчей турнира
     */
    public static List<TournamentMatchInfo> getActiveTournamentMatches(ServerLevel level, 
                                                                       TournamentSystem.Tournament tournament) {
        List<TournamentMatchInfo> matches = new ArrayList<>();
        
        for (TournamentSystem.TournamentMatch match : tournament.getCurrentRoundMatches()) {
            if (!match.isCompleted() && match.getPlayer1() != null && match.getPlayer2() != null) {
                ServerPlayer p1 = match.getPlayer1();
                ServerPlayer p2 = match.getPlayer2();
                
                // Проверяем, есть ли активная игра
                FalkyeGameSession session = findTournamentMatch(level, p1, p2);
                if (session != null) {
                    // Используем рефлексию для доступа к полям roundsWon1 и roundsWon2
                    int roundsWon1 = 0;
                    int roundsWon2 = 0;
                    try {
                        java.lang.reflect.Field roundsWon1Field = FalkyeGameSession.class.getDeclaredField("roundsWon1");
                        roundsWon1Field.setAccessible(true);
                        roundsWon1 = roundsWon1Field.getInt(session);
                        
                        java.lang.reflect.Field roundsWon2Field = FalkyeGameSession.class.getDeclaredField("roundsWon2");
                        roundsWon2Field.setAccessible(true);
                        roundsWon2 = roundsWon2Field.getInt(session);
                    } catch (Exception e) {
                        // Используем значения по умолчанию
                    }
                    
                    matches.add(new TournamentMatchInfo(
                        tournament.getId(),
                        match.getRound(),
                        p1.getUUID(),
                        p1.getName().getString(),
                        p2.getUUID(),
                        p2.getName().getString(),
                        session.getCurrentRound(),
                        roundsWon1,
                        roundsWon2
                    ));
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Создаёт ключ для матча
     */
    private static String createMatchKey(String tournamentId, ServerPlayer player1, ServerPlayer player2) {
        UUID p1UUID = player1.getUUID();
        UUID p2UUID = player2.getUUID();
        
        // Сортируем UUID для консистентности
        if (p1UUID.compareTo(p2UUID) > 0) {
            UUID temp = p1UUID;
            p1UUID = p2UUID;
            p2UUID = temp;
        }
        
        return tournamentId + ":" + p1UUID + ":" + p2UUID;
    }
    
    /**
     * Информация о матче турнира для зрителей
     */
    public static class TournamentMatchInfo {
        private final String tournamentId;
        private final int round;
        private final UUID player1UUID;
        private final String player1Name;
        private final UUID player2UUID;
        private final String player2Name;
        private final int currentGameRound;
        private final int roundsWon1;
        private final int roundsWon2;
        
        public TournamentMatchInfo(String tournamentId, int round, UUID player1UUID, String player1Name,
                                  UUID player2UUID, String player2Name, int currentGameRound,
                                  int roundsWon1, int roundsWon2) {
            this.tournamentId = tournamentId;
            this.round = round;
            this.player1UUID = player1UUID;
            this.player1Name = player1Name;
            this.player2UUID = player2UUID;
            this.player2Name = player2Name;
            this.currentGameRound = currentGameRound;
            this.roundsWon1 = roundsWon1;
            this.roundsWon2 = roundsWon2;
        }
        
        public String getTournamentId() { return tournamentId; }
        public int getRound() { return round; }
        public UUID getPlayer1UUID() { return player1UUID; }
        public String getPlayer1Name() { return player1Name; }
        public UUID getPlayer2UUID() { return player2UUID; }
        public String getPlayer2Name() { return player2Name; }
        public int getCurrentGameRound() { return currentGameRound; }
        public int getRoundsWon1() { return roundsWon1; }
        public int getRoundsWon2() { return roundsWon2; }
    }
}

