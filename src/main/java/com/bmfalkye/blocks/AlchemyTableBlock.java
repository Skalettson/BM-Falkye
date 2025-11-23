package com.bmfalkye.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Стол Алхимика - часть структуры Зала Дуэлей
 */
public class AlchemyTableBlock extends Block {
    public AlchemyTableBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        // Стол Алхимика используется только для структуры, не имеет отдельного функционала
        return InteractionResult.PASS;
    }
}

