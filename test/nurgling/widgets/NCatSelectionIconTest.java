package nurgling.widgets;

import haven.Resource;
import haven.Tex;
import org.json.JSONObject;

import java.awt.image.BufferedImage;

public class NCatSelectionIconTest {
    public static void main(String[] args) {
        BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Tex icon = NCatSelectionIcons.itemIcon(new JSONObject().put("name", "Broken item"), item -> {
            throw new Resource.NoSuchResourceException("gfx/invobjs/missing-test", -1, null);
        }, fallback);

        if (icon == null)
            throw new AssertionError("Missing item resource should return a fallback icon");

        if (!icon.sz().equals(fallback.getWidth(), fallback.getHeight()))
            throw new AssertionError("Fallback icon should use the missing-resource image");
    }
}
