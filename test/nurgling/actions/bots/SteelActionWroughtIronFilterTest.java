package nurgling.actions.bots;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SteelActionWroughtIronFilterTest {
    public static void main(String[] args) throws Exception {
        Path source = Path.of("src", "nurgling", "actions", "bots", "SteelAction.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        if (!text.contains("new NAlias(\"Wrought Iron\")")) {
            throw new AssertionError("SteelAction must inspect steelbox inventories for Wrought Iron");
        }

        if (!text.contains("containersWithWroughtIron")) {
            throw new AssertionError("SteelAction must keep a filtered crucible list");
        }

        if (!text.contains("new FuelToContainers(containersWithWroughtIron)")) {
            throw new AssertionError("SteelAction must refuel only crucibles containing Wrought Iron");
        }

        if (!text.contains("for (Container cont : containersWithWroughtIron)")) {
            throw new AssertionError("SteelAction must light only crucibles containing Wrought Iron");
        }
    }
}
