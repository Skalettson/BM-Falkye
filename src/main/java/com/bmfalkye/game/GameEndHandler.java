package com.bmfalkye.game;

import com.bmfalkye.achievements.AchievementSystem;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardRarity;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerCardCollection;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Обработчик окончания игры
 */
public class GameEndHandler {
    
    public static void handleGameEnd(FalkyeGameSession session) {
        if (session.isGameEnded()) {
            ServerPlayer winner = session.getWinner();
            ServerPlayer player1 = session.getPlayer1();
            ServerPlayer player2 = session.getPlayer2();
            
            // Получаем конфигурацию матча для обработки ставок
            com.bmfalkye.game.MatchConfig config = session.getMatchConfig();
            int betAmount = config != null ? config.getBetAmount() : 0;
            
            // Обрабатываем только игры между игроками (не с villager)
            if (!session.isPlayingWithVillager() && player1 != null && player2 != null) {
                ServerPlayer loser = winner != null && winner.equals(player1) ? player2 : player1;
                
                if (winner != null && loser != null) {
                    // Обрабатываем потерю и получение карт
                    handleCardTransfer(winner, loser);
                    
                    // Обновляем статистику победителя
                    PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) winner.level());
                    PlayerProgress winnerProgress = storage.getPlayerProgress(winner);
                    winnerProgress.recordGameWin(winner);
                    AchievementSystem.checkAchievements(winner, winnerProgress);
                    storage.setPlayerProgress(winner, winnerProgress);
                    
                    // Обновляем рейтинг
                    int winnerRoundsWon = session.getRoundsWon(winner);
                    int loserRoundsWon = session.getRoundsWon(loser);
                    com.bmfalkye.rating.RatingSystem.updateRating(winner, loser, winnerRoundsWon, loserRoundsWon);
                    
                    // Обновляем еженедельный рейтинг
                    if (winner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.leaderboard.WeeklyLeaderboardSystem.updateWeeklyRating(winner);
                        com.bmfalkye.leaderboard.WeeklyLeaderboardSystem.updateWeeklyRating(loser);
                        
                        // Применяем бонусы за контроль территорий
                        com.bmfalkye.territory.TerritoryBonusSystem.applyTerritoryBonuses(winner, 50, serverLevel);
                    }
                    
                    // Обновляем статистику
                    com.bmfalkye.statistics.StatisticsSystem.updateGameStatistics(session, winner, loser);
                    
                    // Записываем аналитику
                    com.bmfalkye.analytics.AnalyticsSystem.recordGameAnalytics(session, winner, loser);
                    
                    // Обновляем систему балансировки карт
                    List<String> winnerUsedCards = session.getUsedCards(winner);
                    List<String> loserUsedCards = session.getUsedCards(loser);
                    com.bmfalkye.balance.CardBalanceSystem.recordGameResult(winnerUsedCards, true);
                    com.bmfalkye.balance.CardBalanceSystem.recordGameResult(loserUsedCards, false);
                    
                    // Сохраняем статистику балансировки
                    if (winner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.storage.CardBalanceStorage balanceStorage = com.bmfalkye.storage.CardBalanceStorage.get(serverLevel);
                        balanceStorage.markDirty();
                    }
                    
                    // Начисляем опыт картам за победу/поражение
                    com.bmfalkye.evolution.CardEvolutionSystem.awardExperienceForWin(winner, session);
                    com.bmfalkye.evolution.CardEvolutionSystem.awardExperienceForLoss(loser, session);
                    
                    // Проверяем прогресс квестов
                    com.bmfalkye.quests.QuestSystem.checkPlayerQuests(winner);
                    
                    // Обрабатываем результат матча арены драфта
                    if (session.isDraftArena()) {
                        com.bmfalkye.draft.DraftArenaManager.handleArenaMatchResult(session, winner, loser);
                    }
                    
                    // Обновляем ежедневные задания
                    com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(winner, 
                        com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_GAMES, 1);
                    com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(winner, 
                        com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_ROUNDS, winnerRoundsWon);
                    
                    winner.sendSystemMessage(Component.translatable("message.bm_falkye.game_won"));
                    
                    // Награда зависит от сложности и ставки
                    int baseXP = 50;
                    if (config != null) {
                        float difficultyMultiplier = config.getDifficulty().getAIMultiplier();
                        baseXP = (int)(baseXP * difficultyMultiplier);
                    }
                    winner.sendSystemMessage(Component.translatable("message.bm_falkye.xp_gained", baseXP));
                    
                    // Обрабатываем ставку через BettingSystem (с защитой от эксплойтов)
                    if (betAmount > 0 && winner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.game.BettingSystem.payWinner(winner, loser, betAmount, serverLevel);
                    } else {
                        // Если ставки не было, всё равно разблокируем (на случай ошибки)
                        com.bmfalkye.game.BetProtectionSystem.unlockBet(winner);
                        com.bmfalkye.game.BetProtectionSystem.unlockBet(loser);
                    }
                }
                
                if (loser != null) {
                    // Обновляем статистику проигравшего
                    PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) loser.level());
                    PlayerProgress loserProgress = storage.getPlayerProgress(loser);
                    loserProgress.recordGameLoss(loser);
                    AchievementSystem.checkAchievements(loser, loserProgress);
                    storage.setPlayerProgress(loser, loserProgress);
                    
                    // Отправляем персональные подсказки после проигрыша (раз в 5 игр)
                    if (loserProgress.getTotalGamesLost() % 5 == 0) {
                        com.bmfalkye.tips.PersonalTipsSystem.sendPersonalTips(loser);
                    }
                    
                    loser.sendSystemMessage(Component.translatable("message.bm_falkye.game_lost"));
                    loser.sendSystemMessage(Component.translatable("message.bm_falkye.xp_gained", 20));
                    
                    // Ставка уже обработана в payWinner
                }
            } else if (session.isPlayingWithVillager() && player1 != null) {
                // Игра с villager/NPC - обновляем только статистику игрока
                PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player1.level());
                PlayerProgress progress = storage.getPlayerProgress(player1);
                
                // Получаем villager для звуковых реакций
                net.minecraft.world.entity.LivingEntity villager = session.getVillagerOpponent();
                
                if (winner != null && winner.equals(player1)) {
                    // Игрок победил - villager проиграл
                    if (villager != null) {
                        com.bmfalkye.game.VillagerAIPlayer.playVillagerReactionOnGameEnd(villager, false);
                    }
                    
                    progress.recordGameWin(player1);
                    
                    // Обновляем систему балансировки карт (только для игрока, villager не учитывается)
                    List<String> playerUsedCards = session.getUsedCards(player1);
                    com.bmfalkye.balance.CardBalanceSystem.recordGameResult(playerUsedCards, true);
                    
                    // Сохраняем статистику балансировки
                    if (player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.storage.CardBalanceStorage balanceStorage = com.bmfalkye.storage.CardBalanceStorage.get(serverLevel);
                        balanceStorage.markDirty();
                    }
                    
                    // Начисляем опыт картам за победу
                    com.bmfalkye.evolution.CardEvolutionSystem.awardExperienceForWin(player1, session);
                    
                    // Проверяем прогресс квестов
                    com.bmfalkye.quests.QuestSystem.checkPlayerQuests(player1);
                    
                    // Обрабатываем результат матча арены драфта (для игрока с AI)
                    if (session.isDraftArena() && winner != null) {
                        ServerPlayer loser = winner.equals(player1) ? null : player1; // AI не имеет loser
                        com.bmfalkye.draft.DraftArenaManager.handleArenaMatchResult(session, winner, loser);
                    }
                    
                    // Обновляем ежедневные задания
                    com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player1, 
                        com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_GAMES, 1);
                    int roundsWon = session.getRoundsWon(player1);
                    com.bmfalkye.daily.DailyRewardSystem.incrementQuestProgress(player1, 
                        com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_ROUNDS, roundsWon);
                    
                    // Уведомляем игрока о победе через лог действий
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§6§lПОБЕДА! Вы выиграли игру!");
                    
                    player1.sendSystemMessage(Component.translatable("message.bm_falkye.game_won"));
                    
                    // Награда зависит от сложности
                    int baseXP = 50;
                    if (config != null) {
                        float difficultyMultiplier = config.getDifficulty().getAIMultiplier();
                        baseXP = (int)(baseXP * difficultyMultiplier);
                    }
                    
                    // Обновляем прогресс сезона
                    com.bmfalkye.season.SeasonSystem.updateSeasonProgress(player1, baseXP);
                    
                    // Уведомляем о получении опыта через лог действий
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§aПолучено опыта: " + baseXP);
                    player1.sendSystemMessage(Component.translatable("message.bm_falkye.xp_gained", baseXP));
                    
                    // Обрабатываем ставку (против NPC)
                    if (betAmount > 0 && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                        currency.addCoins(player1, betAmount * 2);
                        com.bmfalkye.storage.VillagerCurrency villagerCurrency = com.bmfalkye.storage.VillagerCurrency.get(serverLevel);
                        if (villager != null) {
                            villagerCurrency.removeCoins(villager, betAmount);
                        }
                        com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§aВы выиграли " + (betAmount * 2) + " монет!");
                        player1.sendSystemMessage(Component.literal("§aВы выиграли " + (betAmount * 2) + " монет!"));
                    }
                } else {
                    // Игрок проиграл - villager победил
                    if (villager != null) {
                        com.bmfalkye.game.VillagerAIPlayer.playVillagerReactionOnGameEnd(villager, true);
                    }
                    
                    // Обновляем систему балансировки карт (только для игрока, villager не учитывается)
                    List<String> playerUsedCards = session.getUsedCards(player1);
                    com.bmfalkye.balance.CardBalanceSystem.recordGameResult(playerUsedCards, false);
                    
                    // Сохраняем статистику балансировки
                    if (player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.storage.CardBalanceStorage balanceStorage = com.bmfalkye.storage.CardBalanceStorage.get(serverLevel);
                        balanceStorage.markDirty();
                    }
                    
                    // Начисляем опыт картам за поражение
                    com.bmfalkye.evolution.CardEvolutionSystem.awardExperienceForLoss(player1, session);
                    
                    // Уведомляем игрока о поражении через лог действий
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§c§lПОРАЖЕНИЕ! Вы проиграли игру.");
                    
                    // Игрок проиграл - забираем 2 карты
                    List<String> lostCardIds = handleCardLoss(player1);
                    
                    // Уведомляем о потере карт через лог действий
                    if (!lostCardIds.isEmpty()) {
                        com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§cУ вас забрано карт: " + lostCardIds.size());
                        for (String cardId : lostCardIds) {
                            Card card = CardRegistry.getCard(cardId);
                            if (card != null) {
                                com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§cПотеряна карта: " + card.getName());
                            }
                        }
                    }
                    
                    progress.recordGameLoss(player1);
                    player1.sendSystemMessage(Component.translatable("message.bm_falkye.game_lost"));
                    
                    // Уведомляем о получении опыта через лог действий
                    com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§7Получено опыта: 20");
                    player1.sendSystemMessage(Component.translatable("message.bm_falkye.xp_gained", 20));
                    player1.sendSystemMessage(Component.translatable("message.bm_falkye.cards_lost", lostCardIds.size()));
                    
                    // Обрабатываем ставку
                    if (betAmount > 0 && player1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.storage.PlayerCurrency currency = com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                        currency.removeCoins(player1, betAmount);
                        com.bmfalkye.storage.VillagerCurrency villagerCurrency = com.bmfalkye.storage.VillagerCurrency.get(serverLevel);
                        if (villager != null) {
                            villagerCurrency.addCoins(villager, betAmount * 2);
                        }
                        com.bmfalkye.network.NetworkHandler.addActionLog(player1, "§cВы проиграли " + betAmount + " монет.");
                        player1.sendSystemMessage(Component.literal("§cВы проиграли " + betAmount + " монет."));
                    }
                }
                
                AchievementSystem.checkAchievements(player1, progress);
                storage.setPlayerProgress(player1, progress);
            }
            
            // Проверяем, является ли это сюжетной дуэлью
            if (session.getStoryNPC() != null) {
                boolean playerWon = winner != null && winner.equals(player1);
                com.bmfalkye.game.StoryDuelManager.handleStoryDuelEnd(session, playerWon);
            }
            
            // Сохраняем реплей игры
            String replayId = com.bmfalkye.replay.ReplaySystem.saveReplay(session);
            if (replayId != null && player1 != null) {
                player1.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7Реплей игры сохранён. ID: §f" + replayId.substring(0, 8)));
            }
            if (replayId != null && player2 != null) {
                player2.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7Реплей игры сохранён. ID: §f" + replayId.substring(0, 8)));
            }
            
            // Проверяем активные события для игроков
            if (player1 != null) {
                com.bmfalkye.events.EventSystem.checkActiveEvents(player1);
            }
            if (player2 != null) {
                com.bmfalkye.events.EventSystem.checkActiveEvents(player2);
            }
            
            // Вызываем событие окончания игры через API
            // TODO: Реализовать через GameEventSystem при необходимости
            
            // Вызываем события победы/поражения для игроков
            // TODO: Реализовать через GameEventSystem при необходимости
            
            // Завершаем игру
            GameManager.endGame(session);
        }
    }
    
    /**
     * Обрабатывает передачу карт от проигравшего к победителю
     */
    private static void handleCardTransfer(ServerPlayer winner, ServerPlayer loser) {
        if (!(winner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
        
        // Забираем 2 случайные карты у проигравшего
        List<String> lostCardIds = collection.removeRandomCards(loser, 2);
        
        if (lostCardIds.isEmpty()) {
            loser.sendSystemMessage(Component.translatable("message.bm_falkye.no_cards_to_lose"));
            return;
        }
        
        // Передаем карты победителю
        collection.addCards(winner, lostCardIds);
        
        // Проигрываем анимацию и звук для каждой полученной карты
        for (String cardId : lostCardIds) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                playCardObtainAnimation(winner.level(), winner, card.getRarity());
                playCardObtainSound(winner.level(), winner, card.getRarity());
            }
        }
        
        // Уведомления проигравшему
        loser.sendSystemMessage(Component.translatable("message.bm_falkye.cards_lost", lostCardIds.size()));
        com.bmfalkye.network.NetworkHandler.addActionLog(loser, "§cУ вас забрано карт: " + lostCardIds.size());
        for (String cardId : lostCardIds) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                loser.sendSystemMessage(Component.translatable("message.bm_falkye.card_lost_detail", card.getName()));
                com.bmfalkye.network.NetworkHandler.addActionLog(loser, "§cПотеряна карта: " + card.getName());
            }
        }
        
        // Уведомления победителю
        winner.sendSystemMessage(Component.translatable("message.bm_falkye.cards_won", lostCardIds.size()));
        com.bmfalkye.network.NetworkHandler.addActionLog(winner, "§aВы получили карт: " + lostCardIds.size());
        for (String cardId : lostCardIds) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                winner.sendSystemMessage(Component.translatable("message.bm_falkye.card_won_detail", card.getName()));
                com.bmfalkye.network.NetworkHandler.addActionLog(winner, "§aПолучена карта: " + card.getName());
            }
        }
        
        // Отправляем обновлённые коллекции на клиенты
        sendUpdatedCollection(winner, serverLevel, collection);
        sendUpdatedCollection(loser, serverLevel, collection);
    }
    
    /**
     * Отправляет обновлённую коллекцию на клиент
     */
    private static void sendUpdatedCollection(ServerPlayer player, net.minecraft.server.level.ServerLevel serverLevel, PlayerCardCollection collection) {
        java.util.List<Card> cards = collection.getCards(player);
        java.util.List<String> cardIds = cards.stream()
            .map(Card::getId)
            .collect(java.util.stream.Collectors.toList());
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
            new com.bmfalkye.network.NetworkHandler.SendCardCollectionPacket(cardIds),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }
    
    /**
     * Обрабатывает потерю карт при проигрыше против NPC/villager
     * @return Список ID потерянных карт
     */
    private static List<String> handleCardLoss(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return new java.util.ArrayList<>();
        }
        
        PlayerCardCollection collection = PlayerCardCollection.get(serverLevel);
        
        // Забираем 2 случайные карты
        List<String> lostCardIds = collection.removeRandomCards(player, 2);
        
        if (lostCardIds.isEmpty()) {
            return lostCardIds; // Нет карт для потери
        }
        
        // Уведомление игроку
        player.sendSystemMessage(Component.translatable("message.bm_falkye.cards_lost", lostCardIds.size()));
        for (String cardId : lostCardIds) {
            Card card = CardRegistry.getCard(cardId);
            if (card != null) {
                player.sendSystemMessage(Component.translatable("message.bm_falkye.card_lost_detail", card.getName()));
            }
        }
        
        // Отправляем обновлённую коллекцию на клиент
        java.util.List<Card> cards = collection.getCards(player);
        java.util.List<String> cardIds = cards.stream()
            .map(Card::getId)
            .collect(java.util.stream.Collectors.toList());
        com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
            new com.bmfalkye.network.NetworkHandler.SendCardCollectionPacket(cardIds),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
        
        return lostCardIds;
    }
    
    /**
     * Проигрывает анимацию получения карты (как тотем бессмертия)
     */
    private static void playCardObtainAnimation(Level level, ServerPlayer player, CardRarity rarity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        // Партиклы тотема бессмертия
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2.0;
            double radius = 1.0 + (rarity.ordinal() * 0.2);
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + player.getEyeHeight();
            double z = player.getZ() + Math.sin(angle) * radius;
            
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                x, y, z,
                1,
                0.0, 0.1, 0.0,
                0.05
            );
        }
        
        // Дополнительные партиклы в зависимости от редкости
        int extraParticles = rarity.ordinal() * 10;
        for (int i = 0; i < extraParticles; i++) {
            double x = player.getX() + (Math.random() - 0.5) * 2.0;
            double y = player.getY() + player.getEyeHeight() + (Math.random() - 0.5) * 1.0;
            double z = player.getZ() + (Math.random() - 0.5) * 2.0;
            
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                x, y, z,
                1,
                0.0, 0.1, 0.0,
                0.05
            );
        }
    }
    
    /**
     * Проигрывает звук получения карты
     */
    private static void playCardObtainSound(Level level, ServerPlayer player, CardRarity rarity) {
        net.minecraft.sounds.SoundEvent sound = getSoundForRarity(rarity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
            sound, player.getSoundSource(), 1.0f, 1.0f);
    }
    
    /**
     * Получить звук для редкости карты
     */
    private static net.minecraft.sounds.SoundEvent getSoundForRarity(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> net.minecraft.sounds.SoundEvents.ITEM_PICKUP;
            case RARE -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME;
            case EPIC -> net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE;
            case LEGENDARY -> net.minecraft.sounds.SoundEvents.TOTEM_USE;
        };
    }
}

