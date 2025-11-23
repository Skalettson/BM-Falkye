package com.bmfalkye.events;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.game.GameManager;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.game.TurnTimer;
import com.bmfalkye.util.ModLogger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик тиков для проверки таймеров и других игровых событий
 */
@Mod.EventBusSubscriber(modid = BMFalkye.MOD_ID)
public class GameTickHandler {
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Проверяем таймеры для всех активных игр
        // TODO: Хранить список активных игр в GameManager
        // Пока просто проверяем через рефлексию или статический метод
        
        // Проверяем таймеры каждые 20 тиков (1 секунда) для более точного отсчёта времени
        // Это предотвращает зависания таймера и обеспечивает более плавное обновление
        if (event.getServer().getTickCount() % 20 == 0) {
            ServerLevel level = event.getServer().overworld();
            
            // Очищаем истёкший кэш таймеров каждые 5 секунд (100 тиков)
            if (event.getServer().getTickCount() % 100 == 0) {
                com.bmfalkye.game.TurnTimer.cleanupExpiredCache();
            }
            
            // Очищаем истёкшие блокировки ставок каждые 30 секунд (600 тиков)
            if (event.getServer().getTickCount() % 600 == 0) {
                com.bmfalkye.game.BetProtectionSystem.cleanupExpiredLocks();
            }
            
            // Обрабатываем батчи пакетов каждые 2 тика (примерно каждые 100мс)
            if (event.getServer().getTickCount() % 2 == 0) {
                com.bmfalkye.network.PacketBatcher.processBatches(
                    com.bmfalkye.network.NetworkHandler.INSTANCE, event.getServer());
            }
            
            // Оптимизация памяти каждые 5 минут (6000 тиков)
            if (event.getServer().getTickCount() % 6000 == 0) {
                com.bmfalkye.memory.MemoryOptimizer.periodicMemoryCheck();
            }
            
            checkAllGameTimers(level);
            
            // Проверяем автоматические функции систем (реже, чтобы не перегружать сервер)
            // Проверяем каждые 5 минут игрового времени (6000 тиков)
            if (level.getGameTime() % 6000 == 0) {
                checkAutomaticSystems(level);
            }
            
            // Проверяем зависшие игры каждые 5 минут (6000 тиков)
            if (level.getGameTime() % 6000 == 0) {
                cleanupStaleGames(level);
            }
            
            // Обрабатываем очередь пакетов каждый тик
            com.bmfalkye.network.PacketQueue.processQueue();
            
            // Очищаем кэш каждые 10 минут (12000 тиков)
            if (level.getGameTime() % 12000 == 0) {
                // Очищаем кэши (текстуры и статистика)
                com.bmfalkye.util.CacheCleanupManager.cleanupAllCaches();
                
                // Очищаем данные реконнекта
                com.bmfalkye.game.ReconnectManager.cleanupExpired();
            }
            
            // Очищаем устаревшие пакеты в очереди повторной отправки каждые 5 минут (6000 тиков)
            if (level.getGameTime() % 6000 == 0) {
                com.bmfalkye.network.NetworkErrorHandler.cleanupExpiredPackets();
            }
        }
    }
    
    /**
     * Проверяет и запускает автоматические функции систем
     */
    private static void checkAutomaticSystems(ServerLevel level) {
        try {
            // Проверяем и создаём автоматические турниры
            com.bmfalkye.tournament.TournamentSystem.checkAndCreateAutoTournament(level);
            
            // Проверяем и создаём автоматические события
            com.bmfalkye.events.EventSystem.checkAndCreateAutoEvents(level);
            
            // Проверяем окончание сезона и выдаём награды
            com.bmfalkye.season.SeasonSystem.checkSeasonEnd(level);
        } catch (Exception e) {
            ModLogger.error("Ошибка при проверке автоматических систем: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Храним время последнего хода ИИ для каждой игры, чтобы не вызывать слишком часто
    private static final Map<UUID, Long> lastAITurnTime = new java.util.HashMap<>();
    private static final long AI_TURN_DELAY_MS = 1000; // Задержка 1 секунда перед ходом ИИ
    
    // Храним время последней активности для каждой игры (для очистки зависших сессий)
    private static final Map<UUID, Long> lastActivityTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long STALE_GAME_TIMEOUT_MS = 30 * 60 * 1000; // 30 минут бездействия
    
    private static void checkAllGameTimers(ServerLevel level) {
        // Получаем все активные игры из GameManager
        Map<UUID, FalkyeGameSession> activeGames = com.bmfalkye.game.GameManager.getActiveGames();
        
        if (activeGames.isEmpty()) {
            return; // Нет активных игр
        }
        
        // Создаём копию entrySet для безопасной итерации (избегаем ConcurrentModificationException)
        // Это необходимо, так как во время итерации может произойти удаление игры из activeGames
        // (например, когда игра заканчивается и вызывается GameManager.endGame)
        List<Map.Entry<UUID, FalkyeGameSession>> gamesCopy = new ArrayList<>(activeGames.entrySet());
        
        for (Map.Entry<UUID, FalkyeGameSession> entry : gamesCopy) {
            FalkyeGameSession session = entry.getValue();
            if (session != null && !session.isGameEnded() && !session.isRoundEnded()) {
                // Обновляем время последней активности
                UUID sessionId = entry.getKey();
                lastActivityTime.put(sessionId, System.currentTimeMillis());
                
                // Проверяем состояние жителя и игрока (каждую секунду)
                checkVillagerAndPlayerStatus(session);
                
                TurnTimer.checkAndAutoPass(session);
                
                // Проверяем, нужно ли сделать ход ИИ для villager
                if (session.isPlayingWithVillager() && session.isVillagerTurn()) {
                    UUID playerId = session.getPlayer1() != null ? session.getPlayer1().getUUID() : null;
                    if (playerId != null) {
                        long currentTime = System.currentTimeMillis();
                        Long lastAITurn = lastAITurnTime.get(playerId);
                        
                        // Вызываем ход ИИ, если прошло достаточно времени с последнего хода
                        if (lastAITurn == null || (currentTime - lastAITurn) >= AI_TURN_DELAY_MS) {
                            lastAITurnTime.put(playerId, currentTime);
                            // Оптимизация: убрано избыточное логирование
                            
                            // Выполняем ход ИИ в следующем тике сервера
                            level.getServer().execute(() -> {
                                if (!session.isGameEnded() && !session.isRoundEnded() && session.isVillagerTurn()) {
                                    com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                                    
                                    // Обновляем состояние игры после хода ИИ
                                    if (session.getPlayer1() != null) {
                                        com.bmfalkye.network.NetworkHandler.updateGameState(session.getPlayer1(), session);
                                    }
                                }
                                // Оптимизация: убрано избыточное логирование
                            });
                        }
                    }
                }
                
                // Обновляем состояние игры для игроков (периодически)
                // Не логируем каждое обновление, чтобы не засорять лог
            }
        }
    }
    
    /**
     * Очищает зависшие игровые сессии (игры без активности более 30 минут)
     */
    private static void cleanupStaleGames(ServerLevel level) {
        Map<UUID, FalkyeGameSession> activeGames = com.bmfalkye.game.GameManager.getActiveGames();
        Map<UUID, FalkyeGameSession> villagerGames = com.bmfalkye.game.GameManager.getActiveVillagerGames();
        
        long currentTime = System.currentTimeMillis();
        List<UUID> staleSessions = new ArrayList<>();
        
        // Проверяем все активные игры
        for (Map.Entry<UUID, FalkyeGameSession> entry : activeGames.entrySet()) {
            UUID sessionId = entry.getKey();
            FalkyeGameSession session = entry.getValue();
            
            if (session == null || session.isGameEnded()) {
                staleSessions.add(sessionId);
                continue;
            }
            
            // Проверяем, не зависла ли игра
            Long lastActivity = lastActivityTime.get(sessionId);
            if (lastActivity != null && (currentTime - lastActivity) > STALE_GAME_TIMEOUT_MS) {
                ModLogger.warn("Обнаружена зависшая игровая сессия, очистка", 
                    "sessionId", sessionId.toString(),
                    "inactiveTime", (currentTime - lastActivity) / 1000 + " секунд");
                staleSessions.add(sessionId);
                
                // Уведомляем игроков о завершении игры
                if (session.getPlayer1() != null) {
                    session.getPlayer1().sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§cИгра была автоматически завершена из-за неактивности."));
                }
                if (session.getPlayer2() != null) {
                    session.getPlayer2().sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§cИгра была автоматически завершена из-за неактивности."));
                }
                
                // Завершаем игру
                com.bmfalkye.game.GameManager.endGame(session);
            } else {
                // Проверяем, не отключились ли игроки
                boolean player1Online = session.getPlayer1() != null && 
                    session.getPlayer1().isAlive() && 
                    level.getServer().getPlayerList().getPlayer(session.getPlayer1().getUUID()) != null;
                boolean player2Online = session.getPlayer2() != null && 
                    session.getPlayer2().isAlive() && 
                    level.getServer().getPlayerList().getPlayer(session.getPlayer2().getUUID()) != null;
                
                // Если оба игрока отключились, завершаем игру
                if (!session.isPlayingWithVillager() && !player1Online && !player2Online) {
                    ModLogger.warn("Оба игрока отключились, завершаем игру", 
                        "sessionId", sessionId.toString());
                    staleSessions.add(sessionId);
                    com.bmfalkye.game.GameManager.endGame(session);
                } else if (session.isPlayingWithVillager() && !player1Online) {
                    // Игрок отключился во время игры с жителем
                    ModLogger.warn("Игрок отключился во время игры с жителем, завершаем игру", 
                        "sessionId", sessionId.toString());
                    staleSessions.add(sessionId);
                    com.bmfalkye.game.GameManager.endGame(session);
                }
            }
        }
        
        // Удаляем зависшие сессии из отслеживания
        for (UUID sessionId : staleSessions) {
            lastActivityTime.remove(sessionId);
            lastAITurnTime.remove(sessionId);
        }
    }
    
    /**
     * Обновляет время последней активности для игровой сессии
     */
    public static void updateActivityTime(UUID sessionId) {
        lastActivityTime.put(sessionId, System.currentTimeMillis());
    }
    
    /**
     * Проверяет состояние жителя и игрока во время игры
     */
    private static void checkVillagerAndPlayerStatus(FalkyeGameSession session) {
        if (!session.isPlayingWithVillager()) {
            return; // Только для игр с жителем
        }
        
        ServerPlayer player = session.getPlayer1();
        LivingEntity villager = session.getVillagerOpponent();
        
        if (player == null || villager == null) {
            return;
        }
        
        // Проверяем, жив ли игрок
        if (!player.isAlive() || player.isDeadOrDying()) {
            if (!session.isGameEnded()) {
                ModLogger.logGameLogic("Player died during game - ending game", 
                    "player", player.getName().getString());
                session.forceGameEnd(player, false);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cВы погибли! Противник выиграл игру."));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§cВы погибли! Противник выиграл игру.");
            }
            return;
        }
        
        // Проверяем, жив ли житель
        if (!villager.isAlive() || villager.isDeadOrDying()) {
            if (!session.isGameEnded()) {
                ModLogger.logGameLogic("Villager died during game - player wins", 
                    "player", player.getName().getString(), 
                    "villager", villager.getName().getString());
                session.forceGameEnd(player, true);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aПротивник погиб! Вы выиграли игру!"));
                com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                    "§aПротивник погиб! Вы выиграли игру!");
            }
            return;
        }
        
        // Проверяем, не стал ли житель зомби
        // Когда житель превращается в зомби, он становится новой сущностью, но мы можем проверить,
        // что сохраненная ссылка больше не является Villager
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager)) {
            // Житель больше не является Villager - возможно, превратился в зомби
            if (villager instanceof net.minecraft.world.entity.monster.ZombieVillager || 
                villager instanceof net.minecraft.world.entity.monster.Zombie) {
                if (!session.isGameEnded()) {
                    ModLogger.logGameLogic("Villager converted to zombie during game - player wins", 
                        "player", player.getName().getString(), 
                        "villager", villager.getName().getString());
                    session.forceGameEnd(player, true);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aПротивник превратился в зомби! Вы выиграли игру!"));
                    com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                        "§aПротивник превратился в зомби! Вы выиграли игру!");
                }
                return;
            }
        }
        
        // Дополнительная проверка: ищем зомби на месте жителя (проверяем реже для оптимизации)
        // Проверяем только каждые 2 секунды (в GameTickHandler уже проверяется каждые 2 секунды)
        if (villager instanceof net.minecraft.world.entity.npc.Villager) {
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) villager.level();
            if (level != null && level.getGameTime() % 40 == 0) { // Только каждые 2 секунды
                // Ищем зомби-жителей очень близко к позиции жителя (уменьшен радиус для оптимизации)
                java.util.List<net.minecraft.world.entity.monster.ZombieVillager> nearbyZombieVillagers = 
                    level.getEntitiesOfClass(net.minecraft.world.entity.monster.ZombieVillager.class, 
                        villager.getBoundingBox().inflate(0.3)); // Уменьшен радиус с 0.5 до 0.3
                
                if (!nearbyZombieVillagers.isEmpty()) {
                    // Есть зомби-житель очень близко - возможно, это превращенный житель
                    if (!session.isGameEnded()) {
                        session.forceGameEnd(player, true);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aПротивник превратился в зомби! Вы выиграли игру!"));
                        com.bmfalkye.network.NetworkHandler.addActionLog(player, 
                            "§aПротивник превратился в зомби! Вы выиграли игру!");
                    }
                    return;
                }
            }
        }
    }
}

