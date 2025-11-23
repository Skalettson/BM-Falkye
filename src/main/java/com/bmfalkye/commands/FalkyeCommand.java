package com.bmfalkye.commands;

import com.bmfalkye.game.GameManager;
import com.bmfalkye.game.FalkyeGameSession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public class FalkyeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("falkye")
            .then(Commands.literal("accept")
                .executes(FalkyeCommand::acceptChallenge))
            .then(Commands.literal("deny")
                .executes(FalkyeCommand::denyChallenge))
            .then(Commands.literal("statistics")
                .executes(FalkyeCommand::showDetailedStats))
            .then(Commands.literal("tournament")
                .executes(FalkyeCommand::showTournaments)
                .then(Commands.literal("register")
                    .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .executes(FalkyeCommand::registerTournament)))
                .then(Commands.literal("history")
                    .executes(FalkyeCommand::showTournamentHistory)))
            .then(Commands.literal("season")
                .executes(FalkyeCommand::showSeason)
                .then(Commands.literal("ranking")
                    .executes(FalkyeCommand::showSeasonRanking)))
            .then(Commands.literal("replay")
                .executes(FalkyeCommand::showReplays))
            .then(Commands.literal("daily")
                .executes(FalkyeCommand::showDailyRewards)
                .then(Commands.literal("claim")
                    .executes(FalkyeCommand::claimDailyReward)))
            .then(Commands.literal("events")
                .executes(FalkyeCommand::showEvents)
                .then(Commands.literal("participate")
                    .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .executes(FalkyeCommand::participateInEvent)))
                .then(Commands.literal("quests")
                    .executes(FalkyeCommand::showEventQuests)))
            .then(Commands.literal("game")
                .executes(FalkyeCommand::showGameInfo))
            .then(Commands.literal("challenge")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(FalkyeCommand::challengePlayer))
                .then(Commands.literal("villager")
                    .executes(FalkyeCommand::challengeVillager)))
        );
        
        // Регистрируем административные команды
        FalkyeAdminCommand.register(dispatcher);
    }
    
    private static int showGameInfo(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            FalkyeGameSession session = GameManager.getActiveGame(player);
            if (session == null) {
                context.getSource().sendFailure(Component.literal("§cВы не участвуете в активной игре"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== Информация об игре ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("§eРаунд: §f" + session.getCurrentRound() + " / 3"), false);
            
            int score1 = session.getRoundScore(session.getPlayer1());
            int score2 = session.getRoundScore(session.getPlayer2());
            context.getSource().sendSuccess(() -> Component.literal("§aВаши очки: §f" + score1), false);
            context.getSource().sendSuccess(() -> Component.literal("§cОчки противника: §f" + score2), false);
            
            int roundsWon1 = session.getRoundsWon(session.getPlayer1());
            int roundsWon2 = session.getRoundsWon(session.getPlayer2());
            context.getSource().sendSuccess(() -> Component.literal("§aВаши победы: §f" + roundsWon1), false);
            context.getSource().sendSuccess(() -> Component.literal("§cПобеды противника: §f" + roundsWon2), false);
            
            boolean isMyTurn = session.getCurrentPlayerUUID() != null && 
                session.getCurrentPlayerUUID().equals(player.getUUID());
            context.getSource().sendSuccess(() -> Component.literal(isMyTurn ? "§aВаш ход" : "§cХод противника"), false);
            
            if (session.isPlayingWithVillager()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Противник: Житель"), false);
            } else if (session.getPlayer2() != null) {
                context.getSource().sendSuccess(() -> Component.literal("§7Противник: " + session.getPlayer2().getName().getString()), false);
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int challengePlayer(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer challenger) {
            try {
                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                if (challenger.equals(target)) {
                    context.getSource().sendFailure(Component.literal("§cВы не можете вызвать себя на дуэль!"));
                    return 0;
                }
                GameManager.sendDuelChallenge(challenger, target);
                return 1;
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
                return 0;
            }
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int challengeVillager(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Ищем ближайшего жителя
            Villager nearestVillager = null;
            double nearestDistance = Double.MAX_VALUE;
            
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                for (Villager villager : serverLevel.getEntitiesOfClass(Villager.class, 
                    player.getBoundingBox().inflate(32.0))) {
                    if (villager.isBaby()) continue;
                    
                    double distance = player.distanceToSqr(villager);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestVillager = villager;
                    }
                }
            }
            
            if (nearestVillager == null) {
                context.getSource().sendFailure(Component.literal("§cПоблизости нет жителей! (радиус 32 блока)"));
                return 0;
            }
            
            // Сохраняем в final переменную для использования в лямбде
            final Villager finalVillager = nearestVillager;
            final String villagerName = finalVillager.getName().getString();
            
            // Открываем предматчевое меню
            com.bmfalkye.network.NetworkHandler.openPreMatchScreen(
                player, 
                finalVillager.getUUID(), 
                villagerName, 
                true
            );
            
            context.getSource().sendSuccess(() -> Component.literal("§aВызов отправлен жителю: " + villagerName), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }

    private static int acceptChallenge(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            GameManager.acceptChallenge(player);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }

    private static int denyChallenge(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            GameManager.denyChallenge(player);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showDetailedStats(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            com.bmfalkye.statistics.StatisticsSystem.sendStatistics(player);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showTournaments(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.tournament.TournamentSystem.Tournament> tournaments = 
                com.bmfalkye.tournament.TournamentSystem.getActiveTournaments();
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== АКТИВНЫЕ ТУРНИРЫ ==="), false);
            
            if (tournaments.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Нет активных турниров"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7Используйте /falkye admin tournament create для создания"), false);
            } else {
                for (com.bmfalkye.tournament.TournamentSystem.Tournament tournament : tournaments) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§6§l" + tournament.getName()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Тип: §f" + tournament.getType().getDisplayName()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Участников: §f" + tournament.getParticipants().size() + " / " + tournament.getMaxParticipants()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Взнос: §e" + tournament.getEntryFee() + " монет"), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Призовой фонд: §e" + tournament.getPrizePool() + " монет"), false);
                    
                    if (tournament.isStarted()) {
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Статус: §aИдёт | Раунд: §f" + tournament.getCurrentRound()), false);
                        boolean isParticipant = tournament.getParticipants().contains(player);
                        if (isParticipant) {
                            context.getSource().sendSuccess(() -> Component.literal("§aВы участвуете в этом турнире"), false);
                        }
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Статус: §eОжидание участников"), false);
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Используйте: §f/falkye tournament register " + tournament.getId()), false);
                    }
                }
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int registerTournament(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            try {
                String tournamentId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "id");
                boolean registered = com.bmfalkye.tournament.TournamentSystem.registerPlayer(player, tournamentId);
                if (registered) {
                    context.getSource().sendSuccess(() -> Component.literal("§aВы успешно зарегистрированы на турнир!"), true);
                } else {
                    context.getSource().sendFailure(Component.literal("§cНе удалось зарегистрироваться на турнир"));
                }
                return registered ? 1 : 0;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
                return 0;
            }
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showTournamentHistory(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.tournament.TournamentSystem.TournamentHistory> history = 
                com.bmfalkye.tournament.TournamentSystem.getPlayerTournamentHistory(player.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== ИСТОРИЯ ТУРНИРОВ ==="), false);
            
            if (history.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Вы не участвовали в турнирах"), false);
            } else {
                int count = 0;
                for (com.bmfalkye.tournament.TournamentSystem.TournamentHistory entry : history) {
                    if (count >= 10) break;
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§e" + entry.getTournamentName()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Тип: §f" + entry.getType().getDisplayName() + " | Участников: §f" + entry.getParticipantCount()), false);
                    
                    // Определяем место игрока
                    final String place;
                    if (entry.getFirstPlace() != null && entry.getFirstPlace().equals(player.getUUID())) {
                        place = "§6§l1 МЕСТО";
                    } else if (entry.getSecondPlace() != null && entry.getSecondPlace().equals(player.getUUID())) {
                        place = "§e§l2 МЕСТО";
                    } else if (entry.getThirdPlace() != null && entry.getThirdPlace().equals(player.getUUID())) {
                        place = "§c§l3 МЕСТО";
                    } else {
                        place = "§7Участие";
                    }
                    final String finalPlace = place;
                    context.getSource().sendSuccess(() -> Component.literal("§7Место: " + finalPlace), false);
                    count++;
                }
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showSeason(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            com.bmfalkye.season.SeasonSystem.SeasonInfo info = 
                com.bmfalkye.season.SeasonSystem.getPlayerSeasonInfo(player);
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== СЕЗОН " + info.getSeasonNumber() + " ==="), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eУровень сезона: §f" + info.getSeasonLevel() + " / 30"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eОпыт сезона: §f" + info.getSeasonXP() + " / " + info.getXPForNextLevel()), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Осталось дней: §f" + info.getDaysRemaining()), false);
            
            // Показываем награды текущего уровня
            com.bmfalkye.season.SeasonSystem.Season season = null;
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                season = com.bmfalkye.season.SeasonSystem.getCurrentSeason(serverLevel);
            } else {
                season = com.bmfalkye.season.SeasonSystem.getCurrentSeason();
            }
            if (season != null) {
                com.bmfalkye.season.SeasonSystem.SeasonReward reward = 
                    season.getRewardForLevel(info.getSeasonLevel() + 1);
                if (reward != null) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Следующая награда: §f" + reward.getType() + 
                        (reward.getXpAmount() > 0 ? " (" + reward.getXpAmount() + " XP)" : "")), false);
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Используйте: §f/falkye season ranking §7для просмотра рейтинга"), false);
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showSeasonRanking(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.season.SeasonSystem.SeasonRankingEntry> ranking = 
                com.bmfalkye.season.SeasonSystem.getCurrentSeasonRanking();
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== РЕЙТИНГ СЕЗОНА ==="), false);
            
            if (ranking.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Рейтинг пока пуст"), false);
            } else {
                int topCount = Math.min(10, ranking.size());
                int playerRank = -1;
                
                // Находим место игрока
                for (int i = 0; i < ranking.size(); i++) {
                    if (ranking.get(i).getPlayerUUID().equals(player.getUUID())) {
                        playerRank = i + 1;
                        break;
                    }
                }
                
                context.getSource().sendSuccess(() -> Component.literal("§6--- ТОП " + topCount + " ---"), false);
                
                for (int i = 0; i < topCount; i++) {
                    com.bmfalkye.season.SeasonSystem.SeasonRankingEntry entry = ranking.get(i);
                    net.minecraft.server.level.ServerPlayer rankPlayer = 
                        player.server.getPlayerList().getPlayer(entry.getPlayerUUID());
                    final String playerName = rankPlayer != null ? rankPlayer.getName().getString() : "Неизвестный";
                    
                    final String rankColor = i == 0 ? "§6§l" : i == 1 ? "§e§l" : i == 2 ? "§c§l" : "§7";
                    final int rankIndex = i + 1;
                    final com.bmfalkye.season.SeasonSystem.SeasonRankingEntry finalEntry = entry;
                    context.getSource().sendSuccess(() -> Component.literal(
                        rankColor + rankIndex + ". §f" + playerName + " §7| Уровень: §f" + finalEntry.getLevel() + 
                        " §7| XP: §f" + finalEntry.getTotalXP()), false);
                }
                
                final int finalPlayerRank = playerRank;
                if (playerRank > 0 && playerRank > 10) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Ваше место: §f" + finalPlayerRank), false);
                } else if (playerRank > 0) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§aВы в топ-10! Место: §f" + finalPlayerRank), false);
                }
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showReplays(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayInfo> replays = 
                com.bmfalkye.replay.ReplaySystem.getAllReplays();
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== СОХРАНЁННЫЕ РЕПЛЕИ ==="), false);
            
            if (replays.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Нет сохранённых реплеев"), false);
            } else {
                int count = 0;
                for (com.bmfalkye.replay.ReplaySystem.ReplayInfo replayInfo : replays) {
                    if (count >= 10) break; // Показываем только первые 10
                    com.bmfalkye.replay.ReplaySystem.GameReplay replay = replayInfo.getReplay();
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§e" + replay.getPlayer1Name() + " §7vs §e" + replay.getPlayer2Name()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Победитель: §f" + replay.getWinnerName() + 
                        " §7| Раунды: §f" + replay.getRoundsWon1() + " - " + replay.getRoundsWon2()), false);
                    count++;
                }
                if (replays.size() > 10) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7... и ещё " + (replays.size() - 10) + " реплеев"), false);
                }
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showDailyRewards(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            com.bmfalkye.daily.DailyRewardSystem.DailyRewardInfo info = 
                com.bmfalkye.daily.DailyRewardSystem.getDailyRewardInfo(player);
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== ЕЖЕДНЕВНЫЕ НАГРАДЫ ==="), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eДень: §f" + info.getDay() + " / 7"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eСерия: §f" + info.getStreakDays() + " дней"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                info.isClaimed() ? "§aНаграда получена" : "§cНаграда не получена"), false);
            
            if (!info.isClaimed()) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7Используйте: §f/falkye daily claim §7для получения награды"), false);
            }
            
            // Показываем задания
            java.util.List<com.bmfalkye.daily.DailyRewardSystem.DailyQuest> quests = 
                com.bmfalkye.daily.DailyRewardSystem.getPlayerQuests(player);
            
            if (!quests.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§6--- Ежедневные задания ---"), false);
                for (com.bmfalkye.daily.DailyRewardSystem.DailyQuest quest : quests) {
                    int progress = quest.getProgress(player);
                    String progressStr = progress + " / " + quest.getTarget();
                    String status = quest.isCompleted(player) ? "§a✓ Выполнено" : "§7";
                    int completionPercent = quest.getTarget() > 0 ? (progress * 100 / quest.getTarget()) : 0;
                    context.getSource().sendSuccess(() -> Component.literal(
                        status + " §7" + quest.getDescription() + " §f(" + progressStr + ") §7[" + completionPercent + "%]"), false);
                }
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int claimDailyReward(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            boolean claimed = com.bmfalkye.daily.DailyRewardSystem.claimDailyReward(player);
            if (claimed) {
                context.getSource().sendSuccess(() -> Component.literal("§aЕжедневная награда получена!"), true);
            } else {
                context.getSource().sendFailure(Component.literal("§cВы уже получили награду сегодня!"));
            }
            return claimed ? 1 : 0;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showEvents(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.events.EventSystem.GameEvent> events = 
                com.bmfalkye.events.EventSystem.getActiveEvents();
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== АКТИВНЫЕ СОБЫТИЯ ==="), false);
            
            if (events.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Нет активных событий"), false);
            } else {
                for (com.bmfalkye.events.EventSystem.GameEvent event : events) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§6§l" + event.getName()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Тип: §f" + event.getType().getDisplayName()), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7" + event.getDescription()), false);
                    
                    long timeRemaining = event.getTimeRemaining();
                    long hours = timeRemaining / 3600000;
                    long minutes = (timeRemaining % 3600000) / 60000;
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Осталось: §f" + hours + "ч " + minutes + "м"), false);
                    
                    boolean hasParticipated = event.hasPlayerParticipated(player);
                    context.getSource().sendSuccess(() -> Component.literal(
                        hasParticipated ? "§aВы уже участвовали" : "§7Используйте: §f/falkye events participate " + event.getId()), false);
                    
                    // Показываем количество квастов
                    if (!event.getQuests().isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Квастов: §f" + event.getQuests().size()), false);
                    }
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Используйте: §f/falkye events quests §7для просмотра квастов"), false);
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int participateInEvent(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            try {
                String eventId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "id");
                com.bmfalkye.events.EventSystem.giveEventRewards(player, eventId);
                context.getSource().sendSuccess(() -> Component.literal("§aВы успешно получили награды за событие!"), true);
                return 1;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
                return 0;
            }
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
    
    private static int showEventQuests(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<com.bmfalkye.events.EventSystem.GameEvent> events = 
                com.bmfalkye.events.EventSystem.getActiveEvents();
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l=== КВАСТЫ СОБЫТИЙ ==="), false);
            
            if (events.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Нет активных событий"), false);
                return 1;
            }
            
            boolean hasQuests = false;
            for (com.bmfalkye.events.EventSystem.GameEvent event : events) {
                java.util.List<com.bmfalkye.events.EventSystem.EventQuest> quests = event.getQuests();
                if (!quests.isEmpty()) {
                    hasQuests = true;
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§6--- " + event.getName() + " ---"), false);
                    
                    com.bmfalkye.events.EventSystem.EventProgress progress = 
                        com.bmfalkye.events.EventSystem.getPlayerEventProgress(player, event.getId());
                    
                    for (com.bmfalkye.events.EventSystem.EventQuest quest : quests) {
                        int questProgress = progress.getProgress(quest.getType());
                        boolean isCompleted = progress.isQuestCompleted(quest.getType());
                        String status = isCompleted ? "§a✓ Выполнено" : "§7";
                        int completionPercent = quest.getTarget() > 0 ? (questProgress * 100 / quest.getTarget()) : 0;
                        
                        context.getSource().sendSuccess(() -> Component.literal(
                            status + " §7" + quest.getType().getDisplayName() + " §f(" + questProgress + " / " + quest.getTarget() + 
                            ") §7[" + completionPercent + "%]"), false);
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Награда: §e" + quest.getRewardXP() + " XP, §e" + quest.getRewardCoins() + " монет"), false);
                    }
                }
            }
            
            if (!hasQuests) {
                context.getSource().sendSuccess(() -> Component.literal("§7У активных событий нет квастов"), false);
            }
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bm_falkye.players_only"));
            return 0;
        }
    }
}

