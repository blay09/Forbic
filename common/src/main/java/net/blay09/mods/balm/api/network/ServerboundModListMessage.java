package net.blay09.mods.balm.api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record ServerboundModListMessage(Map<String, String> modList) implements CustomPacketPayload {

    public static final Type<ServerboundModListMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("balm-fabric", "mod_list"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
