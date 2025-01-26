package net.blay09.mods.balm.api.event;

import net.minecraft.world.level.LevelAccessor;

public abstract class LevelLoadingEvent {
    private final LevelAccessor level;

    public LevelLoadingEvent(LevelAccessor level) {
        this.level = level;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    public static class Load extends LevelLoadingEvent {
        public Load(LevelAccessor level) {
            super(level);
        }
    }

    public static class Unload extends LevelLoadingEvent {
        public Unload(LevelAccessor level) {
            super(level);
        }
    }

}
