package net.wafflingpenguin.wynnhorse.automation;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.wafflingpenguin.wynnhorse.horse.HorseItemTracker;
import net.wafflingpenguin.wynnhorse.horse.HorseSpawnController;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AutomationController {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NavigationController navigationController = new NavigationController();
    private final HorseSpawnController horseSpawnController = new HorseSpawnController();

    private boolean enabled;
    private int mountRetryCooldownTicks;
    private int horseApproachPauseTicksRemaining;
    private boolean awaitingPostMountAlignment;
    private PostMountAlignmentState postMountAlignmentState;
    private UUID announcedMountedHorseId;
    private UUID announcedApproachHorseId;

    public boolean toggle(final Minecraft minecraft) {
        if (this.enabled) {
            this.disable(minecraft, "manual toggle");
            return false;
        }

        this.enable();
        return true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Component tick(final Minecraft minecraft, final WaypointRoute route, final HorseItemTracker horseItemTracker) {
        if (!this.enabled) {
            return null;
        }

        if (minecraft.player == null || minecraft.level == null) {
            this.disable(minecraft, "left world");
            return null;
        }

        if (this.mountRetryCooldownTicks > 0) {
            this.mountRetryCooldownTicks--;
        }

        HorseSpawnController.HorseStepOutcome horseStepOutcome = this.horseSpawnController.tick(minecraft, horseItemTracker);
        Component horsePhaseMessage = this.handleHorsePhase(minecraft, horseStepOutcome, horseItemTracker);
        if (horseStepOutcome.status() != HorseSpawnController.HorseStepOutcome.Status.MOUNTED) {
            return horsePhaseMessage;
        }

        if (this.awaitingPostMountAlignment) {
            Component alignmentMessage = this.handlePostMountAlignment(minecraft, route);
            if (this.awaitingPostMountAlignment) {
                return horsePhaseMessage != null ? horsePhaseMessage : alignmentMessage;
            }
            if (alignmentMessage != null) {
                return alignmentMessage;
            }
        }

        NavigationController.NavigationOutcome outcome = this.navigationController.tick(minecraft, route);
        Component navigationMessage = switch (outcome.status()) {
            case NAVIGATING -> {
                this.applyForwardMovement(minecraft);
                yield null;
            }
            case PAUSED -> {
                this.releaseMovement(minecraft);
                yield null;
            }
            case NO_TARGET -> {
                this.disable(minecraft, "missing active waypoint");
                yield Component.translatable("message.wynnhorse.automation.no_target");
            }
            case REACHED -> {
                if (route.size() > 1) {
                    int nextIndex = route.advanceToNext();
                    LOGGER.info("Visited waypoint {}, advancing to route index {}", outcome.waypoint().name(), nextIndex);
                    yield null;
                }

                this.disable(minecraft, "reached waypoint " + outcome.waypoint().name());
                yield Component.translatable("message.wynnhorse.waypoint_reached", outcome.waypoint().name());
            }
        };

        if (horsePhaseMessage != null && navigationMessage == null) {
            return horsePhaseMessage;
        }

        return navigationMessage;
    }

    private Component handleHorsePhase(
            final Minecraft minecraft,
            final HorseSpawnController.HorseStepOutcome horseStepOutcome,
            final HorseItemTracker horseItemTracker
    ) {
        return switch (horseStepOutcome.status()) {
            case NO_HORSE_ITEM -> {
                this.disable(minecraft, "missing matching horse item");
                yield horseStepOutcome.message();
            }
            case WAITING -> {
                this.releaseMovement(minecraft);
                yield horseStepOutcome.message();
            }
            case READY_TO_MOUNT -> this.approachAndMountHorse(minecraft, horseStepOutcome.horse(), horseStepOutcome.message(), horseItemTracker);
            case MOUNTED -> {
                this.releaseMovement(minecraft);
                AbstractHorse mountedHorse = horseStepOutcome.horse();
                if (mountedHorse != null && !mountedHorse.getUUID().equals(this.announcedMountedHorseId)) {
                    this.announcedMountedHorseId = mountedHorse.getUUID();
                    this.awaitingPostMountAlignment = true;
                    this.postMountAlignmentState = null;
                    LOGGER.info("Mounted horse {}", mountedHorse.getUUID());
                    yield Component.translatable("message.wynnhorse.horse_mounted", mountedHorse.getDisplayName());
                }
                yield null;
            }
        };
    }

    private Component approachAndMountHorse(
            final Minecraft minecraft,
            final AbstractHorse horse,
            final Component stepMessage,
            final HorseItemTracker horseItemTracker
    ) {
        if (horse == null || !horse.isAlive()) {
            this.releaseMovement(minecraft);
            return stepMessage;
        }

        if (stepMessage != null && !horse.getUUID().equals(this.announcedApproachHorseId)) {
            this.announcedApproachHorseId = horse.getUUID();
            this.horseApproachPauseTicksRemaining = WynnHorseConfig.getHorseApproachPauseTicks();
            this.releaseMovement(minecraft);
            return stepMessage;
        }

        if (this.horseApproachPauseTicksRemaining > 0) {
            this.horseApproachPauseTicksRemaining--;
            this.releaseMovement(minecraft);
            return null;
        }

        NavigationController.DirectNavigationOutcome approachOutcome = this.navigationController.tickTowardsPosition(
                minecraft,
                horse.position(),
                WynnHorseConfig.getHorseMountRange(),
                WynnHorseConfig.getHorseApproachYawStepDegrees()
        );

        switch (approachOutcome.status()) {
            case NAVIGATING -> {
                this.applyForwardMovement(minecraft);
                return stepMessage;
            }
            case PAUSED, NO_TARGET -> {
                this.releaseMovement(minecraft);
                return stepMessage;
            }
            case REACHED -> {
                this.releaseMovement(minecraft);
                Component mountAttemptMessage = this.tryMountHorse(minecraft, horse, horseItemTracker);
                return mountAttemptMessage != null ? mountAttemptMessage : stepMessage;
            }
        }

        return stepMessage;
    }

    private Component tryMountHorse(final Minecraft minecraft, final AbstractHorse horse, final HorseItemTracker horseItemTracker) {
        if (minecraft.player == null || minecraft.gameMode == null) {
            return Component.translatable("message.wynnhorse.horse_mount_retry", horse.getDisplayName());
        }

        if (this.mountRetryCooldownTicks > 0) {
            return null;
        }

        this.mountRetryCooldownTicks = WynnHorseConfig.getHorseMountRetryCooldownTicks();
        this.switchAwayFromHorseItem(minecraft, horseItemTracker);
        var result = minecraft.gameMode.interact(minecraft.player, horse, new EntityHitResult(horse), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }

        LOGGER.info("Attempted to mount horse {} with result {}", horse.getUUID(), result);
        return Component.translatable("message.wynnhorse.horse_mount_attempt", horse.getDisplayName());
    }

    private Component handlePostMountAlignment(final Minecraft minecraft, final WaypointRoute route) {
        if (minecraft.screen != null) {
            this.releaseMovement(minecraft);
            return null;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            this.awaitingPostMountAlignment = false;
            this.postMountAlignmentState = null;
            return null;
        }

        Waypoint waypoint = route.getCurrentWaypoint().orElse(null);
        if (waypoint == null) {
            this.disable(minecraft, "missing active waypoint after mounting");
            return Component.translatable("message.wynnhorse.automation.no_target");
        }

        if (this.postMountAlignmentState == null) {
            this.postMountAlignmentState = this.createPostMountAlignmentState(player, waypoint.position());
            if (this.postMountAlignmentState == null) {
                this.awaitingPostMountAlignment = false;
                return null;
            }
        }

        this.releaseMovement(minecraft);
        PostMountAlignmentState state = this.postMountAlignmentState;
        if (this.alignLook(player, state.targetYaw(), state.targetPitch(), state.yawStepDegrees())) {
            if (state.phase() == PostMountAlignmentPhase.OVERSHOOT) {
                this.postMountAlignmentState = state.beginCorrection();
                LOGGER.info(
                        "Completed post-mount overshoot; correcting back toward yaw {} pitch {}",
                        this.postMountAlignmentState.targetYaw(),
                        this.postMountAlignmentState.targetPitch()
                );
            } else {
                this.awaitingPostMountAlignment = false;
                this.postMountAlignmentState = null;
                LOGGER.info("Completed post-mount alignment toward waypoint {}", waypoint.name());
            }
        }

        return null;
    }

    private PostMountAlignmentState createPostMountAlignmentState(final LocalPlayer player, final Vec3 waypointPosition) {
        float desiredYaw = desiredYaw(player.position(), waypointPosition);
        float currentYaw = player.getYRot();
        float signedYawDifference = Mth.wrapDegrees(desiredYaw - currentYaw);
        if (Math.abs(signedYawDifference) <= WynnHorseConfig.getHorsePostMountFacingToleranceDegrees()) {
            return null;
        }

        double overshootMin = WynnHorseConfig.getHorsePostMountOvershootMinDegrees();
        double overshootMax = Math.max(overshootMin, WynnHorseConfig.getHorsePostMountOvershootMaxDegrees());
        float overshootDirection = signedYawDifference == 0.0F ? (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F) : Math.signum(signedYawDifference);
        float overshootDegrees = (float) ThreadLocalRandom.current().nextDouble(overshootMin, overshootMax + 1.0E-6D);
        float overshootYaw = desiredYaw + overshootDirection * overshootDegrees;

        float basePitch = player.getXRot();
        float overshootPitchOffset = randomSignedPitchOffset();
        float correctionPitchOffset = Mth.clamp(
                -overshootPitchOffset * (float) ThreadLocalRandom.current().nextDouble(0.35D, 0.7D)
                        + (float) ThreadLocalRandom.current().nextDouble(-0.6D, 0.6D),
                (float) -WynnHorseConfig.getHorsePostMountPitchJitterMaxDegrees(),
                (float) WynnHorseConfig.getHorsePostMountPitchJitterMaxDegrees()
        );

        float overshootPitch = clampPitch(basePitch + overshootPitchOffset);
        float correctionPitch = clampPitch(basePitch + correctionPitchOffset);
        LOGGER.info(
                "Starting post-mount alignment: desiredYaw={}, overshootYaw={}, overshootPitch={}, correctionPitch={}",
                desiredYaw,
                overshootYaw,
                overshootPitch,
                correctionPitch
        );
        return this.newOvershootAlignmentState(overshootYaw, overshootPitch, desiredYaw, correctionPitch);
    }

    private PostMountAlignmentState newOvershootAlignmentState(
            final float overshootYaw,
            final float overshootPitch,
            final float finalYaw,
            final float correctionPitch
    ) {
        return new PostMountAlignmentState(
                PostMountAlignmentPhase.OVERSHOOT,
                overshootYaw,
                overshootPitch,
                finalYaw,
                correctionPitch,
                (float) WynnHorseConfig.getHorsePostMountOvershootYawStepDegrees()
        );
    }

    private boolean alignLook(final LocalPlayer player, final float targetYaw, final float targetPitch, final float yawStepDegrees) {
        float updatedYaw = Mth.approachDegrees(player.getYRot(), targetYaw, yawStepDegrees);
        float updatedPitch = Mth.approach(player.getXRot(), targetPitch, (float) WynnHorseConfig.getHorsePostMountPitchStepDegrees());
        player.setYRot(updatedYaw);
        player.setYHeadRot(updatedYaw);
        player.setYBodyRot(updatedYaw);
        player.setXRot(updatedPitch);

        float yawError = Mth.degreesDifferenceAbs(updatedYaw, targetYaw);
        float pitchError = Math.abs(updatedPitch - targetPitch);
        return yawError <= (float) WynnHorseConfig.getHorsePostMountFacingToleranceDegrees()
                && pitchError <= (float) WynnHorseConfig.getHorsePostMountPitchToleranceDegrees();
    }

    private static float desiredYaw(final Vec3 from, final Vec3 to) {
        double deltaX = to.x - from.x;
        double deltaZ = to.z - from.z;
        if (Mth.lengthSquared(deltaX, deltaZ) < 1.0E-6D) {
            return 0.0F;
        }

        return (float) Math.toDegrees(Mth.atan2(deltaZ, deltaX)) - 90.0F;
    }

    private static float clampPitch(final float pitch) {
        return Mth.clamp(pitch, -30.0F, 30.0F);
    }

    private float randomSignedPitchOffset() {
        double minimum = WynnHorseConfig.getHorsePostMountPitchJitterMinDegrees();
        double maximum = Math.max(minimum, WynnHorseConfig.getHorsePostMountPitchJitterMaxDegrees());
        double magnitude = ThreadLocalRandom.current().nextDouble(minimum, maximum + 1.0E-6D);
        return (float) (ThreadLocalRandom.current().nextBoolean() ? magnitude : -magnitude);
    }

    private void applyForwardMovement(final Minecraft minecraft) {
        minecraft.options.keyUp.setDown(true);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    private void enable() {
        this.enabled = true;
        this.horseSpawnController.reset();
        this.mountRetryCooldownTicks = 0;
        this.horseApproachPauseTicksRemaining = 0;
        this.awaitingPostMountAlignment = false;
        this.postMountAlignmentState = null;
        this.announcedMountedHorseId = null;
        this.announcedApproachHorseId = null;
        LOGGER.info("Automation enabled");
    }

    private void disable(final Minecraft minecraft, final String reason) {
        this.enabled = false;
        this.horseSpawnController.reset();
        this.mountRetryCooldownTicks = 0;
        this.horseApproachPauseTicksRemaining = 0;
        this.awaitingPostMountAlignment = false;
        this.postMountAlignmentState = null;
        this.announcedMountedHorseId = null;
        this.announcedApproachHorseId = null;
        this.releaseMovement(minecraft);
        LOGGER.info("Automation disabled: {}", reason);
    }

    private void switchAwayFromHorseItem(final Minecraft minecraft, final HorseItemTracker horseItemTracker) {
        if (horseItemTracker == null) {
            return;
        }

        horseItemTracker.selectNonHorseItem(minecraft);
    }

    private void releaseMovement(final Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    private enum PostMountAlignmentPhase {
        OVERSHOOT,
        CORRECTION
    }

    private record PostMountAlignmentState(
            PostMountAlignmentPhase phase,
            float targetYaw,
            float targetPitch,
            float finalYaw,
            float correctionPitch,
            float yawStepDegrees
    ) {
        private PostMountAlignmentState beginCorrection() {
            return new PostMountAlignmentState(
                    PostMountAlignmentPhase.CORRECTION,
                    this.finalYaw,
                    this.correctionPitch,
                    this.finalYaw,
                    this.correctionPitch,
                    (float) WynnHorseConfig.getHorsePostMountCorrectionYawStepDegrees()
            );
        }
    }
}
