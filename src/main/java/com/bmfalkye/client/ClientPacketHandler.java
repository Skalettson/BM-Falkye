package com.bmfalkye.client;

import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.client.Minecraft;

@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ClientPacketHandler {
    public static void handleOpenGameScreen(FalkyeGameSession session) {
        Minecraft.getInstance().execute(() -> {
            // Используем режим из сессии (если доступен) или по умолчанию 2D
            com.bmfalkye.settings.GameModeSettings.GameMode gameMode = com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
            if (session != null && session.getMatchConfig() != null) {
                gameMode = session.getMatchConfig().getGameMode();
            }
            
            if (gameMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D) {
                Minecraft.getInstance().setScreen(new com.bmfalkye.client.Falkye3DGameScreen(session));
            } else {
                Minecraft.getInstance().setScreen(new FalkyeGameScreen(session));
            }
        });
    }
    
    public static void handleOpenFalkyeGameScreen(com.bmfalkye.game.ClientFalkyeGameSession session) {
        Minecraft.getInstance().execute(() -> {
            // Используем режим из сессии
            com.bmfalkye.settings.GameModeSettings.GameMode gameMode = session != null ? 
                session.getGameMode() : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
            
            if (gameMode == com.bmfalkye.settings.GameModeSettings.GameMode.MODE_3D) {
                Minecraft.getInstance().setScreen(new com.bmfalkye.client.Falkye3DGameScreen(session));
            } else {
                Minecraft.getInstance().setScreen(new FalkyeGameScreen(session));
            }
        });
    }
    
    public static void handleUpdateGameState(com.bmfalkye.game.ClientFalkyeGameSession session) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof FalkyeGameScreen gameScreen) {
                gameScreen.updateGameState(session);
            }
        });
    }
    
    public static void handleUpdateFalkyeGameState(com.bmfalkye.game.ClientFalkyeGameSession session) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof FalkyeGameScreen gameScreen) {
                gameScreen.updateGameState(session);
            }
        });
    }
    
    public static void handleOpenPreMatchScreen(java.util.UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins) {
        handleOpenPreMatchScreen(opponentUUID, opponentName, isNPC, villagerCoins, false);
    }
    
    public static void handleOpenPreMatchScreen(java.util.UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins, boolean isChallenged) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new com.bmfalkye.client.PreMatchScreen(opponentUUID, opponentName, isNPC, villagerCoins, isChallenged));
        });
    }
    
    private static java.util.List<String> cachedCardCollection = new java.util.ArrayList<>();
    private static net.minecraft.client.gui.screens.Screen mainMenuParent = null;
    
    public static void setMainMenuParent(net.minecraft.client.gui.screens.Screen parent) {
        mainMenuParent = parent;
    }
    
    public static net.minecraft.client.gui.screens.Screen getMainMenuParent() {
        return mainMenuParent;
    }
    
    public static void handleCardCollection(java.util.List<String> cardIds) {
        Minecraft.getInstance().execute(() -> {
            // Сохраняем коллекцию в кэш
            cachedCardCollection = new java.util.ArrayList<>(cardIds);
            
            // Обновляем экран коллекции, если он открыт
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.CardCollectionScreen collectionScreen) {
                collectionScreen.updateCollection(cardIds);
            }
            
            // Обновляем редактор колод, если он открыт
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.DeckEditorScreen deckEditorScreen) {
                deckEditorScreen.updateCollection(cardIds);
            }
        });
    }
    
    public static java.util.List<String> getCachedCardCollection() {
        return new java.util.ArrayList<>(cachedCardCollection);
    }
    
    /**
     * Очищает кэш коллекции карт
     */
    public static void clearCardCollectionCache() {
        cachedCardCollection.clear();
    }
    
    public static void handleSendDecksPacket(java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.DeckEditorScreen editorScreen) {
                editorScreen.updateDecksList(decks);
            }
            // Также обновляем PreMatchScreen, если он открыт
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.PreMatchScreen preMatchScreen) {
                preMatchScreen.updateDecksList(decks);
            }
        });
    }
    
    public static void handleSendDeckDataPacket(com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.DeckEditorScreen editorScreen) {
                editorScreen.loadDeckData(deckData);
            }
        });
    }
    
    public static void handleFriendsList(java.util.List<java.util.UUID> friendIds, java.util.List<String> friendNames) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.FriendsScreen friendsScreen) {
                friendsScreen.updateFriendsList(friendIds, friendNames);
            }
        });
    }
    
    public static void handleGuildInfo(com.bmfalkye.network.GuildPackets.SendGuildInfoPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.GuildScreen guildScreen) {
                guildScreen.updateGuildInfo(packet);
            }
        });
    }
    
    public static void handleBossesList(com.bmfalkye.network.BossPackets.SendBossesPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.BossScreen bossScreen) {
                bossScreen.updateBossesList(packet);
            }
        });
    }
    
    public static void handleShopItems(com.bmfalkye.network.ShopPackets.SendShopItemsPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.CardShopScreen shopScreen) {
                shopScreen.updateShopItems(packet);
            }
        });
    }
    
    // Кэш данных эволюции карт
    private static java.util.Map<String, com.bmfalkye.network.NetworkHandler.EvolutionData> cachedEvolutionData = new java.util.HashMap<>();
    private static int cachedSoulDust = 0;
    
    public static void handleCardEvolution(java.util.Map<String, com.bmfalkye.network.NetworkHandler.EvolutionData> evolutionData, int soulDust) {
        Minecraft.getInstance().execute(() -> {
            // Сохраняем данные в кэш
            cachedEvolutionData = new java.util.HashMap<>(evolutionData);
            cachedSoulDust = soulDust;
            
            // Обновляем экран эволюции, если он открыт
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.CardEvolutionScreen evolutionScreen) {
                evolutionScreen.updateEvolutionData(evolutionData, soulDust);
            }
        });
    }
    
    public static java.util.Map<String, com.bmfalkye.network.NetworkHandler.EvolutionData> getCachedEvolutionData() {
        return new java.util.HashMap<>(cachedEvolutionData);
    }
    
    public static int getCachedSoulDust() {
        return cachedSoulDust;
    }
    
    public static void handleQuests(java.util.List<String> activeQuests, java.util.List<String> completedQuests) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.QuestScreen questScreen) {
                questScreen.updateQuests(activeQuests, completedQuests);
            }
        });
    }
    
    public static void handleCustomTournaments(java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> tournaments) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.CustomTournamentScreen screen) {
                screen.updateTournaments(tournaments);
            }
        });
    }
    
    public static void handleTournamentMatches(java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> matches) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.TournamentSpectatorScreen screen) {
                screen.updateMatches(matches);
            }
        });
    }
    
    public static void handleLeaderboard(java.util.List<com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry> hallOfFame,
                                        java.util.List<com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry> weeklyLeaderboard) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof com.bmfalkye.client.HallOfFameScreen screen) {
                screen.updateHallOfFame(hallOfFame);
                screen.updateWeeklyLeaderboard(weeklyLeaderboard);
            }
        });
    }
    
    public static void handleEmote(java.util.UUID senderUUID, String emoteId) {
        Minecraft.getInstance().execute(() -> {
            // Отображаем эмоцию в игре
            com.bmfalkye.cosmetics.EmoteSystem.Emote emote = 
                com.bmfalkye.cosmetics.EmoteSystem.getEmote(emoteId);
            if (emote != null) {
                // TODO: Отобразить анимированную эмоцию на экране
            }
        });
    }
}

