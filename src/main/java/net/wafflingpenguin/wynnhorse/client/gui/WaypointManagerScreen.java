package net.wafflingpenguin.wynnhorse.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.wafflingpenguin.wynnhorse.WaypointRenderStyle;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.client.WynnHorseClient;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRouteCsvStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WaypointManagerScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.wynnhorse.waypoints");
    private static final Component ROUTE_LABEL = Component.translatable("screen.wynnhorse.route");
    private static final Component ROUTE_NAME_LABEL = Component.translatable("screen.wynnhorse.route_name");
    private static final Component SAVED_ROUTES_LABEL = Component.translatable("screen.wynnhorse.saved_routes");
    private static final Component HORSE_ITEM_LABEL = Component.translatable("screen.wynnhorse.horse_item");
    private static final Component NAME_LABEL = Component.translatable("screen.wynnhorse.name");
    private static final Component BEACON_COLOR_LABEL = Component.translatable("screen.wynnhorse.beacon_color");
    private static final Component NEXT_BEACON_COLOR_LABEL = Component.translatable("screen.wynnhorse.next_beacon_color");
    private static final Component TEXT_COLOR_LABEL = Component.translatable("screen.wynnhorse.text_color");
    private static final Component RENDER_STYLE_LABEL = Component.translatable("screen.wynnhorse.render_style");
    private static final Component ROUTE_LINE_COLOR_LABEL = Component.translatable("screen.wynnhorse.route_line_color");
    private static final Component X_LABEL = Component.literal("X");
    private static final Component Y_LABEL = Component.literal("Y");
    private static final Component Z_LABEL = Component.literal("Z");
    private static final int LABEL_TEXT_COLOR = -1;
    private static final int FIELD_TEXT_COLOR = -1;
    private static final int INVALID_TEXT_COLOR = -5636096;
    private static final int LIST_PRIMARY_TEXT_COLOR = -1;
    private static final int LIST_SECONDARY_TEXT_COLOR = -4144960;
    private static final int WAYPOINT_ROW_HEIGHT = 26;
    private static final int ROUTE_FILE_ROW_HEIGHT = 24;
    private static final int ROUTE_BUTTON_BACKGROUND = 0xAA2A2A2A;
    private static final int ROUTE_BUTTON_BORDER = 0xFF7A7A7A;
    private static final int ROUTE_BUTTON_TEXT = -1;

    private final WaypointRoute route;

    private WaypointList waypointList;
    private SavedRouteList savedRouteList;
    private EditBox routeNameField;
    private EditBox horseItemField;
    private EditBox waypointNameField;
    private EditBox waypointXField;
    private EditBox waypointYField;
    private EditBox waypointZField;
    private EditBox beaconColorField;
    private EditBox nextBeaconColorField;
    private EditBox textColorField;
    private EditBox routeLineColorField;
    private Button addCurrentButton;
    private Button removeButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button browseRoutesButton;
    private Button openConfigButton;
    private Button saveRouteButton;
    private Button doneButton;
    private Button newWaypointButton;
    private Button renderStyleButton;

    private UUID selectedWaypointId;
    private boolean syncingFields;
    private boolean browsingSavedRoutes;

    public WaypointManagerScreen() {
        super(TITLE);
        this.route = WynnHorseClient.getWaypointStore().route();
    }

    @Override
    protected void init() {
        int margin = 12;
        int routeFieldY = 40;
        int listLabelY = 68;
        int top = 80;
        int bottomButtonY = this.height - 28;
        int listWidth = Math.max(220, this.width / 2 - 24);
        int listHeight = this.height - top - 44;
        int rightX = margin + listWidth + 12;
        int rightWidth = this.width - rightX - margin;
        int renderStyleLabelY = top + 184;
        int renderStyleButtonY = top + 196;
        int colorLabelY = top + 228;
        int colorFieldY = top + 240;
        int utilityButtonY = top + 272;
        int saveButtonWidth = 60;
        int routeFieldWidth = listWidth - saveButtonWidth - 6;

        this.waypointList = this.addRenderableWidget(new WaypointList(this.minecraft, margin, top, listWidth, listHeight));
        this.savedRouteList = this.addRenderableWidget(new SavedRouteList(this.minecraft, margin, top, listWidth, listHeight));

        this.routeNameField = this.addRenderableWidget(new EditBox(this.font, margin, routeFieldY, routeFieldWidth, 20, ROUTE_NAME_LABEL));
        this.routeNameField.setMaxLength(64);
        this.routeNameField.setValue("route");
        this.routeNameField.setTextColor(FIELD_TEXT_COLOR);

        this.saveRouteButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.save"), button -> this.saveCurrentRoute())
                        .bounds(margin + routeFieldWidth + 6, routeFieldY, saveButtonWidth, 20)
                        .build()
        );

        this.horseItemField = this.addRenderableWidget(new EditBox(this.font, rightX, top + 12, rightWidth, 20, HORSE_ITEM_LABEL));
        this.horseItemField.setMaxLength(64);
        this.horseItemField.setValue(WynnHorseConfig.getHorseItemDisplayName());
        this.horseItemField.setTextColor(FIELD_TEXT_COLOR);

        this.waypointNameField = this.addRenderableWidget(new EditBox(this.font, rightX, top + 60, rightWidth, 20, NAME_LABEL));
        this.waypointNameField.setMaxLength(64);
        this.waypointNameField.setTextColor(FIELD_TEXT_COLOR);
        this.waypointNameField.setResponder(value -> {
            if (this.syncingFields || this.browsingSavedRoutes) {
                return;
            }

            if (this.selectedWaypointId != null && this.route.renameWaypoint(this.selectedWaypointId, value)) {
                this.updateSelectedEntryDisplay();
            }
        });

        int coordinateWidth = (rightWidth - 8) / 3;
        this.waypointXField = this.addRenderableWidget(new EditBox(this.font, rightX, top + 108, coordinateWidth, 20, X_LABEL));
        this.waypointYField = this.addRenderableWidget(new EditBox(this.font, rightX + coordinateWidth + 4, top + 108, coordinateWidth, 20, Y_LABEL));
        this.waypointZField = this.addRenderableWidget(new EditBox(this.font, rightX + (coordinateWidth + 4) * 2, top + 108, coordinateWidth, 20, Z_LABEL));
        this.waypointXField.setTextColor(FIELD_TEXT_COLOR);
        this.waypointYField.setTextColor(FIELD_TEXT_COLOR);
        this.waypointZField.setTextColor(FIELD_TEXT_COLOR);

        this.waypointXField.setResponder(value -> this.onCoordinateFieldsChanged());
        this.waypointYField.setResponder(value -> this.onCoordinateFieldsChanged());
        this.waypointZField.setResponder(value -> this.onCoordinateFieldsChanged());

        this.addLabel(ROUTE_NAME_LABEL, margin, 28, LABEL_TEXT_COLOR);
        this.addLabel(ROUTE_LABEL, margin, listLabelY, LABEL_TEXT_COLOR);
        this.addLabel(SAVED_ROUTES_LABEL, margin, listLabelY, LABEL_TEXT_COLOR).visible = false;
        this.addLabel(HORSE_ITEM_LABEL, rightX, top, LABEL_TEXT_COLOR);
        this.addLabel(NAME_LABEL, rightX, top + 48, LABEL_TEXT_COLOR);
        this.addLabel(X_LABEL, rightX, top + 96, LABEL_TEXT_COLOR);
        this.addLabel(Y_LABEL, rightX + coordinateWidth + 4, top + 96, LABEL_TEXT_COLOR);
        this.addLabel(Z_LABEL, rightX + (coordinateWidth + 4) * 2, top + 96, LABEL_TEXT_COLOR);
        this.addLabel(RENDER_STYLE_LABEL, rightX, renderStyleLabelY, LABEL_TEXT_COLOR);

        this.renderStyleButton = this.addRenderableWidget(
                Button.builder(this.renderStyleButtonMessage(), button -> this.cycleRenderStyle())
                        .bounds(rightX, renderStyleButtonY, rightWidth, 20)
                        .build()
        );

        int colorFieldWidth = (rightWidth - 12) / 4;
        this.addLabel(BEACON_COLOR_LABEL, rightX, colorLabelY, LABEL_TEXT_COLOR);
        this.addLabel(NEXT_BEACON_COLOR_LABEL, rightX + colorFieldWidth + 4, colorLabelY, LABEL_TEXT_COLOR);
        this.addLabel(TEXT_COLOR_LABEL, rightX + (colorFieldWidth + 4) * 2, colorLabelY, LABEL_TEXT_COLOR);
        this.addLabel(ROUTE_LINE_COLOR_LABEL, rightX + (colorFieldWidth + 4) * 3, colorLabelY, LABEL_TEXT_COLOR);

        this.beaconColorField = this.addRenderableWidget(new EditBox(this.font, rightX, colorFieldY, colorFieldWidth, 20, BEACON_COLOR_LABEL));
        this.nextBeaconColorField = this.addRenderableWidget(new EditBox(this.font, rightX + colorFieldWidth + 4, colorFieldY, colorFieldWidth, 20, NEXT_BEACON_COLOR_LABEL));
        this.textColorField = this.addRenderableWidget(new EditBox(this.font, rightX + (colorFieldWidth + 4) * 2, colorFieldY, colorFieldWidth, 20, TEXT_COLOR_LABEL));
        this.routeLineColorField = this.addRenderableWidget(new EditBox(this.font, rightX + (colorFieldWidth + 4) * 3, colorFieldY, colorFieldWidth, 20, ROUTE_LINE_COLOR_LABEL));
        this.beaconColorField.setMaxLength(7);
        this.nextBeaconColorField.setMaxLength(7);
        this.textColorField.setMaxLength(7);
        this.routeLineColorField.setMaxLength(7);
        this.beaconColorField.setValue(WynnHorseConfig.getWaypointBeaconColor());
        this.nextBeaconColorField.setValue(WynnHorseConfig.getActiveWaypointBeaconColor());
        this.textColorField.setValue(WynnHorseConfig.getWaypointTextColor());
        this.routeLineColorField.setValue(WynnHorseConfig.getRouteLineColor());
        this.beaconColorField.setTextColor(FIELD_TEXT_COLOR);
        this.nextBeaconColorField.setTextColor(FIELD_TEXT_COLOR);
        this.textColorField.setTextColor(FIELD_TEXT_COLOR);
        this.routeLineColorField.setTextColor(FIELD_TEXT_COLOR);
        this.beaconColorField.setResponder(value -> this.onColorFieldChanged(this.beaconColorField, WynnHorseConfig::setWaypointBeaconColor));
        this.nextBeaconColorField.setResponder(value -> this.onColorFieldChanged(this.nextBeaconColorField, WynnHorseConfig::setActiveWaypointBeaconColor));
        this.textColorField.setResponder(value -> this.onColorFieldChanged(this.textColorField, WynnHorseConfig::setWaypointTextColor));
        this.routeLineColorField.setResponder(value -> this.onColorFieldChanged(this.routeLineColorField, WynnHorseConfig::setRouteLineColor));

        this.openConfigButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.open_config"), button -> this.openConfigFolder())
                        .bounds(rightX, utilityButtonY, rightWidth, 20)
                        .build()
        );

        int currentX = margin;
        this.addCurrentButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.add_waypoint"), button -> this.addWaypointFromEditor())
                        .bounds(currentX, bottomButtonY, 106, 20)
                        .build()
        );
        currentX += 112;
        this.newWaypointButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.new_waypoint"), button -> this.loadDraftFromCurrentPosition())
                        .bounds(currentX, bottomButtonY, 106, 20)
                        .build()
        );
        currentX += 112;
        this.removeButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.remove"), button -> this.removeSelectedWaypoint())
                        .bounds(currentX, bottomButtonY, 72, 20)
                        .build()
        );
        currentX += 78;
        this.moveUpButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.move_up"), button -> this.moveSelectedWaypointUp())
                        .bounds(currentX, bottomButtonY, 78, 20)
                        .build()
        );
        currentX += 84;
        this.moveDownButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.move_down"), button -> this.moveSelectedWaypointDown())
                        .bounds(currentX, bottomButtonY, 90, 20)
                        .build()
        );
        currentX += 96;
        this.browseRoutesButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.wynnhorse.browse_routes"), button -> this.toggleSavedRoutesBrowser())
                        .bounds(currentX, bottomButtonY, 86, 20)
                        .build()
        );
        this.doneButton = this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> this.closeScreen())
                        .bounds(this.width - margin - 80, bottomButtonY, 80, 20)
                        .build()
        );

        this.refreshWaypointList(null);
        this.refreshSavedRouteList();
        this.loadDraftFromCurrentPosition();
        this.setBrowsingSavedRoutes(false);
        this.updateButtonStates();
        this.updateColorFieldStates();
        this.setInitialFocus(this.routeNameField);
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    @Override
    public void tick() {
        this.updateButtonStates();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 16777215);
    }

    private void closeScreen() {
        this.applyHorseItemField();
        this.minecraft.setScreen(null);
    }

    private void addWaypointFromEditor() {
        if (this.browsingSavedRoutes) {
            return;
        }

        Double x = this.parseCoordinate(this.waypointXField);
        Double y = this.parseCoordinate(this.waypointYField);
        Double z = this.parseCoordinate(this.waypointZField);
        this.updateCoordinateFieldColors(x, y, z);

        if (x == null || y == null || z == null) {
            return;
        }

        String name = this.waypointNameField.getValue();
        if (name.trim().isEmpty()) {
            name = "Waypoint " + (this.route.size() + 1);
        }

        Vec3 position = new Vec3(x, y, z);
        this.route.addWaypoint(name, position);
        this.route.setActiveIndex(this.route.size() - 1);
        this.refreshWaypointList(null);
        this.loadDraftFromCurrentPosition();
        this.setInitialFocus(this.waypointNameField);
    }

    private void removeSelectedWaypoint() {
        if (this.browsingSavedRoutes || this.selectedWaypointId == null) {
            return;
        }

        this.route.removeWaypoint(this.selectedWaypointId);
        this.selectedWaypointId = this.initialSelection();
        this.refreshWaypointList(this.selectedWaypointId);
        if (this.selectedWaypointId == null) {
            this.loadDraftFromCurrentPosition();
        } else {
            this.updateEditorFields();
        }
    }

    private void moveSelectedWaypointUp() {
        if (this.browsingSavedRoutes || this.selectedWaypointId == null) {
            return;
        }

        if (this.route.moveWaypointUp(this.selectedWaypointId)) {
            this.refreshWaypointList(this.selectedWaypointId);
            this.updateEditorFields();
        }
    }

    private void moveSelectedWaypointDown() {
        if (this.browsingSavedRoutes || this.selectedWaypointId == null) {
            return;
        }

        if (this.route.moveWaypointDown(this.selectedWaypointId)) {
            this.refreshWaypointList(this.selectedWaypointId);
            this.updateEditorFields();
        }
    }

    private void toggleSavedRoutesBrowser() {
        if (this.browsingSavedRoutes) {
            this.setBrowsingSavedRoutes(false);
            return;
        }

        this.refreshSavedRouteList();
        this.setBrowsingSavedRoutes(true);
    }

    private void setBrowsingSavedRoutes(final boolean browsingSavedRoutes) {
        this.browsingSavedRoutes = browsingSavedRoutes;
        this.waypointList.setListVisible(!browsingSavedRoutes);
        this.savedRouteList.setListVisible(browsingSavedRoutes);
        this.browseRoutesButton.setMessage(Component.translatable(
                browsingSavedRoutes ? "screen.wynnhorse.back_to_editor" : "screen.wynnhorse.browse_routes"
        ));
        this.updateButtonStates();
    }

    private void saveCurrentRoute() {
        if (this.minecraft == null) {
            return;
        }

        try {
            WaypointRouteCsvStorage.SavedRouteFile savedRoute = WaypointRouteCsvStorage.saveRoute(
                    this.minecraft.gameDirectory.toPath(),
                    this.routeNameField.getValue(),
                    this.route
            );
            this.routeNameField.setValue(savedRoute.displayName());
            this.refreshSavedRouteList();
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.route_saved", savedRoute.fileName()));
        } catch (IOException exception) {
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.route_save_failed"));
        }
    }

    private void openConfigFolder() {
        if (this.minecraft == null) {
            return;
        }

        try {
            Path configDirectory = this.minecraft.gameDirectory.toPath().resolve("config");
            java.nio.file.Files.createDirectories(configDirectory);
            Util.getPlatform().openPath(configDirectory);
        } catch (IOException exception) {
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.config_open_failed"));
        }
    }

    private void loadSavedRoute(final WaypointRouteCsvStorage.SavedRouteFile savedRouteFile) {
        try {
            List<Waypoint> loadedWaypoints = WaypointRouteCsvStorage.loadRoute(savedRouteFile.path());
            this.route.replaceAll(loadedWaypoints);
            this.routeNameField.setValue(savedRouteFile.displayName());
            this.refreshWaypointList(this.initialSelection());
            if (this.route.isEmpty()) {
                this.loadDraftFromCurrentPosition();
            } else {
                this.selectWaypoint(this.initialSelection());
            }
            this.setBrowsingSavedRoutes(false);
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.route_loaded", savedRouteFile.fileName()));
        } catch (IOException exception) {
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.route_load_failed"));
        }
    }

    private void refreshSavedRouteList() {
        if (this.minecraft == null) {
            return;
        }

        try {
            List<WaypointRouteCsvStorage.SavedRouteFile> savedRoutes = WaypointRouteCsvStorage.listRoutes(this.minecraft.gameDirectory.toPath());
            List<SavedRouteList.Entry> entries = new ArrayList<>(savedRoutes.size());
            for (WaypointRouteCsvStorage.SavedRouteFile savedRoute : savedRoutes) {
                entries.add(this.savedRouteList.createEntry(savedRoute));
            }
            this.savedRouteList.replaceEntries(entries);
            this.savedRouteList.refreshScrollAmount();
        } catch (IOException exception) {
            this.savedRouteList.replaceEntries(List.of());
            WynnHorseClient.showStatusOverlay(Component.translatable("message.wynnhorse.route_load_failed"));
        }
    }

    private void onCoordinateFieldsChanged() {
        if (this.syncingFields || this.browsingSavedRoutes || this.selectedWaypointId == null) {
            return;
        }

        Double x = this.parseCoordinate(this.waypointXField);
        Double y = this.parseCoordinate(this.waypointYField);
        Double z = this.parseCoordinate(this.waypointZField);

        this.updateCoordinateFieldColors(x, y, z);

        if (x == null || y == null || z == null) {
            return;
        }

        if (this.route.updateWaypointPosition(this.selectedWaypointId, new Vec3(x, y, z))) {
            this.updateSelectedEntryDisplay();
        }
    }

    private void applyHorseItemField() {
        WynnHorseConfig.setHorseItemDisplayName(this.horseItemField.getValue());
    }

    private void cycleRenderStyle() {
        WynnHorseConfig.setWaypointRenderStyle(WynnHorseConfig.getWaypointRenderStyle().next());
        this.renderStyleButton.setMessage(this.renderStyleButtonMessage());
    }

    private void onColorFieldChanged(final EditBox field, final ColorSetter setter) {
        String normalized = WynnHorseConfig.normalizeHexColor(field.getValue());
        field.setTextColor(normalized == null ? INVALID_TEXT_COLOR : FIELD_TEXT_COLOR);
        if (normalized != null) {
            setter.set(normalized);
        }
    }

    private void refreshWaypointList(final UUID preferredSelection) {
        List<Waypoint> waypoints = this.route.getWaypoints();
        List<WaypointList.Entry> entries = new ArrayList<>(waypoints.size());
        for (int index = 0; index < waypoints.size(); index++) {
            Waypoint waypoint = waypoints.get(index);
            entries.add(this.waypointList.createEntry(waypoint, index));
        }
        this.waypointList.replaceEntries(entries);
        this.selectWaypoint(preferredSelection);
        this.waypointList.refreshScrollAmount();
    }

    private void selectWaypoint(final UUID waypointId) {
        this.selectedWaypointId = waypointId;

        WaypointList.Entry matchingEntry = null;
        if (waypointId != null) {
            List<WaypointList.Entry> entries = this.waypointList.children();
            for (int index = 0; index < entries.size(); index++) {
                WaypointList.Entry entry = entries.get(index);
                if (entry.waypointId.equals(waypointId)) {
                    matchingEntry = entry;
                    this.route.setActiveIndex(index);
                    break;
                }
            }
        }

        this.waypointList.setSelected(matchingEntry);
        this.updateEditorFields();
    }

    private void updateEditorFields() {
        Waypoint waypoint = this.getSelectedWaypoint();

        this.syncingFields = true;
        if (waypoint == null) {
            this.waypointNameField.setValue(this.defaultDraftName());
            Vec3 position = this.defaultDraftPosition();
            this.waypointXField.setValue(this.formatCoordinate(position.x));
            this.waypointYField.setValue(this.formatCoordinate(position.y));
            this.waypointZField.setValue(this.formatCoordinate(position.z));
        } else {
            this.waypointNameField.setValue(waypoint.name());
            this.waypointXField.setValue(this.formatCoordinate(waypoint.position().x));
            this.waypointYField.setValue(this.formatCoordinate(waypoint.position().y));
            this.waypointZField.setValue(this.formatCoordinate(waypoint.position().z));
        }
        this.syncingFields = false;

        this.waypointNameField.setEditable(!this.browsingSavedRoutes);
        this.waypointXField.setEditable(!this.browsingSavedRoutes);
        this.waypointYField.setEditable(!this.browsingSavedRoutes);
        this.waypointZField.setEditable(!this.browsingSavedRoutes);
        this.updateCoordinateFieldColors(
                this.parseCoordinate(this.waypointXField),
                this.parseCoordinate(this.waypointYField),
                this.parseCoordinate(this.waypointZField)
        );
    }

    private void updateButtonStates() {
        this.applyHorseItemField();

        Waypoint waypoint = this.getSelectedWaypoint();
        boolean coordinateFieldsValid = this.parseCoordinate(this.waypointXField) != null
                && this.parseCoordinate(this.waypointYField) != null
                && this.parseCoordinate(this.waypointZField) != null;

        this.addCurrentButton.active = !this.browsingSavedRoutes && this.selectedWaypointId == null && coordinateFieldsValid;
        this.newWaypointButton.active = !this.browsingSavedRoutes && this.minecraft.player != null && this.selectedWaypointId != null;
        this.removeButton.active = !this.browsingSavedRoutes && waypoint != null;
        this.renderStyleButton.active = true;
        this.saveRouteButton.active = !this.browsingSavedRoutes && !this.route.isEmpty();

        int selectedIndex = this.selectedWaypointIndex();
        this.moveUpButton.active = !this.browsingSavedRoutes && selectedIndex > 0;
        this.moveDownButton.active = !this.browsingSavedRoutes && selectedIndex != -1 && selectedIndex < this.route.size() - 1;
        this.doneButton.active = true;
        this.routeNameField.setEditable(!this.browsingSavedRoutes);
    }

    private void loadDraftFromCurrentPosition() {
        this.selectedWaypointId = null;
        this.waypointList.setSelected(null);
        this.updateEditorFields();
    }

    private Component renderStyleButtonMessage() {
        return Component.translatable(
                "screen.wynnhorse.render_style_value",
                Component.translatable(WynnHorseConfig.getWaypointRenderStyle().translationKey())
        );
    }

    private void updateCoordinateFieldColors(final Double x, final Double y, final Double z) {
        this.waypointXField.setTextColor(x == null ? INVALID_TEXT_COLOR : FIELD_TEXT_COLOR);
        this.waypointYField.setTextColor(y == null ? INVALID_TEXT_COLOR : FIELD_TEXT_COLOR);
        this.waypointZField.setTextColor(z == null ? INVALID_TEXT_COLOR : FIELD_TEXT_COLOR);
    }

    private void updateColorFieldStates() {
        this.onColorFieldChanged(this.beaconColorField, WynnHorseConfig::setWaypointBeaconColor);
        this.onColorFieldChanged(this.nextBeaconColorField, WynnHorseConfig::setActiveWaypointBeaconColor);
        this.onColorFieldChanged(this.textColorField, WynnHorseConfig::setWaypointTextColor);
        this.onColorFieldChanged(this.routeLineColorField, WynnHorseConfig::setRouteLineColor);
    }

    private UUID initialSelection() {
        int activeIndex = this.route.getActiveIndex();
        List<Waypoint> waypoints = this.route.getWaypoints();
        if (activeIndex >= 0 && activeIndex < waypoints.size()) {
            return waypoints.get(activeIndex).id();
        }

        return waypoints.isEmpty() ? null : waypoints.getFirst().id();
    }

    private Waypoint getSelectedWaypoint() {
        if (this.selectedWaypointId == null) {
            return null;
        }

        return this.route.getWaypoint(this.selectedWaypointId).orElse(null);
    }

    private int selectedWaypointIndex() {
        if (this.selectedWaypointId == null) {
            return -1;
        }

        List<Waypoint> waypoints = this.route.getWaypoints();
        for (int index = 0; index < waypoints.size(); index++) {
            if (waypoints.get(index).id().equals(this.selectedWaypointId)) {
                return index;
            }
        }

        return -1;
    }

    private Double parseCoordinate(final EditBox field) {
        String value = field.getValue().trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatCoordinate(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String defaultDraftName() {
        return "Waypoint " + (this.route.size() + 1);
    }

    private Vec3 defaultDraftPosition() {
        if (this.minecraft.player != null) {
            return this.minecraft.player.position();
        }

        return Vec3.ZERO;
    }

    private void updateSelectedEntryDisplay() {
        if (this.selectedWaypointId == null) {
            return;
        }

        Waypoint waypoint = this.getSelectedWaypoint();
        if (waypoint == null) {
            return;
        }

        List<WaypointList.Entry> entries = this.waypointList.children();
        int activeIndex = this.route.getActiveIndex();
        for (int index = 0; index < entries.size(); index++) {
            WaypointList.Entry entry = entries.get(index);
            if (entry.waypointId.equals(this.selectedWaypointId)) {
                entry.updateFromWaypoint(waypoint, index, index == activeIndex);
                break;
            }
        }
    }

    private final class WaypointList extends ObjectSelectionList<WaypointList.Entry> {
        private final int x;

        private WaypointList(final Minecraft minecraft, final int x, final int y, final int width, final int height) {
            super(minecraft, width, height, y, WAYPOINT_ROW_HEIGHT);
            this.x = x;
            this.updateSizeAndPosition(width, height, x, y);
        }

        private Entry createEntry(final Waypoint waypoint, final int index) {
            return new Entry(waypoint, index);
        }

        private void setListVisible(final boolean visible) {
            this.visible = visible;
            this.active = visible;
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        @Override
        public void updateSizeAndPosition(final int width, final int height, final int x, final int y) {
            super.updateSizeAndPosition(width, height, x, y);
            this.setX(this.x);
        }

        private final class Entry extends ObjectSelectionList.Entry<WaypointList.Entry> {
            private final UUID waypointId;
            private Component label;
            private Component coordinates;

            private Entry(final Waypoint waypoint, final int index) {
                this.waypointId = waypoint.id();
                this.updateFromWaypoint(waypoint, index, index == WaypointManagerScreen.this.route.getActiveIndex());
            }

            private void updateFromWaypoint(final Waypoint waypoint, final int index, final boolean active) {
                String prefix = active ? "* " : "";
                this.label = Component.literal(prefix + (index + 1) + ". " + waypoint.name());
                this.coordinates = Component.literal(String.format(
                        Locale.ROOT,
                        "X: %.2f  Y: %.2f  Z: %.2f",
                        waypoint.position().x,
                        waypoint.position().y,
                        waypoint.position().z
                ));
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, hovered ? 285212672 : 1426063360);
                graphics.text(WaypointManagerScreen.this.font, this.label, this.getContentX() + 4, this.getContentY() + 3, LIST_PRIMARY_TEXT_COLOR, false);
                graphics.text(WaypointManagerScreen.this.font, this.coordinates, this.getContentX() + 4, this.getContentY() + 14, LIST_SECONDARY_TEXT_COLOR, false);
            }

            @Override
            public boolean keyPressed(final KeyEvent event) {
                if (event.isSelection()) {
                    WaypointManagerScreen.this.selectWaypoint(this.waypointId);
                    return true;
                }

                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                WaypointManagerScreen.this.selectWaypoint(this.waypointId);
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.label.getString() + ", " + this.coordinates.getString());
            }
        }
    }

    private final class SavedRouteList extends ObjectSelectionList<SavedRouteList.Entry> {
        private final int x;

        private SavedRouteList(final Minecraft minecraft, final int x, final int y, final int width, final int height) {
            super(minecraft, width, height, y, ROUTE_FILE_ROW_HEIGHT);
            this.x = x;
            this.updateSizeAndPosition(width, height, x, y);
        }

        private Entry createEntry(final WaypointRouteCsvStorage.SavedRouteFile savedRouteFile) {
            return new Entry(savedRouteFile);
        }

        private void setListVisible(final boolean visible) {
            this.visible = visible;
            this.active = visible;
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        @Override
        public void updateSizeAndPosition(final int width, final int height, final int x, final int y) {
            super.updateSizeAndPosition(width, height, x, y);
            this.setX(this.x);
        }

        private final class Entry extends ObjectSelectionList.Entry<SavedRouteList.Entry> {
            private static final int LOAD_BUTTON_WIDTH = 44;
            private static final int LOAD_BUTTON_HEIGHT = 16;

            private final WaypointRouteCsvStorage.SavedRouteFile savedRouteFile;
            private final Component label;
            private final Component loadLabel = Component.translatable("screen.wynnhorse.load");

            private Entry(final WaypointRouteCsvStorage.SavedRouteFile savedRouteFile) {
                this.savedRouteFile = savedRouteFile;
                this.label = Component.literal(savedRouteFile.displayName());
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, hovered ? 285212672 : 1426063360);
                graphics.text(WaypointManagerScreen.this.font, this.label, this.getContentX() + 4, this.getContentY() + 8, LIST_PRIMARY_TEXT_COLOR, false);

                int buttonX = this.getX() + this.getWidth() - LOAD_BUTTON_WIDTH - 8;
                int buttonY = this.getY() + (this.getHeight() - LOAD_BUTTON_HEIGHT) / 2;
                boolean buttonHovered = mouseX >= buttonX && mouseX < buttonX + LOAD_BUTTON_WIDTH
                        && mouseY >= buttonY && mouseY < buttonY + LOAD_BUTTON_HEIGHT;
                graphics.fill(buttonX, buttonY, buttonX + LOAD_BUTTON_WIDTH, buttonY + LOAD_BUTTON_HEIGHT, buttonHovered ? 0xFF4A4A4A : ROUTE_BUTTON_BACKGROUND);
                graphics.outline(buttonX, buttonY, LOAD_BUTTON_WIDTH, LOAD_BUTTON_HEIGHT, ROUTE_BUTTON_BORDER);
                graphics.centeredText(WaypointManagerScreen.this.font, this.loadLabel, buttonX + LOAD_BUTTON_WIDTH / 2, buttonY + 4, ROUTE_BUTTON_TEXT);
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                int buttonX = this.getX() + this.getWidth() - LOAD_BUTTON_WIDTH - 8;
                int buttonY = this.getY() + (this.getHeight() - LOAD_BUTTON_HEIGHT) / 2;
                if (event.x() >= buttonX && event.x() < buttonX + LOAD_BUTTON_WIDTH
                        && event.y() >= buttonY && event.y() < buttonY + LOAD_BUTTON_HEIGHT) {
                    WaypointManagerScreen.this.loadSavedRoute(this.savedRouteFile);
                    return true;
                }

                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.savedRouteFile.fileName() + ", " + this.loadLabel.getString());
            }
        }
    }

    private StringWidget addLabel(final Component message, final int x, final int y, final int color) {
        StringWidget label = this.addRenderableWidget(new StringWidget(message, this.font));
        label.setX(x);
        label.setY(y);
        label.setFGColor(color);
        return label;
    }

    @FunctionalInterface
    private interface ColorSetter {
        void set(String value);
    }
}
