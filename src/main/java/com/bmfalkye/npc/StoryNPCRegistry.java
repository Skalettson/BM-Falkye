package com.bmfalkye.npc;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.cards.Card;
import com.bmfalkye.cards.CardDeck;
import com.bmfalkye.cards.CardRegistry;
import com.bmfalkye.cards.LeaderCard;
import com.bmfalkye.cards.LeaderRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр сюжетных NPC
 */
public class StoryNPCRegistry {
    private static final Map<String, StoryNPC> NPCs = new HashMap<>();
    
    public static void initializeStoryNPCs() {
        BMFalkye.LOGGER.info("Initializing story NPCs...");
        
        // Инигтус - Маг Дома Пламени
        registerNPC(createInigtusNPC());
        
        // Ректор Ларинец - Дозорные Руин
        registerNPC(createLarinetsNPC());
        
        // Маг Детей Рощения
        registerNPC(createNatureMageNPC());
        
        // Дракон-Хранитель
        registerNPC(createDragonGuardianNPC());
        
        BMFalkye.LOGGER.info("Initialized {} story NPCs", NPCs.size());
    }
    
    private static StoryNPC createInigtusNPC() {
        CardDeck deck = new CardDeck();
        
        // Добавляем карты Дома Пламени
        addCardToDeck(deck, "fire_dragon_ignisar", 1);
        addCardToDeck(deck, "pyro_phoenix", 2);
        addCardToDeck(deck, "solar_knight", 2);
        addCardToDeck(deck, "forge_master", 2);
        addCardToDeck(deck, "fire_mage", 3);
        addCardToDeck(deck, "pyro_master", 3);
        addCardToDeck(deck, "fire_drake", 3);
        addCardToDeck(deck, "lava_wyrm", 2);
        addCardToDeck(deck, "flame_guardian", 2);
        addCardToDeck(deck, "flame_storm", 2);
        addCardToDeck(deck, "solar_beam", 1);
        addCardToDeck(deck, "molten_strike", 2);
        addCardToDeck(deck, "pyro_ritual", 1);
        addCardToDeck(deck, "forge_blessing", 1);
        
        deck.shuffle();
        
        LeaderCard leader = LeaderRegistry.getLeader("leader_fire_architect");
        
        return new StoryNPC(
            "inigtus",
            "Инигтус",
            "Великий маг Дома Пламени, архитектор реальности",
            deck,
            leader,
            4, // Высокая сложность
            "Инигтус смотрит на вас с интересом. 'Ты хочешь испытать силу Дома Пламени? Покажи, на что способен!'",
            "Инигтус улыбается. 'Отличная игра! Ты достоин уважения. Возьми эту карту в знак признания.'",
            "Инигтус усмехается. 'Ещё не готов. Вернись, когда станешь сильнее.'"
        );
    }
    
    private static StoryNPC createLarinetsNPC() {
        CardDeck deck = new CardDeck();
        
        // Добавляем карты Дозорных Руин
        addCardToDeck(deck, "ice_dragon_glacis", 1);
        addCardToDeck(deck, "void_walker", 2);
        addCardToDeck(deck, "dream_walker", 2);
        addCardToDeck(deck, "library_guardian", 2);
        addCardToDeck(deck, "watcher_scholar", 3);
        addCardToDeck(deck, "void_researcher", 3);
        addCardToDeck(deck, "frost_drake", 3);
        addCardToDeck(deck, "crystal_serpent", 2);
        addCardToDeck(deck, "time_warden", 2);
        addCardToDeck(deck, "time_freeze", 1);
        addCardToDeck(deck, "void_rift", 2);
        addCardToDeck(deck, "entropy_whisper", 2);
        addCardToDeck(deck, "watcher_insight", 1);
        addCardToDeck(deck, "time_paradox", 1);
        
        deck.shuffle();
        
        LeaderCard leader = LeaderRegistry.getLeader("leader_watcher_scholar");
        
        return new StoryNPC(
            "larinets",
            "Ректор Ларинец",
            "Ректор Академии Вечной Росы, хранитель знаний",
            deck,
            leader,
            5, // Очень высокая сложность
            "Ректор Ларинец внимательно изучает вас. 'Знания - это сила. Покажи, что ты достоин учиться у нас.'",
            "Ректор кивает с одобрением. 'Ты показал мудрость. Вот награда за твоё мастерство.'",
            "Ректор качает головой. 'Тебе ещё многому нужно научиться. Вернись, когда будешь готов.'"
        );
    }
    
    private static StoryNPC createNatureMageNPC() {
        CardDeck deck = new CardDeck();
        
        // Добавляем карты Детей Рощения
        addCardToDeck(deck, "lightning_dragon_fulgur", 1);
        addCardToDeck(deck, "ancient_tree", 2);
        addCardToDeck(deck, "light_elf", 2);
        addCardToDeck(deck, "grove_keeper", 2);
        addCardToDeck(deck, "nature_guardian", 3);
        addCardToDeck(deck, "tree_singer", 3);
        addCardToDeck(deck, "storm_drake", 3);
        addCardToDeck(deck, "thunder_bird", 2);
        addCardToDeck(deck, "grove_warden", 2);
        addCardToDeck(deck, "nature_heal", 1);
        addCardToDeck(deck, "grove_song", 2);
        addCardToDeck(deck, "world_soul_call", 2);
        addCardToDeck(deck, "grove_blessing", 1);
        addCardToDeck(deck, "world_soul_awakening", 1);
        
        deck.shuffle();
        
        LeaderCard leader = LeaderRegistry.getLeader("leader_nature_harmonizer");
        
        return new StoryNPC(
            "nature_mage",
            "Маг Рощи",
            "Дирижёр мировой души, защитник природы",
            deck,
            leader,
            3, // Средняя сложность
            "Маг Рощи улыбается. 'Природа всегда находит баланс. Покажи, что ты понимаешь это.'",
            "Маг Рощи радостно смеётся. 'Ты в гармонии с природой! Вот награда за твоё понимание.'",
            "Маг Рощи грустно улыбается. 'Баланс ещё не достигнут. Продолжай учиться.'"
        );
    }
    
    private static StoryNPC createDragonGuardianNPC() {
        CardDeck deck = new CardDeck();
        
        // Эпическая колода с драконами всех фракций
        addCardToDeck(deck, "fire_dragon_ignisar", 1);
        addCardToDeck(deck, "ice_dragon_glacis", 1);
        addCardToDeck(deck, "lightning_dragon_fulgur", 1);
        addCardToDeck(deck, "pyro_phoenix", 2);
        addCardToDeck(deck, "void_walker", 2);
        addCardToDeck(deck, "ancient_tree", 2);
        addCardToDeck(deck, "solar_knight", 2);
        addCardToDeck(deck, "dream_walker", 2);
        addCardToDeck(deck, "light_elf", 2);
        addCardToDeck(deck, "fire_drake", 2);
        addCardToDeck(deck, "frost_drake", 2);
        addCardToDeck(deck, "storm_drake", 2);
        addCardToDeck(deck, "flame_storm", 1);
        addCardToDeck(deck, "time_freeze", 1);
        addCardToDeck(deck, "nature_heal", 1);
        
        deck.shuffle();
        
        LeaderCard leader = LeaderRegistry.getLeader("leader_fire_architect");
        
        return new StoryNPC(
            "dragon_guardian",
            "Дракон-Хранитель",
            "Древний страж равновесия, защитник всех трёх фракций",
            deck,
            leader,
            5, // Максимальная сложность
            "Дракон-Хранитель смотрит на вас пронзительным взглядом. 'Ты хочешь испытать силу всех трёх фракций? Покажи, что достоин!'",
            "Дракон-Хранитель склоняет голову. 'Ты доказал свою силу. Вот легендарная награда.'",
            "Дракон-Хранитель рычит. 'Ты ещё не готов. Вернись, когда станешь истинным мастером.'"
        );
    }
    
    private static void addCardToDeck(CardDeck deck, String cardId, int count) {
        Card card = CardRegistry.getCard(cardId);
        if (card != null) {
            for (int i = 0; i < count; i++) {
                deck.addCard(card);
            }
        }
    }
    
    public static void registerNPC(StoryNPC npc) {
        NPCs.put(npc.getId(), npc);
    }
    
    public static StoryNPC getNPC(String id) {
        return NPCs.get(id);
    }
    
    public static CardDeck getNPCDeck(String id) {
        StoryNPC npc = NPCs.get(id);
        return npc != null ? npc.getDeck() : null;
    }
    
    public static LeaderCard getNPCLeader(String id) {
        StoryNPC npc = NPCs.get(id);
        return npc != null ? npc.getLeader() : null;
    }
    
    public static StoryNPC findNPCByVillager(net.minecraft.world.entity.npc.Villager villager) {
        for (StoryNPC npc : NPCs.values()) {
            if (npc.matchesVillager(villager)) {
                return npc;
            }
        }
        return null;
    }
}

