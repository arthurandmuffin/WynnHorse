# WynnHorse

WynnHorse is a client-side Forge mod that automates levelling up horses in Wynncraft.

It is designed for ordered route traversal rather than full pathfinding. You define a loop of waypoints, spawn or acquire a horse from a named hotbar item, and the mod handles steering, mounting, route cycling, and horse rotation when a ridden horse reaches max level.

Development is ongoing. Any use is at your own risk.

## Current Features

- in-game waypoint manager GUI
- quick-add waypoint keybind from current position
- waypoint marker or beacon rendering in-world
- route save/load as CSV
- hotbar horse-item matching by display name and tooltip parsing
- horse overlay showing level/progress information
- automatic horse spawn, mount, and route traversal
- max-level horse rotation at waypoint 1

## Default Controls

- `O`: open waypoint manager
- `P`: add current position as a waypoint
- `J`: toggle automation

## Waypoint Manager

Open the waypoint manager with `O`.

From there you can:

- add, edit, remove, and reorder waypoints
- set the horse item display-name fragment
- change waypoint render style and colors
- save the current route to CSV
- browse and load saved CSV routes
- open the instance `config/` folder

Route files are stored under:

`config/wynnhorse/routes/`

Each CSV route uses one waypoint per line:

```text
x,y,z
```

## Horse Overlay

When matching horse items are found, WynnHorse shows them in the HUD with:

- horse name
- parsed stat such as `x/y`
- `Progress` for inventory horses
- `ETA` for the currently ridden horse while automation is active

Parsed horses are ordered by:

1. max stat `y` descending
2. current stat `x` descending

Maxed parsed horses remain visible, but are skipped for automatic selection.

## Config

Client config is stored in the Minecraft instance config folder:

`config/wynnhorse-client.toml`

This includes steering and horse-automation tuning values.

The GUI also has an `Open Config` button for quick access to that folder.

Currently, after updating the config, relaunch for changes to take place.

In the future: edit configs in GUI.