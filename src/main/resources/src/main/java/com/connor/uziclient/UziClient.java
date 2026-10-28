package com.uzi.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class UziClient implements ClientModInitializer {

    private static final int SCAN_RADIUS = 6;
    private static final double REPORT_THRESHOLD = 5.0;
    private static final int SCAN_COOLDOWN_TICKS = 100;

    private static boolean finderEnabled = false;
    private static int tickCounter = 0;
    private static final Set<Long> reportedChunks = new HashSet<>();

    private static KeyBinding toggleFinderKey;
    private static KeyBinding freeCamKey;

    // Weighted "anomaly" blocks — artificial/player-placed indicators
    private static final Map<Block, Double> SUSPICIOUS_WEIGHTS = new HashMap<>();
    static {
        SUSPICIOUS_WEIGHTS.put(Blocks.TORCH, 1.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.WALL_TORCH, 1.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.LANTERN, 1.5);
        SUSPICIOUS_WEIGHTS.put(Blocks.SOUL_LANTERN, 1.5);
        SUSPICIOUS_WEIGHTS.put(Blocks.GLOWSTONE, 2.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.SEA_LANTERN, 2.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.CHEST, 3.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.TRAPPED_CHEST, 3.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.FURNACE, 2.5);
        SUSPICIOUS_WEIGHTS.put(Blocks.SPAWNER, 4.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.CRAFTING_TABLE, 2.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.BOOKSHELF, 1.5);
        SUSPICIOUS_WEIGHTS.put(Blocks.ENCHANTING_TABLE, 3.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.OBSIDIAN, 1.0);
        SUSPICIOUS_WEIGHTS.put(Blocks.COBBLESTONE, 0.3);
        SUSPICIOUS_WEIGHTS.put(Blocks.STONE_BRICKS, 0.5);
    }

    @Override
    public void onInitializeClient() {
        toggleFinderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.uziclient.togglefinder",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.uziclient"
        ));

        freeCamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.uziclient.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.uziclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        while (toggleFinderKey.wasPressed()) {
            finderEnabled = !finderEnabled;
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Suspicious Chunk Finder: " + (finderEnabled ? "ON" : "OFF")),
                        false
                );
            }
            reportedChunks.clear();
        }

        while (freeCamKey.wasPressed()) {
            FreeCamManager.toggle(client);
        }

        if (!finderEnabled || client.player == null || client.world == null) return;

        tickCounter++;
        if (tickCounter < SCAN_COOLDOWN_TICKS) return;
        tickCounter = 0;

        scanNearbyChunks(client);
    }

    private void scanNearbyChunks(MinecraftClient client) {
        World world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        ChunkPos centerChunk = new ChunkPos(playerPos);

        List<ScoredChunk> results = new ArrayList<>();

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                ChunkPos pos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                long key = pos.toLong();
                if (reportedChunks.contains(key)) continue;

                if (!world.isChunkLoaded(pos.x, pos.z)) continue;
                WorldChunk chunk = world.getChunk(pos.x, pos.z);

                double score = scoreChunk(world, chunk, pos);
                if (score >= REPORT_THRESHOLD) {
                    results.add(new ScoredChunk(pos, score));
                    reportedChunks.add(key);
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));
        for (ScoredChunk sc : results) {
            client.player.sendMessage(
                    Text.literal(String.format(
                            "Suspicious chunk at [%d, %d] — score: %.1f",
                            sc.pos.getStartX(), sc.pos.getStartZ(), sc.score
                    )),
                    false
            );
        }
    }

    private double scoreChunk(World world, WorldChunk chunk, ChunkPos pos) {
        double score = 0.0;
        int minY = world.getBottomY();
        int maxY = minY + world.getHeight();

        int startX = pos.getStartX();
        int startZ = pos.getStartZ();

        for (int x = startX; x < startX + 16; x += 2) {
            for (int z = startZ; z < startZ + 16; z += 2) {
                int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);

                for (int y = minY; y < Math.min(surfaceY + 5, maxY); y += 2) {
                    BlockPos bp = new BlockPos(x, y, z);
                    Block block = world.getBlockState(bp).getBlock();

                    Double weight = SUSPICIOUS_WEIGHTS.get(block);
                    if (weight != null) {
                        score += weight;
                    }

                    // Bonus: artificial light with no natural source nearby (underground)
                    if (y < surfaceY - 3 && world.getLightLevel(bp) > 7) {
                        boolean hasLava = world.getBlockState(bp.down()).getBlock() == Blocks.LAVA
                                || world.getBlockState(bp.up()).getBlock() == Blocks.LAVA;
                        if (!hasLava) {
                            score += 0.5;
                        }
                    }
                }
            }
        }
        return score;
    }

    private static class ScoredChunk {
        final ChunkPos pos;
        final double score;
        ScoredChunk(ChunkPos pos, double score) {
            this.pos = pos;
            this.score = score;
        }
    }
}
