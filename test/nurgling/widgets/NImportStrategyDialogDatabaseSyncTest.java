package nurgling.widgets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NImportStrategyDialogDatabaseSyncTest {
    public static void main(String[] args) throws Exception {
        String dialog = read("src", "nurgling", "widgets", "NImportStrategyDialog.java");
        String service = read("src", "nurgling", "db", "service", "AreaService.java");
        String dao = read("src", "nurgling", "db", "dao", "AreaDao.java");

        String executeImport = extractMethod(dialog, "private void executeImport");
        if (!executeImport.contains("syncImportedAreasToDatabase(strategy)")) {
            throw new AssertionError("Area import must sync to DB immediately instead of waiting for debounced save");
        }
        if (!executeImport.contains("pauseAreaSyncForImport()")) {
            throw new AssertionError("Area import must pause pull sync before changing the local area map");
        }

        if (!dialog.contains("pauseAreaSyncForImport")) {
            throw new AssertionError("Import dialog needs a sync pause helper");
        }

        if (!dialog.contains("replaceAreasToDatabaseAsync")) {
            throw new AssertionError("Full replace import must call DB replace path");
        }

        if (!service.contains("replaceAreasToDatabaseAsync")) {
            throw new AssertionError("AreaService needs a full replace DB export path");
        }

        if (!service.contains("pauseSyncForImport") || !service.contains("importSyncPausedUntil")) {
            throw new AssertionError("AreaService must expose and honor an import sync pause");
        }

        if (!dao.contains("tombstoneAreasNotIn")) {
            throw new AssertionError("Full replace must tombstone live DB areas absent from imported JSON");
        }
    }

    private static String read(String first, String... more) throws Exception {
        return new String(Files.readAllBytes(Path.of(first, more)), StandardCharsets.UTF_8);
    }

    private static String extractMethod(String text, String signature) {
        int start = text.indexOf(signature);
        if (start < 0) throw new AssertionError("Method not found: " + signature);
        int brace = text.indexOf('{', start);
        if (brace < 0) throw new AssertionError("Method body not found: " + signature);

        int depth = 0;
        for (int i = brace; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        throw new AssertionError("Method body did not close: " + signature);
    }
}
