package com.birblett.mixin;

import com.birblett.mixinterface.Solver;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(AbstractMinecartEntity.class)
public class AbstractMinecartEntityMixin implements Solver {

    @Unique private String instructions = null;
    @Unique private int isIndex = 0;
    @Unique private int state = 0;
    @Unique private boolean p2 = false;
    @Unique private BlockPos last = null;
    @Unique private BlockPos last2 = null;
    @Unique private BlockPos pos1 = null;
    @Unique private BlockPos pos2 = null;
    @Unique private List<BlockPos> tntPos = null;
    @Unique private List<BlockPos> firePos = null;
    @Unique private int deathCounter = 0;
    @Unique private int score = 0;
    @Unique private static final BlockPos[] OFFSET = new BlockPos[]{new BlockPos(1, 0, 0), null, new BlockPos(-1, 0, 0),
                                                              new BlockPos(0, 0, -1), new BlockPos(0, 0, 1)};

    @Override
    public void setInstructions(String s, boolean b) {
        this.instructions = s;
        this.p2 = b;
    }

    @Override
    public void setBoardCoords(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public void incrementScore(int dy, int dx) {
        AbstractMinecartEntity self = (AbstractMinecartEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld world) world.getServer().getPlayerManager().getPlayerList().forEach(player ->
                player.sendMessage(Text.of(this.score + " + " + dy + " * 100 + " + dx + " = " + (this.score + dy * 100 + dx))));
        this.score += dy * 100 + dx;
        this.deathCounter = 1;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickMixin(CallbackInfo ci) {
        AbstractMinecartEntity self = (AbstractMinecartEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld world && this.instructions != null) {
            if (this.isIndex < this.instructions.length()) {
                switch (this.state) {
                    case 0:
                        BlockPos offset = AbstractMinecartEntityMixin.OFFSET[((this.instructions.charAt(this.isIndex)) % 9) >> 1];
                        Direction d = Direction.fromVector(-offset.getX(), offset.getY(), -offset.getZ());
                        world.setBlockState((this.last = self.getBlockPos().add(offset).add(0, -2, 0)), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, d));
                        world.setBlockState((this.last2 = self.getBlockPos().add(offset.multiply(2)).add(0, -2, 0)), Blocks.REDSTONE_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                        break;
                    case 1:
                        if (this.last2 != null) world.breakBlock(this.last2, false);
                        break;
                    case 2:
                        if (this.last != null) world.breakBlock(this.last, false);
                        this.isIndex++;
                }
                this.state = (this.state + 1) % 3;
            } else {
                if (this.isIndex++ == this.instructions.length()) {
                    self.setNoGravity(true);
                    self.setInvulnerable(true);
                    BlockPos centerPos = this.pos1.add(this.pos2.add(0, 0, -1));
                    self.teleport(world, centerPos.getX() / 2.0, centerPos.getY(), centerPos.getZ() / 2.0, Set.of(), 0, 0);
                    if (self.getFirstPassenger() instanceof ServerPlayerEntity player) {
                        world.setBlockState(self.getBlockPos().add(0, - 4, 0), Blocks.BARRIER.getDefaultState());
                        player.teleport(world, self.getX(), self.getY() - 3, self.getZ(), -90, 90);
                    }
                    this.tntPos = new ArrayList<>(List.of(pos1.add(0, 2, 0)));
                } else if (this.isIndex == this.instructions.length() + 50)
                    this.firePos = new ArrayList<>(List.of(BlockPos.ofFloored(0, -3, 0)));
                if ((this.state++ & 1) == 0) {
                    if (this.tntPos != null && !this.tntPos.isEmpty()) {
                        List<BlockPos> newBlocks = new ArrayList<>();
                        for (BlockPos pos : this.tntPos) {
                            TntEntity tntEntity = new TntEntity(world, pos.getX(), pos.getY(), pos.getZ(), null);
                            tntEntity.setFuse(0);
                            world.spawnEntity(tntEntity);
                            if (pos == this.tntPos.getFirst() && pos.getZ() < pos2.getZ() - 1) newBlocks.add(pos.add(0, 0, 2));
                            if (pos.getX() >= pos2.getX()) newBlocks.add(pos.add(-2, 0, 0));
                        }
                        this.tntPos = newBlocks;
                    }
                    if (this.firePos != null && !this.firePos.isEmpty()) {
                        List<BlockPos> newBlocks = new ArrayList<>();
                        for (BlockPos offset : this.firePos) {
                            BlockPos pos = offset.add(this.pos1);
                            world.setBlockState(pos, Blocks.SOUL_SOIL.getDefaultState());
                            world.setBlockState(pos.add(0, (this.deathCounter = 1), 0), Blocks.SOUL_FIRE.getDefaultState());
                            ArmorStandEntity armorStandEntity = new ArmorStandEntity(world, pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5);
                            ((Solver) armorStandEntity).setParameters(offset.getX() - 1, offset.getZ() + (this.p2 ? 2 : 1), this);
                            world.spawnEntity(armorStandEntity);
                            if (offset == this.firePos.getFirst() && pos.getZ() < pos2.getZ() - 1) newBlocks.add(offset.add(0, 0, 1));
                            if (pos.getX() >= pos2.getX()) newBlocks.add(offset.add(-1, 0, 0));
                        }
                        this.firePos = newBlocks;
                    }
                }
                if (this.deathCounter > 41) {
                    world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(Text.of("solution: " + this.score)));
                    self.discard();
                }
                if (this.deathCounter > 0) this.deathCounter++;
            }
        }
    }

}