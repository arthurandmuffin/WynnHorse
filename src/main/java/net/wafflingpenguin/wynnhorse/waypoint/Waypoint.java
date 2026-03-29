package net.wafflingpenguin.wynnhorse.waypoint;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

public record Waypoint(UUID id, String name, Vec3 position) {
    public Waypoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(position, "position");

        name = normalizeName(name);
    }

    public static Waypoint create(final String name, final Vec3 position) {
        return new Waypoint(UUID.randomUUID(), name, position);
    }

    public Waypoint withName(final String updatedName) {
        return new Waypoint(this.id, updatedName, this.position);
    }

    public Waypoint withPosition(final Vec3 updatedPosition) {
        return new Waypoint(this.id, this.name, updatedPosition);
    }

    private static String normalizeName(final String name) {
        String trimmed = Objects.requireNonNullElse(name, "").trim();
        return trimmed.isEmpty() ? "Waypoint" : trimmed;
    }
}
