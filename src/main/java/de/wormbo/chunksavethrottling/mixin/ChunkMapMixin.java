package de.wormbo.chunksavethrottling.mixin;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Date;

/**
 * Hook into the {@link ChunkMap} code to prevent too frequent chunk saving.
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Long2LongOpenHashMap nextChunkSaveTimeMillis = new Long2LongOpenHashMap();

    /**
     * Before saving a chunk, make sure it wasn't already saved in the last 10 seconds.
     */
    @Inject(method = "saveChunkIfNeeded", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z"),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    protected void checkIfSavingIsReallyNecessary(ChunkHolder chunkholder, CallbackInfoReturnable<Boolean> cir,
                                                  ChunkAccess chunkaccess) {
        final long chunkId = chunkaccess.getPos().toLong();
        long nextAllowedSaveTime = this.nextChunkSaveTimeMillis.getOrDefault(chunkId, -1L);
        if (nextAllowedSaveTime > System.currentTimeMillis()) {
            cir.setReturnValue(false);
            cir.cancel();
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Saving chunk {} canceled, next allowed time: {}", chunkId, new Date(nextAllowedSaveTime));
        } else {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Saving chunk {} allowed", chunkId);
        }
    }

    /**
     * After a chunk was successfully saved, set its next allowed save time to 10 seconds from now.
     */
    @Inject(method = "saveChunkIfNeeded", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/level/ChunkHolder;refreshAccessibility()V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    protected void setEarliestNextChunkSaveTimeMillis(ChunkHolder chunkholder, CallbackInfoReturnable<Boolean> cir,
                                                      ChunkAccess chunkaccess, boolean wasSaved) {
        final long chunkId = chunkaccess.getPos().toLong();
        if (wasSaved) {
            long newAllowedSaveTime = System.currentTimeMillis() + 10000L;
            this.nextChunkSaveTimeMillis.put(chunkId, newAllowedSaveTime);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Chunk {} can be saved again after {}", chunkId, new Date(newAllowedSaveTime));
        } else {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Chunk {} was not saved", chunkId);
        }
    }

    /**
     * Discard the next allowed save time for a chunk when it gets unloaded.
     */
    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange" +
                    "(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V"))
    protected void removeNextChunkSaveTime(ChunkProgressListener progressListener, ChunkPos chunkPos,
                                           ChunkStatus status) {
        progressListener.onStatusChange(chunkPos, status);
        if (status == null) {
            long chunkId = chunkPos.toLong();
            long lastAllowedSaveTime = this.nextChunkSaveTimeMillis.remove(chunkId);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Removing chunk {} from delay map, last time: {}", chunkId, new Date(lastAllowedSaveTime));
        }
    }
}
