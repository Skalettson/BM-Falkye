package com.bmfalkye.cosmetics;

import com.bmfalkye.util.ModLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Хранилище косметики игроков
 */
public class CosmeticsStorage extends SavedData {
    // Косметика игроков: UUID -> данные косметики
    private final Map<UUID, PlayerCosmetics> playerCosmetics = new HashMap<>();
    
    public static CosmeticsStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            CosmeticsStorage::load,
            CosmeticsStorage::new,
            "bm_falkye_cosmetics"
        );
    }
    
    @NotNull
    public static CosmeticsStorage load(@NotNull CompoundTag tag) {
        CosmeticsStorage storage = new CosmeticsStorage();
        
        try {
            CompoundTag cosmeticsTag = tag.getCompound("playerCosmetics");
            for (String key : cosmeticsTag.getAllKeys()) {
                UUID playerUUID = com.bmfalkye.util.DataLoadValidator.safeParseUUID(key);
                if (playerUUID == null) {
                    ModLogger.warn("Skipping invalid UUID during cosmetics load", "key", key);
                    continue;
                }
                
                CompoundTag playerTag = cosmeticsTag.getCompound(key);
                PlayerCosmetics cosmetics = PlayerCosmetics.load(playerTag);
                storage.playerCosmetics.put(playerUUID, cosmetics);
            }
        } catch (Exception e) {
            com.bmfalkye.BMFalkye.LOGGER.error("Error loading CosmeticsStorage, using empty storage", e);
            ModLogger.error("Error loading CosmeticsStorage", "error", e.getMessage());
            return new CosmeticsStorage();
        }
        
        return storage;
    }
    
    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag cosmeticsTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerCosmetics> entry : playerCosmetics.entrySet()) {
            cosmeticsTag.put(entry.getKey().toString(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("playerCosmetics", cosmeticsTag);
        return tag;
    }
    
    /**
     * Получает косметику игрока
     */
    public PlayerCosmetics getPlayerCosmetics(UUID playerUUID) {
        return playerCosmetics.computeIfAbsent(playerUUID, k -> new PlayerCosmetics());
    }
    
    /**
     * Устанавливает косметику игрока
     */
    public void setPlayerCosmetics(UUID playerUUID, PlayerCosmetics cosmetics) {
        playerCosmetics.put(playerUUID, cosmetics);
        setDirty();
    }
    
    /**
     * Косметика игрока
     */
    public static class PlayerCosmetics {
        private String cardBack = "default"; // Рубашка карт
        private String gameField = "default"; // Игровое поле
        private String avatar = "default"; // Аватар
        private String profileFrame = "default"; // Рамка профиля
        private final Set<String> unlockedEmotes = new HashSet<>(); // Разблокированные эмоции
        
        public String getCardBack() { return cardBack; }
        public void setCardBack(String cardBack) {
            this.cardBack = cardBack;
        }
        
        public String getGameField() { return gameField; }
        public void setGameField(String gameField) {
            this.gameField = gameField;
        }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        public String getProfileFrame() { return profileFrame; }
        public void setProfileFrame(String profileFrame) {
            this.profileFrame = profileFrame;
        }
        
        public Set<String> getUnlockedEmotes() {
            return new HashSet<>(unlockedEmotes);
        }
        
        public void unlockEmote(String emoteId) {
            unlockedEmotes.add(emoteId);
        }
        
        public CompoundTag save(CompoundTag tag) {
            tag.putString("cardBack", cardBack);
            tag.putString("gameField", gameField);
            tag.putString("avatar", avatar);
            tag.putString("profileFrame", profileFrame);
            
            net.minecraft.nbt.ListTag emotesTag = new net.minecraft.nbt.ListTag();
            for (String emote : unlockedEmotes) {
                emotesTag.add(net.minecraft.nbt.StringTag.valueOf(emote));
            }
            tag.put("unlockedEmotes", emotesTag);
            
            return tag;
        }
        
        public static PlayerCosmetics load(CompoundTag tag) {
            PlayerCosmetics cosmetics = new PlayerCosmetics();
            cosmetics.cardBack = tag.getString("cardBack");
            cosmetics.gameField = tag.getString("gameField");
            cosmetics.avatar = tag.getString("avatar");
            cosmetics.profileFrame = tag.getString("profileFrame");
            
            net.minecraft.nbt.ListTag emotesTag = tag.getList("unlockedEmotes", 8);
            for (int i = 0; i < emotesTag.size(); i++) {
                cosmetics.unlockedEmotes.add(emotesTag.getString(i));
            }
            
            return cosmetics;
        }
    }
}

