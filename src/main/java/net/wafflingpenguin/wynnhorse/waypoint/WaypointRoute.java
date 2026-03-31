package net.wafflingpenguin.wynnhorse.waypoint;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WaypointRoute {
    private final List<Waypoint> waypoints = new ArrayList<>();

    private int activeIndex = -1;

    public List<Waypoint> getWaypoints() {
        return List.copyOf(this.waypoints);
    }

    public boolean isEmpty() {
        return this.waypoints.isEmpty();
    }

    public int size() {
        return this.waypoints.size();
    }

    public int getActiveIndex() {
        return this.activeIndex;
    }

    public Optional<Waypoint> getCurrentWaypoint() {
        if (this.activeIndex < 0 || this.activeIndex >= this.waypoints.size()) {
            return Optional.empty();
        }

        return Optional.of(this.waypoints.get(this.activeIndex));
    }

    public Optional<Waypoint> getWaypoint(final UUID waypointId) {
        int index = this.indexOf(waypointId);
        return index == -1 ? Optional.empty() : Optional.of(this.waypoints.get(index));
    }

    public Waypoint addWaypoint(final String name, final Vec3 position) {
        return this.addWaypoint(Waypoint.create(name, position));
    }

    public Waypoint addWaypoint(final Waypoint waypoint) {
        this.waypoints.add(waypoint);

        if (this.activeIndex == -1) {
            this.activeIndex = 0;
        }

        return waypoint;
    }

    public void clear() {
        this.waypoints.clear();
        this.activeIndex = -1;
    }

    public void replaceAll(final List<Waypoint> updatedWaypoints) {
        this.waypoints.clear();
        this.waypoints.addAll(updatedWaypoints);
        this.activeIndex = this.waypoints.isEmpty() ? -1 : 0;
    }

    public boolean removeWaypoint(final UUID waypointId) {
        int removedIndex = this.indexOf(waypointId);
        if (removedIndex == -1) {
            return false;
        }

        this.waypoints.remove(removedIndex);

        if (this.waypoints.isEmpty()) {
            this.activeIndex = -1;
            return true;
        }

        if (removedIndex < this.activeIndex) {
            this.activeIndex--;
        } else if (removedIndex == this.activeIndex) {
            this.activeIndex = Math.min(this.activeIndex, this.waypoints.size() - 1);
        }

        return true;
    }

    public boolean renameWaypoint(final UUID waypointId, final String updatedName) {
        return this.replaceWaypoint(waypointId, waypoint -> waypoint.withName(updatedName));
    }

    public boolean updateWaypointPosition(final UUID waypointId, final Vec3 updatedPosition) {
        return this.replaceWaypoint(waypointId, waypoint -> waypoint.withPosition(updatedPosition));
    }

    public boolean moveWaypointUp(final UUID waypointId) {
        int index = this.indexOf(waypointId);
        if (index <= 0) {
            return false;
        }

        this.swap(index, index - 1);
        return true;
    }

    public boolean moveWaypointDown(final UUID waypointId) {
        int index = this.indexOf(waypointId);
        if (index == -1 || index >= this.waypoints.size() - 1) {
            return false;
        }

        this.swap(index, index + 1);
        return true;
    }

    public boolean setActiveIndex(final int updatedIndex) {
        if (updatedIndex < 0 || updatedIndex >= this.waypoints.size()) {
            return false;
        }

        this.activeIndex = updatedIndex;
        return true;
    }

    public int advanceToNext() {
        if (this.waypoints.isEmpty()) {
            this.activeIndex = -1;
            return this.activeIndex;
        }

        if (this.activeIndex == -1) {
            this.activeIndex = 0;
            return this.activeIndex;
        }

        this.activeIndex = (this.activeIndex + 1) % this.waypoints.size();
        return this.activeIndex;
    }

    private boolean replaceWaypoint(final UUID waypointId, final WaypointUpdater updater) {
        int index = this.indexOf(waypointId);
        if (index == -1) {
            return false;
        }

        this.waypoints.set(index, updater.update(this.waypoints.get(index)));
        return true;
    }

    private int indexOf(final UUID waypointId) {
        for (int index = 0; index < this.waypoints.size(); index++) {
            if (this.waypoints.get(index).id().equals(waypointId)) {
                return index;
            }
        }

        return -1;
    }

    private void swap(final int firstIndex, final int secondIndex) {
        Waypoint first = this.waypoints.get(firstIndex);
        this.waypoints.set(firstIndex, this.waypoints.get(secondIndex));
        this.waypoints.set(secondIndex, first);

        if (this.activeIndex == firstIndex) {
            this.activeIndex = secondIndex;
        } else if (this.activeIndex == secondIndex) {
            this.activeIndex = firstIndex;
        }
    }

    @FunctionalInterface
    private interface WaypointUpdater {
        Waypoint update(Waypoint waypoint);
    }
}
