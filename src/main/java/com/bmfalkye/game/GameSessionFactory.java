package com.bmfalkye.game;

import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import net.minecraft.server.level.ServerPlayer;

/**
 * Фабрика для создания игровых сессий
 */
public class GameSessionFactory {
    
    /**
     * Создаёт игровую сессию между двумя игроками
     */
    public static FalkyeGameSession createPlayerMatch(
            ServerPlayer player1, 
            ServerPlayer player2,
            CardDeck deck1,
            CardDeck deck2,
            LeaderCard leader1,
            LeaderCard leader2) {
        return new FalkyeGameSession(player1, player2, deck1, deck2, leader1, leader2);
    }
    
    /**
     * Создаёт игровую сессию с NPC/villager
     */
    public static FalkyeGameSession createVillagerMatch(
            ServerPlayer player,
            net.minecraft.world.entity.LivingEntity villager,
            CardDeck playerDeck,
            CardDeck villagerDeck,
            LeaderCard playerLeader,
            LeaderCard villagerLeader) {
        FalkyeGameSession session = new FalkyeGameSession(player, null, playerDeck, villagerDeck, playerLeader, villagerLeader);
        session.setVillagerOpponent(villager);
        return session;
    }
    
    /**
     * Создаёт игровую сессию для сюжетного дуэля
     */
    public static FalkyeGameSession createStoryDuel(
            ServerPlayer player,
            net.minecraft.world.entity.LivingEntity npc,
            CardDeck playerDeck,
            CardDeck npcDeck,
            LeaderCard playerLeader,
            LeaderCard npcLeader,
            com.bmfalkye.npc.StoryNPC storyNPC) {
        FalkyeGameSession session = new FalkyeGameSession(player, null, playerDeck, npcDeck, playerLeader, npcLeader);
        session.setVillagerOpponent(npc);
        session.setStoryNPC(storyNPC);
        return session;
    }
}

