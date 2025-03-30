package com.bliss.offload.network;

import com.bliss.offload.client.ClientChunkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestChunkPacket {
    private final int chunkX, chunkZ;
    private final long worldSeed;
    private final byte[] generatorByte;

    public RequestChunkPacket(int chunkX, int chunkZ, long worldSeed, byte[] generatorByte) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldSeed = worldSeed;
        this.generatorByte = generatorByte;
    }

    public RequestChunkPacket(FriendlyByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.worldSeed = buf.readLong();
        this.generatorByte = buf.readByteArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeLong(worldSeed);
        buf.writeByteArray(generatorByte);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientChunkHandler.generateAndSendChunk(chunkX, chunkZ, worldSeed, generatorByte);
        });
        ctx.get().setPacketHandled(true);
    }
}
