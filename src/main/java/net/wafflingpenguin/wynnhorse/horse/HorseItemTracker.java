package net.wafflingpenguin.wynnhorse.horse;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HorseItemTracker {
    private static final Pattern HORSE_STAT_PATTERN = Pattern.compile("(?i)\\b(speed|jump)\\b\\s*:?\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
    private static final Pattern GENERIC_STAT_PATTERN = Pattern.compile("\\b(\\d+)\\s*/\\s*(\\d+)\\b");
    private static final Pattern LEVEL_XP_PATTERN = Pattern.compile("\\b(\\d{1,3})\\s*/\\s*100\\b");
    private static final Comparator<HorseItemMatch> MATCH_ORDER = Comparator
            .comparing(HorseItemMatch::hasParsedStat).reversed()
            .thenComparing(match -> match.statMaximum() == null ? Integer.MIN_VALUE : match.statMaximum(), Comparator.reverseOrder())
            .thenComparing(match -> match.statValue() == null ? Integer.MIN_VALUE : match.statValue(), Comparator.reverseOrder())
            .thenComparingInt(HorseItemMatch::slot);

    private List<HorseItemMatch> matches = List.of();
    private Integer pendingMountedHorseSlot;
    private Integer activeMountedHorseSlot;
    private long revision;

    public void refresh(final Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            this.clearMatches();
            return;
        }

        String query = WynnHorseConfig.getHorseItemDisplayName().trim();
        if (query.isEmpty()) {
            this.clearMatches();
            return;
        }

        String loweredQuery = query.toLowerCase(Locale.ROOT);
        Item.TooltipContext tooltipContext = Item.TooltipContext.of(minecraft.level);
        Inventory inventory = minecraft.player.getInventory();
        List<HorseItemMatch> refreshedMatches = new ArrayList<>();

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            String displayName = stack.getDisplayName().getString();
            if (!displayName.toLowerCase(Locale.ROOT).contains(loweredQuery)) {
                continue;
            }

            refreshedMatches.add(buildMatch(stack, slot, displayName, tooltipContext, minecraft));
        }

        refreshedMatches.sort(MATCH_ORDER);
        this.matches = List.copyOf(refreshedMatches);
        this.revision++;
    }

    public List<HorseItemMatch> getMatches() {
        return this.matches;
    }

    public long getRevision() {
        return this.revision;
    }

    public Optional<HorseItemMatch> getPreferredMatch() {
        if (this.matches.isEmpty()) {
            return Optional.empty();
        }

        for (HorseItemMatch match : this.matches) {
            if (!match.isMaxedParsedStat()) {
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }

    public Optional<HorseItemMatch> selectPreferredMatch(final Minecraft minecraft) {
        this.refresh(minecraft);

        Optional<HorseItemMatch> preferredMatch = this.getPreferredMatch();
        preferredMatch.ifPresent(match -> {
            this.pendingMountedHorseSlot = match.slot();
            minecraft.player.getInventory().setSelectedSlot(match.slot());
        });
        return preferredMatch;
    }

    public void markPendingHorseAsMounted() {
        this.activeMountedHorseSlot = this.pendingMountedHorseSlot;
    }

    public boolean isMountedHorseSlot(final int slot) {
        return this.activeMountedHorseSlot != null && this.activeMountedHorseSlot == slot;
    }

    public Optional<HorseItemMatch> getMountedHorseMatch() {
        if (this.activeMountedHorseSlot == null) {
            return Optional.empty();
        }

        for (HorseItemMatch match : this.matches) {
            if (match.slot() == this.activeMountedHorseSlot.intValue()) {
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }

    public boolean isMountedHorseMaxed() {
        return this.getMountedHorseMatch().map(HorseItemMatch::isMaxedParsedStat).orElse(false);
    }

    public boolean selectMountedHorseItem(final Minecraft minecraft) {
        this.refresh(minecraft);
        if (minecraft.player == null || this.activeMountedHorseSlot == null) {
            return false;
        }

        minecraft.player.getInventory().setSelectedSlot(this.activeMountedHorseSlot);
        return true;
    }

    public void clearMountedHorseState() {
        this.pendingMountedHorseSlot = null;
        this.activeMountedHorseSlot = null;
    }

    public OptionalInt getActiveMountedHorseSlot() {
        return this.activeMountedHorseSlot == null ? OptionalInt.empty() : OptionalInt.of(this.activeMountedHorseSlot);
    }

    public void clearMatches() {
        if (!this.matches.isEmpty()) {
            this.matches = List.of();
            this.revision++;
        }
    }

    public boolean selectNonHorseItem(final Minecraft minecraft) {
        this.refresh(minecraft);
        if (minecraft.player == null) {
            return false;
        }

        Set<Integer> matchingSlots = new HashSet<>();
        for (HorseItemMatch match : this.matches) {
            matchingSlots.add(match.slot());
        }

        Inventory inventory = minecraft.player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        if (!matchingSlots.contains(selectedSlot) && !isMountBlacklistSlot(selectedSlot)) {
            return true;
        }

        int firstEmptyNonHorseSlot = -1;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (matchingSlots.contains(slot) || isMountBlacklistSlot(slot)) {
                continue;
            }

            if (!inventory.getItem(slot).isEmpty()) {
                inventory.setSelectedSlot(slot);
                return true;
            }

            if (firstEmptyNonHorseSlot < 0) {
                firstEmptyNonHorseSlot = slot;
            }
        }

        if (firstEmptyNonHorseSlot >= 0) {
            inventory.setSelectedSlot(firstEmptyNonHorseSlot);
            return true;
        }

        return false;
    }

    private static boolean isMountBlacklistSlot(final int slot) {
        return slot >= Inventory.getSelectionSize() - 2;
    }

    private static HorseItemMatch buildMatch(final ItemStack stack, final int slot, final String displayName, final Item.TooltipContext tooltipContext, final Minecraft minecraft) {
        String parsedStatName = null;
        Integer parsedStatValue = null;
        Integer parsedStatMaximum = null;
        Integer parsedXpPercent = null;

        for (Component tooltipLine : stack.getTooltipLines(tooltipContext, minecraft.player, TooltipFlag.NORMAL)) {
            String line = tooltipLine.getString();

            if (parsedStatValue == null || parsedStatMaximum == null) {
                Matcher labeledMatcher = HORSE_STAT_PATTERN.matcher(line);
                if (labeledMatcher.find()) {
                    parsedStatName = labeledMatcher.group(1);
                    parsedStatValue = Integer.parseInt(labeledMatcher.group(2));
                    parsedStatMaximum = Integer.parseInt(labeledMatcher.group(3));
                    continue;
                }

                Matcher genericMatcher = GENERIC_STAT_PATTERN.matcher(line);
                if (genericMatcher.find()) {
                    parsedStatValue = Integer.parseInt(genericMatcher.group(1));
                    parsedStatMaximum = Integer.parseInt(genericMatcher.group(2));
                    continue;
                }
            }

            if (parsedXpPercent == null) {
                Matcher xpMatcher = LEVEL_XP_PATTERN.matcher(line);
                while (xpMatcher.find()) {
                    int parsedValue = Integer.parseInt(xpMatcher.group(1));
                    if (parsedStatMaximum != null && parsedStatMaximum == 100 && parsedStatValue != null && parsedValue == parsedStatValue) {
                        continue;
                    }

                    parsedXpPercent = Math.max(0, Math.min(100, parsedValue));
                    break;
                }
            }
        }

        return new HorseItemMatch(slot, displayName, parsedStatName, parsedStatValue, parsedStatMaximum, parsedXpPercent);
    }
}
