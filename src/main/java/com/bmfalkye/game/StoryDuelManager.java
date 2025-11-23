package com.bmfalkye.game;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.npc.StoryNPC;
import com.bmfalkye.npc.StoryNPCRegistry;
import com.bmfalkye.player.PlayerProgress;
import com.bmfalkye.storage.PlayerProgressStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

/**
 * Менеджер сюжетных дуэлей с NPC
 */
public class StoryDuelManager {
    
    /**
     * Начинает сюжетную дуэль с NPC (открывает предматчевое меню)
     */
    public static void startStoryDuel(ServerPlayer player, Villager villager) {
        // Проверяем, что это не детский житель
        if (villager.isBaby()) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_is_baby"));
            return;
        }
        
        StoryNPC npc = StoryNPCRegistry.findNPCByVillager(villager);
        
        if (npc == null) {
            // Обычный villager - открываем предматчевое меню
            com.bmfalkye.network.NetworkHandler.openPreMatchScreen(
                player, 
                villager.getUUID(), 
                villager.getName().getString(), 
                true
            );
            return;
        }
        
        // Проверяем, не занят ли игрок
        if (GameManager.getActiveGame(player) != null) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.already_in_game"));
            return;
        }
        
        // Показываем сюжетный текст
        player.sendSystemMessage(Component.literal("§6" + npc.getName() + ": §r" + npc.getStoryText()));
        
        // Открываем предматчевое меню
        com.bmfalkye.network.NetworkHandler.openPreMatchScreen(
            player, 
            villager.getUUID(), 
            npc.getName(), 
            true
        );
    }
    
    /**
     * Начинает сюжетную дуэль с конфигурацией (вызывается после предматчевого меню)
     */
    public static void startStoryDuelWithConfig(ServerPlayer player, Villager villager, com.bmfalkye.game.MatchConfig config) {
        // Проверяем, что это не детский житель
        if (villager.isBaby()) {
            player.sendSystemMessage(Component.translatable("message.bm_falkye.villager_is_baby"));
            return;
        }
        
        StoryNPC npc = StoryNPCRegistry.findNPCByVillager(villager);
        
        if (npc == null) {
            // Обычный villager - используем стандартную логику
            GameManager.startVillagerMatch(player, villager, config);
            return;
        }
        
        // Загружаем колоду игрока
        com.bmfalkye.storage.PlayerDeckStorage storage = 
            com.bmfalkye.storage.PlayerDeckStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        com.bmfalkye.storage.PlayerDeckManager deckManager = 
            com.bmfalkye.storage.PlayerDeckManager.get((net.minecraft.server.level.ServerLevel) player.level());
        
        com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData playerDeckData = null;
        
        // Если выбрана колода, загружаем её из менеджера колод
        if (config.getSelectedDeckName() != null && !config.getSelectedDeckName().isEmpty()) {
            java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> playerDecks = deckManager.getPlayerDecks(player);
            for (com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck : playerDecks) {
                if (deck.getDeckName().equals(config.getSelectedDeckName())) {
                    playerDeckData = deck;
                    break;
                }
            }
        }
        
        // Если колода не найдена или не выбрана, используем сохранённую колоду
        if (playerDeckData == null) {
            playerDeckData = storage.getPlayerDeck(player);
        }
        
        CardDeck playerDeck = playerDeckData.createDeck(player, (net.minecraft.server.level.ServerLevel) player.level());
        LeaderCard playerLeader = config.getSelectedLeader() != null ? 
            config.getSelectedLeader() : playerDeckData.getLeader();
        if (playerLeader == null) {
            playerLeader = com.bmfalkye.cards.LeaderRegistry.getLeader("leader_fire_architect");
        }
        
        // Создаём игровую сессию
        FalkyeGameSession session = com.bmfalkye.game.GameSessionFactory.createStoryDuel(
            player, villager, playerDeck, npc.getDeck(), 
            playerLeader, npc.getLeader(), npc);
        session.setVillagerOpponent((net.minecraft.world.entity.LivingEntity) villager);
        session.setStoryNPC(npc); // Сохраняем ссылку на NPC
        session.setMatchConfig(config); // Сохраняем конфигурацию
        
        // Сохраняем активную игру
        GameManager.getActiveGames().put(player.getUUID(), session);
        GameManager.getActiveVillagerGames().put(player.getUUID(), session);
        
        // Открываем игровой экран
        com.bmfalkye.network.NetworkHandler.openGameScreen(player, session);
        
        BMFalkye.LOGGER.info("Story duel started: {} vs {} with difficulty: {}", 
            player.getName().getString(), npc.getName(), config.getDifficulty().getDisplayName());
    }
    
    /**
     * Обрабатывает окончание сюжетной дуэли
     */
    public static void handleStoryDuelEnd(FalkyeGameSession session, boolean playerWon) {
        StoryNPC npc = session.getStoryNPC();
        if (npc == null) {
            return;
        }
        
        ServerPlayer player = session.getPlayer1();
        if (player == null) {
            return;
        }
        
        if (playerWon) {
            // Победа игрока
            player.sendSystemMessage(Component.literal("§6" + npc.getName() + ": §r" + npc.getVictoryText()));
            
            // Выдаём награды
            giveStoryDuelRewards(player, npc);
            
            // Разблокируем достижение
            PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
            PlayerProgress progress = storage.getPlayerProgress(player);
            progress.unlockAchievement("story_duel_" + npc.getId());
            storage.setPlayerProgress(player, progress);
            
            // Проверяем прогресс квестов (победа в дуэли может быть шагом квеста)
            com.bmfalkye.quests.QuestSystem.checkPlayerQuests(player);
            
            // Проверяем, есть ли квесты, требующие победу над этим NPC
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.bmfalkye.quests.QuestStorage questStorage = 
                    com.bmfalkye.quests.QuestStorage.get(serverLevel);
                java.util.List<String> activeQuests = questStorage.getActiveQuests(player);
                
                for (String questId : activeQuests) {
                    com.bmfalkye.quests.Quest quest = com.bmfalkye.quests.QuestSystem.getQuest(questId);
                    if (quest != null) {
                        java.util.List<com.bmfalkye.quests.Quest.QuestStep> steps = quest.getSteps();
                        com.bmfalkye.quests.QuestStorage.QuestProgress questProgress = 
                            questStorage.getQuestProgress(player, questId);
                        
                        if (questProgress != null) {
                            // Проверяем каждый шаг после текущего
                            for (int i = questProgress.getCurrentStep() + 1; i < steps.size(); i++) {
                                com.bmfalkye.quests.Quest.QuestStep step = steps.get(i);
                                if (step.getType() == com.bmfalkye.quests.Quest.QuestStep.QuestStepType.WIN_DUEL) {
                                    String targetNPC = (String) step.getTarget();
                                    if (targetNPC != null && targetNPC.equals(npc.getId())) {
                                        // Шаг выполнен - победа над нужным NPC
                                        com.bmfalkye.quests.QuestSystem.completeQuestStep(player, questId, step.getId());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Поражение игрока
            player.sendSystemMessage(Component.literal("§6" + npc.getName() + ": §r" + npc.getDefeatText()));
        }
    }
    
    private static void giveStoryDuelRewards(ServerPlayer player, StoryNPC npc) {
        PlayerProgressStorage storage = PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
        PlayerProgress progress = storage.getPlayerProgress(player);
        
        // Опыт в зависимости от сложности
        int xpReward = 100 + (npc.getDifficulty() * 50);
        progress.addExperience(xpReward);
        
        // Случайная карта из колоды NPC
        CardDeck npcDeck = npc.getDeck();
        if (npcDeck != null && !npcDeck.isEmpty()) {
            Card randomCard = npcDeck.getRandomCard();
            if (randomCard != null) {
                progress.unlockCard(randomCard.getId());
                player.sendSystemMessage(Component.translatable("message.bm_falkye.story_reward_card", 
                    randomCard.getName()));
            }
        }
        
        storage.setPlayerProgress(player, progress);
        player.sendSystemMessage(Component.translatable("message.bm_falkye.story_reward_xp", xpReward));
    }
}

