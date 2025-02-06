package net.blay09.mods.balm.fabric.network;

import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.menu.BalmMenuProvider;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.blay09.mods.balm.api.network.ClientboundMessageRegistration;
import net.blay09.mods.balm.api.network.MessageRegistration;
import net.blay09.mods.balm.api.network.ServerboundMessageRegistration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FabricBalmNetworking implements BalmNetworking {

    private static final Map<Class<?>, MessageRegistration<?>> messagesByClass = new HashMap<>();
    private static final Map<ResourceLocation, MessageRegistration<?>> messagesByIdentifier = new HashMap<>();

    private static final List<ClientboundMessageRegistration<?>> clientMessageRegistrations = new ArrayList<>();

    private final Set<String> registeredMods = new HashSet<>();
    private final Set<String> clientOnlyMods = new HashSet<>();
    private final Set<String> serverOnlyMods = new HashSet<>();

    private static Player replyPlayer;

    public Set<String> getRegisteredMods() {
        return registeredMods;
    }

    public boolean isClientOnly(String modId) {
        return clientOnlyMods.contains(modId);
    }

    public boolean isServerOnly(String modId) {
        return serverOnlyMods.contains(modId);
    }

    @Override
    public void allowClientOnly(String modId) {
        clientOnlyMods.add(modId);
    }

    @Override
    public void allowServerOnly(String modId) {
        serverOnlyMods.add(modId);
    }

    @Override
    public void openGui(Player player, MenuProvider menuProvider) {
        if (menuProvider instanceof BalmMenuProvider balmMenuProvider) {
            player.openMenu(new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                    balmMenuProvider.writeScreenOpeningData(player, buf);
                }

                @Override
                public Component getDisplayName() {
                    return balmMenuProvider.getDisplayName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                    return balmMenuProvider.createMenu(i, inventory, player);
                }
            });
        } else {
            player.openMenu(menuProvider);
        }
    }

    @Override
    public <T> void reply(T message) {
        if (replyPlayer == null) {
            throw new IllegalStateException("No player to reply to");
        }

        sendTo(replyPlayer, message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void sendTo(Player player, T message) {
        MessageRegistration<T> messageRegistration = (MessageRegistration<T>) messagesByClass.get(message.getClass());
        ResourceLocation identifier = messageRegistration.getIdentifier();
        FriendlyByteBuf buf = PacketByteBufs.create();
        messageRegistration.getEncodeFunc().accept(message, buf);
        ServerPlayNetworking.send((ServerPlayer) player, identifier, buf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void sendToTracking(ServerLevel world, BlockPos pos, T message) {
        MessageRegistration<T> messageRegistration = (MessageRegistration<T>) messagesByClass.get(message.getClass());
        ResourceLocation identifier = messageRegistration.getIdentifier();
        FriendlyByteBuf buf = PacketByteBufs.create();
        messageRegistration.getEncodeFunc().accept(message, buf);
        for (ServerPlayer player : PlayerLookup.tracking(world, pos)) {
            ServerPlayNetworking.send(player, identifier, buf);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void sendToTracking(Entity entity, T message) {
        MessageRegistration<T> messageRegistration = (MessageRegistration<T>) messagesByClass.get(message.getClass());
        ResourceLocation identifier = messageRegistration.getIdentifier();
        FriendlyByteBuf buf = PacketByteBufs.create();
        messageRegistration.getEncodeFunc().accept(message, buf);
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, identifier, buf);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void sendToAll(MinecraftServer server, T message) {
        MessageRegistration<T> messageRegistration = (MessageRegistration<T>) messagesByClass.get(message.getClass());
        ResourceLocation identifier = messageRegistration.getIdentifier();
        FriendlyByteBuf buf = PacketByteBufs.create();
        messageRegistration.getEncodeFunc().accept(message, buf);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, identifier, buf);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void sendToServer(T message) {
        MessageRegistration<T> messageRegistration = (MessageRegistration<T>) messagesByClass.get(message.getClass());
        ResourceLocation identifier = messageRegistration.getIdentifier();
        FriendlyByteBuf buf = PacketByteBufs.create();
        messageRegistration.getEncodeFunc().accept(message, buf);
        ClientPlayNetworking.send(identifier, buf);
    }

    @Override
    public <T> void registerClientboundPacket(ResourceLocation identifier, Class<T> clazz, BiConsumer<T, FriendlyByteBuf> encodeFunc, Function<FriendlyByteBuf, T> decodeFunc, BiConsumer<Player, T> handler) {
        registeredMods.add(identifier.getNamespace());
        ClientboundMessageRegistration<T> messageRegistration = new ClientboundMessageRegistration<>(identifier, clazz, encodeFunc, decodeFunc, handler);

        messagesByClass.put(clazz, messageRegistration);
        messagesByIdentifier.put(identifier, messageRegistration);

        clientMessageRegistrations.add(messageRegistration);
    }

    @Override
    public <T> void registerServerboundPacket(ResourceLocation identifier, Class<T> clazz, BiConsumer<T, FriendlyByteBuf> encodeFunc, Function<FriendlyByteBuf, T> decodeFunc, BiConsumer<ServerPlayer, T> handler) {
        registeredMods.add(identifier.getNamespace());
        MessageRegistration<T> messageRegistration = new ServerboundMessageRegistration<>(identifier, clazz, encodeFunc, decodeFunc, handler);

        messagesByClass.put(clazz, messageRegistration);
        messagesByIdentifier.put(identifier, messageRegistration);
        ServerPlayNetworking.registerGlobalReceiver(identifier, ((server, player, listener, buf, responseSender) -> {
            T message = messageRegistration.getDecodeFunc().apply(buf);
            server.execute(() -> {
                replyPlayer = player;
                handler.accept(player, message);
                replyPlayer = null;
            });
        }));
    }

    public static void initializeClientHandlers() {
        for (ClientboundMessageRegistration<?> message : clientMessageRegistrations) {
            registerClientHandler(message);
        }
    }

    private static <T> void registerClientHandler(ClientboundMessageRegistration<T> messageRegistration) {
        ResourceLocation identifier = messageRegistration.getIdentifier();
        BiConsumer<Player, T> handler = messageRegistration.getHandler();
        ClientPlayNetworking.registerGlobalReceiver(identifier, ((client, listener, buf, responseSender) -> {
            T message = messageRegistration.getDecodeFunc().apply(buf);
            client.execute(() -> handler.accept(BalmClient.getClientPlayer(), message));
        }));
    }
}
