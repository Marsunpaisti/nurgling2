package nurgling.actions.bots;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SteelActionWroughtIronFilterTest {
    public static void main(String[] args) throws Exception {
        if (!SteelAction.isWroughtIronBar("Bar of Wrought Iron", null)) {
            throw new AssertionError("SteelAction must match wrought iron by display name");
        }

        if (!SteelAction.isWroughtIronBar(null, "gfx/invobjs/bar-wroughtiron")) {
            throw new AssertionError("SteelAction must match wrought iron by resource name");
        }

        if (SteelAction.isWroughtIronBar("Wrought Iron Nugget", "gfx/invobjs/nugget-wroughtiron")) {
            throw new AssertionError("SteelAction must not match wrought iron nuggets");
        }

        Path source = Path.of("src", "nurgling", "actions", "bots", "SteelAction.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        if (!text.contains("hasWroughtIronBar(gui.getWindow(container.cap))")) {
            throw new AssertionError("SteelAction must inspect all steelbox inventories for wrought iron bars");
        }

        if (text.contains("hasWroughtIronBar(gui.getInventory(container.cap))")) {
            throw new AssertionError("SteelAction must not inspect only the first steelbox inventory");
        }

        if (!text.contains("containersWithWroughtIron")) {
            throw new AssertionError("SteelAction must keep a filtered crucible list");
        }

        if (!text.contains("new FuelToContainers(containersWithWroughtIron)")) {
            throw new AssertionError("SteelAction must refuel only crucibles containing Wrought Iron");
        }

        if (!text.contains("setMaxlvl(18)")) {
            throw new AssertionError("SteelAction must fill steel crucibles to the full 18/18 fuel meter");
        }

        if (text.contains("setMaxlvl(15)")) {
            throw new AssertionError("SteelAction must not stop steel crucible fuel at 15/18");
        }

        if (!text.contains("for (Container cont : containersWithWroughtIron)")) {
            throw new AssertionError("SteelAction must light only crucibles containing Wrought Iron");
        }
    }
}
