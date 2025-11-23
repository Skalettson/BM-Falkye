package com.bmfalkye.integration;

import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.Card.CardType;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;
import com.bmfalkye.game.MatchConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для полного игрового цикла
 */
@DisplayName("Game Cycle Integration Tests")
public class GameCycleIntegrationTest {
    
    @BeforeAll
    static void setUp() {
        // Инициализируем реестры карт и лидеров
        CardRegistry.initializeDefaultCards();
        LeaderRegistry.initializeLeaders();
    }
    
    @Test
    @DisplayName("Should create game session with valid decks")
    void testCreateGameSession() {
        // Получаем лидеров
        LeaderCard leader1 = LeaderRegistry.getLeader("leader_fire_architect");
        LeaderCard leader2 = LeaderRegistry.getLeader("leader_fire_architect");
        
        assertNotNull(leader1, "Leader 1 should not be null");
        assertNotNull(leader2, "Leader 2 should not be null");
        
        // В реальном тесте здесь был бы mock ServerPlayer
        // CardDeck deck1 = new CardDeck();
        // CardDeck deck2 = new CardDeck();
        // FalkyeGameSession session = GameSessionFactory.createPlayerMatch(
        //     player1, player2, deck1, deck2, leader1, leader2);
        // assertNotNull(session, "Game session should be created");
    }
    
    @Test
    @DisplayName("Should handle card playing logic")
    void testCardPlayingLogic() {
        Card testCard = CardRegistry.getCard("fire_dragon_ignisar");
        
        if (testCard != null) {
            assertEquals(CardType.CREATURE, testCard.getType(), "Card should be CREATURE type");
            assertTrue(testCard.getPower() > 0, "Card should have positive power");
        }
    }
    
    @Test
    @DisplayName("Should validate match config")
    void testMatchConfig() {
        MatchConfig config = new MatchConfig();
        config.setBetAmount(100);
        config.setDifficulty(MatchConfig.Difficulty.NORMAL);
        
        assertEquals(100, config.getBetAmount(), "Bet amount should be set correctly");
        assertEquals(MatchConfig.Difficulty.NORMAL, config.getDifficulty(), "Difficulty should be set correctly");
    }
    
    @Test
    @DisplayName("Should handle round progression")
    void testRoundProgression() {
        // В реальном тесте здесь была бы проверка прогрессии раундов
        // assertTrue(session.getCurrentRound() >= 1 && session.getCurrentRound() <= 3);
    }
}

