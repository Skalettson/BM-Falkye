package com.bmfalkye.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Блок для отображения трофеев (достижения, редкие карты)
 */
public class TrophyDisplayBlock extends Block {
    public TrophyDisplayBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ItemStack heldItem = player.getItemInHand(hand);
            
            // TODO: Реализовать размещение карт/достижений на блоке
            // Можно использовать TileEntity для хранения отображаемого предмета
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
}

