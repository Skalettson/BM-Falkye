package com.bmfalkye.tournament;

import com.bmfalkye.storage.PlayerCurrency;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Система управления пользовательскими турнирами
 */
public class CustomTournamentSystem {
    private static final double PLATFORM_COMMISSION = 0.10; // 10% комиссия платформы
    
    /**
     * Создаёт пользовательский турнир
     */
    public static CustomTournament createTournament(ServerPlayer organizer, String name,
                                                    CustomTournament.TournamentRules rules,
                                                    int maxParticipants, int entryFee,
                                                    long scheduledStartTime) {
        if (organizer.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            
            // Валидация параметров
            if (maxParticipants != 8 && maxParticipants != 16 && maxParticipants != 32) {
                organizer.sendSystemMessage(Component.literal("§cКоличество участников должно быть 8, 16 или 32!"));
                return null;
            }
            
            if (entryFee < 0) {
                organizer.sendSystemMessage(Component.literal("§cВзнос не может быть отрицательным!"));
                return null;
            }
            
            // Создаём турнир
            CustomTournament tournament = storage.createTournament(
                organizer, name, rules, maxParticipants, entryFee, scheduledStartTime);
            
            organizer.sendSystemMessage(Component.literal("§aТурнир создан: §f" + name));
            organizer.sendSystemMessage(Component.literal("§7ID турнира: §f" + tournament.getId()));
            
            return tournament;
        }
        
        return null;
    }
    
    /**
     * Регистрирует игрока на пользовательский турнир
     */
    public static boolean registerPlayer(ServerPlayer player, String tournamentId) {
        if (player.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            CustomTournament tournament = storage.getTournament(tournamentId);
            
            if (tournament == null) {
                player.sendSystemMessage(Component.literal("§cТурнир не найден!"));
                return false;
            }
            
            if (tournament.isStarted()) {
                player.sendSystemMessage(Component.literal("§cТурнир уже начался!"));
                return false;
            }
            
            // Проверяем входной взнос
            PlayerCurrency currency = PlayerCurrency.get(serverLevel);
            if (!currency.hasEnoughCoins(player, tournament.getEntryFee())) {
                player.sendSystemMessage(Component.literal("§cНедостаточно монет! Требуется: " + tournament.getEntryFee()));
                return false;
            }
            
            // Регистрируем игрока
            if (tournament.registerPlayer(player)) {
                // Списываем взнос
                currency.removeCoins(player, tournament.getEntryFee());
                
                player.sendSystemMessage(Component.literal("§aВы зарегистрированы на турнир: §f" + tournament.getName()));
                
                // Уведомляем организатора
                ServerPlayer organizer = serverLevel.getServer().getPlayerList().getPlayer(tournament.getOrganizerUUID());
                if (organizer != null) {
                    organizer.sendSystemMessage(Component.literal("§7Игрок §f" + player.getName().getString() + 
                        " §7зарегистрировался на ваш турнир"));
                }
                
                return true;
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось зарегистрироваться. " +
                    "Возможно, турнир переполнен или вы уже зарегистрированы."));
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Отменяет регистрацию игрока
     */
    public static boolean unregisterPlayer(ServerPlayer player, String tournamentId) {
        if (player.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            CustomTournament tournament = storage.getTournament(tournamentId);
            
            if (tournament == null || tournament.isStarted()) {
                return false;
            }
            
            if (tournament.unregisterPlayer(player.getUUID())) {
                // Возвращаем взнос
                PlayerCurrency currency = PlayerCurrency.get(serverLevel);
                currency.addCoins(player, tournament.getEntryFee());
                
                player.sendSystemMessage(Component.literal("§aРегистрация отменена. Взнос возвращён."));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Дисквалифицирует игрока (только организатор)
     */
    public static boolean disqualifyPlayer(ServerPlayer organizer, String tournamentId, UUID playerUUID) {
        if (organizer.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            CustomTournament tournament = storage.getTournament(tournamentId);
            
            if (tournament == null) {
                return false;
            }
            
            if (!tournament.getOrganizerUUID().equals(organizer.getUUID())) {
                organizer.sendSystemMessage(Component.literal("§cТолько организатор может дисквалифицировать игроков!"));
                return false;
            }
            
            if (tournament.disqualifyPlayer(organizer.getUUID(), playerUUID)) {
                // Возвращаем взнос дисквалифицированному игроку
                ServerPlayer disqualifiedPlayer = serverLevel.getServer().getPlayerList().getPlayer(playerUUID);
                if (disqualifiedPlayer != null) {
                    PlayerCurrency currency = PlayerCurrency.get(serverLevel);
                    currency.addCoins(disqualifiedPlayer, tournament.getEntryFee());
                    disqualifiedPlayer.sendSystemMessage(Component.literal("§cВы дисквалифицированы из турнира: §f" + tournament.getName()));
                }
                
                organizer.sendSystemMessage(Component.literal("§aИгрок дисквалифицирован."));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Начинает пользовательский турнир
     */
    public static boolean startTournament(ServerPlayer organizer, String tournamentId) {
        if (organizer.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            CustomTournament tournament = storage.getTournament(tournamentId);
            
            if (tournament == null) {
                organizer.sendSystemMessage(Component.literal("§cТурнир не найден!"));
                return false;
            }
            
            if (!tournament.getOrganizerUUID().equals(organizer.getUUID())) {
                organizer.sendSystemMessage(Component.literal("§cТолько организатор может начать турнир!"));
                return false;
            }
            
            if (tournament.isStarted()) {
                organizer.sendSystemMessage(Component.literal("§cТурнир уже начат!"));
                return false;
            }
            
            if (tournament.getRegisteredPlayers().size() < 2) {
                organizer.sendSystemMessage(Component.literal("§cНедостаточно участников! Минимум 2."));
                return false;
            }
            
            // Вычисляем комиссию платформы
            int commission = (int)(tournament.getPrizePool() * PLATFORM_COMMISSION);
            int finalPrizePool = tournament.getPrizePool() - commission;
            
            // Начинаем турнир
            String systemTournamentId = tournament.start(serverLevel);
            if (systemTournamentId != null) {
                // Сохраняем информацию о пользовательском турнире в TournamentSystem
                TournamentSystem.Tournament systemTournament = TournamentSystem.getTournament(systemTournamentId);
                if (systemTournament != null) {
                    // Устанавливаем призовой фонд с учётом комиссии
                    // Призовой фонд будет распределён при завершении турнира
                    systemTournament.setCustomTournamentPrizePool(finalPrizePool);
                    systemTournament.setCustomTournamentId(tournament.getId());
                }
                
                organizer.sendSystemMessage(Component.literal("§aТурнир начат!"));
                organizer.sendSystemMessage(Component.literal("§7Призовой фонд: §e" + finalPrizePool + 
                    " монет (комиссия платформы: §c" + commission + "§7)"));
                
                // Уведомляем всех участников
                for (UUID playerUUID : tournament.getRegisteredPlayers()) {
                    ServerPlayer participant = serverLevel.getServer().getPlayerList().getPlayer(playerUUID);
                    if (participant != null) {
                        participant.sendSystemMessage(Component.literal("§6§lТурнир начался: §f" + tournament.getName()));
                    }
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Добавляет зрителя к турниру
     */
    public static boolean addSpectator(ServerPlayer player, String tournamentId) {
        if (player.level() instanceof ServerLevel serverLevel) {
            CustomTournamentStorage storage = CustomTournamentStorage.get(serverLevel);
            CustomTournament tournament = storage.getTournament(tournamentId);
            
            if (tournament == null) {
                // Проверяем, может быть это системный турнир
                TournamentSystem.Tournament systemTournament = TournamentSystem.getTournament(tournamentId);
                if (systemTournament != null) {
                    // Открываем экран трансляции для системного турнира
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new com.bmfalkye.network.NetworkHandler.OpenTournamentSpectatorPacket(
                            tournamentId, systemTournament.getName()),
                        player.connection.connection, 
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                    return true;
                }
                return false;
            }
            
            tournament.addSpectator(player.getUUID());
            player.sendSystemMessage(Component.literal("§7Вы присоединились как зритель к турниру: §f" + tournament.getName()));
            
            // Открываем экран трансляции
            com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                new com.bmfalkye.network.NetworkHandler.OpenTournamentSpectatorPacket(
                    tournamentId, tournament.getName()),
                player.connection.connection, 
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Получает все активные пользовательские турниры
     */
    public static List<CustomTournament> getActiveTournaments(ServerLevel level) {
        CustomTournamentStorage storage = CustomTournamentStorage.get(level);
        return storage.getActiveTournaments();
    }
}

