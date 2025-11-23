package com.bmfalkye.tournament;

import com.bmfalkye.util.DataLoadValidator;
import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Хранилище пользовательских турниров
 */
public class CustomTournamentStorage extends SavedData {
    // ID турнира -> CustomTournament
    private final Map<String, CustomTournament> tournaments = new HashMap<>();
    
    public static CustomTournamentStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            CustomTournamentStorage::load,
            CustomTournamentStorage::new,
            "bm_falkye_custom_tournaments"
        );
    }
    
    @NotNull
    public static CustomTournamentStorage load(@NotNull CompoundTag tag) {
        CustomTournamentStorage storage = new CustomTournamentStorage();
        
        try {
            CompoundTag tournamentsTag = tag.getCompound("tournaments");
            
            for (String tournamentId : tournamentsTag.getAllKeys()) {
                try {
                    CompoundTag tournamentTag = tournamentsTag.getCompound(tournamentId);
                    CustomTournament tournament = CustomTournament.load(tournamentTag);
                    if (tournament != null) {
                        storage.tournaments.put(tournamentId, tournament);
                    }
                } catch (Exception e) {
                    ModLogger.error("Error loading custom tournament", 
                        "tournamentId", tournamentId, "error", e.getMessage());
                }
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading CustomTournamentStorage, using empty storage", e);
            ModLogger.error("Error loading CustomTournamentStorage", "error", e.getMessage());
            return new CustomTournamentStorage();
        }
        
        return storage;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag tournamentsTag = new CompoundTag();
        
        for (Map.Entry<String, CustomTournament> entry : tournaments.entrySet()) {
            CompoundTag tournamentTag = entry.getValue().save(new CompoundTag());
            tournamentsTag.put(entry.getKey(), tournamentTag);
        }
        
        tag.put("tournaments", tournamentsTag);
        return tag;
    }
    
    /**
     * Создаёт пользовательский турнир
     */
    public CustomTournament createTournament(ServerPlayer organizer, String name, 
                                            CustomTournament.TournamentRules rules,
                                            int maxParticipants, int entryFee, 
                                            long scheduledStartTime) {
        CustomTournament tournament = new CustomTournament(
            organizer.getUUID(), name, rules, maxParticipants, entryFee, scheduledStartTime);
        tournaments.put(tournament.getId(), tournament);
        setDirty();
        return tournament;
    }
    
    /**
     * Получает турнир по ID
     */
    public CustomTournament getTournament(String tournamentId) {
        return tournaments.get(tournamentId);
    }
    
    /**
     * Получает все активные турниры
     */
    public List<CustomTournament> getActiveTournaments() {
        List<CustomTournament> active = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (CustomTournament tournament : tournaments.values()) {
            if (!tournament.isFinished() && 
                (tournament.getScheduledStartTime() == 0 || tournament.getScheduledStartTime() <= currentTime)) {
                active.add(tournament);
            }
        }
        return active;
    }
    
    /**
     * Получает все турниры организатора
     */
    public List<CustomTournament> getTournamentsByOrganizer(UUID organizerUUID) {
        List<CustomTournament> organizerTournaments = new ArrayList<>();
        for (CustomTournament tournament : tournaments.values()) {
            if (tournament.getOrganizerUUID().equals(organizerUUID)) {
                organizerTournaments.add(tournament);
            }
        }
        return organizerTournaments;
    }
    
    /**
     * Удаляет турнир
     */
    public void removeTournament(String tournamentId) {
        tournaments.remove(tournamentId);
        setDirty();
    }
}

