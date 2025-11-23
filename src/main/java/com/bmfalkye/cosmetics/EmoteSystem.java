package com.bmfalkye.cosmetics;

import com.bmfalkye.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Система эмоций и жестов
 */
public class EmoteSystem {
    // Реестр эмоций
    private static final Map<String, Emote> emotes = new HashMap<>();
    
    static {
        // Регистрируем стандартные эмоции
        registerEmote("thumbs_up", new Emote(
            "thumbs_up",
            "👍",
            "Большой палец вверх"
        ));
        
        registerEmote("thumbs_down", new Emote(
            "thumbs_down",
            "👎",
            "Большой палец вниз"
        ));
        
        registerEmote("clap", new Emote(
            "clap",
            "👏",
            "Аплодисменты"
        ));
        
        registerEmote("thinking", new Emote(
            "thinking",
            "🤔",
            "Думаю"
        ));
        
        registerEmote("fire", new Emote(
            "fire",
            "🔥",
            "Огонь"
        ));
    }
    
    /**
     * Регистрирует эмоцию
     */
    public static void registerEmote(String id, Emote emote) {
        emotes.put(id, emote);
    }
    
    /**
     * Получает эмоцию
     */
    public static Emote getEmote(String id) {
        return emotes.get(id);
    }
    
    /**
     * Отправляет эмоцию оппоненту
     */
    public static void sendEmote(ServerPlayer sender, ServerPlayer receiver, String emoteId) {
        Emote emote = getEmote(emoteId);
        if (emote != null) {
            // Отправляем пакет с эмоцией
            NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> receiver),
                new NetworkHandler.SendEmotePacket(sender.getUUID(), emoteId)
            );
            
            receiver.sendSystemMessage(Component.literal(
                "§e" + sender.getName().getString() + " показывает: " + emote.emoji + " " + emote.name));
        }
    }
    
    /**
     * Эмоция
     */
    public static class Emote {
        public final String id;
        public final String emoji;
        public final String name;
        
        public Emote(String id, String emoji, String name) {
            this.id = id;
            this.emoji = emoji;
            this.name = name;
        }
    }
}

