package com.bmfalkye.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;

/**
 * Сетевые пакеты для магазина карт
 */
public class ShopPackets {
    
    public static class RequestShopItemsPacket {
        public RequestShopItemsPacket() {}
        
        public static void encode(RequestShopItemsPacket msg, FriendlyByteBuf buffer) {}
        
        public static RequestShopItemsPacket decode(FriendlyByteBuf buffer) {
            return new RequestShopItemsPacket();
        }
        
        public static void handle(RequestShopItemsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                    // Получаем баланс игрока
                    com.bmfalkye.storage.PlayerCurrency currency = 
                        com.bmfalkye.storage.PlayerCurrency.get(level);
                    int playerCoins = currency.getCoins(player);
                    
                    // Получаем все карты из реестра
                    java.util.List<com.bmfalkye.cards.Card> allCards = 
                        com.bmfalkye.cards.CardRegistry.getAllCards();
                    
                    // Фильтруем карты, которые можно купить (исключаем уже имеющиеся)
                    com.bmfalkye.storage.PlayerCardCollection collection = 
                        com.bmfalkye.storage.PlayerCardCollection.get(level);
                    java.util.List<String> playerCards = collection.getCards(player).stream()
                        .map(com.bmfalkye.cards.Card::getId)
                        .collect(java.util.stream.Collectors.toList());
                    
                    java.util.List<String> shopCardIds = new java.util.ArrayList<>();
                    java.util.List<Integer> shopCardPrices = new java.util.ArrayList<>();
                    
                    for (com.bmfalkye.cards.Card card : allCards) {
                        // Показываем только карты, которых у игрока нет
                        if (!playerCards.contains(card.getId())) {
                            shopCardIds.add(card.getId());
                            // Цена зависит от редкости
                            int price = switch (card.getRarity()) {
                                case COMMON -> 50;
                                case RARE -> 150;
                                case EPIC -> 400;
                                case LEGENDARY -> 1000;
                            };
                            shopCardPrices.add(price);
                        }
                    }
                    
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new SendShopItemsPacket(playerCoins, shopCardIds, shopCardPrices),
                        player.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class BuyCardPacket {
        private final String cardId;
        private final int price;
        
        public BuyCardPacket(String cardId, int price) {
            this.cardId = cardId;
            this.price = price;
        }
        
        public static void encode(BuyCardPacket msg, FriendlyByteBuf buffer) {
            buffer.writeUtf(msg.cardId);
            buffer.writeInt(msg.price);
        }
        
        public static BuyCardPacket decode(FriendlyByteBuf buffer) {
            return new BuyCardPacket(buffer.readUtf(), buffer.readInt());
        }
        
        public static void handle(BuyCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                    com.bmfalkye.storage.PlayerCurrency currency = 
                        com.bmfalkye.storage.PlayerCurrency.get(level);
                    
                    if (currency.hasEnoughCoins(player, msg.price)) {
                        currency.removeCoins(player, msg.price);
                        com.bmfalkye.storage.PlayerCardCollection collection = 
                            com.bmfalkye.storage.PlayerCardCollection.get(level);
                        collection.addCard(player, msg.cardId);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aКарта куплена: " + msg.cardId));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cНедостаточно монет!"));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SendShopItemsPacket {
        private final int playerCoins;
        private final List<String> cardIds;
        private final List<Integer> cardPrices;
        
        public SendShopItemsPacket(int playerCoins, List<String> cardIds, List<Integer> cardPrices) {
            this.playerCoins = playerCoins;
            this.cardIds = cardIds;
            this.cardPrices = cardPrices;
        }
        
        public static void encode(SendShopItemsPacket msg, FriendlyByteBuf buffer) {
            buffer.writeInt(msg.playerCoins);
            buffer.writeInt(msg.cardIds.size());
            for (int i = 0; i < msg.cardIds.size(); i++) {
                buffer.writeUtf(msg.cardIds.get(i));
                buffer.writeInt(i < msg.cardPrices.size() ? msg.cardPrices.get(i) : 0);
            }
        }
        
        public static SendShopItemsPacket decode(FriendlyByteBuf buffer) {
            int coins = buffer.readInt();
            int count = buffer.readInt();
            List<String> ids = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(buffer.readUtf());
                prices.add(buffer.readInt());
            }
            return new SendShopItemsPacket(coins, ids, prices);
        }
        
        public static void handle(SendShopItemsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    com.bmfalkye.client.ClientPacketHandler.handleShopItems(msg);
                });
            });
            ctx.get().setPacketHandled(true);
        }
        
        public int getPlayerCoins() { return playerCoins; }
        public List<String> getCardIds() { return cardIds; }
        public List<Integer> getCardPrices() { return cardPrices; }
    }
}

