package net.wafflingpenguin.wynnhorse;

public enum WaypointRenderStyle {
    MARKER("marker", "screen.wynnhorse.render_style.marker"),
    BEACON("beacon", "screen.wynnhorse.render_style.beacon");

    private final String serializedName;
    private final String translationKey;

    WaypointRenderStyle(final String serializedName, final String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public WaypointRenderStyle next() {
        return this == MARKER ? BEACON : MARKER;
    }

    public static WaypointRenderStyle fromSerializedName(final String value) {
        for (WaypointRenderStyle style : values()) {
            if (style.serializedName.equalsIgnoreCase(value)) {
                return style;
            }
        }

        return MARKER;
    }
}
