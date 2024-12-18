package com.birblett.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHandler.class)
public abstract class PistonHandlerMixin {

    @Shadow protected abstract boolean tryMove(BlockPos pos, Direction dir);
    @Shadow @Final private World world;
    @Shadow @Final private Direction motionDirection;

    @ModifyExpressionValue(method = "tryMove", at = @At(value = "CONSTANT", args = "intValue=12"))
    private int pushLimit1(int original) {
        return Integer.MAX_VALUE;
    }

    @ModifyExpressionValue(method = "tryMove", at = @At(value = "CONSTANT", args = "intValue=12", ordinal = 1))
    private int pushLimit2(int original) {
        return Integer.MAX_VALUE;
    }

    @ModifyExpressionValue(method = "tryMove", at = @At(value = "CONSTANT", args = "intValue=12", ordinal = 2))
    private int pushLimit3(int original) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "isBlockSticky", at = @At("HEAD"), cancellable = true)
    private static void stickyDebris(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS || state.getBlock() == Blocks.PINK_STAINED_GLASS) cir.setReturnValue(true);
    }

    @Inject(method = "isAdjacentBlockStuck", at = @At("HEAD"), cancellable = true)
    private static void noStickDebris(BlockState state, BlockState adjacentState, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS || adjacentState.getBlock() == Blocks.ANCIENT_DEBRIS) cir.setReturnValue(false);
        if (state.getBlock() == Blocks.PINK_STAINED_GLASS || adjacentState.getBlock() == Blocks.PINK_STAINED_GLASS) cir.setReturnValue(false);
    }

    @Inject(method = "tryMoveAdjacentBlock", at = @At("HEAD"), cancellable = true)
    private void moveDebrisAdjacent(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Block b = this.world.getBlockState(pos).getBlock();
        if (b == Blocks.ANCIENT_DEBRIS || b == Blocks.PINK_STAINED_GLASS) {
            Block other = b == Blocks.ANCIENT_DEBRIS ? Blocks.PINK_STAINED_GLASS : Blocks.ANCIENT_DEBRIS;
            BlockPos movePos = b == Blocks.ANCIENT_DEBRIS ? pos.add(0, 0, 1) : pos.add(0, 0, -1);
            if (!pos.add(this.motionDirection.getVector()).equals(movePos) && this.world.getBlockState(movePos).getBlock() == other &&
                    !this.tryMove(movePos, this.motionDirection)) cir.setReturnValue(false);
            else cir.setReturnValue(true);
        }
    }

}
