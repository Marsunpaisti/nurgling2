package nurgling.actions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateTraysWithCurdsDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "nurgling", "actions", "CreateTraysWithCurds.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        if (!source.contains("CreationDiagnostics")) {
            throw new AssertionError("CreateTraysWithCurds must track creation diagnostics");
        }
        require(source, "requestedTrays=", "diagnostics must include requested tray count");
        require(source, "emptyTraysInInventory=", "diagnostics must include initial empty tray count");
        require(source, "availableTraySlots=", "diagnostics must include inventory tray slot count");
        require(source, "trayStorages=", "diagnostics must include tray storage count");
        require(source, "traysObtained=", "diagnostics must include trays obtained count");
        require(source, "curdStorages=", "diagnostics must include curd storage count");
        require(source, "curdContainersScanned=", "diagnostics must include curd containers scanned count");
        require(source, "curdContainersSkippedEmpty=", "diagnostics must include skipped empty curd containers");
        require(source, "curdsFound=", "diagnostics must include total curds found");
        require(source, "createdTrays=", "diagnostics must include final created tray count");
        require(source, "Curd tray creation failed", "diagnostics must log failure reasons");
        require(source, "Curd tray creation summary", "diagnostics must log a final summary");
    }

    private static void require(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + ": missing " + needle);
        }
    }
}
