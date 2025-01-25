package net.blay09.mods.balm.forge.event;


import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerBeforeStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ForgeBalmCommonEvents {

    public static void registerEvents(ForgeBalmEvents events) {
        events.registerTickEvent(TickType.Server, TickPhase.Start, (ServerTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.START) {
                    handler.handle(ServerLifecycleHooks.getCurrentServer());
                }
            });
        });
        events.registerTickEvent(TickType.Server, TickPhase.End, (ServerTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.END) {
                    handler.handle(ServerLifecycleHooks.getCurrentServer());
                }
            });
        });
        events.registerTickEvent(TickType.ServerLevel, TickPhase.Start, (ServerLevelTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.START && orig.side == LogicalSide.SERVER) {
                    handler.handle(orig.level);
                }
            });
        });
        events.registerTickEvent(TickType.ServerLevel, TickPhase.End, (ServerLevelTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.END && orig.side == LogicalSide.SERVER) {
                    handler.handle(orig.level);
                }
            });
        });

        events.registerTickEvent(TickType.ServerPlayer, TickPhase.Start, (ServerPlayerTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.START && orig.side == LogicalSide.SERVER) {
                    handler.handle(((ServerPlayer) orig.player));
                }
            });
        });

        events.registerTickEvent(TickType.ServerPlayer, TickPhase.End, (ServerPlayerTickHandler handler) -> {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent orig) -> {
                if (orig.phase == TickEvent.Phase.END && orig.side == LogicalSide.SERVER) {
                    handler.handle(((ServerPlayer) orig.player));
                }
            });
        });

        events.registerEvent(ServerBeforeStartingEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.server.ServerAboutToStartEvent orig) -> {
                final ServerBeforeStartingEvent event = new ServerBeforeStartingEvent(orig.getServer());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ServerStartedEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.server.ServerStartedEvent orig) -> {
                final ServerStartedEvent event = new ServerStartedEvent(orig.getServer());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ServerStoppedEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.server.ServerStoppedEvent orig) -> {
                final ServerStoppedEvent event = new ServerStoppedEvent(orig.getServer());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(UseBlockEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerInteractEvent.RightClickBlock orig) -> {
                final UseBlockEvent event = new UseBlockEvent(orig.getEntity(), orig.getLevel(), orig.getHand(), orig.getHitVec());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCancellationResult(event.getInteractionResult());
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(UseItemEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerInteractEvent.RightClickItem orig) -> {
                final UseItemEvent event = new UseItemEvent(orig.getEntity(), orig.getLevel(), orig.getHand());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCancellationResult(event.getInteractionResult());
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(PlayerLoginEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.PlayerLoggedInEvent orig) -> {
                final PlayerLoginEvent event = new PlayerLoginEvent((ServerPlayer) orig.getEntity());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(PlayerLogoutEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.PlayerLoggedOutEvent orig) -> {
                final PlayerLogoutEvent event = new PlayerLogoutEvent((ServerPlayer) orig.getEntity());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(BreakBlockEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (BlockEvent.BreakEvent orig) -> {
                BlockEntity blockEntity = orig.getLevel().getBlockEntity(orig.getPos());
                final BreakBlockEvent event = new BreakBlockEvent((Level) orig.getLevel(), orig.getPlayer(), orig.getPos(), orig.getState(), blockEntity);
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(PlayerRespawnEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.PlayerRespawnEvent orig) -> {
                final PlayerRespawnEvent event = new PlayerRespawnEvent(((ServerPlayer) orig.getEntity()), (ServerPlayer) orig.getEntity());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(LivingFallEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.entity.living.LivingFallEvent orig) -> {
                final LivingFallEvent event = new LivingFallEvent(orig.getEntity());
                events.fireEventHandlers(priority, event);

                if (event.getFallDamageOverride() != null) {
                    orig.setDamageMultiplier(0f);
                    event.getEntity().hurt(event.getEntity().level().damageSources().fall(), event.getFallDamageOverride());
                }

                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(LivingDamageEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.entity.living.LivingDamageEvent orig) -> {
                final LivingDamageEvent event = new LivingDamageEvent(orig.getEntity(), orig.getSource(), orig.getAmount());
                events.fireEventHandlers(priority, event);
                orig.setAmount(event.getDamageAmount());
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(CropGrowEvent.Pre.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (BlockEvent.CropGrowEvent.Pre orig) -> {
                if (orig.getLevel() instanceof Level level) {
                    final CropGrowEvent.Pre event = new CropGrowEvent.Pre(level, orig.getPos(), orig.getState());
                    events.fireEventHandlers(priority, event);
                    if (event.isCanceled()) {
                        orig.setResult(Event.Result.DENY);
                    }
                }
            });
        });

        events.registerEvent(CropGrowEvent.Post.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (BlockEvent.CropGrowEvent.Post orig) -> {
                if (orig.getLevel() instanceof Level level) {
                    final CropGrowEvent.Post event = new CropGrowEvent.Post(level, orig.getPos(), orig.getState());
                    events.fireEventHandlers(priority, event);
                }
            });
        });

        events.registerEvent(ChunkTrackingEvent.Start.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ChunkWatchEvent.Watch orig) -> {
                final ChunkTrackingEvent.Start event = new ChunkTrackingEvent.Start(orig.getLevel(), orig.getPlayer(), orig.getPos());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ChunkTrackingEvent.Stop.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ChunkWatchEvent.UnWatch orig) -> {
                final ChunkTrackingEvent.Stop event = new ChunkTrackingEvent.Stop(orig.getLevel(), orig.getPlayer(), orig.getPos());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(TossItemEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (ItemTossEvent orig) -> {
                final TossItemEvent event = new TossItemEvent(orig.getPlayer(), orig.getEntity().getItem());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(PlayerAttackEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (AttackEntityEvent orig) -> {
                final PlayerAttackEvent event = new PlayerAttackEvent(orig.getEntity(), orig.getTarget());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(LivingHealEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.entity.living.LivingHealEvent orig) -> {
                final LivingHealEvent event = new LivingHealEvent(orig.getEntity(), orig.getAmount());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(DigSpeedEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.BreakSpeed orig) -> {
                final DigSpeedEvent event = new DigSpeedEvent(orig.getEntity(), orig.getState(), orig.getOriginalSpeed());
                events.fireEventHandlers(priority, event);
                if (event.getSpeedOverride() != null) {
                    orig.setNewSpeed(event.getSpeedOverride());
                }
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(PlayerChangedDimensionEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.PlayerChangedDimensionEvent orig) -> {
                final PlayerChangedDimensionEvent event = new PlayerChangedDimensionEvent((ServerPlayer) orig.getEntity(), orig.getFrom(), orig.getTo());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ItemCraftedEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (PlayerEvent.ItemCraftedEvent orig) -> {
                final ItemCraftedEvent event = new ItemCraftedEvent(orig.getEntity(), orig.getCrafting(), orig.getInventory());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(LivingDeathEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.entity.living.LivingDeathEvent orig) -> {
                final LivingDeathEvent event = new LivingDeathEvent(orig.getEntity(), orig.getSource());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(EntityAddedEvent.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (EntityJoinLevelEvent orig) -> {
                final EntityAddedEvent event = new EntityAddedEvent(orig.getEntity(), orig.getLevel());
                events.fireEventHandlers(priority, event);
                if (event.isCanceled()) {
                    orig.setCanceled(true);
                }
            });
        });

        events.registerEvent(ChunkEvent.Load.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.level.ChunkEvent.Load orig) -> {
                final ChunkEvent.Load event = new ChunkEvent.Load(orig.getLevel(), orig.getChunk());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(ChunkEvent.Unload.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.level.ChunkEvent.Unload orig) -> {
                final ChunkEvent.Unload event = new ChunkEvent.Unload(orig.getLevel(), orig.getChunk());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(LevelEvent.Load.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.level.LevelEvent.Load orig) -> {
                final LevelEvent.Load event = new LevelEvent.Load(orig.getLevel());
                events.fireEventHandlers(priority, event);
            });
        });

        events.registerEvent(LevelEvent.Unload.class, priority -> {
            MinecraftForge.EVENT_BUS.addListener(ForgeBalmEvents.toForge(priority), (net.minecraftforge.event.level.LevelEvent.Unload orig) -> {
                final LevelEvent.Unload event = new LevelEvent.Unload(orig.getLevel());
                events.fireEventHandlers(priority, event);
            });
        });

    }

}
