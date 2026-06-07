package haven;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GameUIStdoutMirrorTest {
    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "haven", "GameUI.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        require(source, "System.out.println(\"[GameUI.msg] \" + msg)", "GameUI.msg must mirror to stdout");
        require(source, "System.err.println(\"[GameUI.error] \" + msg)", "GameUI.error must mirror to stderr");
    }

    private static void require(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + ": missing " + needle);
        }
    }
}
