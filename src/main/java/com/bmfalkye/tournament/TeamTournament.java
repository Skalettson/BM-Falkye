package com.bmfalkye.tournament;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Групповой турнир (команды)
 */
public class TeamTournament extends TournamentSystem.Tournament {
    private final Map<String, Team> teams = new HashMap<>();
    private final int teamSize;
    
    public TeamTournament(String name, com.bmfalkye.tournament.TournamentSystem.TournamentType type, int maxTeams, int teamSize, 
                         int entryFee, List<com.bmfalkye.cards.Card> prizes) {
        super(name, type, maxTeams * teamSize, entryFee, prizes);
        this.teamSize = teamSize;
    }
    
    /**
     * Регистрирует команду
     */
    public boolean registerTeam(String teamName, List<ServerPlayer> players) {
        if (players.size() != teamSize) {
            return false;
        }
        
        Team team = new Team(teamName, players);
        teams.put(teamName, team);
        
        for (ServerPlayer player : players) {
            addParticipant(player);
        }
        
        return true;
    }
    
    /**
     * Команда
     */
    public static class Team {
        private final String name;
        private final List<ServerPlayer> players;
        private int wins = 0;
        private int losses = 0;
        
        public Team(String name, List<ServerPlayer> players) {
            this.name = name;
            this.players = new ArrayList<>(players);
        }
        
        public String getName() { return name; }
        public List<ServerPlayer> getPlayers() { return players; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        
        public void addWin() { wins++; }
        public void addLoss() { losses++; }
    }
}

