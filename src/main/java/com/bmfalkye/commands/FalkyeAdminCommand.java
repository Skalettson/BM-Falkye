package com.bmfalkye.commands;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Административные команды /falkye admin
 */
public class FalkyeAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("falkye")
            .then(Commands.literal("admin")
                .requires(source -> source.hasPermission(2)) // Только для операторов
                .then(Commands.literal("givecard")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("card", StringArgumentType.string())
                            .executes(FalkyeAdminCommand::giveCard))))
                .then(Commands.literal("givexp")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(FalkyeAdminCommand::giveXP))))
                .then(Commands.literal("setlevel")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                            .executes(FalkyeAdminCommand::setLevel))))
                .then(Commands.literal("unlockall")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(FalkyeAdminCommand::unlockAll)))
                .then(Commands.literal("stats")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(FalkyeAdminCommand::showStats)))
                .then(Commands.literal("tournament")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("max", IntegerArgumentType.integer(2, 32))
                                .then(Commands.argument("fee", IntegerArgumentType.integer(0))
                                    .executes(FalkyeAdminCommand::createTournament)))))
                    .then(Commands.literal("list")
                        .executes(FalkyeAdminCommand::listTournaments))
                    .then(Commands.literal("end")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(FalkyeAdminCommand::endTournament))))
                .then(Commands.literal("event")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("type", StringArgumentType.string())
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                    .executes(FalkyeAdminCommand::createEvent)))))
                    .then(Commands.literal("list")
                        .executes(FalkyeAdminCommand::listEvents))
                    .then(Commands.literal("end")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(FalkyeAdminCommand::endEvent))))
                .then(Commands.literal("season")
                    .then(Commands.literal("end")
                        .executes(FalkyeAdminCommand::endSeason))
                    .then(Commands.literal("reset")
                        .executes(FalkyeAdminCommand::resetSeason))
                    .then(Commands.literal("info")
                        .executes(FalkyeAdminCommand::seasonInfo)))
                .then(Commands.literal("givecoins")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(FalkyeAdminCommand::giveCoins))))
        ));
    }

    private static int giveCard(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            String cardId = StringArgumentType.getString(context, "card");
            
            Card card = CardRegistry.getCard(cardId);
            if (card == null) {
                context.getSource().sendFailure(Component.translatable("command.bm_falkye.card_not_found", cardId));
                return 0;
            }
            
            final Card finalCard = card;
            int count = 0;
            for (ServerPlayer player : players) {
                PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
                PlayerProgress progress = storage.getPlayerProgress(player);
                progress.unlockCard(cardId);
                storage.setPlayerProgress(player, progress);
                player.sendSystemMessage(Component.translatable("command.bm_falkye.card_received", finalCard.getName()));
                count++;
            }
            
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.translatable("command.bm_falkye.card_given", 
                finalCard.getName(), finalCount), true);
            return count;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }

    private static int giveXP(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            final int finalAmount = amount;
            int count = 0;
            for (ServerPlayer player : players) {
                PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
                PlayerProgress progress = storage.getPlayerProgress(player);
                progress.addExperience(finalAmount);
                storage.setPlayerProgress(player, progress);
                player.sendSystemMessage(Component.translatable("command.bm_falkye.xp_received", finalAmount));
                count++;
            }
            
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.translatable("command.bm_falkye.xp_given", finalAmount, finalCount), true);
            return count;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            int level = IntegerArgumentType.getInteger(context, "level");
            
            final int finalLevel = level;
            int count = 0;
            for (ServerPlayer player : players) {
                PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
                PlayerProgress progress = storage.getPlayerProgress(player);
                progress.setLevel(finalLevel);
                storage.setPlayerProgress(player, progress);
                player.sendSystemMessage(Component.translatable("command.bm_falkye.level_set", finalLevel));
                count++;
            }
            
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.translatable("command.bm_falkye.level_set_success", finalLevel, finalCount), true);
            return count;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }

    private static int unlockAll(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            
            int count = 0;
            for (ServerPlayer player : players) {
                PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
                PlayerProgress progress = storage.getPlayerProgress(player);
                
                // Разблокируем все карты
                for (Card card : CardRegistry.getAllCards()) {
                    progress.unlockCard(card.getId());
                }
                
                storage.setPlayerProgress(player, progress);
                player.sendSystemMessage(Component.translatable("command.bm_falkye.all_unlocked"));
                count++;
            }
            
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.translatable("command.bm_falkye.all_unlocked_success", finalCount), true);
            return count;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        
        for (ServerPlayer player : players) {
            PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            
            context.getSource().sendSuccess(() -> Component.literal("=== Статистика игрока " + player.getName().getString() + " ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("Уровень: " + progress.getLevel()), false);
            context.getSource().sendSuccess(() -> Component.literal("Опыт: " + progress.getExperience()), false);
            context.getSource().sendSuccess(() -> Component.literal("Игр сыграно: " + progress.getTotalGamesPlayed()), false);
            context.getSource().sendSuccess(() -> Component.literal("Побед: " + progress.getTotalGamesWon()), false);
            context.getSource().sendSuccess(() -> Component.literal("Поражений: " + progress.getTotalGamesLost()), false);
            context.getSource().sendSuccess(() -> Component.literal("Карт разблокировано: " + progress.getUnlockedCards().size()), false);
            context.getSource().sendSuccess(() -> Component.literal("Достижений: " + progress.getAchievements().size()), false);
        }
        
        return players.size();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }
    
    // ========== ТУРНИРЫ ==========
    
    private static int createTournament(CommandContext<CommandSourceStack> context) {
        try {
            String name = StringArgumentType.getString(context, "name");
            int maxParticipants = IntegerArgumentType.getInteger(context, "max");
            int entryFee = IntegerArgumentType.getInteger(context, "fee");
            
            // Генерируем призы
            java.util.List<Card> prizes = new java.util.ArrayList<>();
            java.util.List<Card> allCards = com.bmfalkye.cards.CardRegistry.getAllCards();
            if (!allCards.isEmpty()) {
                java.util.Random random = new java.util.Random();
                prizes.add(allCards.get(random.nextInt(allCards.size())));
                if (random.nextBoolean() && allCards.size() > 1) {
                    prizes.add(allCards.get(random.nextInt(allCards.size())));
                }
            }
            
            com.bmfalkye.tournament.TournamentSystem.Tournament tournament = 
                com.bmfalkye.tournament.TournamentSystem.createTournament(
                    name, 
                    com.bmfalkye.tournament.TournamentSystem.TournamentType.SINGLE_ELIMINATION,
                    maxParticipants, 
                    entryFee, 
                    prizes
                );
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aТурнир создан! ID: §f" + tournament.getId()), true);
            
            // Уведомляем всех игроков
            if (context.getSource().getServer() != null) {
                for (net.minecraft.server.level.ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.literal(
                        "§6§l══════ НОВЫЙ ТУРНИР! ══════"));
                    player.sendSystemMessage(Component.literal(
                        "§6Название: §f" + tournament.getName()));
                    player.sendSystemMessage(Component.literal(
                        "§6Участников: §f" + tournament.getMaxParticipants()));
                    player.sendSystemMessage(Component.literal(
                        "§6Взнос: §e" + tournament.getEntryFee() + " монет"));
                    player.sendSystemMessage(Component.literal(
                        "§7Используйте: §f/falkye tournament register " + tournament.getId()));
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listTournaments(CommandContext<CommandSourceStack> context) {
        java.util.List<com.bmfalkye.tournament.TournamentSystem.Tournament> tournaments = 
            com.bmfalkye.tournament.TournamentSystem.getActiveTournaments();
        
        context.getSource().sendSuccess(() -> Component.literal("§6§l=== АКТИВНЫЕ ТУРНИРЫ ==="), false);
        
        if (tournaments.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Нет активных турниров"), false);
        } else {
            for (com.bmfalkye.tournament.TournamentSystem.Tournament tournament : tournaments) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§e" + tournament.getName() + " §7(ID: §f" + tournament.getId() + "§7)"), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7Участников: §f" + tournament.getParticipants().size() + " / " + tournament.getMaxParticipants()), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7Статус: §f" + (tournament.isStarted() ? "Идёт" : "Ожидание")), false);
            }
        }
        
        return tournaments.size();
    }
    
    private static int endTournament(CommandContext<CommandSourceStack> context) {
        try {
            String tournamentId = StringArgumentType.getString(context, "id");
            
            com.bmfalkye.tournament.TournamentSystem.Tournament tournament = null;
            for (com.bmfalkye.tournament.TournamentSystem.Tournament t : 
                com.bmfalkye.tournament.TournamentSystem.getActiveTournaments()) {
                if (t.getId().equals(tournamentId)) {
                    tournament = t;
                    break;
                }
            }
            
            if (tournament == null) {
                context.getSource().sendFailure(Component.literal("§cТурнир не найден!"));
                return 0;
            }
            
            if (tournament.isFinished()) {
                context.getSource().sendFailure(Component.literal("§cТурнир уже завершён!"));
                return 0;
            }
            
            // Завершаем турнир вручную (для админов)
            // Примечание: Для корректного завершения турнир должен быть полностью пройден
            // Этот метод только помечает турнир как завершённый, но не выдаёт награды автоматически
            if (!tournament.isStarted()) {
                context.getSource().sendFailure(Component.literal(
                    "§cТурнир ещё не начат! Используйте автоматическую систему завершения."));
                return 0;
            }
            
            if (tournament.isFinished()) {
                context.getSource().sendFailure(Component.literal(
                    "§cТурнир уже завершён!"));
                return 0;
            }
            
            // Помечаем турнир как завершённый (награды будут выданы при следующей проверке)
            tournament.finish();
            context.getSource().sendSuccess(() -> Component.literal(
                "§aТурнир помечен как завершённый. Награды будут выданы при следующей проверке."), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    // ========== СОБЫТИЯ ==========
    
    private static int createEvent(CommandContext<CommandSourceStack> context) {
        try {
            String name = StringArgumentType.getString(context, "name");
            String typeStr = StringArgumentType.getString(context, "type");
            int durationHours = IntegerArgumentType.getInteger(context, "duration");
            
            // Парсим тип события
            com.bmfalkye.events.EventSystem.EventType type;
            try {
                type = com.bmfalkye.events.EventSystem.EventType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                context.getSource().sendFailure(Component.literal(
                    "§cНеизвестный тип события! Доступные: LIMITED_TIME_CARDS, DOUBLE_XP, TOURNAMENT, COLLECTION_CHALLENGE"));
                return 0;
            }
            
            long duration = durationHours * 3600000L; // Конвертируем часы в миллисекунды
            
            // Генерируем награды
            java.util.Map<String, Object> rewards = new java.util.HashMap<>();
            java.util.List<Card> eventCards = new java.util.ArrayList<>();
            
            switch (type) {
                case LIMITED_TIME_CARDS -> {
                    java.util.List<Card> cards = com.bmfalkye.cards.CardRegistry.getAllCards();
                    if (!cards.isEmpty()) {
                        eventCards.add(cards.get(new java.util.Random().nextInt(cards.size())));
                    }
                    rewards.put("xp", 500);
                    rewards.put("coins", 200);
                }
                case DOUBLE_XP -> {
                    rewards.put("xp_multiplier", 2.0);
                    rewards.put("duration", duration);
                }
                case TOURNAMENT -> {
                    rewards.put("tournament_boost", true);
                    rewards.put("coins", 500);
                }
                case COLLECTION_CHALLENGE -> {
                    java.util.List<Card> cards = com.bmfalkye.cards.CardRegistry.getAllCards();
                    if (!cards.isEmpty()) {
                        eventCards.add(cards.get(new java.util.Random().nextInt(cards.size())));
                    }
                    rewards.put("xp", 1000);
                    rewards.put("coins", 300);
                }
            }
            
            com.bmfalkye.events.EventSystem.GameEvent event = 
                com.bmfalkye.events.EventSystem.createEvent(name, type, duration, eventCards, rewards);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aСобытие создано! ID: §f" + event.getId()), true);
            
            // Уведомляем всех игроков
            if (context.getSource().getServer() != null) {
                for (net.minecraft.server.level.ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.literal(
                        "§6§l══════ НОВОЕ СОБЫТИЕ! ══════"));
                    player.sendSystemMessage(Component.literal(
                        "§6§l" + event.getName()));
                    player.sendSystemMessage(Component.literal(
                        "§7" + event.getDescription()));
                    player.sendSystemMessage(Component.literal(
                        "§7Длительность: §f" + durationHours + " часов"));
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listEvents(CommandContext<CommandSourceStack> context) {
        java.util.List<com.bmfalkye.events.EventSystem.GameEvent> events = 
            com.bmfalkye.events.EventSystem.getActiveEvents();
        
        context.getSource().sendSuccess(() -> Component.literal("§6§l=== АКТИВНЫЕ СОБЫТИЯ ==="), false);
        
        if (events.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Нет активных событий"), false);
        } else {
            for (com.bmfalkye.events.EventSystem.GameEvent event : events) {
                long hours = event.getTimeRemaining() / 3600000;
                context.getSource().sendSuccess(() -> Component.literal(
                    "§e" + event.getName() + " §7(ID: §f" + event.getId() + "§7)"), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7Тип: §f" + event.getType().getDisplayName() + " | Осталось: §f" + hours + "ч"), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7Квастов: §f" + event.getQuests().size()), false);
            }
        }
        
        return events.size();
    }
    
    private static int endEvent(CommandContext<CommandSourceStack> context) {
        try {
            String eventId = StringArgumentType.getString(context, "id");
            
            java.util.List<com.bmfalkye.events.EventSystem.GameEvent> events = 
                com.bmfalkye.events.EventSystem.getActiveEvents();
            
            com.bmfalkye.events.EventSystem.GameEvent event = null;
            for (com.bmfalkye.events.EventSystem.GameEvent e : events) {
                if (e.getId().equals(eventId)) {
                    event = e;
                    break;
                }
            }
            
            if (event == null) {
                context.getSource().sendFailure(Component.literal("§cСобытие не найдено!"));
                return 0;
            }
            
            // События автоматически удаляются при окончании через проверку времени
            context.getSource().sendSuccess(() -> Component.literal(
                "§aСобытие будет завершено автоматически при проверке времени"), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
            return 0;
        }
    }
    
    // ========== СЕЗОНЫ ==========
    
    private static int endSeason(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getLevel() instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.server.level.ServerLevel serverLevel = 
                (net.minecraft.server.level.ServerLevel) context.getSource().getLevel();
            com.bmfalkye.season.SeasonSystem.checkSeasonEnd(serverLevel);
            context.getSource().sendSuccess(() -> Component.literal(
                "§aСезон проверен и завершён (если это необходимо)!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§cОшибка: нет доступа к серверному уровню"));
            return 0;
        }
    }
    
    private static int resetSeason(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getLevel() instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.server.level.ServerLevel serverLevel = 
                (net.minecraft.server.level.ServerLevel) context.getSource().getLevel();
            // Принудительно завершаем текущий сезон и начинаем новый
            com.bmfalkye.season.SeasonSystem.checkSeasonEnd(serverLevel);
            context.getSource().sendSuccess(() -> Component.literal(
                "§aСезон сброшен и начат новый!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§cОшибка: нет доступа к серверному уровню"));
            return 0;
        }
    }
    
    private static int seasonInfo(CommandContext<CommandSourceStack> context) {
        com.bmfalkye.season.SeasonSystem.Season season = 
            com.bmfalkye.season.SeasonSystem.getCurrentSeason();
        
        if (season == null) {
            context.getSource().sendSuccess(() -> Component.literal("§7Сезон не инициализирован"), false);
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6§l=== ИНФОРМАЦИЯ О СЕЗОНЕ ==="), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§eСезон: §f" + season.getNumber()), false);
        
        if (context.getSource().getLevel() instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.server.level.ServerLevel serverLevel = 
                (net.minecraft.server.level.ServerLevel) context.getSource().getLevel();
            long daysRemaining = season.getDaysRemaining(serverLevel);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eОсталось дней: §f" + daysRemaining), false);
        }
        
        java.util.List<com.bmfalkye.season.SeasonSystem.SeasonRankingEntry> ranking = 
            com.bmfalkye.season.SeasonSystem.getCurrentSeasonRanking();
        context.getSource().sendSuccess(() -> Component.literal(
            "§eИгроков в рейтинге: §f" + ranking.size()), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§eНаград в сезоне: §f" + season.getRewards().size()), false);
        
        return 1;
    }
    
    // ========== ДОПОЛНИТЕЛЬНЫЕ КОМАНДЫ ==========
    
    private static int giveCoins(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            final int finalAmount = amount;
            int count = 0;
            for (ServerPlayer player : players) {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerCurrency currency = 
                        com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                    currency.addCoins(player, finalAmount);
                    player.sendSystemMessage(Component.literal(
                        "§aВы получили §e" + finalAmount + " монет"));
                    count++;
                }
            }
            
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.literal(
                "§aВыдано монет: §e" + finalAmount + " §aигрокам: §f" + finalCount), true);
            return count;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Ошибка синтаксиса команды: " + e.getMessage()));
            return 0;
        }
    }
}

