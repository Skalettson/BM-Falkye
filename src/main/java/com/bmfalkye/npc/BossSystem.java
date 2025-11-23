package com.bmfalkye.npc;

import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Система боссов - особые NPC с уникальными колодами
 */
public class BossSystem {
    // ID босса -> Босс
    private static final Map<String, Boss> bosses = new HashMap<>();
    // UUID игрока -> Set ID побеждённых боссов
    private static final Map<UUID, Set<String>> defeatedBosses = new HashMap<>();
    
    /**
     * Регистрирует босса
     */
    public static void registerBoss(Boss boss) {
        bosses.put(boss.getId(), boss);
    }
    
    /**
     * Получает босса по ID
     */
    public static Boss getBoss(String id) {
        return bosses.get(id);
    }
    
    /**
     * Получает всех боссов
     */
    public static Collection<Boss> getAllBosses() {
        return bosses.values();
    }
    
    /**
     * Получает доступных боссов для игрока
     */
    public static List<Boss> getAvailableBosses(ServerPlayer player) {
        Set<String> defeated = defeatedBosses.getOrDefault(player.getUUID(), Collections.emptySet());
        List<Boss> available = new ArrayList<>();
        
        for (Boss boss : bosses.values()) {
            // Проверяем требования для разблокировки
            if (boss.isUnlocked(player) && !defeated.contains(boss.getId())) {
                available.add(boss);
            }
        }
        
        return available;
    }
    
    /**
     * Отмечает босса как побеждённого
     */
    public static void markBossDefeated(ServerPlayer player, String bossId) {
        defeatedBosses.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(bossId);
    }
    
    /**
     * Проверяет, побеждён ли босс
     */
    public static boolean isBossDefeated(ServerPlayer player, String bossId) {
        return defeatedBosses.getOrDefault(player.getUUID(), Collections.emptySet()).contains(bossId);
    }
    
    /**
     * Создаёт временного NPC-босса рядом с игроком
     */
    public static net.minecraft.world.entity.npc.Villager createBossNPC(
            net.minecraft.server.level.ServerLevel level, Boss boss, ServerPlayer player) {
        if (level == null || boss == null || player == null) {
            return null;
        }
        
        // Создаём villager рядом с игроком (на 2 блока впереди)
        net.minecraft.core.BlockPos playerPos = player.blockPosition();
        net.minecraft.world.entity.npc.Villager villager = new net.minecraft.world.entity.npc.Villager(
            net.minecraft.world.entity.EntityType.VILLAGER, level);
        
        // Позиционируем villager перед игроком
        double x = playerPos.getX() + (player.getLookAngle().x * 2);
        double y = playerPos.getY();
        double z = playerPos.getZ() + (player.getLookAngle().z * 2);
        villager.setPos(x, y, z);
        
        // Устанавливаем имя босса
        villager.setCustomName(net.minecraft.network.chat.Component.literal(boss.getName()));
        villager.setCustomNameVisible(true);
        
        // Отключаем AI, чтобы босс не двигался
        villager.setNoAi(true);
        
        // Сохраняем ID босса в NBT для идентификации
        net.minecraft.nbt.CompoundTag nbt = villager.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        nbt.putString("BossId", boss.getId());
        nbt.putBoolean("IsBossNPC", true);
        villager.load(nbt);
        
        // Спавним villager в мире
        level.addFreshEntity(villager);
        
        return villager;
    }
    
    /**
     * Босс
     */
    public static class Boss {
        private final String id;
        private final String name;
        private final String description;
        private final CardDeck deck;
        private final LeaderCard leader;
        private final int difficulty; // 1-10
        private final List<String> unlockRequirements; // Требования для разблокировки
        private final Map<String, Integer> rewards; // Награды за победу
        
        public Boss(String id, String name, String description, CardDeck deck, 
                   LeaderCard leader, int difficulty, List<String> unlockRequirements,
                   Map<String, Integer> rewards) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.deck = deck;
            this.leader = leader;
            this.difficulty = difficulty;
            this.unlockRequirements = unlockRequirements != null ? unlockRequirements : new ArrayList<>();
            this.rewards = rewards != null ? rewards : new HashMap<>();
        }
        
        /**
         * Проверяет, разблокирован ли босс для игрока
         */
        public boolean isUnlocked(ServerPlayer player) {
            if (unlockRequirements.isEmpty()) {
                return true; // Нет требований
            }
            
            // Проверяем требования (например, уровень, победы над другими боссами)
            com.bmfalkye.storage.PlayerProgressStorage storage = 
                com.bmfalkye.storage.PlayerProgressStorage.get((net.minecraft.server.level.ServerLevel) player.level());
            com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(player);
            
            for (String requirement : unlockRequirements) {
                if (requirement.startsWith("level:")) {
                    int requiredLevel = Integer.parseInt(requirement.substring(6));
                    if (progress.getLevel() < requiredLevel) {
                        return false;
                    }
                } else if (requirement.startsWith("boss:")) {
                    String requiredBoss = requirement.substring(5);
                    if (!BossSystem.isBossDefeated(player, requiredBoss)) {
                        return false;
                    }
                }
            }
            
            return true;
        }
        
        // Геттеры
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public CardDeck getDeck() { return deck; }
        public LeaderCard getLeader() { return leader; }
        public int getDifficulty() { return difficulty; }
        public Map<String, Integer> getRewards() { return new HashMap<>(rewards); }
    }
}

