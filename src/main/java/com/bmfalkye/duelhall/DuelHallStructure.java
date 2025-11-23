package com.bmfalkye.duelhall;

import com.bmfalkye.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка структуры Зала Дуэлей
 * Структура: 1 Картографический Стол + 8 Книжных Полок + 1 Стол Алхимика
 */
public class DuelHallStructure {
    private final BlockPos centerPos; // Позиция Картографического Стола
    private final List<BlockPos> bookshelfPositions = new ArrayList<>();
    private final BlockPos alchemyTablePos;
    
    private DuelHallStructure(BlockPos centerPos, List<BlockPos> bookshelfPositions, BlockPos alchemyTablePos) {
        this.centerPos = centerPos;
        this.bookshelfPositions.addAll(bookshelfPositions);
        this.alchemyTablePos = alchemyTablePos;
    }
    
    /**
     * Проверяет структуру Зала Дуэлей вокруг указанной позиции
     */
    public static DuelHallStructure checkStructure(ServerLevel level, BlockPos centerPos) {
        // Проверяем, что в центре стоит Картографический Стол
        BlockState centerState = level.getBlockState(centerPos);
        if (!centerState.is(ModBlocks.CARTOGRAPHY_TABLE.get())) {
            return null;
        }
        
        // Ищем 8 книжных полок в радиусе 5 блоков
        List<BlockPos> bookshelves = new ArrayList<>();
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.BOOKSHELF)) {
                        bookshelves.add(pos);
                    }
                }
            }
        }
        
        if (bookshelves.size() < 8) {
            return null; // Недостаточно книжных полок
        }
        
        // Ищем Стол Алхимика в радиусе 5 блоков
        BlockPos alchemyTablePos = null;
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.ALCHEMY_TABLE.get())) {
                        alchemyTablePos = pos;
                        break;
                    }
                }
                if (alchemyTablePos != null) break;
            }
            if (alchemyTablePos != null) break;
        }
        
        if (alchemyTablePos == null) {
            return null; // Стол Алхимика не найден
        }
        
        return new DuelHallStructure(centerPos, bookshelves, alchemyTablePos);
    }
    
    public boolean contains(BlockPos pos) {
        return pos.equals(centerPos) || 
               pos.equals(alchemyTablePos) || 
               bookshelfPositions.contains(pos);
    }
    
    public BlockPos getCenterPos() { return centerPos; }
    public List<BlockPos> getBookshelfPositions() { return bookshelfPositions; }
    public BlockPos getAlchemyTablePos() { return alchemyTablePos; }
}

