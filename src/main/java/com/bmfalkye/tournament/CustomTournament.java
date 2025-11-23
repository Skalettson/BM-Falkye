package com.bmfalkye.tournament;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Пользовательский турнир, созданный игроком
 */
public class CustomTournament {
    private final String id;
    private final UUID organizerUUID;
    private final String name;
    private final TournamentRules rules;
    private final int maxParticipants;
    private final int entryFee;
    private final long scheduledStartTime;
    
    private final List<UUID> registeredPlayers = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final Set<UUID> disqualifiedPlayers = new HashSet<>();
    
    private int prizePool = 0;
    private boolean started = false;
    private boolean finished = false;
    private String tournamentSystemId = null; // ID в TournamentSystem после начала
    
    public CustomTournament(UUID organizerUUID, String name, TournamentRules rules,
                           int maxParticipants, int entryFee, long scheduledStartTime) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.organizerUUID = organizerUUID;
        this.name = name;
        this.rules = rules;
        this.maxParticipants = maxParticipants;
        this.entryFee = entryFee;
        this.scheduledStartTime = scheduledStartTime;
    }
    
    /**
     * Регистрирует игрока на турнир
     */
    public boolean registerPlayer(ServerPlayer player) {
        if (started) {
            return false;
        }
        
        if (registeredPlayers.size() >= maxParticipants) {
            return false;
        }
        
        if (registeredPlayers.contains(player.getUUID())) {
            return false;
        }
        
        if (disqualifiedPlayers.contains(player.getUUID())) {
            return false;
        }
        
        registeredPlayers.add(player.getUUID());
        prizePool += entryFee;
        return true;
    }
    
    /**
     * Отменяет регистрацию игрока
     */
    public boolean unregisterPlayer(UUID playerUUID) {
        if (started) {
            return false;
        }
        
        if (registeredPlayers.remove(playerUUID)) {
            prizePool -= entryFee;
            return true;
        }
        return false;
    }
    
    /**
     * Дисквалифицирует игрока (только организатор)
     */
    public boolean disqualifyPlayer(UUID organizerUUID, UUID playerUUID) {
        if (!this.organizerUUID.equals(organizerUUID)) {
            return false; // Только организатор может дисквалифицировать
        }
        
        if (registeredPlayers.remove(playerUUID)) {
            disqualifiedPlayers.add(playerUUID);
            prizePool -= entryFee;
            return true;
        }
        return false;
    }
    
    /**
     * Добавляет зрителя
     */
    public void addSpectator(UUID playerUUID) {
        if (!spectators.contains(playerUUID)) {
            spectators.add(playerUUID);
        }
    }
    
    /**
     * Удаляет зрителя
     */
    public void removeSpectator(UUID playerUUID) {
        spectators.remove(playerUUID);
    }
    
    /**
     * Начинает турнир (создаёт в TournamentSystem)
     */
    public String start(ServerLevel level) {
        if (started || registeredPlayers.size() < 2) {
            return null;
        }
        
        // Создаём турнир в TournamentSystem
        List<com.bmfalkye.cards.Card> prizes = new ArrayList<>();
        // TODO: Добавить карты в призы на основе призового фонда
        
        TournamentSystem.Tournament tournament = TournamentSystem.createTournament(
            name,
            rules == TournamentRules.LEGACY ? 
                TournamentSystem.TournamentType.SINGLE_ELIMINATION : 
                TournamentSystem.TournamentType.SINGLE_ELIMINATION,
            maxParticipants,
            entryFee,
            prizes
        );
        
        // Регистрируем всех игроков
        for (UUID playerUUID : registeredPlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                TournamentSystem.registerPlayer(player, tournament.getId());
            }
        }
        
        // Сохраняем призовой фонд для распределения при завершении
        // Комиссия будет вычтена при выдаче призов
        
        this.tournamentSystemId = tournament.getId();
        this.started = true;
        
        return tournament.getId();
    }
    
    /**
     * Сохраняет турнир в NBT
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putString("id", id);
        tag.putUUID("organizerUUID", organizerUUID);
        tag.putString("name", name);
        tag.putString("rules", rules.name());
        tag.putInt("maxParticipants", maxParticipants);
        tag.putInt("entryFee", entryFee);
        tag.putLong("scheduledStartTime", scheduledStartTime);
        tag.putInt("prizePool", prizePool);
        tag.putBoolean("started", started);
        tag.putBoolean("finished", finished);
        if (tournamentSystemId != null) {
            tag.putString("tournamentSystemId", tournamentSystemId);
        }
        
        // Сохраняем зарегистрированных игроков
        ListTag registeredTag = new ListTag();
        for (UUID uuid : registeredPlayers) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            registeredTag.add(uuidTag);
        }
        tag.put("registeredPlayers", registeredTag);
        
        // Сохраняем зрителей
        ListTag spectatorsTag = new ListTag();
        for (UUID uuid : spectators) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            spectatorsTag.add(uuidTag);
        }
        tag.put("spectators", spectatorsTag);
        
        // Сохраняем дисквалифицированных
        ListTag disqualifiedTag = new ListTag();
        for (UUID uuid : disqualifiedPlayers) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            disqualifiedTag.add(uuidTag);
        }
        tag.put("disqualifiedPlayers", disqualifiedTag);
        
        return tag;
    }
    
    /**
     * Загружает турнир из NBT
     */
    public static CustomTournament load(CompoundTag tag) {
        try {
            UUID organizerUUID = tag.getUUID("organizerUUID");
            String name = tag.getString("name");
            TournamentRules rules = TournamentRules.valueOf(tag.getString("rules"));
            int maxParticipants = tag.getInt("maxParticipants");
            int entryFee = tag.getInt("entryFee");
            long scheduledStartTime = tag.getLong("scheduledStartTime");
            
            CustomTournament tournament = new CustomTournament(
                organizerUUID, name, rules, maxParticipants, entryFee, scheduledStartTime);
            
            tournament.prizePool = tag.getInt("prizePool");
            tournament.started = tag.getBoolean("started");
            tournament.finished = tag.getBoolean("finished");
            if (tag.contains("tournamentSystemId")) {
                tournament.tournamentSystemId = tag.getString("tournamentSystemId");
            }
            
            // Загружаем зарегистрированных игроков
            ListTag registeredTag = tag.getList("registeredPlayers", 10); // TAG_Compound
            for (int i = 0; i < registeredTag.size(); i++) {
                CompoundTag uuidTag = registeredTag.getCompound(i);
                tournament.registeredPlayers.add(uuidTag.getUUID("uuid"));
            }
            
            // Загружаем зрителей
            ListTag spectatorsTag = tag.getList("spectators", 10);
            for (int i = 0; i < spectatorsTag.size(); i++) {
                CompoundTag uuidTag = spectatorsTag.getCompound(i);
                tournament.spectators.add(uuidTag.getUUID("uuid"));
            }
            
            // Загружаем дисквалифицированных
            ListTag disqualifiedTag = tag.getList("disqualifiedPlayers", 10);
            for (int i = 0; i < disqualifiedTag.size(); i++) {
                CompoundTag uuidTag = disqualifiedTag.getCompound(i);
                tournament.disqualifiedPlayers.add(uuidTag.getUUID("uuid"));
            }
            
            return tournament;
        } catch (Exception e) {
            com.bmfalkye.util.ModLogger.error("Error loading CustomTournament", "error", e.getMessage());
            return null;
        }
    }
    
    // Геттеры
    public String getId() { return id; }
    public UUID getOrganizerUUID() { return organizerUUID; }
    public String getName() { return name; }
    public TournamentRules getRules() { return rules; }
    public int getMaxParticipants() { return maxParticipants; }
    public int getEntryFee() { return entryFee; }
    public long getScheduledStartTime() { return scheduledStartTime; }
    public List<UUID> getRegisteredPlayers() { return new ArrayList<>(registeredPlayers); }
    public List<UUID> getSpectators() { return new ArrayList<>(spectators); }
    public Set<UUID> getDisqualifiedPlayers() { return new HashSet<>(disqualifiedPlayers); }
    public int getPrizePool() { return prizePool; }
    public boolean isStarted() { return started; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public String getTournamentSystemId() { return tournamentSystemId; }
    
    /**
     * Правила турнира
     */
    public enum TournamentRules {
        STANDARD("Стандарт", "Только базовые карты без улучшений"),
        LEGACY("Наследие", "Все карты с улучшениями");
        
        private final String displayName;
        private final String description;
        
        TournamentRules(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}

