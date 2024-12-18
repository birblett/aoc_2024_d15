package com.birblett.command;

import com.birblett.mixinterface.Solver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SolveCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((CommandManager.literal("solve")
                .requires(ServerCommandSource::isExecutedByPlayer))
                .then(CommandManager.literal("1").executes(source -> solver(source, false)))
                .then(CommandManager.literal("2").executes(source -> solver(source, true))));
    }

    public static int solver(CommandContext<ServerCommandSource> source, boolean p2) {
        File[] files = new File(source.getSource().getServer().getPath("").toString()).listFiles((d, name) -> name.endsWith(".txt") && name.startsWith("in"));
        if (files != null && files.length > 0) {
            try {
                String[] s = Files.readString(Paths.get(files[0].toURI())).split("\r\n\r\n|\n\n");
                String[] board = s[0].split("\r\n|\n");
                ServerPlayerEntity player = source.getSource().getPlayer();
                if (player != null) {
                    ServerWorld world = player.getServerWorld();
                    int x = (int) (player.getX()) + (board[0].length() / (p2 ? 1 : 2)), y = (int) player.getY(), z = (int) (player.getZ()) - (board.length / (p2 ? 1 : 2));
                    for (int dx = 0; dx < board.length; dx++) {
                        for (int az = 0; az < board[0].length(); az++) {
                            int dz = p2 ? az * 2 : az;
                            switch (board[dx].charAt(az)) {
                                case '#':
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y, z + dz), Blocks.BEE_NEST.getDefaultState());
                                    if (p2) world.setBlockState(BlockPos.ofFloored(x - dx, y, z + dz + 1), Blocks.BEE_NEST.getDefaultState());
                                    break;
                                case 'O':
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y, z + dz), Blocks.ANCIENT_DEBRIS.getDefaultState());
                                    if (p2) world.setBlockState(BlockPos.ofFloored(x - dx, y, z + dz + 1), Blocks.PINK_STAINED_GLASS.getDefaultState());
                                    break;
                                case '@':
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y, z + dz), Blocks.GLASS.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y + 1, z + dz), Blocks.SLIME_BLOCK.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y + 3, z + dz), Blocks.RAIL.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y + 2, z + dz + 1), Blocks.WARPED_FENCE.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y + 2, z + dz - 1), Blocks.WARPED_FENCE.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx + 1, y + 2, z + dz), Blocks.WARPED_FENCE.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx - 1, y + 2, z + dz), Blocks.WARPED_FENCE.getDefaultState());
                                    world.setBlockState(BlockPos.ofFloored(x - dx, y + 2, z + dz), Blocks.SLIME_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                                    player.teleport(world, x - dx, y + board.length / (p2 ? 2 : 3), z + dz + 2, -90, 90);
                                    MinecartEntity entity = new MinecartEntity(world, x - dx, y + 3, z + dz + 0.5);
                                    ((Solver) entity).setInstructions(s[1].replaceAll("\r\n|\n", ""), p2);
                                    ((Solver) entity).setBoardCoords(BlockPos.ofFloored(x - 1, y, z + (p2 ? 2 : 1)),
                                            BlockPos.ofFloored(x - board.length + 3, y, z + (board[0].length() - 1) * (p2 ? 2 : 1)));
                                    world.spawnEntity(entity);
                                    //player.startRiding(entity, true);
                            }
                        }
                    }

                }
            } catch (IOException e) {
                source.getSource().sendMessage(Text.of("."));
            }
        }
        return 1;
    }

}
