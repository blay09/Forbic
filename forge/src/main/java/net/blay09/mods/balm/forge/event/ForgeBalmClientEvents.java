package net.blay09.mods.balm.forge.event;

import net.blay09.mods.balm.api.event.client.RecipesUpdatedEvent;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.balm.api.event.client.*;
import net.blay09.mods.balm.api.event.client.RenderHandEvent;
import net.blay09.mods.balm.api.event.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

public class ForgeBalmClientEvents {

    public static void registerEvents(ForgeBalmEvents events) {
        events.registerTickEvent(TickType.Client, TickPhase.Start, (ClientTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.START) {
                    handler.handle(Minecraft.getInstance());
                }
            });
        });
        events.registerTickEvent(TickType.Client, TickPhase.End, (ClientTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.END) {
                    handler.handle(Minecraft.getInstance());
                }
            });
        });
        events.registerTickEvent(TickType.ClientLevel, TickPhase.Start, (ClientLevelTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.START) {
                    handler.handle(Minecraft.getInstance().level);
                }
            });
        });
        events.registerTickEvent(TickType.ClientLevel, TickPhase.End, (ClientLevelTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.END) {
                    handler.handle(Minecraft.getInstance().level);
                }
            });
        });

        events.registerEvent(ClientStartedEvent.class, priority -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeBalmEvents.toForge(priority), (FMLLoadCompleteEvent orig) -> {
                orig.enqueueWork(() -> {
                    final ClientStartedEvent event = new ClientStartedEvent(Minecraft.getInstance());
                    events.fireEventHandlers(priority, event);
                });
            });
        });

        events.registerEvent(ConnectedToServerEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ClientPlayerNetworkEvent.LoggingIn orig) -> {
                final ConnectedToServerEvent event = new ConnectedToServerEvent(Minecraft.getInstance());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(DisconnectedFromServerEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ClientPlayerNetworkEvent.LoggingOut orig) -> {
                final DisconnectedFromServerEvent event = new DisconnectedFromServerEvent(Minecraft.getInstance());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ScreenInitEvent.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.Init.Pre orig) -> {
                final ScreenInitEvent.Pre event = new ScreenInitEvent.Pre(orig.getScreen());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenInitEvent.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.Init.Post orig) -> {
                final ScreenInitEvent.Post event = new ScreenInitEvent.Post(orig.getScreen());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ScreenDrawEvent.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.Render.Pre orig) -> {
                final ScreenDrawEvent.Pre event = new ScreenDrawEvent.Pre(orig.getScreen(),
                        orig.getGuiGraphics(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getPartialTick());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ContainerScreenDrawEvent.Background.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ContainerScreenEvent.Render.Background orig) -> {
                final ContainerScreenDrawEvent.Background event = new ContainerScreenDrawEvent.Background(orig.getContainerScreen(),
                        orig.getGuiGraphics(),
                        orig.getMouseX(),
                        orig.getMouseY());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ContainerScreenDrawEvent.Foreground.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ContainerScreenEvent.Render.Foreground orig) -> {
                final ContainerScreenDrawEvent.Foreground event = new ContainerScreenDrawEvent.Foreground(orig.getContainerScreen(),
                        orig.getGuiGraphics(),
                        orig.getMouseX(),
                        orig.getMouseY());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ScreenDrawEvent.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.Render.Post orig) -> {
                final ScreenDrawEvent.Post event = new ScreenDrawEvent.Post(orig.getScreen(),
                        orig.getGuiGraphics(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getPartialTick());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ScreenMouseEvent.Click.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseButtonPressed.Pre orig) -> {
                final ScreenMouseEvent.Click.Pre event = new ScreenMouseEvent.Click.Pre(orig.getScreen(), orig.getMouseX(), orig.getMouseY(), orig.getButton());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenMouseEvent.Click.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseButtonPressed.Post orig) -> {
                final ScreenMouseEvent.Click.Post event = new ScreenMouseEvent.Click.Post(orig.getScreen(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getButton());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenMouseEvent.Drag.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseDragged.Pre orig) -> {
                final ScreenMouseEvent.Drag.Pre event = new ScreenMouseEvent.Drag.Pre(orig.getScreen(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getMouseButton(),
                        orig.getDragX(),
                        orig.getDragY());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenMouseEvent.Drag.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseDragged.Post orig) -> {
                final ScreenMouseEvent.Drag.Post event = new ScreenMouseEvent.Drag.Post(orig.getScreen(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getMouseButton(),
                        orig.getDragX(),
                        orig.getDragY());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenMouseEvent.Release.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseButtonReleased.Pre orig) -> {
                final ScreenMouseEvent.Release.Pre event = new ScreenMouseEvent.Release.Pre(orig.getScreen(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getButton());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenMouseEvent.Release.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.MouseButtonReleased.Post orig) -> {
                final ScreenMouseEvent.Release.Post event = new ScreenMouseEvent.Release.Post(orig.getScreen(),
                        orig.getMouseX(),
                        orig.getMouseY(),
                        orig.getButton());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenKeyEvent.Press.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.KeyPressed.Pre orig) -> {
                final ScreenKeyEvent.Press.Pre event = new ScreenKeyEvent.Press.Pre(orig.getScreen(),
                        orig.getKeyCode(),
                        orig.getScanCode(),
                        orig.getModifiers());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenKeyEvent.Press.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.KeyPressed.Post orig) -> {
                final ScreenKeyEvent.Press.Post event = new ScreenKeyEvent.Press.Post(orig.getScreen(),
                        orig.getKeyCode(),
                        orig.getScanCode(),
                        orig.getModifiers());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenKeyEvent.Release.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.KeyReleased.Pre orig) -> {
                final ScreenKeyEvent.Release.Pre event = new ScreenKeyEvent.Release.Pre(orig.getScreen(),
                        orig.getKeyCode(),
                        orig.getScanCode(),
                        orig.getModifiers());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ScreenKeyEvent.Release.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.KeyReleased.Post orig) -> {
                final ScreenKeyEvent.Release.Post event = new ScreenKeyEvent.Release.Post(orig.getScreen(),
                        orig.getKeyCode(),
                        orig.getScanCode(),
                        orig.getModifiers());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(FovUpdateEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ComputeFovModifierEvent orig) -> {
                final FovUpdateEvent event = new FovUpdateEvent(orig.getPlayer());
                events.fireEventHandlers(priority, event);
                if (event.getFov() != null) {
                    orig.setNewFovModifier(event.getFov());
                }
            });
        });

        events.registerEvent(ItemTooltipEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.entity.player.ItemTooltipEvent orig) -> {
                final ItemTooltipEvent event = new ItemTooltipEvent(orig.getItemStack(), orig.getEntity(), orig.getToolTip(), orig.getFlags());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(UseItemInputEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (InputEvent.InteractionKeyMappingTriggered orig) -> {
                if (orig.isUseItem()) {
                    final UseItemInputEvent event = new UseItemInputEvent(orig.getHand());
                    events.fireEventHandlers(priority, event);
                    if (event.isCanceled()) {
                        orig.setSwingHand(false);
                        orig.setCanceled(true);
                    }
                }
            });
        });

        events.registerEvent(RenderHandEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.client.event.RenderHandEvent orig) -> {
                final RenderHandEvent event = new RenderHandEvent(orig.getHand(), orig.getItemStack(), orig.getSwingProgress());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(KeyInputEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (InputEvent.Key orig) -> {
                final KeyInputEvent event = new KeyInputEvent(orig.getKey(), orig.getScanCode(), orig.getAction(), orig.getModifiers());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(BlockHighlightDrawEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (RenderHighlightEvent.Block orig) -> {
                final BlockHighlightDrawEvent event = new BlockHighlightDrawEvent(orig.getTarget(),
                        orig.getPoseStack(),
                        orig.getMultiBufferSource(),
                        orig.getCamera());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(OpenScreenEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ScreenEvent.Opening orig) -> {
                final OpenScreenEvent event = new OpenScreenEvent(orig.getScreen());
                events.fireEventHandlers(priority, event);
                if (event.getNewScreen() != null) {
                    orig.setNewScreen(event.getNewScreen());
                }
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });
    }
}
