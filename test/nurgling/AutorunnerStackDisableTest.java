package nurgling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutorunnerStackDisableTest {
    public static void main(String[] args) throws Exception {
        Path utilsPath = Path.of("src", "nurgling", "NUtils.java");
        String utils = new String(Files.readAllBytes(utilsPath), StandardCharsets.UTF_8);
        if (!utils.contains("public static boolean stackSwitch(boolean state)")) {
            throw new AssertionError("stackSwitch must report whether the requested state is active");
        }
        if (!utils.contains("return inv.bundle.a == state")) {
            throw new AssertionError("stackSwitch must return false when the bundle button is unavailable and state did not change");
        }
        if (!utils.contains("findBundleButton(inv)")) {
            throw new AssertionError("stackSwitch must try to materialize the bundle button when pagBundle is not cached");
        }
        if (!utils.contains("paginae/act/itemcomb")) {
            throw new AssertionError("stackSwitch must search menu pages for the inventory stacking action");
        }

        Path mapPath = Path.of("src", "nurgling", "NMapView.java");
        String map = new String(Files.readAllBytes(mapPath), StandardCharsets.UTF_8);
        int waitComplete = map.indexOf("WaitForMap");
        int retry = map.indexOf("NUtils.stackSwitch(false)");
        int runner = map.indexOf("runner.run(boundGui)");
        if (retry < 0 || runner < 0 || retry > runner) {
            throw new AssertionError("autorunner must retry disabling stacks before ScenarioRunner starts");
        }
        if (waitComplete >= 0 && retry < waitComplete) {
            throw new AssertionError("autorunner stack retry must happen after map/UI initialization waits");
        }
    }
}
