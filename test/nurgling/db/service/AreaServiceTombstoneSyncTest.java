package nurgling.db.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AreaServiceTombstoneSyncTest {
    public static void main(String[] args) throws Exception {
        String source = new String(Files.readAllBytes(
            Path.of("src", "nurgling", "db", "service", "AreaService.java")), StandardCharsets.UTF_8);

        if (!source.contains("shouldApplyTombstone")) {
            throw new AssertionError("Tombstone sync needs an explicit guard for unsynced local areas");
        }

        String method = extractMethod(source, "public List<NArea> checkForUpdatesAndMerge");
        if (!method.contains("shouldApplyTombstone(local)")) {
            throw new AssertionError("Tombstones must not delete brand-new local areas with reused DB ids");
        }
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
