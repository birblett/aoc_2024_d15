package com.birblett.mixinterface;

import net.minecraft.util.math.BlockPos;

public interface Solver {

    default void setInstructions(String s, boolean b) {}
    default void setBoardCoords(BlockPos pos1, BlockPos pos2) {}
    default void incrementScore(int dy, int dx) {}
    default void setParameters(int dy, int dx, Solver parent) {}

}
