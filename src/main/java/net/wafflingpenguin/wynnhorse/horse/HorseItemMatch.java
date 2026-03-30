package net.wafflingpenguin.wynnhorse.horse;

public record HorseItemMatch(int slot, String displayName, String statName, Integer statValue, Integer statMaximum) {
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
}
