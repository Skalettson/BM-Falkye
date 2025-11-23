package com.bmfalkye.cards;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для класса {@link LeaderCard}.
 * 
 * <p>Проверяет функциональность карт лидеров, включая:
 * <ul>
 *   <li>Создание карты лидера</li>
 *   <li>Использование способности лидера</li>
 *   <li>Состояние использования (использована/не использована)</li>
 *   <li>Сброс состояния</li>
 * </ul>
 * 
 * <p><b>Внимание:</b> Полное тестирование требует моков для {@link com.bmfalkye.game.FalkyeGameSession}
 * и {@link net.minecraft.server.level.ServerPlayer}, так как эти классы зависят от Minecraft Forge.
 * 
 * @author BeforeMine Team
 * @since 1.0
 */
class LeaderCardTest {
    
    /**
     * Тест создания карты лидера.
     */
    @Test
    void testLeaderCardCreation() {
        LeaderCard leader = new LeaderCard(
            "test_leader",
            "Тестовый лидер",
            "Дом Пламени",
            "Описание тестового лидера",
            (session, player) -> {
                // Пустая способность для теста
            }
        );
        
        assertEquals("test_leader", leader.getId());
        assertEquals("Тестовый лидер", leader.getName());
        assertEquals("Дом Пламени", leader.getFaction());
        assertEquals("Описание тестового лидера", leader.getDescription());
        assertFalse(leader.isUsed());
    }
    
    /**
     * Тест начального состояния (лидер не использован).
     */
    @Test
    void testInitialState() {
        LeaderCard leader = createTestLeader();
        
        assertFalse(leader.isUsed(), "Лидер должен быть не использован при создании");
    }
    
    /**
     * Тест получения свойств лидера.
     */
    @Test
    void testLeaderProperties() {
        LeaderCard leader = new LeaderCard(
            "fire_architect",
            "Архитектор Пламени",
            "Дом Пламени",
            "Увеличивает силу всех карт огня на 1",
            (session, player) -> {}
        );
        
        assertEquals("fire_architect", leader.getId());
        assertEquals("Архитектор Пламени", leader.getName());
        assertEquals("Дом Пламени", leader.getFaction());
        assertEquals("Увеличивает силу всех карт огня на 1", leader.getDescription());
        
        // getAbility() должен возвращать описание
        assertEquals(leader.getDescription(), leader.getAbility());
    }
    
    /**
     * Тест сброса состояния лидера.
     */
    @Test
    void testReset() {
        LeaderCard leader = createTestLeader();
        
        // Используем лидера (устанавливаем состояние)
        // В реальном тесте здесь был бы вызов leader.use(), но для этого нужны моки
        // Поэтому просто проверяем метод reset()
        leader.reset();
        
        assertFalse(leader.isUsed(), "После сброса лидер должен быть не использован");
    }
    
    /**
     * Тест множественного сброса.
     */
    @Test
    void testMultipleResets() {
        LeaderCard leader = createTestLeader();
        
        // Выполняем сброс несколько раз
        leader.reset();
        assertFalse(leader.isUsed());
        
        leader.reset();
        assertFalse(leader.isUsed());
        
        leader.reset();
        assertFalse(leader.isUsed());
    }
    
    /**
     * Тест различных фракций лидеров.
     */
    @Test
    void testDifferentFactions() {
        LeaderCard fireLeader = new LeaderCard("fire", "Огненный", "Дом Пламени", 
                                              "Описание", (s, p) -> {});
        LeaderCard ruinLeader = new LeaderCard("ruin", "Руинный", "Дозорные Руин", 
                                              "Описание", (s, p) -> {});
        LeaderCard natureLeader = new LeaderCard("nature", "Природный", "Дети Рощения", 
                                                 "Описание", (s, p) -> {});
        
        assertEquals("Дом Пламени", fireLeader.getFaction());
        assertEquals("Дозорные Руин", ruinLeader.getFaction());
        assertEquals("Дети Рощения", natureLeader.getFaction());
    }
    
    /**
     * Тест того, что способность не null.
     */
    @Test
    void testAbilityNotNull() {
        LeaderCard leader = createTestLeader();
        
        // Проверяем, что описание способности возвращается
        assertNotNull(leader.getAbility());
        assertFalse(leader.getAbility().isEmpty());
    }
    
    /**
     * Вспомогательный метод для создания тестовой карты лидера.
     * 
     * @return созданная карта лидера для тестирования
     */
    private LeaderCard createTestLeader() {
        return new LeaderCard(
            "test_leader",
            "Тестовый лидер",
            "Тестовая фракция",
            "Тестовое описание способности",
            (session, player) -> {
                // Пустая способность для теста
                // В реальном тесте здесь была бы проверка выполнения способности
            }
        );
    }
    
    /**
     * Тест создания лидера с null параметрами (должен обрабатываться корректно).
     * 
     * <p>В реальной реализации может потребоваться валидация входных данных.
     */
    @Test
    void testNullHandling() {
        // Этот тест проверяет текущее поведение
        // В будущем можно добавить валидацию
        LeaderCard leader = new LeaderCard(
            "test",
            "Test",
            "Faction",
            "Description",
            null // null способность
        );
        
        // Проверяем, что лидер создан (если конструктор позволяет null)
        assertNotNull(leader);
        assertEquals("test", leader.getId());
    }
}

