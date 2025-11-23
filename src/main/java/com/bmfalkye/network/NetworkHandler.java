package com.bmfalkye.network;

import com.bmfalkye.BMFalkye;
import com.bmfalkye.game.FalkyeGameSession;
import com.bmfalkye.util.ModLogger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BMFalkye.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    // Дебаунсинг для updateGameState - предотвращает слишком частую отправку пакетов
    private static final java.util.Map<UUID, Long> lastUpdateTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long UPDATE_DEBOUNCE_MS = 33; // Минимум 33мс между обновлениями (примерно 30 FPS для обновлений)
    
    public static void register() {
        ModLogger.info("Registering network packets");
        INSTANCE.registerMessage(packetId++, OpenGameScreenPacket.class,
            OpenGameScreenPacket::encode,
            OpenGameScreenPacket::decode,
            OpenGameScreenPacket::handle);
        
        INSTANCE.registerMessage(packetId++, PlayCardPacket.class,
            PlayCardPacket::encode,
            PlayCardPacket::decode,
            PlayCardPacket::handle);
        
        INSTANCE.registerMessage(packetId++, PassPacket.class,
            PassPacket::encode,
            PassPacket::decode,
            PassPacket::handle);
        
        INSTANCE.registerMessage(packetId++, UseLeaderPacket.class,
            UseLeaderPacket::encode,
            UseLeaderPacket::decode,
            UseLeaderPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SurrenderPacket.class,
            SurrenderPacket::encode,
            SurrenderPacket::decode,
            SurrenderPacket::handle);
        
        INSTANCE.registerMessage(packetId++, UpdateGameStatePacket.class,
            UpdateGameStatePacket::encode,
            UpdateGameStatePacket::decode,
            UpdateGameStatePacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenPreMatchScreenPacket.class,
            OpenPreMatchScreenPacket::encode,
            OpenPreMatchScreenPacket::decode,
            OpenPreMatchScreenPacket::handle);
        
        INSTANCE.registerMessage(packetId++, StartMatchPacket.class,
            StartMatchPacket::encode,
            StartMatchPacket::decode,
            StartMatchPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestCardCollectionPacket.class,
            RequestCardCollectionPacket::encode,
            RequestCardCollectionPacket::decode,
            RequestCardCollectionPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendCardCollectionPacket.class,
            SendCardCollectionPacket::encode,
            SendCardCollectionPacket::decode,
            SendCardCollectionPacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenEncyclopediaPacket.class,
            OpenEncyclopediaPacket::encode,
            OpenEncyclopediaPacket::decode,
            OpenEncyclopediaPacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenDeckEditorPacket.class,
            OpenDeckEditorPacket::encode,
            OpenDeckEditorPacket::decode,
            OpenDeckEditorPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SaveDeckPacket.class,
            SaveDeckPacket::encode,
            SaveDeckPacket::decode,
            SaveDeckPacket::handle);
        
        INSTANCE.registerMessage(packetId++, LoadDeckPacket.class,
            LoadDeckPacket::encode,
            LoadDeckPacket::decode,
            LoadDeckPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestDecksPacket.class,
            RequestDecksPacket::encode,
            RequestDecksPacket::decode,
            RequestDecksPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendDecksPacket.class,
            SendDecksPacket::encode,
            SendDecksPacket::decode,
            SendDecksPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendDeckDataPacket.class,
            SendDeckDataPacket::encode,
            SendDeckDataPacket::decode,
            SendDeckDataPacket::handle);
        
        INSTANCE.registerMessage(packetId++, ActionLogPacket.class,
            ActionLogPacket::encode,
            ActionLogPacket::decode,
            ActionLogPacket::handle);
        
        // Админ-панель пакеты
        INSTANCE.registerMessage(packetId++, OpenAdminPanelPacket.class,
            OpenAdminPanelPacket::encode,
            OpenAdminPanelPacket::decode,
            OpenAdminPanelPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminGiveCardPacket.class,
            AdminGiveCardPacket::encode,
            AdminGiveCardPacket::decode,
            AdminGiveCardPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminGiveXPPacket.class,
            AdminGiveXPPacket::encode,
            AdminGiveXPPacket::decode,
            AdminGiveXPPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminSetLevelPacket.class,
            AdminSetLevelPacket::encode,
            AdminSetLevelPacket::decode,
            AdminSetLevelPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminUnlockAllPacket.class,
            AdminUnlockAllPacket::encode,
            AdminUnlockAllPacket::decode,
            AdminUnlockAllPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminShowStatsPacket.class,
            AdminShowStatsPacket::encode,
            AdminShowStatsPacket::decode,
            AdminShowStatsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminStatsResponsePacket.class,
            AdminStatsResponsePacket::encode,
            AdminStatsResponsePacket::decode,
            AdminStatsResponsePacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminDeleteReplayPacket.class,
            AdminDeleteReplayPacket::encode,
            AdminDeleteReplayPacket::decode,
            AdminDeleteReplayPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminDeleteAllReplaysPacket.class,
            AdminDeleteAllReplaysPacket::encode,
            AdminDeleteAllReplaysPacket::decode,
            AdminDeleteAllReplaysPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminGiveCoinsPacket.class,
            AdminGiveCoinsPacket::encode,
            AdminGiveCoinsPacket::decode,
            AdminGiveCoinsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, AdminGiveAchievementPacket.class,
            AdminGiveAchievementPacket::encode,
            AdminGiveAchievementPacket::decode,
            AdminGiveAchievementPacket::handle);
        
        INSTANCE.registerMessage(packetId++, DenyChallengePacket.class,
            DenyChallengePacket::encode,
            DenyChallengePacket::decode,
            DenyChallengePacket::handle);
        
        INSTANCE.registerMessage(packetId++, CancelChallengePacket.class,
            CancelChallengePacket::encode,
            CancelChallengePacket::decode,
            CancelChallengePacket::handle);
        
        INSTANCE.registerMessage(packetId++, ClosePreMatchScreenPacket.class,
            ClosePreMatchScreenPacket::encode,
            ClosePreMatchScreenPacket::decode,
            ClosePreMatchScreenPacket::handle);
        
        // Пакеты для синхронизации систем
        INSTANCE.registerMessage(packetId++, RequestStatisticsPacket.class,
            RequestStatisticsPacket::encode,
            RequestStatisticsPacket::decode,
            RequestStatisticsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendStatisticsPacket.class,
            SendStatisticsPacket::encode,
            SendStatisticsPacket::decode,
            SendStatisticsPacket::handle);
        
        // Пакеты для системы эволюции карт
        INSTANCE.registerMessage(packetId++, RequestCardEvolutionPacket.class,
            RequestCardEvolutionPacket::encode,
            RequestCardEvolutionPacket::decode,
            RequestCardEvolutionPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendCardEvolutionPacket.class,
            SendCardEvolutionPacket::encode,
            SendCardEvolutionPacket::decode,
            SendCardEvolutionPacket::handle);
        
        INSTANCE.registerMessage(packetId++, UnlockBranchPacket.class,
            UnlockBranchPacket::encode,
            UnlockBranchPacket::decode,
            UnlockBranchPacket::handle);
        
        // Пакеты для системы квестов
        INSTANCE.registerMessage(packetId++, RequestQuestsPacket.class,
            RequestQuestsPacket::encode,
            RequestQuestsPacket::decode,
            RequestQuestsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendQuestsPacket.class,
            SendQuestsPacket::encode,
            SendQuestsPacket::decode,
            SendQuestsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, StartQuestPacket.class,
            StartQuestPacket::encode,
            StartQuestPacket::decode,
            StartQuestPacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenMainMenuPacket.class,
            OpenMainMenuPacket::encode,
            OpenMainMenuPacket::decode,
            OpenMainMenuPacket::handle);
        
        // Пакеты для системы драфта
        INSTANCE.registerMessage(packetId++, RequestDraftDataPacket.class,
            RequestDraftDataPacket::encode,
            RequestDraftDataPacket::decode,
            RequestDraftDataPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendDraftDataPacket.class,
            SendDraftDataPacket::encode,
            SendDraftDataPacket::decode,
            SendDraftDataPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SelectDraftCardPacket.class,
            SelectDraftCardPacket::encode,
            SelectDraftCardPacket::decode,
            SelectDraftCardPacket::handle);
        
        INSTANCE.registerMessage(packetId++, StartDraftPacket.class,
            StartDraftPacket::encode,
            StartDraftPacket::decode,
            StartDraftPacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenDraftScreenPacket.class,
            OpenDraftScreenPacket::encode,
            OpenDraftScreenPacket::decode,
            OpenDraftScreenPacket::handle);
        
        INSTANCE.registerMessage(packetId++, StartArenaPacket.class,
            StartArenaPacket::encode,
            StartArenaPacket::decode,
            StartArenaPacket::handle);
        
        // Пакеты для пользовательских турниров
        INSTANCE.registerMessage(packetId++, RequestCustomTournamentsPacket.class,
            RequestCustomTournamentsPacket::encode,
            RequestCustomTournamentsPacket::decode,
            RequestCustomTournamentsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendCustomTournamentsPacket.class,
            SendCustomTournamentsPacket::encode,
            SendCustomTournamentsPacket::decode,
            SendCustomTournamentsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, CreateCustomTournamentPacket.class,
            CreateCustomTournamentPacket::encode,
            CreateCustomTournamentPacket::decode,
            CreateCustomTournamentPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RegisterForCustomTournamentPacket.class,
            RegisterForCustomTournamentPacket::encode,
            RegisterForCustomTournamentPacket::decode,
            RegisterForCustomTournamentPacket::handle);
        
        INSTANCE.registerMessage(packetId++, StartCustomTournamentPacket.class,
            StartCustomTournamentPacket::encode,
            StartCustomTournamentPacket::decode,
            StartCustomTournamentPacket::handle);
        
        // Пакеты для трансляции турниров
        INSTANCE.registerMessage(packetId++, AddTournamentSpectatorPacket.class,
            AddTournamentSpectatorPacket::encode,
            AddTournamentSpectatorPacket::decode,
            AddTournamentSpectatorPacket::handle);
        
        INSTANCE.registerMessage(packetId++, OpenTournamentSpectatorPacket.class,
            OpenTournamentSpectatorPacket::encode,
            OpenTournamentSpectatorPacket::decode,
            OpenTournamentSpectatorPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestTournamentMatchesPacket.class,
            RequestTournamentMatchesPacket::encode,
            RequestTournamentMatchesPacket::decode,
            RequestTournamentMatchesPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendTournamentMatchesPacket.class,
            SendTournamentMatchesPacket::encode,
            SendTournamentMatchesPacket::decode,
            SendTournamentMatchesPacket::handle);
        
        INSTANCE.registerMessage(packetId++, WatchTournamentMatchPacket.class,
            WatchTournamentMatchPacket::encode,
            WatchTournamentMatchPacket::decode,
            WatchTournamentMatchPacket::handle);
        
        // Пакеты для лидербордов
        INSTANCE.registerMessage(packetId++, RequestLeaderboardPacket.class,
            RequestLeaderboardPacket::encode,
            RequestLeaderboardPacket::decode,
            RequestLeaderboardPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendLeaderboardPacket.class,
            SendLeaderboardPacket::encode,
            SendLeaderboardPacket::decode,
            SendLeaderboardPacket::handle);
        
        // Пакеты для косметики и эмоций
        INSTANCE.registerMessage(packetId++, SendEmotePacket.class,
            SendEmotePacket::encode,
            SendEmotePacket::decode,
            SendEmotePacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestTournamentsPacket.class,
            RequestTournamentsPacket::encode,
            RequestTournamentsPacket::decode,
            RequestTournamentsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendTournamentsPacket.class,
            SendTournamentsPacket::encode,
            SendTournamentsPacket::decode,
            SendTournamentsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestSeasonPacket.class,
            RequestSeasonPacket::encode,
            RequestSeasonPacket::decode,
            RequestSeasonPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendSeasonPacket.class,
            SendSeasonPacket::encode,
            SendSeasonPacket::decode,
            SendSeasonPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestDailyRewardsPacket.class,
            RequestDailyRewardsPacket::encode,
            RequestDailyRewardsPacket::decode,
            RequestDailyRewardsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendDailyRewardsPacket.class,
            SendDailyRewardsPacket::encode,
            SendDailyRewardsPacket::decode,
            SendDailyRewardsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestReplaysPacket.class,
            RequestReplaysPacket::encode,
            RequestReplaysPacket::decode,
            RequestReplaysPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendReplaysPacket.class,
            SendReplaysPacket::encode,
            SendReplaysPacket::decode,
            SendReplaysPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestReplayPacket.class,
            RequestReplayPacket::encode,
            RequestReplayPacket::decode,
            RequestReplayPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendReplayPacket.class,
            SendReplayPacket::encode,
            SendReplayPacket::decode,
            SendReplayPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RequestEventsPacket.class,
            RequestEventsPacket::encode,
            RequestEventsPacket::decode,
            RequestEventsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, SendEventsPacket.class,
            SendEventsPacket::encode,
            SendEventsPacket::decode,
            SendEventsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, ParticipateInEventPacket.class,
            ParticipateInEventPacket::encode,
            ParticipateInEventPacket::decode,
            ParticipateInEventPacket::handle);
        
        INSTANCE.registerMessage(packetId++, RegisterForTournamentPacket.class,
            RegisterForTournamentPacket::encode,
            RegisterForTournamentPacket::decode,
            RegisterForTournamentPacket::handle);
        
        INSTANCE.registerMessage(packetId++, ClaimDailyRewardPacket.class,
            ClaimDailyRewardPacket::encode,
            ClaimDailyRewardPacket::decode,
            ClaimDailyRewardPacket::handle);
        
        // Пакеты для системы друзей
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.FriendsPackets.RequestFriendsPacket.class,
            com.bmfalkye.network.FriendsPackets.RequestFriendsPacket::encode,
            com.bmfalkye.network.FriendsPackets.RequestFriendsPacket::decode,
            com.bmfalkye.network.FriendsPackets.RequestFriendsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.FriendsPackets.SendFriendsPacket.class,
            com.bmfalkye.network.FriendsPackets.SendFriendsPacket::encode,
            com.bmfalkye.network.FriendsPackets.SendFriendsPacket::decode,
            com.bmfalkye.network.FriendsPackets.SendFriendsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.FriendsPackets.AddFriendPacket.class,
            com.bmfalkye.network.FriendsPackets.AddFriendPacket::encode,
            com.bmfalkye.network.FriendsPackets.AddFriendPacket::decode,
            com.bmfalkye.network.FriendsPackets.AddFriendPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.FriendsPackets.RemoveFriendPacket.class,
            com.bmfalkye.network.FriendsPackets.RemoveFriendPacket::encode,
            com.bmfalkye.network.FriendsPackets.RemoveFriendPacket::decode,
            com.bmfalkye.network.FriendsPackets.RemoveFriendPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket.class,
            com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket::encode,
            com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket::decode,
            com.bmfalkye.network.FriendsPackets.AddFriendByNamePacket::handle);
        
        // Пакеты для системы гильдий
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.GuildPackets.RequestGuildInfoPacket.class,
            com.bmfalkye.network.GuildPackets.RequestGuildInfoPacket::encode,
            com.bmfalkye.network.GuildPackets.RequestGuildInfoPacket::decode,
            com.bmfalkye.network.GuildPackets.RequestGuildInfoPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.GuildPackets.SendGuildInfoPacket.class,
            com.bmfalkye.network.GuildPackets.SendGuildInfoPacket::encode,
            com.bmfalkye.network.GuildPackets.SendGuildInfoPacket::decode,
            com.bmfalkye.network.GuildPackets.SendGuildInfoPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.GuildPackets.CreateGuildPacket.class,
            com.bmfalkye.network.GuildPackets.CreateGuildPacket::encode,
            com.bmfalkye.network.GuildPackets.CreateGuildPacket::decode,
            com.bmfalkye.network.GuildPackets.CreateGuildPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.GuildPackets.LeaveGuildPacket.class,
            com.bmfalkye.network.GuildPackets.LeaveGuildPacket::encode,
            com.bmfalkye.network.GuildPackets.LeaveGuildPacket::decode,
            com.bmfalkye.network.GuildPackets.LeaveGuildPacket::handle);
        
        // Пакеты для системы боссов
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.BossPackets.RequestBossesPacket.class,
            com.bmfalkye.network.BossPackets.RequestBossesPacket::encode,
            com.bmfalkye.network.BossPackets.RequestBossesPacket::decode,
            com.bmfalkye.network.BossPackets.RequestBossesPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.BossPackets.SendBossesPacket.class,
            com.bmfalkye.network.BossPackets.SendBossesPacket::encode,
            com.bmfalkye.network.BossPackets.SendBossesPacket::decode,
            com.bmfalkye.network.BossPackets.SendBossesPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.BossPackets.ChallengeBossPacket.class,
            com.bmfalkye.network.BossPackets.ChallengeBossPacket::encode,
            com.bmfalkye.network.BossPackets.ChallengeBossPacket::decode,
            com.bmfalkye.network.BossPackets.ChallengeBossPacket::handle);
        
        // Пакеты для магазина карт
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.ShopPackets.RequestShopItemsPacket.class,
            com.bmfalkye.network.ShopPackets.RequestShopItemsPacket::encode,
            com.bmfalkye.network.ShopPackets.RequestShopItemsPacket::decode,
            com.bmfalkye.network.ShopPackets.RequestShopItemsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.ShopPackets.SendShopItemsPacket.class,
            com.bmfalkye.network.ShopPackets.SendShopItemsPacket::encode,
            com.bmfalkye.network.ShopPackets.SendShopItemsPacket::decode,
            com.bmfalkye.network.ShopPackets.SendShopItemsPacket::handle);
        
        INSTANCE.registerMessage(packetId++, com.bmfalkye.network.ShopPackets.BuyCardPacket.class,
            com.bmfalkye.network.ShopPackets.BuyCardPacket::encode,
            com.bmfalkye.network.ShopPackets.BuyCardPacket::decode,
            com.bmfalkye.network.ShopPackets.BuyCardPacket::handle);
    }
    
    public static void openGameScreen(ServerPlayer player, FalkyeGameSession session) {
        com.bmfalkye.util.ModLogger.logNetwork("Opening game screen", 
            "player", player.getName().getString(),
            "round", session.getCurrentRound(),
            "isVillagerGame", session.isPlayingWithVillager());
        
        // Создаём временный ClientFalkyeGameSession для передачи на клиент
        // В реальности нужно сериализовать FalkyeGameSession в ClientFalkyeGameSession
        int remainingTime = com.bmfalkye.game.TurnTimer.getRemainingTime(session);
        UUID currentPlayerUUID = session.getCurrentPlayerUUID();
        int timeoutCount = currentPlayerUUID != null ? 
            com.bmfalkye.game.TurnTimer.getTimeOutCount(currentPlayerUUID) : 0;
        com.bmfalkye.game.ClientFalkyeGameSession clientSession = new com.bmfalkye.game.ClientFalkyeGameSession(
            session.getPlayer1().getUUID(), 
            session.getPlayer2() != null ? session.getPlayer2().getUUID() : java.util.UUID.randomUUID(),
            session.getRoundScore(session.getPlayer1()), 
            session.getRoundScore(session.getPlayer2()),
            session.getCurrentRound(), 
            session.getCurrentPlayerUUID(),
            session.getHand(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getHand(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getMeleeRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getRangedRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getSiegeRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getMeleeRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getRangedRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getSiegeRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            session.getWeather(), 
            session.getLeader(session.getPlayer1()) != null ? session.getLeader(session.getPlayer1()).getId() : "",
            session.getLeader(session.getPlayer2()) != null ? session.getLeader(session.getPlayer2()).getId() : "",
            session.getRoundsWon(session.getPlayer1()), 
            session.getRoundsWon(session.getPlayer2()),
            session.hasPassed(session.getPlayer1()), 
            session.hasPassed(session.getPlayer2()),
            session.isPlayingWithVillager(),
            remainingTime,
            timeoutCount,
            session.getPowerModifiers(), // Получаем модификаторы для ВСЕХ карт на поле, чтобы оба игрока видели изменения
            session.getRevealedCards(player), // Передаём показанные карты для текущего игрока
            session.getLocationType(), // Передаём тип локации
            session.getMatchConfig() != null ? session.getMatchConfig().getGameMode() : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D // Передаём режим игры
        );
        // Используем безопасную отправку с обработкой ошибок для критичного пакета
        OpenGameScreenPacket packet = new OpenGameScreenPacket(clientSession);
        NetworkErrorHandler.SendResult result = NetworkErrorHandler.sendPacketSafely(
            INSTANCE, packet, player, NetworkDirection.PLAY_TO_CLIENT,
            NetworkErrorHandler.PacketPriority.CRITICAL,
            () -> new OpenGameScreenPacket(clientSession) // Поставщик для повторной отправки
        );
        
        if (result != NetworkErrorHandler.SendResult.SUCCESS) {
            ModLogger.warn("Failed to send OpenGameScreenPacket", 
                "player", player.getName().getString(),
                "result", result.name());
        }
    }
    
    /**
     * Немедленное обновление состояния игры (без дебаунсинга) для критичных событий
     */
    public static void updateGameStateImmediate(ServerPlayer player, FalkyeGameSession session) {
        if (player == null || session == null) {
            return;
        }
        
        // Обновляем время последнего обновления, но не блокируем
        UUID playerUUID = player.getUUID();
        lastUpdateTime.put(playerUUID, System.currentTimeMillis());
        
        // Вызываем основной метод обновления
        updateGameStateInternal(player, session);
    }
    
    /**
     * Обновление состояния игры с дебаунсингом (для периодических обновлений)
     */
    public static void updateGameState(ServerPlayer player, FalkyeGameSession session) {
        if (player == null || session == null) {
            return;
        }
        
        // Дебаунсинг: проверяем, не слишком ли часто отправляем обновления
        // НО: не блокируем обновления после важных событий (игра карты, пас, смена хода)
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerUUID);
        
        // Разрешаем обновление, если прошло достаточно времени ИЛИ это критичное обновление
        // (критичные обновления обрабатываются через специальные методы без дебаунсинга)
        if (lastUpdate != null && (currentTime - lastUpdate) < UPDATE_DEBOUNCE_MS) {
            // Слишком часто - пропускаем обновление (но только для не-критичных обновлений)
            // Критичные обновления должны вызываться через updateGameStateImmediate
            return;
        }
        
        lastUpdateTime.put(playerUUID, currentTime);
        
        updateGameStateInternal(player, session);
    }
    
    /**
     * Внутренний метод обновления состояния игры (без дебаунсинга)
     */
    private static void updateGameStateInternal(ServerPlayer player, FalkyeGameSession session) {
        // Создаём ClientFalkyeGameSession из FalkyeGameSession
        int remainingTime = com.bmfalkye.game.TurnTimer.getRemainingTime(session);
        UUID currentPlayerUUID = session.getCurrentPlayerUUID();
        int timeoutCount = currentPlayerUUID != null ? 
            com.bmfalkye.game.TurnTimer.getTimeOutCount(currentPlayerUUID) : 0;
        
        // Получаем UUID второго игрока (для villager используем UUID жителя)
        UUID player2UUID = session.getPlayer2() != null ? session.getPlayer2().getUUID() : 
            (session.isPlayingWithVillager() && session.getVillagerOpponent() != null ? 
                session.getVillagerOpponent().getUUID() : java.util.UUID.randomUUID());
        
        // Получаем руки игроков (для villager используем null)
        java.util.List<String> hand1Ids = session.getHand(session.getPlayer1()).stream()
            .map(c -> c.getId()).collect(java.util.stream.Collectors.toList());
        java.util.List<String> hand2Ids = session.isPlayingWithVillager() ? 
            session.getHand(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (session.getPlayer2() != null ? 
                session.getHand(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        // Получаем ряды игроков (для villager используем null)
        java.util.List<String> melee2Ids = session.isPlayingWithVillager() ? 
            session.getMeleeRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (session.getPlayer2() != null ? 
                session.getMeleeRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        java.util.List<String> ranged2Ids = session.isPlayingWithVillager() ? 
            session.getRangedRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (session.getPlayer2() != null ? 
                session.getRangedRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        java.util.List<String> siege2Ids = session.isPlayingWithVillager() ? 
            session.getSiegeRow(null).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
            (session.getPlayer2() != null ? 
                session.getSiegeRow(session.getPlayer2()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()) :
                new java.util.ArrayList<>());
        
        com.bmfalkye.game.ClientFalkyeGameSession clientSession = new com.bmfalkye.game.ClientFalkyeGameSession(
            session.getPlayer1().getUUID(), 
            player2UUID,
            session.getRoundScore(session.getPlayer1()), 
            session.getRoundScore(session.getPlayer2()),
            session.getCurrentRound(), 
            session.getCurrentPlayerUUID(),
            hand1Ids,
            hand2Ids,
            session.getMeleeRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getRangedRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            session.getSiegeRow(session.getPlayer1()).stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toList()),
            melee2Ids,
            ranged2Ids,
            siege2Ids,
            new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            session.getWeather(), 
            session.getLeader(session.getPlayer1()) != null ? session.getLeader(session.getPlayer1()).getId() : "",
            session.getLeader(session.getPlayer2()) != null ? session.getLeader(session.getPlayer2()).getId() : "",
            session.getRoundsWon(session.getPlayer1()), 
            session.getRoundsWon(session.getPlayer2()),
            session.hasPassed(session.getPlayer1()), 
            session.hasPassed(session.getPlayer2()),
            session.isPlayingWithVillager(),
            remainingTime,
            timeoutCount,
            session.getPowerModifiers(), // Получаем модификаторы для ВСЕХ карт на поле, чтобы оба игрока видели изменения
            session.getRevealedCards(player), // Передаём показанные карты для текущего игрока
            session.getLocationType(), // Передаём тип локации
            session.getMatchConfig() != null ? session.getMatchConfig().getGameMode() : com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D // Передаём режим игры
        );
        // Используем безопасную отправку с обработкой ошибок для критичного пакета
        UpdateGameStatePacket packet = new UpdateGameStatePacket(clientSession);
        NetworkErrorHandler.SendResult result = NetworkErrorHandler.sendPacketSafely(
            INSTANCE, packet, player, NetworkDirection.PLAY_TO_CLIENT,
            NetworkErrorHandler.PacketPriority.CRITICAL,
            () -> {
                // Создаём новый пакет с актуальным состоянием для повторной отправки
                return new UpdateGameStatePacket(clientSession);
            }
        );
        
        if (result != NetworkErrorHandler.SendResult.SUCCESS) {
            ModLogger.warn("Failed to send UpdateGameStatePacket", 
                "player", player.getName().getString(),
                "result", result.name());
        }
    }
    
    /**
     * Добавляет сообщение в лог действий игрока (с батчингом)
     */
    public static void addActionLog(ServerPlayer player, String message) {
        if (player == null) return;
        
        // Используем батчинг для логов действий (низкий приоритет)
        ActionLogPacket packet = new ActionLogPacket(message);
        PacketBatcher.addToBatch(INSTANCE, packet, player, NetworkDirection.PLAY_TO_CLIENT,
            NetworkErrorHandler.PacketPriority.LOW,
            () -> new ActionLogPacket(message));
    }
    
    public static void openPreMatchScreen(ServerPlayer player, UUID opponentUUID, String opponentName, boolean isNPC) {
        openPreMatchScreen(player, opponentUUID, opponentName, isNPC, -1, false);
    }
    
    public static void openPreMatchScreen(ServerPlayer player, UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins, boolean isChallenged) {
        // Получаем монеты жителя, если это NPC
        final int actualVillagerCoins;
        if (isNPC && villagerCoins < 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(opponentUUID);
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                com.bmfalkye.storage.VillagerCurrency currency = 
                    com.bmfalkye.storage.VillagerCurrency.get(serverLevel);
                actualVillagerCoins = currency.getCoins(livingEntity);
            } else {
                actualVillagerCoins = villagerCoins;
            }
        } else {
            actualVillagerCoins = villagerCoins;
        }
        // Используем безопасную отправку с обработкой ошибок для важного пакета
        OpenPreMatchScreenPacket packet = new OpenPreMatchScreenPacket(opponentUUID, opponentName, isNPC, actualVillagerCoins, isChallenged);
        NetworkErrorHandler.SendResult result = NetworkErrorHandler.sendPacketSafely(
            INSTANCE, packet, player, NetworkDirection.PLAY_TO_CLIENT,
            NetworkErrorHandler.PacketPriority.HIGH,
            () -> new OpenPreMatchScreenPacket(opponentUUID, opponentName, isNPC, actualVillagerCoins, isChallenged)
        );
        
        if (result != NetworkErrorHandler.SendResult.SUCCESS) {
            ModLogger.warn("Failed to send OpenPreMatchScreenPacket", 
                "player", player.getName().getString(),
                "result", result.name());
        }
    }
    
    // Пакет для открытия игрового экрана
    public static class OpenGameScreenPacket {
        private final com.bmfalkye.game.ClientFalkyeGameSession session;
        
        public OpenGameScreenPacket(com.bmfalkye.game.ClientFalkyeGameSession session) {
            this.session = session;
        }
        
        public static void encode(OpenGameScreenPacket msg, FriendlyByteBuf buffer) {
            // Сериализуем ClientFalkyeGameSession
            encodeFalkyeGameSessionForClient(msg.session, buffer);
        }
        
        
        public static OpenGameScreenPacket decode(FriendlyByteBuf buffer) {
            com.bmfalkye.game.ClientFalkyeGameSession session = NetworkHandler.decodeFalkyeGameSession(buffer);
            return new OpenGameScreenPacket(session);
        }
        
        public static void handle(OpenGameScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    // На клиенте используем ClientFalkyeGameSession напрямую
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.FalkyeGameScreen(msg.session));
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public com.bmfalkye.game.ClientFalkyeGameSession getSession() {
            return session;
        }
    }
    
    // Пакет для игры картой
    public static class PlayCardPacket {
        private final String cardId;
        private final int row; // 0 = ближний, 1 = дальний, 2 = осада
        
        public PlayCardPacket(String cardId, int row) {
            this.cardId = cardId;
            this.row = row;
        }
        
        public static void encode(PlayCardPacket msg, FriendlyByteBuf buffer) {
            String cardId = msg.cardId != null ? msg.cardId : "";
            if (cardId.length() > 32767) {
                cardId = cardId.substring(0, 32767);
            }
            buffer.writeUtf(cardId);
            buffer.writeInt(msg.row);
        }
        
        public static PlayCardPacket decode(FriendlyByteBuf buffer) {
            return new PlayCardPacket(buffer.readUtf(), buffer.readInt());
        }
        
        public static void handle(PlayCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !com.bmfalkye.util.InputValidator.isPlayerValid(player)) {
                    return;
                }
                
                // Валидация входных данных
                if (!com.bmfalkye.util.InputValidator.isValidCardId(msg.cardId)) {
                    ModLogger.warn("Invalid card ID in PlayCardPacket", "cardId", msg.cardId);
                    return;
                }
                
                if (!com.bmfalkye.util.InputValidator.isValidCardRow(msg.row)) {
                    ModLogger.warn("Invalid card row in PlayCardPacket", "row", msg.row);
                    return;
                }
                
                FalkyeGameSession session = com.bmfalkye.game.GameManager.getActiveGame(player);
                if (session == null) {
                    ModLogger.warn("No active game session for player", "player", player.getName().getString());
                    return;
                }
                
                // Валидация: проверяем, что это ход игрока
                if (!session.isPlayerTurn(player)) {
                    ModLogger.warn("Player tried to play card out of turn", "player", player.getName().getString());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cНе ваш ход!"));
                    return;
                }
                
                com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(msg.cardId);
                if (card == null) {
                    ModLogger.warn("Card not found in registry", "cardId", msg.cardId);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cКарта не найдена: " + msg.cardId));
                    return;
                }
                
                // Проверка на честность игры
                if (!com.bmfalkye.anticheat.AntiCheatSystem.canPlayerAct(player, session, "playCard")) {
                    ModLogger.warn("Anti-cheat blocked action", "player", player.getName().getString(), "action", "playCard");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cДействие заблокировано системой защиты."));
                    return;
                }
                
                // Валидация: проверяем, что карта есть в руке игрока (с проверкой на читерство)
                if (!com.bmfalkye.anticheat.AntiCheatSystem.validateCardInHand(player, session, msg.cardId)) {
                    ModLogger.warn("Player tried to play card not in hand", "player", player.getName().getString(), "cardId", msg.cardId);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cУ вас нет этой карты в руке!"));
                    return;
                }
                
                java.util.List<com.bmfalkye.cards.Card> hand = session.getHand(player);
                if (!hand.contains(card)) {
                    ModLogger.warn("Player tried to play card not in hand", "player", player.getName().getString(), "cardId", msg.cardId);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cУ вас нет этой карты в руке!"));
                    return;
                }
                
                FalkyeGameSession.CardRow row = switch (msg.row) {
                    case 0 -> FalkyeGameSession.CardRow.MELEE;
                    case 1 -> FalkyeGameSession.CardRow.RANGED;
                    case 2 -> FalkyeGameSession.CardRow.SIEGE;
                    default -> FalkyeGameSession.CardRow.MELEE;
                };
                boolean success = session.playCard(player, card, row);
                if (success) {
                    // Показываем подсказку о том, как играть карты (для новых игроков)
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.tutorial.TutorialSystem.showPlayCardHint(player, serverLevel);
                    }
                    
                    // Обновляем состояние игры только если карта была успешно сыграна
                    // Используем немедленное обновление для критичных событий
                    NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                    if (session.getPlayer2() != null) {
                        NetworkHandler.updateGameStateImmediate(session.getPlayer2(), session);
                    }
                    
                    // Если играем с villager и теперь его ход, делаем ход AI в следующем тике
                    if (session.isPlayingWithVillager() && session.isVillagerTurn()) {
                        ModLogger.logGameLogic("Villager turn after player move", "triggering AI");
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.getServer().execute(() -> {
                                com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                                NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                            });
                        }
                    }
                } else {
                    ModLogger.warn("Failed to play card", "player", player.getName().getString(), "cardId", msg.cardId);
                    // Если карта не была сыграна, отправляем сообщение игроку
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cНе удалось сыграть карту. Проверьте, ваш ли это ход."));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для паса
    public static class PassPacket {
        public static void encode(PassPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static PassPacket decode(FriendlyByteBuf buffer) {
            return new PassPacket();
        }
        
        public static void handle(PassPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.util.ModLogger.logNetwork("PassPacket received", 
                        "player", player.getName().getString());
                    
                    FalkyeGameSession session = com.bmfalkye.game.GameManager.getActiveGame(player);
                    
                    // Проверка на честность игры
                    if (session != null && !com.bmfalkye.anticheat.AntiCheatSystem.canPlayerAct(player, session, "pass")) {
                        ModLogger.warn("Anti-cheat blocked action", "player", player.getName().getString(), "action", "pass");
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cДействие заблокировано системой защиты."));
                        return;
                    }
                    
                    if (session != null) {
                        session.pass(player);
                        
                        // Показываем подсказку о пасе (для новых игроков)
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            com.bmfalkye.tutorial.TutorialSystem.showPassHint(player, serverLevel);
                        }
                        
                        NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                        if (session.getPlayer2() != null) {
                            NetworkHandler.updateGameStateImmediate(session.getPlayer2(), session);
                        }
                        
                        // Если играем с villager и теперь его ход, делаем ход AI в следующем тике
                        if (session.isPlayingWithVillager() && session.isVillagerTurn()) {
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.getServer().execute(() -> {
                                    com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                                    NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                                });
                            }
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для использования лидера
    public static class UseLeaderPacket {
        public static void encode(UseLeaderPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static UseLeaderPacket decode(FriendlyByteBuf buffer) {
            return new UseLeaderPacket();
        }
        
        public static void handle(UseLeaderPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ModLogger.logNetwork("UseLeaderPacket received", "player", player.getName().getString());
                    FalkyeGameSession session = com.bmfalkye.game.GameManager.getActiveGame(player);
                    
                    // Проверка на честность игры
                    if (session != null && !com.bmfalkye.anticheat.AntiCheatSystem.canPlayerAct(player, session, "useLeader")) {
                        ModLogger.warn("Anti-cheat blocked action", "player", player.getName().getString(), "action", "useLeader");
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cДействие заблокировано системой защиты."));
                        return;
                    }
                    
                    if (session != null) {
                        ModLogger.logGameLogic("Using leader ability", "player", player.getName().getString());
                        boolean success = session.useLeader(player);
                        if (success) {
                            ModLogger.logGameLogic("Leader ability used successfully", "player", player.getName().getString());
                            
                            // Показываем подсказку об использовании лидера (для новых игроков)
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                com.bmfalkye.tutorial.TutorialSystem.showLeaderHint(player, serverLevel);
                            }
                            
                            // Обновляем состояние игры только если лидер был успешно использован
                            NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                            if (session.getPlayer2() != null) {
                                NetworkHandler.updateGameState(session.getPlayer2(), session);
                            }
                            
                            // Если играем с villager и теперь его ход, делаем ход AI в следующем тике
                            if (session.isPlayingWithVillager() && session.isVillagerTurn()) {
                                ModLogger.logGameLogic("Villager turn after leader use", "triggering AI");
                                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                    serverLevel.getServer().execute(() -> {
                                        com.bmfalkye.game.VillagerAIPlayer.makeAITurn(session);
                                        NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                                    });
                                }
                            }
                        } else {
                            ModLogger.warn("Failed to use leader ability", "player", player.getName().getString());
                            // Если лидер не был использован, отправляем сообщение игроку
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cНе удалось использовать лидера. Проверьте, ваш ли это ход и не использован ли лидер ранее."));
                        }
                    } else {
                        ModLogger.warn("Active game not found for leader use", "player", player.getName().getString());
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для сдачи
    public static class SurrenderPacket {
        public static void encode(SurrenderPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static SurrenderPacket decode(FriendlyByteBuf buffer) {
            return new SurrenderPacket();
        }
        
        public static void handle(SurrenderPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.util.ModLogger.logNetwork("SurrenderPacket received", 
                        "player", player.getName().getString());
                    
                    FalkyeGameSession session = com.bmfalkye.game.GameManager.getActiveGame(player);
                    
                    // Проверка на честность игры
                    if (session != null && !com.bmfalkye.anticheat.AntiCheatSystem.canPlayerAct(player, session, "surrender")) {
                        ModLogger.warn("Anti-cheat blocked action", "player", player.getName().getString(), "action", "surrender");
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cДействие заблокировано системой защиты."));
                        return;
                    }
                    
                    if (session != null && !session.isGameEnded()) {
                        // Проверяем баланс игрока
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            com.bmfalkye.storage.PlayerCurrency currency = 
                                com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                            
                            int surrenderCost = 200;
                            if (currency.hasEnoughCoins(player, surrenderCost)) {
                                // Забираем монеты
                                currency.removeCoins(player, surrenderCost);
                                
                                // Завершаем игру с поражением
                                session.forceGameEnd(player, false);
                                
                                // Отправляем сообщение игроку (с флагом overlay для видимости)
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§cВы сдались! С вас списано " + surrenderCost + " монет."), true);
                                
                                // Обновляем состояние игры (это закроет экран на клиенте)
                                NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                                if (session.getPlayer2() != null) {
                                    NetworkHandler.updateGameState(session.getPlayer2(), session);
                                }
                            } else {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§cУ вас недостаточно монет для сдачи! Нужно: " + surrenderCost));
                            }
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для завершения хода
    public static class EndTurnPacket {
        public static void encode(EndTurnPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static EndTurnPacket decode(FriendlyByteBuf buffer) {
            return new EndTurnPacket();
        }
        
        public static void handle(EndTurnPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    FalkyeGameSession session = com.bmfalkye.game.GameManager.getActiveGame(player);
                    if (session != null && session.getCurrentPlayer() != null && session.getCurrentPlayer().equals(player)) {
                        // В FalkyeGameSession нет метода endTurn, используем pass
                        session.pass(player);
                        // Обновляем состояние игры для обоих игроков
                        NetworkHandler.updateGameStateImmediate(session.getPlayer1(), session);
                        if (session.getPlayer2() != null) {
                            NetworkHandler.updateGameStateImmediate(session.getPlayer2(), session);
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для обновления состояния игры
    static class UpdateGameStatePacket {
        private final com.bmfalkye.game.ClientFalkyeGameSession session;
        
        public UpdateGameStatePacket(com.bmfalkye.game.ClientFalkyeGameSession session) {
            this.session = session;
        }
        
        public static void encode(UpdateGameStatePacket msg, FriendlyByteBuf buffer) {
            // Используем тот же метод, что и для OpenGameScreenPacket
            encodeFalkyeGameSessionForClient(msg.session, buffer);
        }
        
        public static UpdateGameStatePacket decode(FriendlyByteBuf buffer) {
            com.bmfalkye.game.ClientFalkyeGameSession session = decodeFalkyeGameSession(buffer);
            return new UpdateGameStatePacket(session);
        }
        
        public static void handle(UpdateGameStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleUpdateFalkyeGameState(msg.session);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public com.bmfalkye.game.ClientFalkyeGameSession getSession() {
            return session;
        }
    }
    
    // Вспомогательные методы для сериализации FalkyeGameSession
    private static void encodeFalkyeGameSession(FalkyeGameSession session, FriendlyByteBuf buffer) {
        // Записываем UUID игроков
        buffer.writeUUID(session.getPlayer1().getUUID());
        buffer.writeUUID(session.getPlayer2().getUUID());
        
        // Записываем информацию о раундах
        buffer.writeInt(session.getCurrentRound());
        buffer.writeInt(session.getRoundsWon(session.getPlayer1()));
        buffer.writeInt(session.getRoundsWon(session.getPlayer2()));
        buffer.writeInt(session.getRoundScore(session.getPlayer1()));
        buffer.writeInt(session.getRoundScore(session.getPlayer2()));
        
        // Записываем UUID текущего игрока
        buffer.writeUUID(session.getCurrentPlayerUUID());
        buffer.writeBoolean(session.hasPassed(session.getPlayer1()));
        buffer.writeBoolean(session.hasPassed(session.getPlayer2()));
        buffer.writeBoolean(session.isRoundEnded());
        buffer.writeBoolean(session.isGameEnded());
        
        // Записываем погоду
        buffer.writeInt(session.getWeather().ordinal());
        
        // Записываем карты в руке
        List<com.bmfalkye.cards.Card> hand1 = session.getHand(session.getPlayer1());
        buffer.writeInt(hand1.size());
        for (com.bmfalkye.cards.Card card : hand1) {
            buffer.writeUtf(card.getId());
        }
        
        List<com.bmfalkye.cards.Card> hand2 = session.getHand(session.getPlayer2());
        buffer.writeInt(hand2.size());
        for (com.bmfalkye.cards.Card card : hand2) {
            buffer.writeUtf(card.getId());
        }
        
        // Записываем карты на полях (3 ряда для каждого игрока)
        writeCardList(buffer, session.getMeleeRow(session.getPlayer1()));
        writeCardList(buffer, session.getRangedRow(session.getPlayer1()));
        writeCardList(buffer, session.getSiegeRow(session.getPlayer1()));
        writeCardList(buffer, session.getMeleeRow(session.getPlayer2()));
        writeCardList(buffer, session.getRangedRow(session.getPlayer2()));
        writeCardList(buffer, session.getSiegeRow(session.getPlayer2()));
    }
    
    private static void writeCardList(FriendlyByteBuf buffer, List<com.bmfalkye.cards.Card> cards) {
        buffer.writeInt(cards.size());
        for (com.bmfalkye.cards.Card card : cards) {
            String cardId = card.getId();
            if (cardId.length() > 32767) {
                cardId = cardId.substring(0, 32767);
            }
            buffer.writeUtf(cardId);
        }
    }
    
    private static com.bmfalkye.game.ClientFalkyeGameSession decodeFalkyeGameSession(FriendlyByteBuf buffer) {
        // Читаем UUID игроков
        UUID player1UUID = buffer.readUUID();
        UUID player2UUID = buffer.readUUID();
        
        // Читаем информацию о раундах
        int currentRound = buffer.readInt();
        int roundsWon1 = buffer.readInt();
        int roundsWon2 = buffer.readInt();
        int roundScore1 = buffer.readInt();
        int roundScore2 = buffer.readInt();
        
        UUID currentPlayerUUID = buffer.readUUID();
        boolean player1Passed = buffer.readBoolean();
        boolean player2Passed = buffer.readBoolean();
        boolean roundEnded = buffer.readBoolean();
        boolean gameEnded = buffer.readBoolean();
        
        // Читаем погоду
        com.bmfalkye.game.FalkyeGameSession.WeatherType weather = 
            com.bmfalkye.game.FalkyeGameSession.WeatherType.values()[buffer.readInt()];
        
        // Читаем оставшееся время и счётчик таймаутов (ДО карт, как в encode!)
        int remainingTime = 60; // Дефолт
        int timeoutCount = 0; // Дефолт
        if (buffer.isReadable()) {
            try {
                remainingTime = buffer.readInt();
                // Пытаемся прочитать timeoutCount (может отсутствовать для обратной совместимости)
                if (buffer.isReadable()) {
                    timeoutCount = buffer.readInt();
                }
            } catch (Exception e) {
                // Игнорируем, если поле отсутствует (обратная совместимость)
                remainingTime = 60;
                timeoutCount = 0;
            }
        }
        
        // Читаем карты в руке
        List<String> hand1Ids = readCardIdList(buffer);
        List<String> hand2Ids = readCardIdList(buffer);
        
        // Читаем карты на полях
        List<String> melee1Ids = readCardIdList(buffer);
        List<String> ranged1Ids = readCardIdList(buffer);
        List<String> siege1Ids = readCardIdList(buffer);
        List<String> melee2Ids = readCardIdList(buffer);
        List<String> ranged2Ids = readCardIdList(buffer);
        List<String> siege2Ids = readCardIdList(buffer);
        
        // Читаем модификаторы силы (может отсутствовать для обратной совместимости)
        java.util.Map<String, Integer> powerModifiers = new java.util.HashMap<>();
        if (buffer.isReadable()) {
            try {
                int modifiersSize = buffer.readInt();
                for (int i = 0; i < modifiersSize; i++) {
                    String cardId = buffer.readUtf();
                    int modifier = buffer.readInt();
                    powerModifiers.put(cardId, modifier);
                }
            } catch (Exception e) {
                // Игнорируем, если поле отсутствует (обратная совместимость)
                powerModifiers = new java.util.HashMap<>();
            }
        }
        
        // Читаем показанные карты (может отсутствовать для обратной совместимости)
        List<String> revealedCards = new java.util.ArrayList<>();
        if (buffer.isReadable()) {
            try {
                int revealedSize = buffer.readInt();
                for (int i = 0; i < revealedSize; i++) {
                    revealedCards.add(buffer.readUtf());
                }
            } catch (Exception e) {
                // Игнорируем, если поле отсутствует (обратная совместимость)
                revealedCards = new java.util.ArrayList<>();
            }
        }
        
        // Читаем тип локации (может отсутствовать для обратной совместимости)
        com.bmfalkye.game.LocationEffect.LocationType locationType = com.bmfalkye.game.LocationEffect.LocationType.NONE;
        if (buffer.isReadable()) {
            try {
                int locationOrdinal = buffer.readInt();
                com.bmfalkye.game.LocationEffect.LocationType[] values = com.bmfalkye.game.LocationEffect.LocationType.values();
                if (locationOrdinal >= 0 && locationOrdinal < values.length) {
                    locationType = values[locationOrdinal];
                }
            } catch (Exception e) {
                // Игнорируем, если поле отсутствует (обратная совместимость)
                locationType = com.bmfalkye.game.LocationEffect.LocationType.NONE;
            }
        }
        
        // Читаем режим игры (может отсутствовать для обратной совместимости)
        com.bmfalkye.settings.GameModeSettings.GameMode gameMode = com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
        if (buffer.isReadable()) {
            try {
                int gameModeOrdinal = buffer.readInt();
                com.bmfalkye.settings.GameModeSettings.GameMode[] values = com.bmfalkye.settings.GameModeSettings.GameMode.values();
                if (gameModeOrdinal >= 0 && gameModeOrdinal < values.length) {
                    gameMode = values[gameModeOrdinal];
                }
            } catch (Exception e) {
                // Игнорируем, если поле отсутствует (обратная совместимость)
                gameMode = com.bmfalkye.settings.GameModeSettings.GameMode.MODE_2D;
            }
        }
        
        // Создаём клиентскую версию сессии
        return new com.bmfalkye.game.ClientFalkyeGameSession(
            player1UUID, player2UUID, roundScore1, roundScore2, currentRound, currentPlayerUUID,
            hand1Ids, hand2Ids, melee1Ids, ranged1Ids, siege1Ids,
            melee2Ids, ranged2Ids, siege2Ids, new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            weather, "", "", roundsWon1, roundsWon2, player1Passed, player2Passed, false, remainingTime, timeoutCount,
            powerModifiers, revealedCards, locationType, gameMode
        );
    }
    
    private static List<String> readCardIdList(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(buffer.readUtf());
        }
        return list;
    }
    
    private static void writeCardIdList(FriendlyByteBuf buffer, List<String> cardIds) {
        buffer.writeInt(cardIds.size());
        for (String id : cardIds) {
            String safeId = id != null ? id : "";
            if (safeId.length() > 32767) {
                safeId = safeId.substring(0, 32767);
            }
            buffer.writeUtf(safeId);
        }
    }
    
    private static void encodeFalkyeGameSessionForClient(com.bmfalkye.game.ClientFalkyeGameSession session, FriendlyByteBuf buffer) {
        buffer.writeUUID(session.getPlayer1UUID());
        buffer.writeUUID(session.getPlayer2UUID());
        buffer.writeInt(session.getCurrentRound());
        // Используем геттеры для внутренних полей напрямую
        buffer.writeInt(session.getRoundsWon1());
        buffer.writeInt(session.getRoundsWon2());
        buffer.writeInt(session.getRoundScore1());
        buffer.writeInt(session.getRoundScore2());
        buffer.writeUUID(session.getCurrentPlayerUUID());
        buffer.writeBoolean(session.getPlayer1Passed());
        buffer.writeBoolean(session.getPlayer2Passed());
        buffer.writeBoolean(session.isRoundEnded());
        buffer.writeBoolean(session.isGameEnded());
        buffer.writeInt(session.getWeather().ordinal());
        buffer.writeInt(session.getRemainingTime());
        buffer.writeInt(session.getTimeoutCount());
        // Используем геттеры для ID карт напрямую
        writeCardIdList(buffer, session.getHand1Ids());
        writeCardIdList(buffer, session.getHand2Ids());
        writeCardIdList(buffer, session.getMelee1Ids());
        writeCardIdList(buffer, session.getRanged1Ids());
        writeCardIdList(buffer, session.getSiege1Ids());
        writeCardIdList(buffer, session.getMelee2Ids());
        writeCardIdList(buffer, session.getRanged2Ids());
        writeCardIdList(buffer, session.getSiege2Ids());
        // Записываем модификаторы силы
        java.util.Map<String, Integer> powerModifiers = session.getPowerModifiers();
        buffer.writeInt(powerModifiers.size());
        for (java.util.Map.Entry<String, Integer> entry : powerModifiers.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
        
        // Записываем показанные карты
        List<String> revealedCards = session.getRevealedCards();
        buffer.writeInt(revealedCards.size());
        for (String cardId : revealedCards) {
            buffer.writeUtf(cardId);
        }
        
        // Записываем тип локации
        buffer.writeInt(session.getLocationType().ordinal());
        
        // Записываем режим игры
        buffer.writeInt(session.getGameMode().ordinal());
    }
    
    // Пакет для открытия экрана предматча
    public static class OpenPreMatchScreenPacket {
        private final UUID opponentUUID;
        private final String opponentName;
        private final boolean isNPC;
        private final int villagerCoins; // Монеты жителя (-1 если не NPC)
        private final boolean isChallenged; // true если игрок тот, на кого вызвали
        
        public OpenPreMatchScreenPacket(UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins) {
            this(opponentUUID, opponentName, isNPC, villagerCoins, false);
        }
        
        public OpenPreMatchScreenPacket(UUID opponentUUID, String opponentName, boolean isNPC, int villagerCoins, boolean isChallenged) {
            this.opponentUUID = opponentUUID;
            this.opponentName = opponentName;
            this.isNPC = isNPC;
            this.villagerCoins = villagerCoins;
            this.isChallenged = isChallenged;
        }
        
        public static void encode(OpenPreMatchScreenPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUUID(msg.opponentUUID);
            // Ограничиваем длину строки, чтобы избежать StringIndexOutOfBoundsException
            String name = msg.opponentName != null ? msg.opponentName : "";
            if (name.length() > 32767) {
                name = name.substring(0, 32767);
            }
            buffer.writeUtf(name);
            buffer.writeBoolean(msg.isNPC);
            buffer.writeInt(msg.villagerCoins);
            buffer.writeBoolean(msg.isChallenged);
        }
        
        public static OpenPreMatchScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenPreMatchScreenPacket(
                buffer.readUUID(),
                buffer.readUtf(32767), // Ограничиваем длину строки
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readBoolean()
            );
        }
        
        public static void handle(OpenPreMatchScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleOpenPreMatchScreen(
                        msg.opponentUUID, msg.opponentName, msg.isNPC, msg.villagerCoins, msg.isChallenged);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public UUID getOpponentUUID() { return opponentUUID; }
        public String getOpponentName() { return opponentName; }
        public boolean isNPC() { return isNPC; }
        public int getVillagerCoins() { return villagerCoins; }
        public boolean isChallenged() { return isChallenged; }
    }
    
    // Пакет для начала матча
    public static class StartMatchPacket {
        private final UUID opponentUUID;
        private final com.bmfalkye.game.MatchConfig config;
        
        public StartMatchPacket(UUID opponentUUID, com.bmfalkye.game.MatchConfig config) {
            this.opponentUUID = opponentUUID;
            this.config = config;
        }
        
        public static void encode(StartMatchPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUUID(msg.opponentUUID);
            // Сериализуем MatchConfig
            buffer.writeInt(msg.config.getBetAmount());
            buffer.writeInt(msg.config.getDifficulty().ordinal());
            buffer.writeBoolean(msg.config.isAllowLeader());
            buffer.writeBoolean(msg.config.isAllowWeather());
            buffer.writeInt(msg.config.getMaxRounds());
            buffer.writeInt(msg.config.getTurnTimeLimit());
            buffer.writeInt(msg.config.getGameMode().ordinal()); // Режим игры
        }
        
        public static StartMatchPacket decode(FriendlyByteBuf buffer) {
            UUID opponentUUID = buffer.readUUID();
            int betAmount = buffer.readInt();
            int difficultyOrdinal = buffer.readInt();
            com.bmfalkye.game.MatchConfig config = new com.bmfalkye.game.MatchConfig();
            config.setBetAmount(betAmount);
            if (difficultyOrdinal >= 0 && difficultyOrdinal < com.bmfalkye.game.MatchConfig.Difficulty.values().length) {
                config.setDifficulty(com.bmfalkye.game.MatchConfig.Difficulty.values()[difficultyOrdinal]);
            }
            // Читаем новые поля (с проверкой на совместимость со старыми версиями)
            if (buffer.readableBytes() >= 10) { // Минимум для новых полей
                config.setAllowLeader(buffer.readBoolean());
                config.setAllowWeather(buffer.readBoolean());
                config.setMaxRounds(buffer.readInt());
                config.setTurnTimeLimit(buffer.readInt());
                // Читаем режим игры (если доступен)
                if (buffer.readableBytes() >= 4) {
                    int gameModeOrdinal = buffer.readInt();
                    if (gameModeOrdinal >= 0 && gameModeOrdinal < com.bmfalkye.settings.GameModeSettings.GameMode.values().length) {
                        config.setGameMode(com.bmfalkye.settings.GameModeSettings.GameMode.values()[gameModeOrdinal]);
                    }
                }
            }
            return new StartMatchPacket(opponentUUID, config);
        }
        
        public static void handle(StartMatchPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.util.ModLogger.logNetwork("StartMatchPacket received", 
                        "player", player.getName().getString(),
                        "opponentUUID", msg.opponentUUID.toString());
                    
                    // Проверяем, есть ли ожидающий вызов (для игры между игроками)
                    java.util.Map<UUID, com.bmfalkye.game.GameManager.PendingChallenge> pendingChallenges = 
                        com.bmfalkye.game.GameManager.getPendingChallenges();
                    com.bmfalkye.game.GameManager.PendingChallenge challenge = pendingChallenges.get(player.getUUID());
                    
                    if (challenge != null && msg.opponentUUID.equals(challenge.getChallengerUUID())) {
                        // Это принятие вызова от target - начинаем игру между игроками
                        ServerPlayer challenger = player.server.getPlayerList().getPlayer(challenge.getChallengerUUID());
                        if (challenger != null && challenger.isAlive()) {
                            // Удаляем вызов
                            pendingChallenges.remove(player.getUUID());
                            
                            // Начинаем игру между игроками (target, challenger, config)
                            // Важно: передаем player (target) как первого игрока, challenger как второго
                            com.bmfalkye.game.GameManager.startPlayerMatch(player, challenger, msg.config);
                        } else {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.bm_falkye.challenger_offline"));
                            pendingChallenges.remove(player.getUUID());
                        }
                    } else {
                        // Стандартная логика для NPC или если вызова нет
                        com.bmfalkye.game.GameManager.startMatchWithConfig(player, msg.opponentUUID, msg.config);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
        
        public UUID getOpponentUUID() { return opponentUUID; }
        public com.bmfalkye.game.MatchConfig getConfig() { return config; }
    }
    
    // Пакет для запроса коллекции карт
    public static class RequestCardCollectionPacket {
        public static void encode(RequestCardCollectionPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static RequestCardCollectionPacket decode(FriendlyByteBuf buffer) {
            return new RequestCardCollectionPacket();
        }
        
        public static void handle(RequestCardCollectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                    java.util.List<com.bmfalkye.cards.Card> cards = collection.getCards(player);
                    java.util.List<String> cardIds = cards.stream()
                        .map(com.bmfalkye.cards.Card::getId)
                        .collect(java.util.stream.Collectors.toList());
                    
                    // Отправляем коллекцию обратно на клиент
                    INSTANCE.sendTo(new SendCardCollectionPacket(cardIds),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // Пакет для отправки коллекции карт на клиент
    public static class SendCardCollectionPacket {
        private final List<String> cardIds;
        
        public SendCardCollectionPacket(List<String> cardIds) {
            this.cardIds = cardIds;
        }
        
        public static void encode(SendCardCollectionPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.cardIds.size());
            for (String cardId : msg.cardIds) {
                String safeId = cardId != null ? cardId : "";
                if (safeId.length() > 32767) {
                    safeId = safeId.substring(0, 32767);
                }
                buffer.writeUtf(safeId);
            }
        }
        
        public static SendCardCollectionPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            List<String> cardIds = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                cardIds.add(buffer.readUtf());
            }
            return new SendCardCollectionPacket(cardIds);
        }
        
        public static void handle(SendCardCollectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleCardCollection(msg.cardIds);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public List<String> getCardIds() { return cardIds; }
    }
    
    /**
     * Пакет для открытия энциклопедии карт
     */
    public static class OpenEncyclopediaPacket {
        public OpenEncyclopediaPacket() {}
        
        public static void encode(OpenEncyclopediaPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static OpenEncyclopediaPacket decode(FriendlyByteBuf buffer) {
            return new OpenEncyclopediaPacket();
        }
        
        public static void handle(OpenEncyclopediaPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Проверяем, с какой стороны пришел пакет
                if (ctx.get().getDirection().getReceptionSide().isServer()) {
                    // Пакет пришел на сервер - отправляем обратно клиенту, который его запросил
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    // Отправляем пакет обратно клиенту, который его запросил
                    INSTANCE.sendTo(new OpenEncyclopediaPacket(),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                } else {
                    // Пакет пришел на клиент - открываем экран
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            // Открываем энциклопедию с родительским экраном
                            net.minecraft.client.gui.screens.Screen parent = 
                                com.bmfalkye.client.ClientPacketHandler.getMainMenuParent();
                            net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.bmfalkye.client.CardEncyclopediaScreen(parent));
                        });
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для открытия редактора колод
     */
    public static class OpenDeckEditorPacket {
        public OpenDeckEditorPacket() {}
        
        public static void encode(OpenDeckEditorPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static OpenDeckEditorPacket decode(FriendlyByteBuf buffer) {
            return new OpenDeckEditorPacket();
        }
        
        public static void handle(OpenDeckEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Проверяем, с какой стороны пришел пакет
                if (ctx.get().getDirection().getReceptionSide().isServer()) {
                    // Пакет пришел на сервер - отправляем обратно клиенту, который его запросил
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        INSTANCE.sendTo(new OpenDeckEditorPacket(),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                } else {
                    // Пакет пришел на клиент - открываем экран
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            // Открываем редактор колод с родительским экраном
                            net.minecraft.client.gui.screens.Screen parent = 
                                com.bmfalkye.client.ClientPacketHandler.getMainMenuParent();
                            net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.bmfalkye.client.DeckEditorScreen(parent));
                        });
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для сохранения колоды
     */
    public static class SaveDeckPacket {
        private final String deckName;
        private final java.util.List<String> cardIds;
        private final String leaderId;
        private final int slotIndex;
        
        public SaveDeckPacket(String deckName, java.util.List<String> cardIds, String leaderId, int slotIndex) {
            this.deckName = deckName;
            this.cardIds = cardIds;
            this.leaderId = leaderId;
            this.slotIndex = slotIndex;
        }
        
        public static void encode(SaveDeckPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.deckName);
            buffer.writeInt(msg.cardIds.size());
            for (String cardId : msg.cardIds) {
                buffer.writeUtf(cardId);
            }
            buffer.writeUtf(msg.leaderId != null ? msg.leaderId : "");
            buffer.writeInt(msg.slotIndex);
        }
        
        public static SaveDeckPacket decode(FriendlyByteBuf buffer) {
            String deckName = buffer.readUtf();
            int cardCount = buffer.readInt();
            java.util.List<String> cardIds = new java.util.ArrayList<>();
            for (int i = 0; i < cardCount; i++) {
                cardIds.add(buffer.readUtf());
            }
            String leaderId = buffer.readUtf();
            if (leaderId.isEmpty()) leaderId = null;
            int slotIndex = buffer.readInt();
            return new SaveDeckPacket(deckName, cardIds, leaderId, slotIndex);
        }
        
        public static void handle(SaveDeckPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerDeckManager manager = 
                        com.bmfalkye.storage.PlayerDeckManager.get(serverLevel);
                    
                    // Проверяем, что все карты есть в коллекции игрока
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                    java.util.Set<String> playerCards = collection.getPlayerCollection(player);
                    
                    java.util.List<String> validCardIds = new java.util.ArrayList<>();
                    for (String cardId : msg.cardIds) {
                        if (playerCards.contains(cardId)) {
                            validCardIds.add(cardId);
                        }
                    }
                    
                    // Создаём данные колоды
                    com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData = 
                        new com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData();
                    deckData.setDeckName(msg.deckName);
                    deckData.setCardIds(validCardIds);
                    deckData.setLeaderId(msg.leaderId);
                    
                    // Сохраняем колоду
                    java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks = 
                        manager.getPlayerDecks(player);
                    
                    if (msg.slotIndex >= 0 && msg.slotIndex < decks.size()) {
                        // Обновляем существующую колоду
                        decks.set(msg.slotIndex, deckData);
                    } else if (msg.slotIndex == -1) {
                        // Добавляем новую колоду
                        if (!manager.addDeck(player, deckData)) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cНе удалось сохранить колоду: достигнут лимит колод (10)"));
                            return;
                        }
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНеверный индекс слота колоды"));
                        return;
                    }
                    
                    manager.setDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aКолода сохранена: " + msg.deckName));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для загрузки колоды
     */
    public static class LoadDeckPacket {
        private final int slotIndex;
        
        public LoadDeckPacket(int slotIndex) {
            this.slotIndex = slotIndex;
        }
        
        public static void encode(LoadDeckPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.slotIndex);
        }
        
        public static LoadDeckPacket decode(FriendlyByteBuf buffer) {
            return new LoadDeckPacket(buffer.readInt());
        }
        
        public static void handle(LoadDeckPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerDeckManager manager = 
                        com.bmfalkye.storage.PlayerDeckManager.get(serverLevel);
                    
                    com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData = 
                        manager.getDeck(player, msg.slotIndex);
                    
                    if (deckData == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cКолода не найдена в слоте " + msg.slotIndex));
                        return;
                    }
                    
                    // Отправляем данные колоды клиенту
                    INSTANCE.sendTo(new SendDeckDataPacket(deckData), 
                        player.connection.connection, 
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса списка колод
     */
    public static class RequestDecksPacket {
        public RequestDecksPacket() {}
        
        public static void encode(RequestDecksPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static RequestDecksPacket decode(FriendlyByteBuf buffer) {
            return new RequestDecksPacket();
        }
        
        public static void handle(RequestDecksPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.PlayerDeckManager manager = 
                        com.bmfalkye.storage.PlayerDeckManager.get(serverLevel);
                    
                    java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks = 
                        manager.getPlayerDecks(player);
                    
                    // Отправляем список колод клиенту
                    INSTANCE.sendTo(new SendDecksPacket(decks), 
                        player.connection.connection, 
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки списка колод клиенту
     */
    public static class SendDecksPacket {
        private final java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks;
        
        public SendDecksPacket(java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks) {
            this.decks = decks;
        }
        
        public static void encode(SendDecksPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.decks.size());
            for (com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deck : msg.decks) {
                buffer.writeUtf(deck.getDeckName());
                buffer.writeInt(deck.getCardIds().size());
                for (String cardId : deck.getCardIds()) {
                    buffer.writeUtf(cardId);
                }
                buffer.writeUtf(deck.getLeaderId() != null ? deck.getLeaderId() : "");
            }
        }
        
        public static SendDecksPacket decode(FriendlyByteBuf buffer) {
            int deckCount = buffer.readInt();
            java.util.List<com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData> decks = 
                new java.util.ArrayList<>();
            
            for (int i = 0; i < deckCount; i++) {
                String deckName = buffer.readUtf();
                int cardCount = buffer.readInt();
                java.util.List<String> cardIds = new java.util.ArrayList<>();
                for (int j = 0; j < cardCount; j++) {
                    cardIds.add(buffer.readUtf());
                }
                String leaderId = buffer.readUtf();
                if (leaderId.isEmpty()) leaderId = null;
                
                com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData = 
                    new com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData();
                deckData.setDeckName(deckName);
                deckData.setCardIds(cardIds);
                deckData.setLeaderId(leaderId);
                decks.add(deckData);
            }
            
            return new SendDecksPacket(decks);
        }
        
        public static void handle(SendDecksPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleSendDecksPacket(msg.decks);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки данных одной колоды клиенту
     */
    public static class SendDeckDataPacket {
        private final com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData;
        
        public SendDeckDataPacket(com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData) {
            this.deckData = deckData;
        }
        
        public static void encode(SendDeckDataPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.deckData.getDeckName());
            buffer.writeInt(msg.deckData.getCardIds().size());
            for (String cardId : msg.deckData.getCardIds()) {
                buffer.writeUtf(cardId);
            }
            buffer.writeUtf(msg.deckData.getLeaderId() != null ? msg.deckData.getLeaderId() : "");
        }
        
        public static SendDeckDataPacket decode(FriendlyByteBuf buffer) {
            String deckName = buffer.readUtf();
            int cardCount = buffer.readInt();
            java.util.List<String> cardIds = new java.util.ArrayList<>();
            for (int i = 0; i < cardCount; i++) {
                cardIds.add(buffer.readUtf());
            }
            String leaderId = buffer.readUtf();
            if (leaderId.isEmpty()) leaderId = null;
            
            com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData deckData = 
                new com.bmfalkye.storage.PlayerDeckStorage.PlayerDeckData();
            deckData.setDeckName(deckName);
            deckData.setCardIds(cardIds);
            deckData.setLeaderId(leaderId);
            
            return new SendDeckDataPacket(deckData);
        }
        
        public static void handle(SendDeckDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleSendDeckDataPacket(msg.deckData);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для добавления сообщения в лог действий
     */
    public static class ActionLogPacket {
        private final String message;
        
        public ActionLogPacket(String message) {
            this.message = message;
        }
        
        public static void encode(ActionLogPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.message);
        }
        
        public static ActionLogPacket decode(FriendlyByteBuf buffer) {
            return new ActionLogPacket(buffer.readUtf());
        }
        
        public static void handle(ActionLogPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.FalkyeGameScreen gameScreen) {
                            gameScreen.addActionLog(msg.message);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для открытия админ-панели
     */
    public static class OpenAdminPanelPacket {
        public OpenAdminPanelPacket() {}
        
        public static void encode(OpenAdminPanelPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static OpenAdminPanelPacket decode(FriendlyByteBuf buffer) {
            return new OpenAdminPanelPacket();
        }
        
        public static void handle(OpenAdminPanelPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.AdminPanelScreen());
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для выдачи карты игроку (админ)
     */
    public static class AdminGiveCardPacket {
        private final String playerName;
        private final String cardId;
        
        public AdminGiveCardPacket(String playerName, String cardId) {
            this.playerName = playerName;
            this.cardId = cardId;
        }
        
        public static void encode(AdminGiveCardPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
            buffer.writeUtf(msg.cardId);
        }
        
        public static AdminGiveCardPacket decode(FriendlyByteBuf buffer) {
            return new AdminGiveCardPacket(buffer.readUtf(), buffer.readUtf());
        }
        
        public static void handle(AdminGiveCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.cards.Card card = com.bmfalkye.cards.CardRegistry.getCard(msg.cardId);
                        if (card != null) {
                            com.bmfalkye.storage.PlayerProgressStorage storage = 
                                com.bmfalkye.storage.PlayerProgressStorage.get(
                                    (net.minecraft.server.level.ServerLevel) target.level());
                            com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                            progress.unlockCard(msg.cardId);
                            storage.setPlayerProgress(target, progress);
                            target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "command.bm_falkye.card_received", card.getName()));
                            admin.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "command.bm_falkye.card_given", card.getName(), 1), true);
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для выдачи опыта игроку (админ)
     */
    public static class AdminGiveXPPacket {
        private final String playerName;
        private final int amount;
        
        public AdminGiveXPPacket(String playerName, int amount) {
            this.playerName = playerName;
            this.amount = amount;
        }
        
        public static void encode(AdminGiveXPPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
            buffer.writeInt(msg.amount);
        }
        
        public static AdminGiveXPPacket decode(FriendlyByteBuf buffer) {
            return new AdminGiveXPPacket(buffer.readUtf(), buffer.readInt());
        }
        
        public static void handle(AdminGiveXPPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        progress.addExperience(msg.amount);
                        storage.setPlayerProgress(target, progress);
                        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.xp_received", msg.amount));
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.xp_given", msg.amount, 1), true);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для установки уровня игроку (админ)
     */
    public static class AdminSetLevelPacket {
        private final String playerName;
        private final int level;
        
        public AdminSetLevelPacket(String playerName, int level) {
            this.playerName = playerName;
            this.level = level;
        }
        
        public static void encode(AdminSetLevelPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
            buffer.writeInt(msg.level);
        }
        
        public static AdminSetLevelPacket decode(FriendlyByteBuf buffer) {
            return new AdminSetLevelPacket(buffer.readUtf(), buffer.readInt());
        }
        
        public static void handle(AdminSetLevelPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        progress.setLevel(msg.level);
                        storage.setPlayerProgress(target, progress);
                        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.level_set", msg.level));
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.level_set_success", msg.level, 1), true);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для разблокировки всех карт игроку (админ)
     */
    public static class AdminUnlockAllPacket {
        private final String playerName;
        
        public AdminUnlockAllPacket(String playerName) {
            this.playerName = playerName;
        }
        
        public static void encode(AdminUnlockAllPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
        }
        
        public static AdminUnlockAllPacket decode(FriendlyByteBuf buffer) {
            return new AdminUnlockAllPacket(buffer.readUtf());
        }
        
        public static void handle(AdminUnlockAllPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        for (com.bmfalkye.cards.Card card : com.bmfalkye.cards.CardRegistry.getAllCards()) {
                            progress.unlockCard(card.getId());
                        }
                        storage.setPlayerProgress(target, progress);
                        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.all_unlocked"));
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "command.bm_falkye.all_unlocked_success", 1), true);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса статистики игрока (админ)
     */
    public static class AdminShowStatsPacket {
        private final String playerName;
        
        public AdminShowStatsPacket(String playerName) {
            this.playerName = playerName;
        }
        
        public static void encode(AdminShowStatsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
        }
        
        public static AdminShowStatsPacket decode(FriendlyByteBuf buffer) {
            return new AdminShowStatsPacket(buffer.readUtf());
        }
        
        public static void handle(AdminShowStatsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        
                        StringBuilder stats = new StringBuilder();
                        stats.append("=== Статистика игрока ").append(target.getName().getString()).append(" ===\n");
                        stats.append("Уровень: ").append(progress.getLevel()).append("\n");
                        stats.append("Опыт: ").append(progress.getExperience()).append("\n");
                        stats.append("Игр сыграно: ").append(progress.getTotalGamesPlayed()).append("\n");
                        stats.append("Побед: ").append(progress.getTotalGamesWon()).append("\n");
                        stats.append("Поражений: ").append(progress.getTotalGamesLost()).append("\n");
                        stats.append("Карт разблокировано: ").append(progress.getUnlockedCards().size()).append("\n");
                        stats.append("Достижений: ").append(progress.getAchievements().size());
                        
                        INSTANCE.sendTo(
                            new AdminStatsResponsePacket(stats.toString()),
                            admin.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для ответа со статистикой игрока (админ)
     */
    public static class AdminStatsResponsePacket {
        private final String stats;
        
        public AdminStatsResponsePacket(String stats) {
            this.stats = stats;
        }
        
        public static void encode(AdminStatsResponsePacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.stats);
        }
        
        public static AdminStatsResponsePacket decode(FriendlyByteBuf buffer) {
            return new AdminStatsResponsePacket(buffer.readUtf());
        }
        
        public static void handle(AdminStatsResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.AdminPanelScreen adminScreen) {
                            adminScreen.updatePlayerStats(msg.stats);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для удаления реплея (админ)
     */
    public static class AdminDeleteReplayPacket {
        private final String replayId;
        
        public AdminDeleteReplayPacket(String replayId) {
            this.replayId = replayId;
        }
        
        public static void encode(AdminDeleteReplayPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.replayId);
        }
        
        public static AdminDeleteReplayPacket decode(FriendlyByteBuf buffer) {
            return new AdminDeleteReplayPacket(buffer.readUtf());
        }
        
        public static void handle(AdminDeleteReplayPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    boolean deleted = com.bmfalkye.replay.ReplaySystem.deleteReplay(msg.replayId);
                    if (deleted) {
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aРеплей удалён"));
                    } else {
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cРеплей не найден"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для удаления всех реплеев (админ)
     */
    public static class AdminDeleteAllReplaysPacket {
        public AdminDeleteAllReplaysPacket() {}
        
        public static void encode(AdminDeleteAllReplaysPacket msg, FriendlyByteBuf buffer) {}
        
        public static AdminDeleteAllReplaysPacket decode(FriendlyByteBuf buffer) {
            return new AdminDeleteAllReplaysPacket();
        }
        
        public static void handle(AdminDeleteAllReplaysPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayInfo> replays = 
                        com.bmfalkye.replay.ReplaySystem.getAllReplays();
                    int count = 0;
                    for (com.bmfalkye.replay.ReplaySystem.ReplayInfo info : replays) {
                        if (com.bmfalkye.replay.ReplaySystem.deleteReplay(info.getReplayId())) {
                            count++;
                        }
                    }
                    admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aУдалено реплеев: " + count));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для выдачи монет игроку (админ)
     */
    public static class AdminGiveCoinsPacket {
        private final String playerName;
        private final int amount;
        
        public AdminGiveCoinsPacket(String playerName, int amount) {
            this.playerName = playerName;
            this.amount = amount;
        }
        
        public static void encode(AdminGiveCoinsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
            buffer.writeInt(msg.amount);
        }
        
        public static AdminGiveCoinsPacket decode(FriendlyByteBuf buffer) {
            return new AdminGiveCoinsPacket(buffer.readUtf(), buffer.readInt());
        }
        
        public static void handle(AdminGiveCoinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        // Используем систему монет из PlayerProgress
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        progress.addCoins(msg.amount);
                        storage.setPlayerProgress(target, progress);
                        target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aВы получили " + msg.amount + " монет!"));
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aИгроку " + msg.playerName + " выдано " + msg.amount + " монет"), true);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для выдачи достижения игроку (админ)
     */
    public static class AdminGiveAchievementPacket {
        private final String playerName;
        private final String achievementId;
        
        public AdminGiveAchievementPacket(String playerName, String achievementId) {
            this.playerName = playerName;
            this.achievementId = achievementId;
        }
        
        public static void encode(AdminGiveAchievementPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.playerName);
            buffer.writeUtf(msg.achievementId);
        }
        
        public static AdminGiveAchievementPacket decode(FriendlyByteBuf buffer) {
            return new AdminGiveAchievementPacket(buffer.readUtf(), buffer.readUtf());
        }
        
        public static void handle(AdminGiveAchievementPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer admin = ctx.get().getSender();
                if (admin != null && admin.hasPermissions(2)) {
                    ServerPlayer target = admin.getServer().getPlayerList().getPlayerByName(msg.playerName);
                    if (target != null) {
                        com.bmfalkye.storage.PlayerProgressStorage storage = 
                            com.bmfalkye.storage.PlayerProgressStorage.get(
                                (net.minecraft.server.level.ServerLevel) target.level());
                        com.bmfalkye.player.PlayerProgress progress = storage.getPlayerProgress(target);
                        progress.unlockAchievement(msg.achievementId);
                        storage.setPlayerProgress(target, progress);
                        target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aВы получили достижение: " + msg.achievementId));
                        admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aИгроку " + msg.playerName + " выдано достижение: " + msg.achievementId), true);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отказа от вызова (от target)
     */
    public static class DenyChallengePacket {
        public DenyChallengePacket() {}
        
        public static void encode(DenyChallengePacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static DenyChallengePacket decode(FriendlyByteBuf buffer) {
            return new DenyChallengePacket();
        }
        
        public static void handle(DenyChallengePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    // Вызываем метод отказа от вызова
                    com.bmfalkye.game.GameManager.denyChallenge(player);
                    
                    // Закрываем экран у обоих игроков
                    // Находим challenger через pendingChallenges
                    java.util.Map<UUID, com.bmfalkye.game.GameManager.PendingChallenge> pendingChallenges = 
                        com.bmfalkye.game.GameManager.getPendingChallenges();
                    com.bmfalkye.game.GameManager.PendingChallenge challenge = pendingChallenges.get(player.getUUID());
                    
                    if (challenge != null) {
                        ServerPlayer challenger = player.server.getPlayerList().getPlayer(challenge.getChallengerUUID());
                        if (challenger != null && challenger.isAlive()) {
                            // Закрываем экран у challenger (отправляем пакет закрытия)
                            INSTANCE.sendTo(new ClosePreMatchScreenPacket(),
                                challenger.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                        }
                    }
                    
                    // Закрываем экран у target (отправляем пакет закрытия)
                    INSTANCE.sendTo(new ClosePreMatchScreenPacket(),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отмены вызова (от challenger)
     */
    public static class CancelChallengePacket {
        public CancelChallengePacket() {}
        
        public static void encode(CancelChallengePacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static CancelChallengePacket decode(FriendlyByteBuf buffer) {
            return new CancelChallengePacket();
        }
        
        public static void handle(CancelChallengePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    // Находим вызов, где player является challenger
                    java.util.Map<UUID, com.bmfalkye.game.GameManager.PendingChallenge> pendingChallenges = 
                        com.bmfalkye.game.GameManager.getPendingChallenges();
                    
                    // Ищем вызов, где player - challenger
                    com.bmfalkye.game.GameManager.PendingChallenge challengeToRemove = null;
                    UUID targetUUID = null;
                    
                    for (java.util.Map.Entry<UUID, com.bmfalkye.game.GameManager.PendingChallenge> entry : pendingChallenges.entrySet()) {
                        if (entry.getValue().getChallengerUUID().equals(player.getUUID())) {
                            challengeToRemove = entry.getValue();
                            targetUUID = entry.getKey();
                            break;
                        }
                    }
                    
                    if (challengeToRemove != null && targetUUID != null) {
                        // Удаляем вызов
                        pendingChallenges.remove(targetUUID);
                        
                        // Уведомляем target об отмене вызова
                        ServerPlayer target = player.server.getPlayerList().getPlayer(targetUUID);
                        if (target != null && target.isAlive()) {
                            target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.bm_falkye.challenge_cancelled", player.getName()));
                            
                            // Закрываем экран у target
                            INSTANCE.sendTo(new ClosePreMatchScreenPacket(),
                                target.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                        }
                        
                        // Закрываем экран у challenger
                        INSTANCE.sendTo(new ClosePreMatchScreenPacket(),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для закрытия экрана настройки матча
     */
    public static class ClosePreMatchScreenPacket {
        public ClosePreMatchScreenPacket() {}
        
        public static void encode(ClosePreMatchScreenPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static ClosePreMatchScreenPacket decode(FriendlyByteBuf buffer) {
            return new ClosePreMatchScreenPacket();
        }
        
        public static void handle(ClosePreMatchScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.PreMatchScreen) {
                            net.minecraft.client.Minecraft.getInstance().setScreen(null);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // ========== Пакеты для синхронизации систем ==========
    
    /**
     * Пакет запроса статистики
     */
    public static class RequestStatisticsPacket {
        public RequestStatisticsPacket() {}
        
        public static void encode(RequestStatisticsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestStatisticsPacket decode(FriendlyByteBuf buffer) {
            return new RequestStatisticsPacket();
        }
        
        public static void handle(RequestStatisticsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.statistics.StatisticsSystem.PlayerStatistics stats = 
                        com.bmfalkye.statistics.StatisticsSystem.getPlayerStatistics(player);
                    
                    // Сериализуем статистику
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(stats.getTotalGames());
                    data.writeInt(stats.getWins());
                    data.writeInt(stats.getLosses());
                    data.writeDouble(stats.getWinRate());
                    data.writeInt(stats.getRoundsWon());
                    data.writeInt(stats.getRoundsLost());
                    data.writeDouble(stats.getRoundWinRate());
                    data.writeInt(stats.getRating());
                    data.writeInt(stats.getRank().ordinal());
                    data.writeInt(stats.getLevel());
                    data.writeUtf(stats.getMostPlayedCard());
                    
                    // Фракции
                    data.writeInt(stats.getFactionWins().size());
                    for (java.util.Map.Entry<String, Integer> entry : stats.getFactionWins().entrySet()) {
                        data.writeUtf(entry.getKey());
                        data.writeInt(entry.getValue());
                    }
                    
                    INSTANCE.sendTo(new SendStatisticsPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки статистики
     */
    public static class SendStatisticsPacket {
        private final FriendlyByteBuf data;
        
        public SendStatisticsPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendStatisticsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendStatisticsPacket decode(FriendlyByteBuf buffer) {
            return new SendStatisticsPacket(buffer);
        }
        
        public static void handle(SendStatisticsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.StatisticsScreen screen) {
                            screen.updateStatistics(msg.data);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса турниров
     */
    public static class RequestTournamentsPacket {
        public RequestTournamentsPacket() {}
        
        public static void encode(RequestTournamentsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestTournamentsPacket decode(FriendlyByteBuf buffer) {
            return new RequestTournamentsPacket();
        }
        
        public static void handle(RequestTournamentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    java.util.List<com.bmfalkye.tournament.TournamentSystem.Tournament> tournaments = 
                        com.bmfalkye.tournament.TournamentSystem.getActiveTournaments();
                    
                    // Также получаем историю турниров игрока
                    java.util.List<com.bmfalkye.tournament.TournamentSystem.TournamentHistory> playerHistory = 
                        com.bmfalkye.tournament.TournamentSystem.getPlayerTournamentHistory(player.getUUID());
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(tournaments.size());
                    for (com.bmfalkye.tournament.TournamentSystem.Tournament t : tournaments) {
                        data.writeUtf(t.getName());
                        data.writeUtf(t.getId());
                        data.writeUtf(t.getType().getDisplayName());
                        data.writeInt(t.getParticipants().size());
                        data.writeInt(t.getMaxParticipants());
                        data.writeInt(t.getEntryFee());
                        data.writeInt(t.getPrizePool());
                        data.writeInt(t.getCurrentRound());
                        data.writeBoolean(t.isStarted());
                        data.writeBoolean(t.isFinished());
                        // Используем реальное время турнира
                        long startTime = t.getStartTime();
                        long endTime = t.getEndTime();
                        data.writeLong(startTime);
                        data.writeLong(endTime);
                    }
                    
                    // Добавляем историю турниров
                    data.writeInt(playerHistory.size());
                    for (com.bmfalkye.tournament.TournamentSystem.TournamentHistory history : playerHistory) {
                        data.writeUtf(history.getTournamentName());
                        data.writeUtf(history.getType().getDisplayName());
                        data.writeInt(history.getParticipantCount());
                        // Проверяем, какое место занял игрок
                        int place = -1;
                        if (history.getFirstPlace() != null && history.getFirstPlace().equals(player.getUUID())) {
                            place = 1;
                        } else if (history.getSecondPlace() != null && history.getSecondPlace().equals(player.getUUID())) {
                            place = 2;
                        } else if (history.getThirdPlace() != null && history.getThirdPlace().equals(player.getUUID())) {
                            place = 3;
                        }
                        data.writeInt(place);
                        data.writeLong(history.getEndTime());
                    }
                    
                    INSTANCE.sendTo(new SendTournamentsPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки турниров
     */
    public static class SendTournamentsPacket {
        private final FriendlyByteBuf data;
        
        public SendTournamentsPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendTournamentsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendTournamentsPacket decode(FriendlyByteBuf buffer) {
            return new SendTournamentsPacket(buffer);
        }
        
        public static void handle(SendTournamentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.TournamentScreen screen) {
                            screen.updateTournaments(msg.data);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса сезона
     */
    public static class RequestSeasonPacket {
        public RequestSeasonPacket() {}
        
        public static void encode(RequestSeasonPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestSeasonPacket decode(FriendlyByteBuf buffer) {
            return new RequestSeasonPacket();
        }
        
        public static void handle(RequestSeasonPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.bmfalkye.season.SeasonSystem.initializeSeason(serverLevel);
                    }
                    
                    com.bmfalkye.season.SeasonSystem.SeasonInfo info = 
                        com.bmfalkye.season.SeasonSystem.getPlayerSeasonInfo(player);
                    
                    // Получаем рейтинговую таблицу текущего сезона
                    java.util.List<com.bmfalkye.season.SeasonSystem.SeasonRankingEntry> ranking = 
                        com.bmfalkye.season.SeasonSystem.getCurrentSeasonRanking();
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(info.getSeasonNumber());
                    data.writeInt(info.getSeasonLevel());
                    data.writeInt(info.getSeasonXP());
                    data.writeInt(info.getXPForNextLevel());
                    data.writeInt((int) info.getDaysRemaining());
                    
                    // Добавляем рейтинговую таблицу (топ-10)
                    int topCount = Math.min(10, ranking.size());
                    data.writeInt(topCount);
                    int playerRank = -1;
                    for (int i = 0; i < topCount; i++) {
                        com.bmfalkye.season.SeasonSystem.SeasonRankingEntry entry = ranking.get(i);
                        if (entry.getPlayerUUID().equals(player.getUUID())) {
                            playerRank = i + 1;
                        }
                        net.minecraft.server.level.ServerPlayer rankPlayer = 
                            player.server.getPlayerList().getPlayer(entry.getPlayerUUID());
                        String playerName = rankPlayer != null ? rankPlayer.getName().getString() : "Неизвестный";
                        data.writeUtf(playerName);
                        data.writeInt(entry.getLevel());
                        data.writeInt(entry.getTotalXP());
                    }
                    data.writeInt(playerRank); // Место игрока в рейтинге
                    
                    INSTANCE.sendTo(new SendSeasonPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки сезона
     */
    public static class SendSeasonPacket {
        private final FriendlyByteBuf data;
        
        public SendSeasonPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendSeasonPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendSeasonPacket decode(FriendlyByteBuf buffer) {
            return new SendSeasonPacket(buffer);
        }
        
        public static void handle(SendSeasonPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.SeasonScreen screen) {
                            screen.updateSeason(msg.data);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса ежедневных наград
     */
    public static class RequestDailyRewardsPacket {
        public RequestDailyRewardsPacket() {}
        
        public static void encode(RequestDailyRewardsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestDailyRewardsPacket decode(FriendlyByteBuf buffer) {
            return new RequestDailyRewardsPacket();
        }
        
        public static void handle(RequestDailyRewardsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.daily.DailyRewardSystem.DailyRewardInfo rewardInfo = 
                        com.bmfalkye.daily.DailyRewardSystem.getDailyRewardInfo(player);
                    java.util.List<com.bmfalkye.daily.DailyRewardSystem.DailyQuest> quests = 
                        com.bmfalkye.daily.DailyRewardSystem.getPlayerQuests(player);
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(rewardInfo.getDay());
                    data.writeBoolean(rewardInfo.isClaimed());
                    data.writeInt(rewardInfo.getStreakDays());
                    
                    data.writeInt(quests.size());
                    for (com.bmfalkye.daily.DailyRewardSystem.DailyQuest quest : quests) {
                        data.writeUtf(quest.getDescription());
                        data.writeInt(quest.getTarget());
                        data.writeInt(quest.getProgress(player));
                        // Награды для заданий (базовые значения)
                        int rewardXP = quest.getTarget() * 10;
                        int rewardCoins = quest.getType() == com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_GAMES ? 50 : 0;
                        data.writeInt(rewardXP);
                        data.writeInt(rewardCoins);
                    }
                    
                    INSTANCE.sendTo(new SendDailyRewardsPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки ежедневных наград
     */
    public static class SendDailyRewardsPacket {
        private final FriendlyByteBuf data;
        
        public SendDailyRewardsPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendDailyRewardsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendDailyRewardsPacket decode(FriendlyByteBuf buffer) {
            return new SendDailyRewardsPacket(buffer);
        }
        
        public static void handle(SendDailyRewardsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.DailyRewardsScreen screen) {
                            screen.updateDailyRewards(msg.data);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса реплеев
     */
    public static class RequestReplaysPacket {
        public RequestReplaysPacket() {}
        
        public static void encode(RequestReplaysPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestReplaysPacket decode(FriendlyByteBuf buffer) {
            return new RequestReplaysPacket();
        }
        
        public static void handle(RequestReplaysPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayInfo> replays = 
                        com.bmfalkye.replay.ReplaySystem.getAllReplays();
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(replays.size());
                    for (com.bmfalkye.replay.ReplaySystem.ReplayInfo info : replays) {
                        com.bmfalkye.replay.ReplaySystem.GameReplay replay = info.getReplay();
                        data.writeUtf(info.getReplayId()); // Добавляем replayId
                        data.writeUtf(replay.getPlayer1Name());
                        data.writeUtf(replay.getPlayer2Name());
                        data.writeUtf(replay.getWinnerName());
                        data.writeInt(replay.getRoundsWon1());
                        data.writeInt(replay.getRoundsWon2());
                        data.writeLong(info.getTimestamp());
                    }
                    
                    INSTANCE.sendTo(new SendReplaysPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки реплеев
     */
    public static class SendReplaysPacket {
        private final FriendlyByteBuf data;
        
        public SendReplaysPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendReplaysPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendReplaysPacket decode(FriendlyByteBuf buffer) {
            return new SendReplaysPacket(buffer);
        }
        
        public static void handle(SendReplaysPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        // Обновляем ReplayScreen, если он открыт
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.ReplayScreen screen) {
                            screen.updateReplays(msg.data);
                        }
                        // Также обновляем AdminPanelScreen, если он открыт и на вкладке реплеев
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.AdminPanelScreen adminScreen) {
                            // Создаём копию данных для админ-панели
                            net.minecraft.network.FriendlyByteBuf dataCopy = new net.minecraft.network.FriendlyByteBuf(
                                io.netty.buffer.Unpooled.copiedBuffer(msg.data));
                            adminScreen.updateReplaysList(dataCopy);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса конкретного реплея
     */
    public static class RequestReplayPacket {
        private final String replayId;
        
        public RequestReplayPacket(String replayId) {
            this.replayId = replayId;
        }
        
        public static void encode(RequestReplayPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.replayId);
        }
        
        public static RequestReplayPacket decode(FriendlyByteBuf buffer) {
            return new RequestReplayPacket(buffer.readUtf());
        }
        
        public static void handle(RequestReplayPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.replay.ReplaySystem.GameReplay replay = 
                        com.bmfalkye.replay.ReplaySystem.getReplay(msg.replayId);
                    
                    if (replay == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cРеплей не найден!"));
                        return;
                    }
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeUtf(msg.replayId);
                    data.writeUtf(replay.getPlayer1Name());
                    data.writeUtf(replay.getPlayer2Name());
                    data.writeUtf(replay.getWinnerName());
                    data.writeInt(replay.getRoundsWon1());
                    data.writeInt(replay.getRoundsWon2());
                    data.writeLong(replay.getTimestamp());
                    data.writeInt(replay.getDuration());
                    
                    // Записываем ходы
                    java.util.List<com.bmfalkye.replay.ReplaySystem.ReplayMove> moves = replay.getMoves();
                    data.writeInt(moves.size());
                    for (com.bmfalkye.replay.ReplaySystem.ReplayMove move : moves) {
                        data.writeUtf(move.getPlayerName());
                        data.writeUtf(move.getAction());
                        data.writeUtf(move.getCardId() != null ? move.getCardId() : "");
                        data.writeInt(move.getRound());
                        data.writeLong(move.getTimestamp());
                    }
                    
                    INSTANCE.sendTo(new SendReplayPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки конкретного реплея
     */
    public static class SendReplayPacket {
        private final FriendlyByteBuf data;
        
        public SendReplayPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendReplayPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendReplayPacket decode(FriendlyByteBuf buffer) {
            return new SendReplayPacket(buffer);
        }
        
        public static void handle(SendReplayPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        // Получаем текущий экран (список реплеев) как parentScreen
                        net.minecraft.client.gui.screens.Screen currentScreen = 
                            net.minecraft.client.Minecraft.getInstance().screen;
                        // Открываем экран просмотра реплея
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.ReplayViewerScreen(msg.data, currentScreen));
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет запроса событий
     */
    public static class RequestEventsPacket {
        public RequestEventsPacket() {}
        
        public static void encode(RequestEventsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestEventsPacket decode(FriendlyByteBuf buffer) {
            return new RequestEventsPacket();
        }
        
        public static void handle(RequestEventsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    java.util.List<com.bmfalkye.events.EventSystem.GameEvent> events = 
                        com.bmfalkye.events.EventSystem.getActiveEvents();
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer());
                    data.writeInt(events.size());
                    for (com.bmfalkye.events.EventSystem.GameEvent event : events) {
                        data.writeUtf(event.getId());
                        data.writeUtf(event.getName());
                        data.writeUtf(event.getDescription());
                        data.writeUtf(event.getType().getDisplayName());
                        data.writeBoolean(event.hasPlayerParticipated(player));
                        // Используем реальное оставшееся время
                        long timeRemaining = Math.max(0, event.getTimeRemaining());
                        data.writeLong(timeRemaining);
                        
                        // Извлекаем награды из Map
                        java.util.Map<String, Object> rewards = event.getRewards();
                        int rewardXP = rewards.containsKey("xp") ? (Integer) rewards.get("xp") : 0;
                        int rewardCoins = rewards.containsKey("coins") ? (Integer) rewards.get("coins") : 0;
                        data.writeInt(rewardXP);
                        data.writeInt(rewardCoins);
                        
                        // Добавляем информацию о квастах события
                        java.util.List<com.bmfalkye.events.EventSystem.EventQuest> quests = event.getQuests();
                        data.writeInt(quests.size());
                        for (com.bmfalkye.events.EventSystem.EventQuest quest : quests) {
                            data.writeUtf(quest.getType().getDisplayName());
                            data.writeInt(quest.getTarget());
                            
                            // Получаем прогресс игрока
                            com.bmfalkye.events.EventSystem.EventProgress progress = 
                                com.bmfalkye.events.EventSystem.getPlayerEventProgress(player, event.getId());
                            int questProgress = progress.getProgress(quest.getType());
                            data.writeInt(questProgress);
                            data.writeBoolean(progress.isQuestCompleted(quest.getType()));
                            
                            data.writeInt(quest.getRewardXP());
                            data.writeInt(quest.getRewardCoins());
                            data.writeBoolean(quest.getRewardCard() != null);
                            if (quest.getRewardCard() != null) {
                                data.writeUtf(quest.getRewardCard().getId());
                                data.writeUtf(quest.getRewardCard().getName());
                            }
                        }
                    }
                    
                    INSTANCE.sendTo(new SendEventsPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет отправки событий
     */
    public static class SendEventsPacket {
        private final FriendlyByteBuf data;
        
        public SendEventsPacket(FriendlyByteBuf data) {
            this.data = data;
        }
        
        public static void encode(SendEventsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBytes(msg.data);
        }
        
        public static SendEventsPacket decode(FriendlyByteBuf buffer) {
            return new SendEventsPacket(buffer);
        }
        
        public static void handle(SendEventsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.EventsScreen screen) {
                            screen.updateEvents(msg.data);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет участия в событии
     */
    public static class ParticipateInEventPacket {
        private final String eventId;
        
        public ParticipateInEventPacket(String eventId) {
            this.eventId = eventId;
        }
        
        public static void encode(ParticipateInEventPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.eventId);
        }
        
        public static ParticipateInEventPacket decode(FriendlyByteBuf buffer) {
            return new ParticipateInEventPacket(buffer.readUtf());
        }
        
        public static void handle(ParticipateInEventPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.events.EventSystem.giveEventRewards(player, msg.eventId);
                    // Отправляем обновлённые данные
                    INSTANCE.sendTo(new RequestEventsPacket(), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет регистрации на турнир
     */
    public static class RegisterForTournamentPacket {
        private final String tournamentId;
        
        public RegisterForTournamentPacket(String tournamentId) {
            this.tournamentId = tournamentId;
        }
        
        public static void encode(RegisterForTournamentPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.tournamentId);
        }
        
        public static RegisterForTournamentPacket decode(FriendlyByteBuf buffer) {
            return new RegisterForTournamentPacket(buffer.readUtf());
        }
        
        public static void handle(RegisterForTournamentPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    boolean registered = com.bmfalkye.tournament.TournamentSystem.registerPlayer(player, msg.tournamentId);
                    if (registered) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aВы успешно зарегистрированы на турнир!"));
                    }
                    // Отправляем обновлённые данные турниров
                    INSTANCE.sendTo(new RequestTournamentsPacket(), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет получения ежедневной награды
     */
    public static class ClaimDailyRewardPacket {
        public ClaimDailyRewardPacket() {}
        
        public static void encode(ClaimDailyRewardPacket msg, FriendlyByteBuf buffer) {}
        
        public static ClaimDailyRewardPacket decode(FriendlyByteBuf buffer) {
            return new ClaimDailyRewardPacket();
        }
        
        public static void handle(ClaimDailyRewardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    boolean claimed = com.bmfalkye.daily.DailyRewardSystem.claimDailyReward(player);
                    if (claimed) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aЕжедневная награда получена!"));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cВы уже получили награду сегодня!"));
                    }
                    // Отправляем обновлённые данные обратно
                    com.bmfalkye.daily.DailyRewardSystem.DailyRewardInfo rewardInfo = 
                        com.bmfalkye.daily.DailyRewardSystem.getDailyRewardInfo(player);
                    java.util.List<com.bmfalkye.daily.DailyRewardSystem.DailyQuest> quests = 
                        com.bmfalkye.daily.DailyRewardSystem.getPlayerQuests(player);
                    
                    FriendlyByteBuf data = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    data.writeInt(rewardInfo.getDay());
                    data.writeBoolean(rewardInfo.isClaimed());
                    data.writeInt(rewardInfo.getStreakDays());
                    
                    data.writeInt(quests.size());
                    for (com.bmfalkye.daily.DailyRewardSystem.DailyQuest quest : quests) {
                        data.writeUtf(quest.getDescription());
                        data.writeInt(quest.getTarget());
                        data.writeInt(quest.getProgress(player));
                        int rewardXP = quest.getTarget() * 10;
                        int rewardCoins = quest.getType() == com.bmfalkye.daily.DailyRewardSystem.QuestType.WIN_GAMES ? 50 : 0;
                        data.writeInt(rewardXP);
                        data.writeInt(rewardCoins);
                    }
                    
                    INSTANCE.sendTo(new SendDailyRewardsPacket(data), 
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса данных эволюции карт игрока
     */
    public static class RequestCardEvolutionPacket {
        public RequestCardEvolutionPacket() {}
        
        public static void encode(RequestCardEvolutionPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestCardEvolutionPacket decode(FriendlyByteBuf buffer) {
            return new RequestCardEvolutionPacket();
        }
        
        public static void handle(RequestCardEvolutionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.storage.CardEvolutionStorage storage = 
                        com.bmfalkye.storage.CardEvolutionStorage.get(serverLevel);
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(serverLevel);
                    
                    // Получаем все карты игрока и их данные эволюции
                    java.util.List<String> cardIds = new java.util.ArrayList<>(collection.getPlayerCollection(player));
                    java.util.Map<String, EvolutionData> evolutionData = new java.util.HashMap<>();
                    
                    for (String cardId : cardIds) {
                        com.bmfalkye.storage.CardEvolutionStorage.CardEvolutionData data = 
                            storage.getCardEvolution(player, cardId);
                        evolutionData.put(cardId, new EvolutionData(
                            data.getLevel(),
                            data.getExperience(),
                            data.getExperienceForNextLevel(),
                            new java.util.HashSet<>(data.getUnlockedBranches())
                        ));
                    }
                    
                    // Также отправляем количество Пыли Душ
                    com.bmfalkye.storage.PlayerCurrency currency = 
                        com.bmfalkye.storage.PlayerCurrency.get(serverLevel);
                    int soulDust = currency.getSoulDust(player);
                    
                    INSTANCE.sendTo(new SendCardEvolutionPacket(evolutionData, soulDust),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки данных эволюции карт на клиент
     */
    public static class SendCardEvolutionPacket {
        private final java.util.Map<String, EvolutionData> evolutionData;
        private final int soulDust;
        
        public SendCardEvolutionPacket(java.util.Map<String, EvolutionData> evolutionData, int soulDust) {
            this.evolutionData = evolutionData;
            this.soulDust = soulDust;
        }
        
        public static void encode(SendCardEvolutionPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.evolutionData.size());
            for (java.util.Map.Entry<String, EvolutionData> entry : msg.evolutionData.entrySet()) {
                String safeId = entry.getKey() != null ? entry.getKey() : "";
                if (safeId.length() > 32767) {
                    safeId = safeId.substring(0, 32767);
                }
                buffer.writeUtf(safeId);
                EvolutionData data = entry.getValue();
                buffer.writeInt(data.level);
                buffer.writeInt(data.experience);
                buffer.writeInt(data.experienceForNextLevel);
                buffer.writeInt(data.unlockedBranches.size());
                for (String branchId : data.unlockedBranches) {
                    String safeBranchId = branchId != null ? branchId : "";
                    if (safeBranchId.length() > 32767) {
                        safeBranchId = safeBranchId.substring(0, 32767);
                    }
                    buffer.writeUtf(safeBranchId);
                }
            }
            buffer.writeInt(msg.soulDust);
        }
        
        public static SendCardEvolutionPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            java.util.Map<String, EvolutionData> evolutionData = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                String cardId = buffer.readUtf();
                int level = buffer.readInt();
                int experience = buffer.readInt();
                int experienceForNextLevel = buffer.readInt();
                int branchesSize = buffer.readInt();
                java.util.Set<String> unlockedBranches = new java.util.HashSet<>();
                for (int j = 0; j < branchesSize; j++) {
                    unlockedBranches.add(buffer.readUtf());
                }
                evolutionData.put(cardId, new EvolutionData(level, experience, experienceForNextLevel, unlockedBranches));
            }
            int soulDust = buffer.readInt();
            return new SendCardEvolutionPacket(evolutionData, soulDust);
        }
        
        public static void handle(SendCardEvolutionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleCardEvolution(msg.evolutionData, msg.soulDust);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.Map<String, EvolutionData> getEvolutionData() { return evolutionData; }
        public int getSoulDust() { return soulDust; }
    }
    
    /**
     * Пакет для открытия ветки улучшения карты
     */
    public static class UnlockBranchPacket {
        private final String cardId;
        private final String branchId;
        
        public UnlockBranchPacket(String cardId, String branchId) {
            this.cardId = cardId;
            this.branchId = branchId;
        }
        
        public static void encode(UnlockBranchPacket msg, FriendlyByteBuf buffer) {
            String safeCardId = msg.cardId != null ? msg.cardId : "";
            String safeBranchId = msg.branchId != null ? msg.branchId : "";
            if (safeCardId.length() > 32767) safeCardId = safeCardId.substring(0, 32767);
            if (safeBranchId.length() > 32767) safeBranchId = safeBranchId.substring(0, 32767);
            buffer.writeUtf(safeCardId);
            buffer.writeUtf(safeBranchId);
        }
        
        public static UnlockBranchPacket decode(FriendlyByteBuf buffer) {
            return new UnlockBranchPacket(buffer.readUtf(), buffer.readUtf());
        }
        
        public static void handle(UnlockBranchPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    int cost = com.bmfalkye.evolution.CardEvolutionSystem.getBranchUnlockCost(msg.cardId, msg.branchId);
                    boolean success = com.bmfalkye.evolution.CardEvolutionSystem.unlockBranchWithSoulDust(
                        player, msg.cardId, msg.branchId, cost);
                    
                    if (success) {
                        // Отправляем обновлённые данные эволюции
                        INSTANCE.sendTo(new RequestCardEvolutionPacket(),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Данные эволюции карты для передачи по сети
     */
    public static class EvolutionData {
        public final int level;
        public final int experience;
        public final int experienceForNextLevel;
        public final java.util.Set<String> unlockedBranches;
        
        public EvolutionData(int level, int experience, int experienceForNextLevel, java.util.Set<String> unlockedBranches) {
            this.level = level;
            this.experience = experience;
            this.experienceForNextLevel = experienceForNextLevel;
            this.unlockedBranches = new java.util.HashSet<>(unlockedBranches);
        }
    }
    
    /**
     * Пакет для запроса данных квестов игрока
     */
    public static class RequestQuestsPacket {
        public RequestQuestsPacket() {}
        
        public static void encode(RequestQuestsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestQuestsPacket decode(FriendlyByteBuf buffer) {
            return new RequestQuestsPacket();
        }
        
        public static void handle(RequestQuestsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.quests.QuestStorage storage = 
                        com.bmfalkye.quests.QuestStorage.get(serverLevel);
                    java.util.List<String> activeQuests = storage.getActiveQuests(player);
                    java.util.List<String> completedQuests = storage.getCompletedQuests(player);
                    
                    INSTANCE.sendTo(new SendQuestsPacket(activeQuests, completedQuests),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки данных квестов на клиент
     */
    public static class SendQuestsPacket {
        private final java.util.List<String> activeQuests;
        private final java.util.List<String> completedQuests;
        
        public SendQuestsPacket(java.util.List<String> activeQuests, java.util.List<String> completedQuests) {
            this.activeQuests = activeQuests;
            this.completedQuests = completedQuests;
        }
        
        public static void encode(SendQuestsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.activeQuests.size());
            for (String questId : msg.activeQuests) {
                String safeId = questId != null ? questId : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
            buffer.writeInt(msg.completedQuests.size());
            for (String questId : msg.completedQuests) {
                String safeId = questId != null ? questId : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
        }
        
        public static SendQuestsPacket decode(FriendlyByteBuf buffer) {
            int activeSize = buffer.readInt();
            java.util.List<String> activeQuests = new java.util.ArrayList<>();
            for (int i = 0; i < activeSize; i++) {
                activeQuests.add(buffer.readUtf());
            }
            int completedSize = buffer.readInt();
            java.util.List<String> completedQuests = new java.util.ArrayList<>();
            for (int i = 0; i < completedSize; i++) {
                completedQuests.add(buffer.readUtf());
            }
            return new SendQuestsPacket(activeQuests, completedQuests);
        }
        
        public static void handle(SendQuestsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleQuests(msg.activeQuests, msg.completedQuests);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.List<String> getActiveQuests() { return activeQuests; }
        public java.util.List<String> getCompletedQuests() { return completedQuests; }
    }
    
    /**
     * Пакет для начала квеста
     */
    public static class StartQuestPacket {
        private final String questId;
        
        public StartQuestPacket(String questId) {
            this.questId = questId;
        }
        
        public static void encode(StartQuestPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.questId != null ? msg.questId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
        }
        
        public static StartQuestPacket decode(FriendlyByteBuf buffer) {
            return new StartQuestPacket(buffer.readUtf());
        }
        
        public static void handle(StartQuestPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.quests.QuestSystem.startQuest(player, msg.questId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для открытия главного меню мода
     */
    public static class OpenMainMenuPacket {
        public OpenMainMenuPacket() {}
        
        public static void encode(OpenMainMenuPacket msg, FriendlyByteBuf buffer) {
            // Пустой пакет
        }
        
        public static OpenMainMenuPacket decode(FriendlyByteBuf buffer) {
            return new OpenMainMenuPacket();
        }
        
        public static void handle(OpenMainMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.FalkyeMainMenuScreen());
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса данных драфта
     */
    public static class RequestDraftDataPacket {
        public RequestDraftDataPacket() {}
        
        public static void encode(RequestDraftDataPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestDraftDataPacket decode(FriendlyByteBuf buffer) {
            return new RequestDraftDataPacket();
        }
        
        public static void handle(RequestDraftDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.bmfalkye.draft.DraftStorage storage = 
                        com.bmfalkye.draft.DraftStorage.get(serverLevel);
                    com.bmfalkye.draft.DraftSession session = storage.getSession(player);
                    
                    if (session != null && session.isActive()) {
                        com.bmfalkye.draft.DraftSession.CardChoice currentChoice = session.getCurrentChoice();
                        if (currentChoice != null) {
                            java.util.List<String> cardIds1 = new java.util.ArrayList<>();
                            java.util.List<String> cardIds2 = new java.util.ArrayList<>();
                            java.util.List<String> cardIds3 = new java.util.ArrayList<>();
                            
                            java.util.List<com.bmfalkye.cards.Card> cards = currentChoice.getCards();
                            if (cards.size() > 0) cardIds1.add(cards.get(0).getId());
                            if (cards.size() > 1) cardIds2.add(cards.get(1).getId());
                            if (cards.size() > 2) cardIds3.add(cards.get(2).getId());
                            
                            INSTANCE.sendTo(new SendDraftDataPacket(
                                cardIds1, cardIds2, cardIds3,
                                session.getCurrentChoiceIndex(),
                                session.getSelectedCards(),
                                session.isCompleted()
                            ), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки данных драфта на клиент
     */
    public static class SendDraftDataPacket {
        private final java.util.List<String> cardIds1;
        private final java.util.List<String> cardIds2;
        private final java.util.List<String> cardIds3;
        private final int currentChoiceIndex;
        private final java.util.List<String> selectedCards;
        private final boolean completed;
        
        public SendDraftDataPacket(java.util.List<String> cardIds1, java.util.List<String> cardIds2, 
                                   java.util.List<String> cardIds3, int currentChoiceIndex,
                                   java.util.List<String> selectedCards, boolean completed) {
            this.cardIds1 = cardIds1 != null ? cardIds1 : new java.util.ArrayList<>();
            this.cardIds2 = cardIds2 != null ? cardIds2 : new java.util.ArrayList<>();
            this.cardIds3 = cardIds3 != null ? cardIds3 : new java.util.ArrayList<>();
            this.currentChoiceIndex = currentChoiceIndex;
            this.selectedCards = selectedCards != null ? selectedCards : new java.util.ArrayList<>();
            this.completed = completed;
        }
        
        public static void encode(SendDraftDataPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.cardIds1.size());
            for (String id : msg.cardIds1) {
                String safeId = id != null ? id : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
            buffer.writeInt(msg.cardIds2.size());
            for (String id : msg.cardIds2) {
                String safeId = id != null ? id : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
            buffer.writeInt(msg.cardIds3.size());
            for (String id : msg.cardIds3) {
                String safeId = id != null ? id : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
            buffer.writeInt(msg.currentChoiceIndex);
            buffer.writeInt(msg.selectedCards.size());
            for (String id : msg.selectedCards) {
                String safeId = id != null ? id : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                buffer.writeUtf(safeId);
            }
            buffer.writeBoolean(msg.completed);
        }
        
        public static SendDraftDataPacket decode(FriendlyByteBuf buffer) {
            int size1 = buffer.readInt();
            java.util.List<String> cardIds1 = new java.util.ArrayList<>();
            for (int i = 0; i < size1; i++) {
                cardIds1.add(buffer.readUtf());
            }
            int size2 = buffer.readInt();
            java.util.List<String> cardIds2 = new java.util.ArrayList<>();
            for (int i = 0; i < size2; i++) {
                cardIds2.add(buffer.readUtf());
            }
            int size3 = buffer.readInt();
            java.util.List<String> cardIds3 = new java.util.ArrayList<>();
            for (int i = 0; i < size3; i++) {
                cardIds3.add(buffer.readUtf());
            }
            int currentIndex = buffer.readInt();
            int selectedSize = buffer.readInt();
            java.util.List<String> selected = new java.util.ArrayList<>();
            for (int i = 0; i < selectedSize; i++) {
                selected.add(buffer.readUtf());
            }
            boolean completed = buffer.readBoolean();
            return new SendDraftDataPacket(cardIds1, cardIds2, cardIds3, currentIndex, selected, completed);
        }
        
        public static void handle(SendDraftDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof 
                            com.bmfalkye.client.DraftScreen screen) {
                            screen.updateDraftData(msg.cardIds1, msg.cardIds2, msg.cardIds3,
                                msg.currentChoiceIndex, msg.selectedCards, msg.completed);
                        }
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.List<String> getCardIds1() { return cardIds1; }
        public java.util.List<String> getCardIds2() { return cardIds2; }
        public java.util.List<String> getCardIds3() { return cardIds3; }
        public int getCurrentChoiceIndex() { return currentChoiceIndex; }
        public java.util.List<String> getSelectedCards() { return selectedCards; }
        public boolean isCompleted() { return completed; }
    }
    
    /**
     * Пакет для выбора карты в драфте
     */
    public static class SelectDraftCardPacket {
        private final int choiceIndex;
        private final int cardIndex;
        
        public SelectDraftCardPacket(int choiceIndex, int cardIndex) {
            this.choiceIndex = choiceIndex;
            this.cardIndex = cardIndex;
        }
        
        public static void encode(SelectDraftCardPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.choiceIndex);
            buffer.writeInt(msg.cardIndex);
        }
        
        public static SelectDraftCardPacket decode(FriendlyByteBuf buffer) {
            return new SelectDraftCardPacket(buffer.readInt(), buffer.readInt());
        }
        
        public static void handle(SelectDraftCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    boolean success = com.bmfalkye.draft.DraftSystem.selectCard(player, msg.choiceIndex, msg.cardIndex);
                    if (success) {
                        // Отправляем обновлённые данные обратно
                        INSTANCE.sendTo(new RequestDraftDataPacket(),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для начала драфта
     */
    public static class StartDraftPacket {
        private final boolean useTicket;
        
        public StartDraftPacket(boolean useTicket) {
            this.useTicket = useTicket;
        }
        
        public static void encode(StartDraftPacket msg, FriendlyByteBuf buffer) {
            buffer.writeBoolean(msg.useTicket);
        }
        
        public static StartDraftPacket decode(FriendlyByteBuf buffer) {
            return new StartDraftPacket(buffer.readBoolean());
        }
        
        public static void handle(StartDraftPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    boolean success = com.bmfalkye.draft.DraftSystem.startDraft(player, msg.useTicket);
                    if (success) {
                        // Открываем экран драфта
                        INSTANCE.sendTo(new OpenDraftScreenPacket(),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для открытия экрана драфта
     */
    public static class OpenDraftScreenPacket {
        public OpenDraftScreenPacket() {}
        
        public static void encode(OpenDraftScreenPacket msg, FriendlyByteBuf buffer) {}
        
        public static OpenDraftScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenDraftScreenPacket();
        }
        
        public static void handle(OpenDraftScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.DraftScreen());
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для начала арены драфта
     */
    public static class StartArenaPacket {
        public StartArenaPacket() {}
        
        public static void encode(StartArenaPacket msg, FriendlyByteBuf buffer) {}
        
        public static StartArenaPacket decode(FriendlyByteBuf buffer) {
            return new StartArenaPacket();
        }
        
        public static void handle(StartArenaPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    boolean success = com.bmfalkye.draft.DraftSystem.startArena(player);
                    if (!success) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНе удалось начать арену. Убедитесь, что драфт завершён."));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса пользовательских турниров
     */
    public static class RequestCustomTournamentsPacket {
        public RequestCustomTournamentsPacket() {}
        
        public static void encode(RequestCustomTournamentsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestCustomTournamentsPacket decode(FriendlyByteBuf buffer) {
            return new RequestCustomTournamentsPacket();
        }
        
        public static void handle(RequestCustomTournamentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    java.util.List<com.bmfalkye.tournament.CustomTournament> tournaments = 
                        com.bmfalkye.tournament.CustomTournamentSystem.getActiveTournaments(serverLevel);
                    
                    java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> infos = 
                        new java.util.ArrayList<>();
                    
                    for (com.bmfalkye.tournament.CustomTournament tournament : tournaments) {
                        infos.add(new com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo(
                            tournament.getId(),
                            tournament.getName(),
                            tournament.getRegisteredPlayers().size(),
                            tournament.getMaxParticipants(),
                            tournament.getEntryFee(),
                            tournament.getRules().getDisplayName(),
                            tournament.isStarted()
                        ));
                    }
                    
                    INSTANCE.sendTo(new SendCustomTournamentsPacket(infos),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки пользовательских турниров
     */
    public static class SendCustomTournamentsPacket {
        private final java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> tournaments;
        
        public SendCustomTournamentsPacket(java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> tournaments) {
            this.tournaments = tournaments;
        }
        
        public static void encode(SendCustomTournamentsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.tournaments.size());
            for (com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo info : msg.tournaments) {
                String safeId = info.id != null ? info.id : "";
                String safeName = info.name != null ? info.name : "";
                String safeRules = info.rules != null ? info.rules : "";
                if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
                if (safeName.length() > 32767) safeName = safeName.substring(0, 32767);
                if (safeRules.length() > 32767) safeRules = safeRules.substring(0, 32767);
                buffer.writeUtf(safeId);
                buffer.writeUtf(safeName);
                buffer.writeInt(info.participants);
                buffer.writeInt(info.maxParticipants);
                buffer.writeInt(info.entryFee);
                buffer.writeUtf(safeRules);
                buffer.writeBoolean(info.started);
            }
        }
        
        public static SendCustomTournamentsPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> tournaments = 
                new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                tournaments.add(new com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readUtf(),
                    buffer.readBoolean()
                ));
            }
            return new SendCustomTournamentsPacket(tournaments);
        }
        
        public static void handle(SendCustomTournamentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleCustomTournaments(msg.tournaments);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.List<com.bmfalkye.client.CustomTournamentScreen.CustomTournamentInfo> getTournaments() {
            return tournaments;
        }
    }
    
    /**
     * Пакет для создания пользовательского турнира
     */
    public static class CreateCustomTournamentPacket {
        private final String name;
        private final String rules;
        private final int maxParticipants;
        private final int entryFee;
        private final long scheduledStartTime;
        
        public CreateCustomTournamentPacket(String name, com.bmfalkye.tournament.CustomTournament.TournamentRules rules,
                                           int maxParticipants, int entryFee, long scheduledStartTime) {
            this.name = name;
            this.rules = rules.name();
            this.maxParticipants = maxParticipants;
            this.entryFee = entryFee;
            this.scheduledStartTime = scheduledStartTime;
        }
        
        public static void encode(CreateCustomTournamentPacket msg, FriendlyByteBuf buffer) {
            String safeName = msg.name != null ? msg.name : "";
            String safeRules = msg.rules != null ? msg.rules : "";
            if (safeName.length() > 32767) safeName = safeName.substring(0, 32767);
            if (safeRules.length() > 32767) safeRules = safeRules.substring(0, 32767);
            buffer.writeUtf(safeName);
            buffer.writeUtf(safeRules);
            buffer.writeInt(msg.maxParticipants);
            buffer.writeInt(msg.entryFee);
            buffer.writeLong(msg.scheduledStartTime);
        }
        
        public static CreateCustomTournamentPacket decode(FriendlyByteBuf buffer) {
            return new CreateCustomTournamentPacket(
                buffer.readUtf(),
                com.bmfalkye.tournament.CustomTournament.TournamentRules.valueOf(buffer.readUtf()),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readLong()
            );
        }
        
        public static void handle(CreateCustomTournamentPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.tournament.CustomTournament.TournamentRules rules = 
                        com.bmfalkye.tournament.CustomTournament.TournamentRules.valueOf(msg.rules);
                    com.bmfalkye.tournament.CustomTournamentSystem.createTournament(
                        player, msg.name, rules, msg.maxParticipants, msg.entryFee, msg.scheduledStartTime);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для регистрации на пользовательский турнир
     */
    public static class RegisterForCustomTournamentPacket {
        private final String tournamentId;
        
        public RegisterForCustomTournamentPacket(String tournamentId) {
            this.tournamentId = tournamentId;
        }
        
        public static void encode(RegisterForCustomTournamentPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
        }
        
        public static RegisterForCustomTournamentPacket decode(FriendlyByteBuf buffer) {
            return new RegisterForCustomTournamentPacket(buffer.readUtf());
        }
        
        public static void handle(RegisterForCustomTournamentPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.tournament.CustomTournamentSystem.registerPlayer(player, msg.tournamentId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для начала пользовательского турнира
     */
    public static class StartCustomTournamentPacket {
        private final String tournamentId;
        
        public StartCustomTournamentPacket(String tournamentId) {
            this.tournamentId = tournamentId;
        }
        
        public static void encode(StartCustomTournamentPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
        }
        
        public static StartCustomTournamentPacket decode(FriendlyByteBuf buffer) {
            return new StartCustomTournamentPacket(buffer.readUtf());
        }
        
        public static void handle(StartCustomTournamentPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.tournament.CustomTournamentSystem.startTournament(player, msg.tournamentId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для добавления зрителя к турниру
     */
    public static class AddTournamentSpectatorPacket {
        private final String tournamentId;
        
        public AddTournamentSpectatorPacket(String tournamentId) {
            this.tournamentId = tournamentId;
        }
        
        public static void encode(AddTournamentSpectatorPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
        }
        
        public static AddTournamentSpectatorPacket decode(FriendlyByteBuf buffer) {
            return new AddTournamentSpectatorPacket(buffer.readUtf());
        }
        
        public static void handle(AddTournamentSpectatorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    com.bmfalkye.tournament.CustomTournamentSystem.addSpectator(player, msg.tournamentId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для открытия экрана трансляции турнира
     */
    public static class OpenTournamentSpectatorPacket {
        private final String tournamentId;
        private final String tournamentName;
        
        public OpenTournamentSpectatorPacket(String tournamentId, String tournamentName) {
            this.tournamentId = tournamentId;
            this.tournamentName = tournamentName;
        }
        
        public static void encode(OpenTournamentSpectatorPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            String safeName = msg.tournamentName != null ? msg.tournamentName : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            if (safeName.length() > 32767) safeName = safeName.substring(0, 32767);
            buffer.writeUtf(safeId);
            buffer.writeUtf(safeName);
        }
        
        public static OpenTournamentSpectatorPacket decode(FriendlyByteBuf buffer) {
            return new OpenTournamentSpectatorPacket(buffer.readUtf(), buffer.readUtf());
        }
        
        public static void handle(OpenTournamentSpectatorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.bmfalkye.client.TournamentSpectatorScreen(
                                null, msg.tournamentId, msg.tournamentName));
                    });
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса активных матчей турнира
     */
    public static class RequestTournamentMatchesPacket {
        private final String tournamentId;
        
        public RequestTournamentMatchesPacket(String tournamentId) {
            this.tournamentId = tournamentId;
        }
        
        public static void encode(RequestTournamentMatchesPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
        }
        
        public static RequestTournamentMatchesPacket decode(FriendlyByteBuf buffer) {
            return new RequestTournamentMatchesPacket(buffer.readUtf());
        }
        
        public static void handle(RequestTournamentMatchesPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Ищем турнир
                    com.bmfalkye.tournament.TournamentSystem.Tournament tournament = 
                        com.bmfalkye.tournament.TournamentSystem.getTournament(msg.tournamentId);
                    
                    if (tournament == null) {
                        // Проверяем пользовательский турнир
                        com.bmfalkye.tournament.CustomTournamentStorage storage = 
                            com.bmfalkye.tournament.CustomTournamentStorage.get(serverLevel);
                        com.bmfalkye.tournament.CustomTournament customTournament = 
                            storage.getTournament(msg.tournamentId);
                        
                        if (customTournament != null && customTournament.getTournamentSystemId() != null) {
                            tournament = com.bmfalkye.tournament.TournamentSystem.getTournament(
                                customTournament.getTournamentSystemId());
                        }
                    }
                    
                    if (tournament != null) {
                        java.util.List<com.bmfalkye.tournament.TournamentSpectatorManager.TournamentMatchInfo> matches = 
                            com.bmfalkye.tournament.TournamentSpectatorManager.getActiveTournamentMatches(
                                serverLevel, tournament);
                        
                        java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> matchInfos = 
                            new java.util.ArrayList<>();
                        
                        for (com.bmfalkye.tournament.TournamentSpectatorManager.TournamentMatchInfo match : matches) {
                            matchInfos.add(new com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo(
                                match.getTournamentId(),
                                match.getRound(),
                                match.getPlayer1UUID(),
                                match.getPlayer1Name(),
                                match.getPlayer2UUID(),
                                match.getPlayer2Name(),
                                match.getCurrentGameRound(),
                                match.getRoundsWon1(),
                                match.getRoundsWon2()
                            ));
                        }
                        
                        INSTANCE.sendTo(new SendTournamentMatchesPacket(matchInfos),
                            player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки активных матчей турнира
     */
    public static class SendTournamentMatchesPacket {
        private final java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> matches;
        
        public SendTournamentMatchesPacket(java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> matches) {
            this.matches = matches;
        }
        
        public static void encode(SendTournamentMatchesPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.matches.size());
            for (com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo match : msg.matches) {
                String safeTournamentId = match.tournamentId != null ? match.tournamentId : "";
                String safePlayer1Name = match.player1Name != null ? match.player1Name : "";
                String safePlayer2Name = match.player2Name != null ? match.player2Name : "";
                if (safeTournamentId.length() > 32767) safeTournamentId = safeTournamentId.substring(0, 32767);
                if (safePlayer1Name.length() > 32767) safePlayer1Name = safePlayer1Name.substring(0, 32767);
                if (safePlayer2Name.length() > 32767) safePlayer2Name = safePlayer2Name.substring(0, 32767);
                buffer.writeUtf(safeTournamentId);
                buffer.writeInt(match.round);
                buffer.writeUUID(match.player1UUID);
                buffer.writeUtf(safePlayer1Name);
                buffer.writeUUID(match.player2UUID);
                buffer.writeUtf(safePlayer2Name);
                buffer.writeInt(match.currentGameRound);
                buffer.writeInt(match.roundsWon1);
                buffer.writeInt(match.roundsWon2);
            }
        }
        
        public static SendTournamentMatchesPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> matches = 
                new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                matches.add(new com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo(
                    buffer.readUtf(),
                    buffer.readInt(),
                    buffer.readUUID(),
                    buffer.readUtf(),
                    buffer.readUUID(),
                    buffer.readUtf(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt()
                ));
            }
            return new SendTournamentMatchesPacket(matches);
        }
        
        public static void handle(SendTournamentMatchesPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleTournamentMatches(msg.matches);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public java.util.List<com.bmfalkye.client.TournamentSpectatorScreen.MatchInfo> getMatches() {
            return matches;
        }
    }
    
    /**
     * Пакет для просмотра матча турнира
     */
    public static class WatchTournamentMatchPacket {
        private final String tournamentId;
        private final java.util.UUID player1UUID;
        private final java.util.UUID player2UUID;
        
        public WatchTournamentMatchPacket(String tournamentId, java.util.UUID player1UUID, java.util.UUID player2UUID) {
            this.tournamentId = tournamentId;
            this.player1UUID = player1UUID;
            this.player2UUID = player2UUID;
        }
        
        public static void encode(WatchTournamentMatchPacket msg, FriendlyByteBuf buffer) {
            String safeId = msg.tournamentId != null ? msg.tournamentId : "";
            if (safeId.length() > 32767) safeId = safeId.substring(0, 32767);
            buffer.writeUtf(safeId);
            buffer.writeUUID(msg.player1UUID);
            buffer.writeUUID(msg.player2UUID);
        }
        
        public static WatchTournamentMatchPacket decode(FriendlyByteBuf buffer) {
            return new WatchTournamentMatchPacket(
                buffer.readUtf(),
                buffer.readUUID(),
                buffer.readUUID()
            );
        }
        
        public static void handle(WatchTournamentMatchPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer spectator = ctx.get().getSender();
                if (spectator != null && spectator.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    ServerPlayer player1 = serverLevel.getServer().getPlayerList().getPlayer(msg.player1UUID);
                    ServerPlayer player2 = serverLevel.getServer().getPlayerList().getPlayer(msg.player2UUID);
                    
                    if (player1 != null && player2 != null) {
                        // Находим активную игровую сессию
                        com.bmfalkye.tournament.TournamentSpectatorManager.TournamentMatchInfo matchInfo = null;
                        com.bmfalkye.tournament.TournamentSystem.Tournament tournament = 
                            com.bmfalkye.tournament.TournamentSystem.getTournament(msg.tournamentId);
                        
                        if (tournament == null) {
                            // Проверяем пользовательский турнир
                            com.bmfalkye.tournament.CustomTournamentStorage storage = 
                                com.bmfalkye.tournament.CustomTournamentStorage.get(serverLevel);
                            com.bmfalkye.tournament.CustomTournament customTournament = 
                                storage.getTournament(msg.tournamentId);
                            
                            if (customTournament != null && customTournament.getTournamentSystemId() != null) {
                                tournament = com.bmfalkye.tournament.TournamentSystem.getTournament(
                                    customTournament.getTournamentSystemId());
                            }
                        }
                        
                        if (tournament != null) {
                            java.util.List<com.bmfalkye.tournament.TournamentSpectatorManager.TournamentMatchInfo> matches = 
                                com.bmfalkye.tournament.TournamentSpectatorManager.getActiveTournamentMatches(
                                    serverLevel, tournament);
                            
                            for (com.bmfalkye.tournament.TournamentSpectatorManager.TournamentMatchInfo match : matches) {
                                if (match.getPlayer1UUID().equals(msg.player1UUID) && 
                                    match.getPlayer2UUID().equals(msg.player2UUID)) {
                                    matchInfo = match;
                                    break;
                                }
                            }
                        }
                        
                        // Находим игровую сессию
                        com.bmfalkye.game.FalkyeGameSession session = 
                            com.bmfalkye.tournament.TournamentSpectatorManager.findTournamentMatch(
                                serverLevel, player1, player2);
                        
                        if (session != null) {
                            // Добавляем зрителя
                            com.bmfalkye.tournament.TournamentSpectatorManager.addSpectatorToMatch(
                                spectator, msg.tournamentId, player1, player2);
                            
                            // Открываем экран игры для зрителя (используем существующий метод)
                            com.bmfalkye.network.NetworkHandler.openGameScreen(spectator, session);
                        } else {
                            spectator.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cМатч не найден или уже завершён"));
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для запроса лидербордов
     */
    public static class RequestLeaderboardPacket {
        public RequestLeaderboardPacket() {}
        
        public static void encode(RequestLeaderboardPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestLeaderboardPacket decode(FriendlyByteBuf buffer) {
            return new RequestLeaderboardPacket();
        }
        
        public static void handle(RequestLeaderboardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Получаем Зал Славы
                    com.bmfalkye.leaderboard.LeaderboardStorage storage = 
                        com.bmfalkye.leaderboard.LeaderboardStorage.get(serverLevel);
                    java.util.List<com.bmfalkye.leaderboard.LeaderboardStorage.HallOfFameEntry> hallOfFame = 
                        storage.getHallOfFame();
                    
                    // Получаем еженедельный рейтинг
                    java.util.List<com.bmfalkye.leaderboard.LeaderboardStorage.LeaderboardEntry> weekly = 
                        com.bmfalkye.leaderboard.WeeklyLeaderboardSystem.getWeeklyLeaderboard(serverLevel);
                    
                    // Конвертируем в клиентские форматы
                    java.util.List<com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry> hallOfFameEntries = 
                        new java.util.ArrayList<>();
                    for (com.bmfalkye.leaderboard.LeaderboardStorage.HallOfFameEntry entry : hallOfFame) {
                        hallOfFameEntries.add(new com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry(
                            entry.playerUUID, entry.playerName, entry.season));
                    }
                    
                    java.util.List<com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry> weeklyEntries = 
                        new java.util.ArrayList<>();
                    for (com.bmfalkye.leaderboard.LeaderboardStorage.LeaderboardEntry entry : weekly) {
                        weeklyEntries.add(new com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry(
                            entry.playerUUID, entry.playerName, entry.rating));
                    }
                    
                    INSTANCE.sendTo(new SendLeaderboardPacket(hallOfFameEntries, weeklyEntries),
                        player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки лидербордов
     */
    public static class SendLeaderboardPacket {
        private final java.util.List<com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry> hallOfFame;
        private final java.util.List<com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry> weeklyLeaderboard;
        
        public SendLeaderboardPacket(java.util.List<com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry> hallOfFame,
                                    java.util.List<com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry> weeklyLeaderboard) {
            this.hallOfFame = hallOfFame;
            this.weeklyLeaderboard = weeklyLeaderboard;
        }
        
        public static void encode(SendLeaderboardPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.hallOfFame.size());
            for (com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry entry : msg.hallOfFame) {
                String safeName = entry.playerName != null ? entry.playerName : "";
                if (safeName.length() > 32767) safeName = safeName.substring(0, 32767);
                buffer.writeUUID(entry.playerUUID);
                buffer.writeUtf(safeName);
                buffer.writeInt(entry.season);
            }
            
            buffer.writeInt(msg.weeklyLeaderboard.size());
            for (com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry entry : msg.weeklyLeaderboard) {
                String safeName = entry.playerName != null ? entry.playerName : "";
                if (safeName.length() > 32767) safeName = safeName.substring(0, 32767);
                buffer.writeUUID(entry.playerUUID);
                buffer.writeUtf(safeName);
                buffer.writeInt(entry.rating);
            }
        }
        
        public static SendLeaderboardPacket decode(FriendlyByteBuf buffer) {
            int hallOfFameSize = buffer.readInt();
            java.util.List<com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry> hallOfFame = 
                new java.util.ArrayList<>();
            for (int i = 0; i < hallOfFameSize; i++) {
                hallOfFame.add(new com.bmfalkye.client.HallOfFameScreen.HallOfFameEntry(
                    buffer.readUUID(), buffer.readUtf(), buffer.readInt()));
            }
            
            int weeklySize = buffer.readInt();
            java.util.List<com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry> weeklyLeaderboard = 
                new java.util.ArrayList<>();
            for (int i = 0; i < weeklySize; i++) {
                weeklyLeaderboard.add(new com.bmfalkye.client.HallOfFameScreen.LeaderboardEntry(
                    buffer.readUUID(), buffer.readUtf(), buffer.readInt()));
            }
            
            return new SendLeaderboardPacket(hallOfFame, weeklyLeaderboard);
        }
        
        public static void handle(SendLeaderboardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleLeaderboard(msg.hallOfFame, msg.weeklyLeaderboard);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Пакет для отправки эмоции
     */
    public static class SendEmotePacket {
        private final UUID senderUUID;
        private final String emoteId;
        
        public SendEmotePacket(UUID senderUUID, String emoteId) {
            this.senderUUID = senderUUID;
            this.emoteId = emoteId;
        }
        
        public static void encode(SendEmotePacket msg, FriendlyByteBuf buffer) {
            buffer.writeUUID(msg.senderUUID);
            String safeEmoteId = msg.emoteId != null ? msg.emoteId : "";
            if (safeEmoteId.length() > 32767) safeEmoteId = safeEmoteId.substring(0, 32767);
            buffer.writeUtf(safeEmoteId);
        }
        
        public static SendEmotePacket decode(FriendlyByteBuf buffer) {
            return new SendEmotePacket(buffer.readUUID(), buffer.readUtf());
        }
        
        public static void handle(SendEmotePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleEmote(msg.senderUUID, msg.emoteId);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}

