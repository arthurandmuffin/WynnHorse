package net.wafflingpenguin.wynnhorse.waypoint;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class WaypointRouteCsvStorage {
    private static final String ROUTES_DIRECTORY = "config/wynnhorse/routes";

    private WaypointRouteCsvStorage() {
    }

    public static SavedRouteFile saveRoute(final Path gameDirectory, final String routeName, final WaypointRoute route) throws IOException {
        Path routesDirectory = ensureRoutesDirectory(gameDirectory);
        String normalizedFileName = normalizeFileName(routeName);
        Path routeFile = routesDirectory.resolve(normalizedFileName);

        List<String> lines = new ArrayList<>(route.size());
        for (Waypoint waypoint : route.getWaypoints()) {
            lines.add(String.format(
                    Locale.ROOT,
                    "%s,%s,%s",
                    Double.toString(waypoint.position().x),
                    Double.toString(waypoint.position().y),
                    Double.toString(waypoint.position().z)
            ));
        }

        Files.write(routeFile, lines, StandardCharsets.UTF_8);
        return SavedRouteFile.fromPath(routeFile);
    }

    public static List<SavedRouteFile> listRoutes(final Path gameDirectory) throws IOException {
        Path routesDirectory = ensureRoutesDirectory(gameDirectory);
        try (Stream<Path> files = Files.list(routesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .map(SavedRouteFile::fromPath)
                    .toList();
        }
    }

    public static List<Waypoint> loadRoute(final Path routeFile) throws IOException {
        List<String> lines = Files.readAllLines(routeFile, StandardCharsets.UTF_8);
        List<Waypoint> loadedWaypoints = new ArrayList<>();
        int waypointNumber = 1;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String trimmedLine = lines.get(lineIndex).trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            String[] parts = trimmedLine.split(",");
            if (parts.length != 3) {
                throw new IOException("Invalid waypoint CSV line " + (lineIndex + 1) + ": expected x,y,z");
            }

            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                loadedWaypoints.add(Waypoint.create("Waypoint " + waypointNumber++, new Vec3(x, y, z)));
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid waypoint CSV line " + (lineIndex + 1) + ": invalid number", exception);
            }
        }

        return loadedWaypoints;
    }

    public static Path ensureRoutesDirectory(final Path gameDirectory) throws IOException {
        Path routesDirectory = gameDirectory.resolve(ROUTES_DIRECTORY);
        Files.createDirectories(routesDirectory);
        return routesDirectory;
    }

    private static String normalizeFileName(final String routeName) {
        String trimmed = routeName == null ? "" : routeName.trim();
        String baseName = trimmed.isEmpty() ? "route" : trimmed;
        String sanitized = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return sanitized;
        }
        return sanitized + ".csv";
    }

    public record SavedRouteFile(String fileName, String displayName, Path path) {
        private static SavedRouteFile fromPath(final Path path) {
            String fileName = path.getFileName().toString();
            String displayName = fileName.toLowerCase(Locale.ROOT).endsWith(".csv")
                    ? fileName.substring(0, fileName.length() - 4)
                    : fileName;
            return new SavedRouteFile(fileName, displayName, path);
        }
    }
}
