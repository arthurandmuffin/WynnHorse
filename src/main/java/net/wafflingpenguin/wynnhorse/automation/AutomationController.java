package net.wafflingpenguin.wynnhorse.automation;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;
import org.slf4j.Logger;

public final class AutomationController {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NavigationController navigationController = new NavigationController();

    private boolean enabled;

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

    public Component tick(final Minecraft minecraft, final WaypointRoute route) {
        if (!this.enabled) {
            return null;
        }

        if (minecraft.player == null || minecraft.level == null) {
            this.disable(minecraft, "left world");
            return null;
        }

        NavigationController.NavigationOutcome outcome = this.navigationController.tick(minecraft, route);
        return switch (outcome.status()) {
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
        LOGGER.info("Automation enabled");
    }

    private void disable(final Minecraft minecraft, final String reason) {
        this.enabled = false;
        this.releaseMovement(minecraft);
        LOGGER.info("Automation disabled: {}", reason);
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
}
