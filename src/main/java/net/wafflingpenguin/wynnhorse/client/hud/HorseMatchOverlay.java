package net.wafflingpenguin.wynnhorse.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.client.WynnHorseClient;
import net.wafflingpenguin.wynnhorse.horse.HorseItemMatch;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;
import java.util.OptionalInt;

public final class HorseMatchOverlay {
    private static final Component TITLE = Component.translatable("hud.wynnhorse.horse_matches");
    private static final int PANEL_BACKGROUND = 0xA0101010;
    private static final int PANEL_BORDER = 0xD0565656;
    private static final int TITLE_COLOR = 0xFFFFD640;
    private static final int SLOT_HIGHLIGHT = 0x553A7A9B;
    private static final int SLOT_TEXT_COLOR = 0xFFD0D0D0;
    private static final int STAT_TEXT_COLOR = 0xFFFFB347;
    private static final int DETAIL_TEXT_COLOR = 0xFF9ED2E6;
    private static final int STATUS_MESSAGE_BACKGROUND = 0xA0202020;
    private static final int STATUS_MESSAGE_BORDER = 0xD0808080;
    private static final int STATUS_MESSAGE_TEXT = 0xFFFFFFFF;
    private static final int PADDING = 6;
    private static final int ROW_SPACING = 2;
    private static OverlayLayout cachedLayout;
    private static long cachedRevision = Long.MIN_VALUE;
    private static boolean cachedAutomationEnabled;
    private static int cachedMountedHorseSlot = Integer.MIN_VALUE;

    private HorseMatchOverlay() {
    }

    public static void render(final CustomizeGuiOverlayEvent.Chat event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        List<HorseItemMatch> matches = WynnHorseClient.getHorseItemTracker().getMatches();
        if (matches.isEmpty()) {
            renderStatusMessage(graphics, minecraft.font);
            return;
        }

        Font font = minecraft.font;
        boolean automationEnabled = WynnHorseClient.isAutomationEnabled();
        OptionalInt mountedHorseSlot = WynnHorseClient.getHorseItemTracker().getActiveMountedHorseSlot();
        OverlayLayout layout = getLayout(font, matches, automationEnabled, mountedHorseSlot);

        int x = 8;
        int y = 24;

        graphics.fill(x, y, x + layout.panelWidth() + (PADDING * 2), y + layout.panelHeight(), PANEL_BACKGROUND);
        graphics.outline(x, y, layout.panelWidth() + (PADDING * 2), layout.panelHeight(), PANEL_BORDER);
        graphics.text(font, TITLE, x + PADDING, y + PADDING, TITLE_COLOR, false);

        int selectedSlot = minecraft.player.getInventory().getSelectedSlot();
        int textY = y + PADDING + font.lineHeight + 2;
        int textColor = WynnHorseConfig.parseHexColor(WynnHorseConfig.getWaypointTextColor(), SLOT_TEXT_COLOR);

        for (HorseItemMatch match : matches) {
            int currentRowHeight = rowHeight(font, match);
            if (match.slot() == selectedSlot) {
                graphics.fill(x + 1, textY - 1, x + layout.panelWidth() + (PADDING * 2) - 1, textY + currentRowHeight - 1, SLOT_HIGHLIGHT);
            }

            graphics.text(font, match.labelText(), x + PADDING, textY, textColor, false);
            int nextX = x + PADDING + font.width(match.labelText());
            if (match.hasParsedStat()) {
                int statX = nextX + 4;
                graphics.text(font, match.statText(), statX, textY, STAT_TEXT_COLOR, false);
                nextX = statX + font.width(match.statText());
            }

            if (match.hasTimingEstimate()) {
                boolean useEtaLabel = automationEnabled && WynnHorseClient.getHorseItemTracker().isMountedHorseSlot(match.slot());
                int detailX = x + PADDING + layout.detailsColumnOffset() + 10;
                graphics.text(font, match.timingText(useEtaLabel), detailX, textY, DETAIL_TEXT_COLOR, false);
            }

            textY += font.lineHeight + ROW_SPACING;
        }

        renderStatusMessage(graphics, font);
    }

    private static int leftColumnWidth(final Font font, final HorseItemMatch match) {
        return match.hasParsedStat()
                ? font.width(match.labelText()) + 4 + font.width(match.statText())
                : font.width(match.labelText());
    }

    private static OverlayLayout getLayout(
            final Font font,
            final List<HorseItemMatch> matches,
            final boolean automationEnabled,
            final OptionalInt mountedHorseSlot
    ) {
        int mountedSlotValue = mountedHorseSlot.orElse(Integer.MIN_VALUE);
        long revision = WynnHorseClient.getHorseItemTracker().getRevision();
        if (cachedLayout != null
                && cachedRevision == revision
                && cachedAutomationEnabled == automationEnabled
                && cachedMountedHorseSlot == mountedSlotValue) {
            return cachedLayout;
        }

        int panelWidth = font.width(TITLE.getString());
        int panelHeight = font.lineHeight + PADDING * 2 + 2;
        int detailsColumnOffset = 0;
        for (HorseItemMatch match : matches) {
            detailsColumnOffset = Math.max(detailsColumnOffset, leftColumnWidth(font, match));
        }
        for (HorseItemMatch match : matches) {
            panelWidth = Math.max(panelWidth, rowWidth(font, match, detailsColumnOffset, automationEnabled));
            panelHeight += rowHeight(font, match);
        }

        cachedRevision = revision;
        cachedAutomationEnabled = automationEnabled;
        cachedMountedHorseSlot = mountedSlotValue;
        cachedLayout = new OverlayLayout(panelWidth, panelHeight, detailsColumnOffset);
        return cachedLayout;
    }

    private static int rowWidth(final Font font, final HorseItemMatch match, final int detailsColumnOffset, final boolean automationEnabled) {
        int width = leftColumnWidth(font, match);

        if (match.hasTimingEstimate()) {
            boolean useEtaLabel = automationEnabled && WynnHorseClient.getHorseItemTracker().isMountedHorseSlot(match.slot());
            width = Math.max(width, detailsColumnOffset) + 10 + font.width(match.timingText(useEtaLabel));
        }

        return width;
    }

    private static int rowHeight(final Font font, final HorseItemMatch match) {
        return font.lineHeight + ROW_SPACING;
    }

    private static void renderStatusMessage(final GuiGraphicsExtractor graphics, final Font font) {
        Component statusMessage = WynnHorseClient.getStatusOverlayMessage();
        if (statusMessage == null) {
            return;
        }

        int messageWidth = font.width(statusMessage);
        int x = (graphics.guiWidth() - messageWidth) / 2 - PADDING;
        int y = 8;
        int width = messageWidth + (PADDING * 2);
        int height = font.lineHeight + (PADDING * 2);

        graphics.fill(x, y, x + width, y + height, STATUS_MESSAGE_BACKGROUND);
        graphics.outline(x, y, width, height, STATUS_MESSAGE_BORDER);
        graphics.centeredText(font, statusMessage, x + width / 2, y + PADDING, STATUS_MESSAGE_TEXT);
    }

    private record OverlayLayout(int panelWidth, int panelHeight, int detailsColumnOffset) {
    }
}
