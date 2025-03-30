package com.bliss.offload.server;

import com.bliss.offload.network.OffloadNetworking;
import com.bliss.offload.network.RequestChunkPacket;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServerChunkManager {

    public static void requestChunkFromClient(ServerPlayer player, int chunkX, int chunkZ) {

        // Get world info
        ServerLevel world = player.serverLevel();
        long worldSeed = world.getSeed();

        // Get generator and serialise
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        byte[] generatorByte = serializeWithCodec(ChunkGenerator.CODEC, generator, world.registryAccess());


        // Send packet to client
        RequestChunkPacket packet = new RequestChunkPacket(chunkX, chunkZ, worldSeed, generatorByte);
        //OffloadNetworking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // Serializer
    public static <T> byte[] serializeWithCodec(Codec<T> codec, T object, net.minecraft.core.RegistryAccess registryAccess) {
        RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, registryAccess);

        // Encode object into Tag using NbtOps
        Tag tag = codec.encodeStart(registryOps, object).getOrThrow(false, System.err::println);
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            // Properly write the NBT data to the stream
            if (tag instanceof CompoundTag compoundTag) {
                NbtIo.write(compoundTag, dos);
            } else {
                // If not a compound tag, convert to compound and store as value
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("value", tag);
                NbtIo.write(wrapper, dos);
            }
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize data", e);
        }
    }
}
