package nurgling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DebugDiagnosticsGateTest {
    public static void main(String[] args) throws Exception {
        assertDebugPrefixGated("src/nurgling/NUtils.java", "[NavArea]");
        assertDebugPrefixGated("src/nurgling/actions/FreeContainers.java", "[FreeContainers]");
        assertDebugPrefixGated("src/nurgling/actions/TransferItems2.java", "[TransferItems2]");
        assertDebugPrefixGated("src/nurgling/areas/NContext.java", "[NContext]");
        assertDebugPrefixGated("src/nurgling/navigation/AreaNavigationHelper.java", "[AreaCorner]");
        assertDebugPrefixGated("src/nurgling/navigation/ChunkNavPlanner.java", "[ChunkNav]");
    }

    private static void assertDebugPrefixGated(String file, String prefix) throws Exception {
        String source = new String(Files.readAllBytes(Path.of(file)), StandardCharsets.UTF_8);
        String directChatPrefix = ".msg(\"" + prefix;
        if (source.contains(directChatPrefix)) {
            throw new AssertionError(prefix + " diagnostics in " + file + " must use NUtils.debugMsg instead of direct chat output");
        }
    }
}
