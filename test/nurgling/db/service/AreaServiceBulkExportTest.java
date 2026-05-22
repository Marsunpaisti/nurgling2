package nurgling.db.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AreaServiceBulkExportTest {
    public static void main(String[] args) throws Exception {
        Path source = Path.of("src", "nurgling", "db", "service", "AreaService.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        String method = extractMethod(text, "public CompletableFuture<Integer> exportAreasToDatabaseAsync");
        if (method.contains("saveArea(area, profile)")) {
            throw new AssertionError("Bulk export must not call public saveArea while executeWithRetry holds a connection");
        }
    }

    private static String extractMethod(String text, String signature) {
        int start = text.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Method not found: " + signature);
        }
        int brace = text.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signature);
        }

        int depth = 0;
        for (int i = brace; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body did not close: " + signature);
    }
}
