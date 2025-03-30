package com.bliss.offload.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class OffloadNetworking {

    private static final String PROTOCAL = "1";
    public static final SimpleChannel INSTNACE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("offload", "networking"),
            () -> PROTOCAL,
            PROTOCAL::equals,
            PROTOCAL::equals
    );

    public static void registerPackets() {
        INSTNACE.registerMessage(0, RequestChunkPacket.class,
        RequestChunkPacket::toBytes,
        RequestChunkPacket::new,
        RequestChunkPacket::handle);
    }

}
