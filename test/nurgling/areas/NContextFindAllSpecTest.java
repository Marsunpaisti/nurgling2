package nurgling.areas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NContextFindAllSpecTest {
    public static void main(String[] args) throws Exception {
        String source = new String(Files.readAllBytes(
            Path.of("src", "nurgling", "areas", "NContext.java")), StandardCharsets.UTF_8);

        String method = extractMethod(source, "public static ArrayList<NArea> findAllSpec");
        if (!method.contains("gui.map.glob.map.areas.values()")) {
            throw new AssertionError("findAllSpec must search registered areas, not transient map overlays");
        }
        if (method.contains("dist < Double.MAX_VALUE")) {
            throw new AssertionError("findAllSpec must not drop configured areas only because distance is unavailable");
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
