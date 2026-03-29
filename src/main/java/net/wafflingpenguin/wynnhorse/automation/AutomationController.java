package net.wafflingpenguin.wynnhorse.automation;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public final class AutomationController {
    private static final Logger LOGGER = LogUtils.getLogger();

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

    public void tick(final Minecraft minecraft) {
        if (!this.enabled) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            this.disable(minecraft, "left world");
        }
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
