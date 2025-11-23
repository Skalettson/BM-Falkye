package com.bmfalkye.blocks;

import com.bmfalkye.client.FalkyeMainMenuScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Картографический Стол - основной блок Зала Дуэлей
 * Открывает доступ ко всем экранам мода
 */
public class CartographyTableBlock extends Block {
    public CartographyTableBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.bmfalkye.duelhall.DuelHallManager hallManager = 
                    com.bmfalkye.duelhall.DuelHallManager.get(serverLevel);
                
                com.bmfalkye.duelhall.DuelHallManager.DuelHall hall = 
                    hallManager.findHallAt(pos);
                
                if (hall == null || !hall.isActive()) {
                    // Пытаемся активировать Зал Дуэлей
                    hall = hallManager.checkAndActivateHall(serverLevel, pos, serverPlayer);
                    if (hall != null) {
                        player.sendSystemMessage(Component.literal("§aЗал Дуэлей активирован!"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cЗал Дуэлей не активирован. " +
                            "Установите все необходимые блоки: 1 Картографический Стол, 8 Книжных Полок, 1 Стол Алхимика"));
                        return InteractionResult.SUCCESS;
                    }
                }
                
                if (hall != null && hall.isActive()) {
                    // Открываем главное меню мода через пакет
                    com.bmfalkye.network.NetworkHandler.INSTANCE.sendTo(
                        new com.bmfalkye.network.NetworkHandler.OpenMainMenuPacket(),
                        serverPlayer.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                    player.sendSystemMessage(Component.literal("§6Добро пожаловать в Зал Дуэлей!"));
                } else {
                    player.sendSystemMessage(Component.literal("§cНе удалось активировать Зал Дуэлей"));
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

