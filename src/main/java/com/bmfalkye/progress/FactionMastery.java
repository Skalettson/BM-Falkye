package com.bmfalkye.progress;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/**
 * Система мастерства фракций
 */
public class FactionMastery {
    private final Map<String, Integer> factionLevels = new HashMap<>();
    private final Map<String, Integer> factionXP = new HashMap<>();
    
    private static final int MAX_LEVEL = 20;
    private static final int BASE_XP_PER_LEVEL = 100;
    
    /**
     * Добавляет опыт к фракции
     */
    public void addFactionXP(String faction, int xp) {
        int currentXP = factionXP.getOrDefault(faction, 0);
        int currentLevel = factionLevels.getOrDefault(faction, 1);
        
        currentXP += xp;
        
        // Проверяем повышение уровня
        while (currentLevel < MAX_LEVEL) {
            int xpNeeded = getXPForLevel(currentLevel);
            if (currentXP >= xpNeeded) {
                currentXP -= xpNeeded;
                currentLevel++;
            } else {
                break;
            }
        }
        
        factionXP.put(faction, currentXP);
        factionLevels.put(faction, currentLevel);
    }
    
    /**
     * Получает уровень фракции
     */
    public int getFactionLevel(String faction) {
        return factionLevels.getOrDefault(faction, 1);
    }
    
    /**
     * Получает опыт фракции
     */
    public int getFactionXP(String faction) {
        return factionXP.getOrDefault(faction, 0);
    }
    
    /**
     * Вычисляет опыт, необходимый для уровня
     */
    private int getXPForLevel(int level) {
        return BASE_XP_PER_LEVEL * level;
    }
    
    /**
     * Получает бонус к силе карт фракции
     */
    public int getFactionPowerBonus(String faction) {
        int level = getFactionLevel(faction);
        // +1 к силе за каждые 5 уровней
        return level / 5;
    }
    
    /**
     * Сохраняет данные в NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag levelsTag = new CompoundTag();
        CompoundTag xpTag = new CompoundTag();
        
        for (Map.Entry<String, Integer> entry : factionLevels.entrySet()) {
            levelsTag.putInt(entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<String, Integer> entry : factionXP.entrySet()) {
            xpTag.putInt(entry.getKey(), entry.getValue());
        }
        
        tag.put("levels", levelsTag);
        tag.put("xp", xpTag);
        return tag;
    }
    
    /**
     * Загружает данные из NBT
     */
    public static FactionMastery load(CompoundTag tag) {
        FactionMastery mastery = new FactionMastery();
        
        if (tag.contains("levels")) {
            CompoundTag levelsTag = tag.getCompound("levels");
            for (String key : levelsTag.getAllKeys()) {
                mastery.factionLevels.put(key, levelsTag.getInt(key));
            }
        }
        
        if (tag.contains("xp")) {
            CompoundTag xpTag = tag.getCompound("xp");
            for (String key : xpTag.getAllKeys()) {
                mastery.factionXP.put(key, xpTag.getInt(key));
            }
        }
        
        return mastery;
    }
}

