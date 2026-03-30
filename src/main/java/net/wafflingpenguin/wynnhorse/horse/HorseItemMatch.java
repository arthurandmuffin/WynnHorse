package net.wafflingpenguin.wynnhorse.horse;

import net.minecraft.util.Mth;

import java.util.Locale;

public record HorseItemMatch(int slot, String displayName, String statName, Integer statValue, Integer statMaximum, Integer xpPercent) {
    public boolean hasParsedStat() {
        return this.statValue != null && this.statMaximum != null;
    }

    public boolean isMaxedParsedStat() {
        return this.hasParsedStat() && this.statValue.intValue() >= this.statMaximum.intValue();
    }

    public String labelText() {
        return this.hasParsedStat() ? this.displayName + ":" : this.displayName;
    }

    public String statText() {
        if (!this.hasParsedStat()) {
            return "";
        }

        return this.statValue + "/" + this.statMaximum;
    }

    public boolean hasTimingEstimate() {
        return this.hasParsedStat() && this.statValue.intValue() > 0 && this.statMaximum.intValue() > 1;
    }

    public String timingText(final boolean useEtaLabel) {
        if (!this.hasTimingEstimate()) {
            return "";
        }

        if (useEtaLabel) {
            return "ETA: " + formatDurationMinutes(this.estimatedMinutesRemainingToMax()) + "   " + this.progressPercentText();
        }

        return "Progress: " + this.progressPercentText();
    }

    public double estimatedMinutesRemainingToMax() {
        if (!this.hasTimingEstimate()) {
            return 0.0D;
        }

        int currentLevel = this.statValue.intValue();
        int maximumLevel = this.statMaximum.intValue();
        double remainingMinutes = levelDurationMinutes(currentLevel, this.effectiveXpPercent());
        for (int level = currentLevel + 1; level < maximumLevel; level++) {
            remainingMinutes += levelDurationMinutes(level, 0);
        }

        return Math.max(remainingMinutes, 0.0D);
    }

    public double estimatedProgressFraction() {
        if (!this.hasTimingEstimate()) {
            return 0.0D;
        }

        int currentLevel = this.statValue.intValue();
        int maximumLevel = this.statMaximum.intValue();
        double elapsedMinutes = 0.0D;
        for (int level = 1; level < currentLevel; level++) {
            elapsedMinutes += levelDurationMinutes(level, 0);
        }
        elapsedMinutes += fullLevelDurationMinutes(currentLevel) * (this.effectiveXpPercent() / 100.0D);

        double totalMinutes = 0.0D;
        for (int level = 1; level < maximumLevel; level++) {
            totalMinutes += levelDurationMinutes(level, 0);
        }

        if (totalMinutes <= 1.0E-6D) {
            return 0.0D;
        }

        return Mth.clamp(elapsedMinutes / totalMinutes, 0.0D, 1.0D);
    }

    public String progressPercentText() {
        int percent = (int) Math.round(this.estimatedProgressFraction() * 100.0D);
        return Mth.clamp(percent, 0, 100) + "%";
    }

    private int effectiveXpPercent() {
        if (this.xpPercent == null) {
            return 0;
        }

        return Mth.clamp(this.xpPercent.intValue(), 0, 100);
    }

    private static double levelDurationMinutes(final int currentLevel, final int xpPercent) {
        return fullLevelDurationMinutes(currentLevel) * ((100.0D - Mth.clamp(xpPercent, 0, 100)) / 100.0D);
    }

    private static double fullLevelDurationMinutes(final int currentLevel) {
        return ((3.0D * currentLevel) + 2.0D) / 6.0D;
    }

    private static String formatDurationMinutes(final double totalMinutes) {
        int roundedMinutes = Math.max(0, (int) Math.ceil(totalMinutes));
        int hours = roundedMinutes / 60;
        int minutes = roundedMinutes % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d h %02d m", hours, minutes);
        }

        return String.format(Locale.ROOT, "%d m", minutes);
    }
}
