package com.bmfalkye.npc;

import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.LeaderCard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

/**
 * Сюжетный NPC для дуэлей
 */
public class StoryNPC {
    private final String id;
    private final String name;
    private final String description;
    private final CardDeck deck;
    private final LeaderCard leader;
    private final int difficulty; // 1-5 (1 = легко, 5 = очень сложно)
    private final String storyText;
    private final String victoryText;
    private final String defeatText;
    private UUID villagerUUID; // UUID villager, с которым связан этот NPC
    
    public StoryNPC(String id, String name, String description, CardDeck deck, 
                   LeaderCard leader, int difficulty, String storyText, 
                   String victoryText, String defeatText) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.deck = deck;
        this.leader = leader;
        this.difficulty = difficulty;
        this.storyText = storyText;
        this.victoryText = victoryText;
        this.defeatText = defeatText;
    }
    
    public void setVillager(Villager villager) {
        this.villagerUUID = villager.getUUID();
    }
    
    public boolean matchesVillager(Villager villager) {
        return villagerUUID != null && villagerUUID.equals(villager.getUUID());
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putInt("difficulty", difficulty);
        tag.putString("storyText", storyText);
        tag.putString("victoryText", victoryText);
        tag.putString("defeatText", defeatText);
        if (villagerUUID != null) {
            tag.putUUID("villagerUUID", villagerUUID);
        }
        return tag;
    }
    
    public static StoryNPC fromNBT(CompoundTag tag) {
        String id = tag.getString("id");
        String name = tag.getString("name");
        String description = tag.getString("description");
        int difficulty = tag.getInt("difficulty");
        String storyText = tag.getString("storyText");
        String victoryText = tag.getString("victoryText");
        String defeatText = tag.getString("defeatText");
        
        // Загружаем колоду и лидера из реестра
        CardDeck deck = StoryNPCRegistry.getNPCDeck(id);
        LeaderCard leader = StoryNPCRegistry.getNPCLeader(id);
        
        StoryNPC npc = new StoryNPC(id, name, description, deck, leader, difficulty, 
                                   storyText, victoryText, defeatText);
        if (tag.hasUUID("villagerUUID")) {
            npc.villagerUUID = tag.getUUID("villagerUUID");
        }
        return npc;
    }
    
    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CardDeck getDeck() { return deck; }
    public LeaderCard getLeader() { return leader; }
    public int getDifficulty() { return difficulty; }
    public String getStoryText() { return storyText; }
    public String getVictoryText() { return victoryText; }
    public String getDefeatText() { return defeatText; }
    public UUID getVillagerUUID() { return villagerUUID; }
}

