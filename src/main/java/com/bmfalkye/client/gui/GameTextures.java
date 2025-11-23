package com.bmfalkye.client.gui;

import net.minecraft.resources.ResourceLocation;

/**
 * Утилита для работы с текстурами игрового интерфейса
 */
public class GameTextures {
    private static final String MOD_ID = "bm_falkye";
    
    // Текстуры карт по редкости
    public static final ResourceLocation CARD_COMMON = new ResourceLocation(MOD_ID, "textures/gui/cards_common.png");
    public static final ResourceLocation CARD_RARE = new ResourceLocation(MOD_ID, "textures/gui/cards_rare.png");
    public static final ResourceLocation CARD_EPIC = new ResourceLocation(MOD_ID, "textures/gui/cards_epic.png");
    public static final ResourceLocation CARD_LEGENDARY = new ResourceLocation(MOD_ID, "textures/gui/cards_legendary.png");
    
    // Текстуры игрового интерфейса
    public static final ResourceLocation GAME_GUI = new ResourceLocation(MOD_ID, "textures/gui/game_gui.png");
    public static final ResourceLocation GAME_INFO = new ResourceLocation(MOD_ID, "textures/gui/game_info.png");
    public static final ResourceLocation GAME_ACTIVITY_LOG = new ResourceLocation(MOD_ID, "textures/gui/game_activity_log.png");
    public static final ResourceLocation GAME_CARD_ROW = new ResourceLocation(MOD_ID, "textures/gui/game_card_row.png");
    public static final ResourceLocation PLAYER_DECK = new ResourceLocation(MOD_ID, "textures/gui/player_deck.png");
    public static final ResourceLocation OPPONENT_DECK = new ResourceLocation(MOD_ID, "textures/gui/opponent_deck.png");
    public static final ResourceLocation MOD_BUTTON = new ResourceLocation(MOD_ID, "textures/gui/mod_button.png");
    
    /**
     * Получает текстуру карты по редкости
     */
    public static ResourceLocation getCardTextureByRarity(com.bmfalkye.cards.CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> CARD_COMMON;
            case RARE -> CARD_RARE;
            case EPIC -> CARD_EPIC;
            case LEGENDARY -> CARD_LEGENDARY;
        };
    }
}

