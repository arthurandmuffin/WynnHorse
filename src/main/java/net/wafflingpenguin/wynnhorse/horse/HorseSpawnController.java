package net.wafflingpenguin.wynnhorse.horse;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HorseSpawnController {
    private static final Logger LOGGER = LogUtils.getLogger();

    private UUID targetHorseId;
    private UUID startupClearHorseId;
    private int detectionTicksRemaining;
    private int retryDelayTicksRemaining;
    private int startupClearRetryTicksRemaining;
    private boolean awaitingDetection;
    private boolean startupHorseClearComplete;
    private String lastHorseScanDebugSummary = "";

    public HorseStepOutcome tick(final Minecraft minecraft, final HorseItemTracker horseItemTracker) {
        if (minecraft.player == null || minecraft.level == null) {
            return HorseStepOutcome.waiting(null);
        }

        if (minecraft.player.getVehicle() instanceof AbstractHorse riddenHorse) {
            this.targetHorseId = riddenHorse.getUUID();
            this.awaitingDetection = false;
            this.startupHorseClearComplete = true;
            return HorseStepOutcome.mounted(riddenHorse);
        }

        Optional<AbstractHorse> resolvedTarget = this.resolveTargetHorse(minecraft);
        if (resolvedTarget.isPresent()) {
            return HorseStepOutcome.readyToMount(resolvedTarget.get(), null);
        }

        if (!this.startupHorseClearComplete) {
            HorseStepOutcome startupClearOutcome = this.handleStartupHorseClear(minecraft, horseItemTracker);
            if (startupClearOutcome != null) {
                return startupClearOutcome;
            }
        }

        if (this.retryDelayTicksRemaining > 0) {
            this.retryDelayTicksRemaining--;
            return HorseStepOutcome.waiting(null);
        }

        if (!this.awaitingDetection) {
            Optional<HorseItemMatch> preferredMatch = horseItemTracker.selectPreferredMatch(minecraft);
            if (preferredMatch.isEmpty()) {
                return HorseStepOutcome.noHorseItem(Component.translatable("message.wynnhorse.automation.no_horse_item"));
            }

            if (!this.alignForHorseSpawn(minecraft)) {
                return HorseStepOutcome.waiting(null);
            }

            this.awaitingDetection = true;
            this.detectionTicksRemaining = WynnHorseConfig.getHorseSpawnDetectionTimeoutTicks();

            Component attemptMessage = Component.translatable("message.wynnhorse.horse_spawn_attempt", expectedHorseName(minecraft));
            if (minecraft.gameMode != null) {
                var result = this.useHorseSpawnItem(minecraft);
                if (result.consumesAction()) {
                    minecraft.player.swing(InteractionHand.MAIN_HAND);
                }
                LOGGER.info("Attempted horse spawn item use with result {}", result);
            } else {
                LOGGER.warn("Unable to use horse item because game mode is unavailable");
            }

            return HorseStepOutcome.waiting(attemptMessage);
        }

        Optional<AbstractHorse> spawnedHorse = this.findDetectedHorseCandidate(minecraft);
        if (spawnedHorse.isPresent()) {
            AbstractHorse horse = spawnedHorse.get();
            this.targetHorseId = horse.getUUID();
            this.awaitingDetection = false;
            LOGGER.info("Detected horse candidate {}", horse.getUUID());
            return HorseStepOutcome.readyToMount(horse, detectedHorseMessage(horse));
        }

        this.detectionTicksRemaining--;
        if (this.detectionTicksRemaining <= 0) {
            this.awaitingDetection = false;
            this.retryDelayTicksRemaining = WynnHorseConfig.getHorseSpawnRetryDelayTicks();
            LOGGER.info("Horse detection timed out, retrying spawn loop");
            return HorseStepOutcome.waiting(Component.translatable("message.wynnhorse.horse_spawn_retry", expectedHorseName(minecraft)));
        }

        return HorseStepOutcome.waiting(null);
    }

    public void reset() {
        this.targetHorseId = null;
        this.startupClearHorseId = null;
        this.detectionTicksRemaining = 0;
        this.retryDelayTicksRemaining = 0;
        this.startupClearRetryTicksRemaining = 0;
        this.awaitingDetection = false;
        this.startupHorseClearComplete = false;
        this.lastHorseScanDebugSummary = "";
    }

    private Optional<AbstractHorse> resolveTargetHorse(final Minecraft minecraft) {
        if (this.targetHorseId == null || minecraft.level == null) {
            return Optional.empty();
        }

        Entity entity = minecraft.level.getEntity(this.targetHorseId);
        if (entity instanceof AbstractHorse horse && horse.isAlive()) {
            return Optional.of(horse);
        }

        this.targetHorseId = null;
        return Optional.empty();
    }

    private Optional<AbstractHorse> findNamedHorse(final Minecraft minecraft) {
        String expectedName = expectedHorseName(minecraft);
        String playerName = minecraft.player.getName().getString();
        List<AbstractHorse> nearbyHorses = this.findNearbyHorses(minecraft);
        this.logNearbyHorseNames(minecraft, expectedName, nearbyHorses);

        for (AbstractHorse horse : nearbyHorses) {
            if (!horse.hasCustomName()) {
                continue;
            }

            String actualName = horse.getDisplayName().getString();
            Optional<String> extractedPlayerName = extractPlayerNameSegment(actualName);
            if (extractedPlayerName.isPresent() && extractedPlayerName.get().equals(playerName)) {
                LOGGER.info(
                        "Horse name segment match succeeded for {} using actual='{}' extractedPlayerName='{}'",
                        horse.getUUID(),
                        actualName,
                        extractedPlayerName.get()
                );
                return Optional.of(horse);
            }

            this.logNameMismatch(expectedName, playerName, actualName, extractedPlayerName.orElse("<missing>"), horse);
        }

        return Optional.empty();
    }

    private Optional<AbstractHorse> findDetectedHorseCandidate(final Minecraft minecraft) {
        return this.findNamedHorse(minecraft);
    }

    private List<AbstractHorse> findNearbyHorses(final Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return List.of();
        }

        double radius = WynnHorseConfig.getHorseSpawnDetectionRadius();
        AABB searchBounds = minecraft.player.getBoundingBox().inflate(radius, radius, radius);

        return minecraft.level.getEntities(minecraft.player, searchBounds, entity -> entity instanceof AbstractHorse).stream()
                .map(entity -> (AbstractHorse) entity)
                .filter(AbstractHorse::isAlive)
                .sorted(Comparator.comparingDouble(horse -> horse.distanceToSqr(minecraft.player)))
                .toList();
    }

    private static String expectedHorseName(final Minecraft minecraft) {
        return minecraft.player.getName().getString() + "'s horse";
    }

    private static Component detectedHorseMessage(final AbstractHorse horse) {
        if (horse.hasCustomName()) {
            return Component.translatable("message.wynnhorse.horse_detected_named", horse.getDisplayName());
        }

        return Component.translatable("message.wynnhorse.horse_detected");
    }

    private void logNearbyHorseNames(final Minecraft minecraft, final String expectedName, final List<AbstractHorse> nearbyHorses) {
        if (minecraft.player == null) {
            return;
        }

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("expected='").append(expectedName).append("'; ");

        if (nearbyHorses.isEmpty()) {
            summaryBuilder.append("nearby=[]");
        } else {
            summaryBuilder.append("nearby=[");
            for (int index = 0; index < nearbyHorses.size(); index++) {
                AbstractHorse horse = nearbyHorses.get(index);
                if (index > 0) {
                    summaryBuilder.append("; ");
                }

                String customName = horse.getCustomName() == null ? "<null>" : horse.getCustomName().getString();
                summaryBuilder.append("{uuid=").append(horse.getUUID())
                        .append(", hasCustomName=").append(horse.hasCustomName())
                        .append(", displayName='").append(horse.getDisplayName().getString()).append('\'')
                        .append(", customName='").append(customName).append('\'')
                        .append(", distance=").append(String.format(java.util.Locale.ROOT, "%.2f", Math.sqrt(horse.distanceToSqr(minecraft.player))))
                        .append('}');
            }
            summaryBuilder.append(']');
        }

        String summary = summaryBuilder.toString();
        if (!summary.equals(this.lastHorseScanDebugSummary)) {
            this.lastHorseScanDebugSummary = summary;
            LOGGER.info("Horse name scan: {}", summary);
        }
    }

    private void logNameMismatch(
            final String expectedName,
            final String playerName,
            final String actualName,
            final String extractedPlayerName,
            final AbstractHorse horse
    ) {
        LOGGER.info(
                "Horse name mismatch for {}: expected='{}' (len={}, codes={}) playerName='{}' (len={}, codes={}) actual='{}' (len={}, codes={}) extractedPlayerName='{}' (len={}, codes={})",
                horse.getUUID(),
                expectedName,
                expectedName.length(),
                codePoints(expectedName),
                playerName,
                playerName.length(),
                codePoints(playerName),
                actualName,
                actualName.length(),
                codePoints(actualName),
                extractedPlayerName,
                extractedPlayerName.length(),
                codePoints(extractedPlayerName)
        );
    }

    private static String codePoints(final String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append((int) value.charAt(index));
        }
        return builder.toString();
    }

    private static Optional<String> extractPlayerNameSegment(final String displayName) {
        int firstMarker = displayName.indexOf('\u00A7');
        if (firstMarker < 0 || firstMarker + 2 > displayName.length()) {
            return Optional.empty();
        }

        int nameStart = firstMarker + 2;
        int secondMarker = displayName.indexOf('\u00A7', nameStart);
        if (secondMarker < 0 || secondMarker <= nameStart) {
            return Optional.empty();
        }

        return Optional.of(displayName.substring(nameStart, secondMarker));
    }

    private HorseStepOutcome handleStartupHorseClear(final Minecraft minecraft, final HorseItemTracker horseItemTracker) {
        AbstractHorse nearbyHorse = this.resolveStartupClearHorse(minecraft);
        if (nearbyHorse == null) {
            this.startupHorseClearComplete = true;
            this.startupClearHorseId = null;
            return null;
        }

        if (minecraft.player == null || minecraft.gameMode == null) {
            return HorseStepOutcome.waiting(Component.translatable("message.wynnhorse.horse_startup_clear_attempt", nearbyHorse.getDisplayName()));
        }

        if (this.startupClearRetryTicksRemaining > 0) {
            this.startupClearRetryTicksRemaining--;
            return HorseStepOutcome.waiting(null);
        }

        Optional<HorseItemMatch> preferredMatch = horseItemTracker.selectPreferredMatch(minecraft);
        if (preferredMatch.isEmpty()) {
            return HorseStepOutcome.noHorseItem(Component.translatable("message.wynnhorse.automation.no_horse_item"));
        }

        var result = minecraft.gameMode.interact(minecraft.player, nearbyHorse, new net.minecraft.world.phys.EntityHitResult(nearbyHorse), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }

        this.startupClearRetryTicksRemaining = WynnHorseConfig.getHorseMountRetryCooldownTicks();
        LOGGER.info("Attempted startup horse clear on {} with result {}", nearbyHorse.getUUID(), result);
        return HorseStepOutcome.waiting(Component.translatable("message.wynnhorse.horse_startup_clear_attempt", nearbyHorse.getDisplayName()));
    }

    private AbstractHorse resolveStartupClearHorse(final Minecraft minecraft) {
        if (minecraft.level == null) {
            return null;
        }

        if (this.startupClearHorseId != null) {
            Entity existing = minecraft.level.getEntity(this.startupClearHorseId);
            if (existing instanceof AbstractHorse horse && horse.isAlive()) {
                return horse;
            }
            this.startupClearHorseId = null;
        }

        List<AbstractHorse> nearbyHorses = this.findNearbyHorses(minecraft);
        if (nearbyHorses.isEmpty()) {
            return null;
        }

        AbstractHorse horse = nearbyHorses.getFirst();
        this.startupClearHorseId = horse.getUUID();
        LOGGER.info("Startup horse clear targeting nearby horse {}", horse.getUUID());
        return horse;
    }

    private boolean alignForHorseSpawn(final Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }

        float targetPitch = (float) WynnHorseConfig.getHorseSpawnLookPitchDegrees();
        float updatedPitch = Mth.approach(
                minecraft.player.getXRot(),
                targetPitch,
                (float) WynnHorseConfig.getHorseSpawnLookPitchStepDegrees()
        );
        minecraft.player.setXRot(updatedPitch);
        return Math.abs(updatedPitch - targetPitch) <= 1.0F;
    }

    private net.minecraft.world.InteractionResult useHorseSpawnItem(final Minecraft minecraft) {
        if (minecraft.player == null || minecraft.gameMode == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null) {
            hitResult = minecraft.player.pick(5.0D, 0.0F, false);
        }

        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            LOGGER.info("Attempting horse spawn item use on block at {}", blockHitResult.getBlockPos());
            return minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, blockHitResult);
        }

        LOGGER.info("Attempting horse spawn item use with generic item interaction");
        return minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    }

    public record HorseStepOutcome(Status status, AbstractHorse horse, Component message) {
        public static HorseStepOutcome waiting(final Component message) {
            return new HorseStepOutcome(Status.WAITING, null, message);
        }

        public static HorseStepOutcome readyToMount(final AbstractHorse horse, final Component message) {
            return new HorseStepOutcome(Status.READY_TO_MOUNT, horse, message);
        }

        public static HorseStepOutcome mounted(final AbstractHorse horse) {
            return new HorseStepOutcome(Status.MOUNTED, horse, null);
        }

        public static HorseStepOutcome noHorseItem(final Component message) {
            return new HorseStepOutcome(Status.NO_HORSE_ITEM, null, message);
        }

        public enum Status {
            WAITING,
            READY_TO_MOUNT,
            MOUNTED,
            NO_HORSE_ITEM
        }
    }
}
