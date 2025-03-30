package com.bliss.offload.client;

import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.RandomState;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientChunkHandler {

    public static void generateAndSendChunk(int chunkX, int chunkZ, long worldSeed, byte[] generatorByte) {
        CompletableFuture.runAsync(() -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel clientWorld = mc.level;
            if (clientWorld == null) return;

            try {
                ChunkGenerator generator = deserializeChunkGenerator(generatorByte, clientWorld);
            

                RandomState randomState = RandomState.create(
                    clientWorld.registryAccess().asGetterLookup(),
                    clientWorld.registryAccess()
                    .registryOrThrow(Registries.NOISE_SETTINGS)
                    .holders().findFirst().get().unwrapKey().get(),
                    worldSeed
                );

                
                LevelChunk chunk = generateChunk(chunkX, chunkZ, generator, randomState, clientWorld);

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to generate chunk: " + e.getMessage());
            }
        });
    }

    private static LevelChunk generateChunk(int chunkX, int chunkZ, ChunkGenerator generator, RandomState randomState, ClientLevel world) {
        try {
            ProtoChunk protoChunk = new ProtoChunk(
                new ChunkPos(chunkX, chunkZ),
                UpgradeData.EMPTY,
                world,
                world.registryAccess().registryOrThrow(Registries.BIOME),
                null
            );

            List<ChunkAccess> surroundingChunks = new ArrayList<>();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        surroundingChunks.add(protoChunk);
                    } else {
                        surroundingChunks.add(new ProtoChunk(
                            new ChunkPos(chunkX + x, chunkZ + z),
                            UpgradeData.EMPTY,
                            world,
                            world.registryAccess().registryOrThrow(Registries.BIOME),
                            null
                        ));
                    }
                }
            }
            WorldGenRegion worldGenRegion = new WorldGenRegion(
                world.getServer().getLevel(world.dimension()),
                surroundingChunks,
                ChunkStatus.SURFACE,
                0
            );

            StructureManager structureManager = new StructureManager(
                worldGenRegion,  // Using WorldGenRegion as LevelAccessor
                null,
                null
            );

            LevelChunk.PostLoadProcessor postProcessor = null;
            
            generator.buildSurface(worldGenRegion, structureManager, randomState, protoChunk);

            return new LevelChunk(world.getServer().getLevel(world.dimension()), protoChunk, postProcessor);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to generate chunk: " + e.getMessage());
            return null;
        }
    }

    private static ChunkGenerator deserializeChunkGenerator(byte[] data, ClientLevel world) {
        return deserializeWithCodec(ChunkGenerator.CODEC, data, world.registryAccess());
    }

    private static <T> T deserializeWithCodec(Codec<T> codec, byte[] data, RegistryAccess registryAccess) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            // Read the NBT data
            CompoundTag tag = NbtIo.read(dis);
            
            // Check if we have a wrapper tag with a "value" key
            if (tag.contains("value")) {
                Tag valueTag = tag.get("value");
                return codec.parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), valueTag)
                        .getOrThrow(false, error -> System.err.println("Failed to parse: " + error));
            } else {
                // Directly parse the compound tag
                return codec.parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), tag)
                        .getOrThrow(false, error -> System.err.println("Failed to parse: " + error));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize data", e);
        }
    }
    
}


