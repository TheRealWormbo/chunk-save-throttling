package de.wormbo.chunksavethrottling;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * All relevant logic is actually in {@link de.wormbo.chunksavethrottling.mixin.ChunkMapMixin}.
 */
@Mod("chunksavethrottling")
public class ChunkSaveThrottling {
    private static final Logger LOGGER = LogManager.getLogger();

    public ChunkSaveThrottling() {
        LOGGER.info("Chunk save throttling active.");
    }
}
