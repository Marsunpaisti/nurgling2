package nurgling.actions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProcessCheeseOrderInBatchesStartStepTest {
    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "nurgling", "actions", "ProcessCheeseOrderInBatches.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        if (!source.contains("getStartStep(order)")) {
            throw new AssertionError("Curd creation must select the pending start step, not the first pending status entry");
        }

        String method = extractMethod(source, "private CheeseOrder.StepStatus getStartStep");
        if (!method.contains("step.place.equals(\"start\")") || !method.contains("step.left > 0")) {
            throw new AssertionError("getStartStep must only return unfinished start steps");
        }

        if (source.contains("getCurrentStep(order)")) {
            throw new AssertionError("Curd creation must not use getCurrentStep(order), which lets later stages block start work");
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
