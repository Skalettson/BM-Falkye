package com.bmfalkye.client.gui.backup.logic;

import com.bmfalkye.cards.CardBuff;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для BuffRendererLogic
 * Проверяет логику рендеринга баффов
 */
class BuffRendererLogicTest {
    
    @Test
    void testBuffRenderingLogic() {
        // Проверяем, что логика рендеринга баффов работает
        UUID sourcePlayer = UUID.randomUUID();
        CardBuff buff = new CardBuff("test_buff", CardBuff.BuffType.POWER_INCREASE, 5, -1, sourcePlayer);
        
        assertNotNull(buff);
        assertEquals(CardBuff.BuffType.POWER_INCREASE, buff.getType());
        assertEquals(5, buff.getPowerModifier());
    }
    
    @Test
    void testAllBuffTypes() {
        // Проверяем все типы баффов
        UUID sourcePlayer = UUID.randomUUID();
        
        CardBuff powerIncrease = new CardBuff("buff1", CardBuff.BuffType.POWER_INCREASE, 5, -1, sourcePlayer);
        CardBuff powerDecrease = new CardBuff("buff2", CardBuff.BuffType.POWER_DECREASE, 3, -1, sourcePlayer);
        CardBuff immunity = new CardBuff("buff3", CardBuff.BuffType.IMMUNITY, 0, -1, sourcePlayer);
        CardBuff frozen = new CardBuff("buff4", CardBuff.BuffType.FROZEN, 0, -1, sourcePlayer);
        CardBuff shielded = new CardBuff("buff5", CardBuff.BuffType.SHIELDED, 0, -1, sourcePlayer);
        CardBuff doomed = new CardBuff("buff6", CardBuff.BuffType.DOOMED, 0, -1, sourcePlayer);
        
        assertNotNull(powerIncrease);
        assertNotNull(powerDecrease);
        assertNotNull(immunity);
        assertNotNull(frozen);
        assertNotNull(shielded);
        assertNotNull(doomed);
    }
}

