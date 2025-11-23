package com.bmfalkye.blocks;

import com.bmfalkye.territory.GuildTerritoryManager;
import com.bmfalkye.territory.TerritoryCaptureSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Блок точки силы (заброшенный алтарь, древний обелиск)
 */
public class TerritoryPointBlock extends Block {
    private final String pointName;
    
    public TerritoryPointBlock(Properties properties, String pointName) {
        super(properties);
        this.pointName = pointName;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level instanceof ServerLevel serverLevel) {
                GuildTerritoryManager manager = GuildTerritoryManager.get(serverLevel);
                
                // Регистрируем точку, если она ещё не зарегистрирована
                if (manager.getTerritoryPoint(pos) == null) {
                    manager.registerTerritoryPoint(pos, pointName);
                }
                
                // Обрабатываем взаимодействие через систему захвата
                TerritoryCaptureSystem.handleTerritoryInteraction(serverPlayer, pos, serverLevel);
            }
        }
        
        return InteractionResult.SUCCESS;
    }
}

