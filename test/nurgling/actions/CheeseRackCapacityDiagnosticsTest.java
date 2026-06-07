package nurgling.actions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheeseRackCapacityDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        String clearSource = readSource("src", "nurgling", "actions", "ClearRacksAndRecordCapacity.java");
        String botSource = readSource("src", "nurgling", "actions", "bots", "CheeseProductionBot.java");
        String batchesSource = readSource("src", "nurgling", "actions", "ProcessCheeseOrderInBatches.java");

        if (!clearSource.contains("getLastCapacityDiagnostics()")) {
            throw new AssertionError("ClearRacksAndRecordCapacity must expose capacity diagnostics");
        }
        if (!clearSource.contains("areas=") || !clearSource.contains("racks=") || !clearSource.contains("empty=")) {
            throw new AssertionError("Capacity diagnostics must include area, rack, and overlay counts");
        }
        if (!clearSource.contains("recordedCapacity=")) {
            throw new AssertionError("Capacity diagnostics must include recorded capacity");
        }
        if (!botSource.contains("capacityDiagnostics") || !botSource.contains("movedToArea=") || !botSource.contains("availableNow=")) {
            throw new AssertionError("CheeseProductionBot must pass post-buffer capacity diagnostics to curd creation");
        }
        if (!batchesSource.contains("capacityDiagnostics") || !batchesSource.contains("capacity details:")) {
            throw new AssertionError("No-rack-space message must include capacity diagnostics");
        }
    }

    private static String readSource(String first, String... more) throws Exception {
        return new String(Files.readAllBytes(Path.of(first, more)), StandardCharsets.UTF_8);
    }
}
