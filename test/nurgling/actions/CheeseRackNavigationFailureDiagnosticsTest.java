package nurgling.actions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheeseRackNavigationFailureDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "nurgling", "actions", "ClearRacksAndRecordCapacity.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        require(source, "boolean navigationSuccess = NUtils.navigateToArea(area)", "rack scan must check area navigation result");
        require(source, "Cannot scan cheese racks", "rack scan must log a user-facing pathing failure");
        require(source, "navigationFailed=", "capacity diagnostics must include navigation failure count");
        require(source, "failedAreas=", "capacity diagnostics must include failed area names");
        require(source, "scan.navigationFailed++", "failed navigation must increment diagnostic count");
        require(source, "scan.failedAreas.add", "failed navigation must record the area that could not be scanned");
    }

    private static void require(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + ": missing " + needle);
        }
    }
}
