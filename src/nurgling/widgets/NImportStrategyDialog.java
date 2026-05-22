package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.i18n.L10n;
import nurgling.areas.NArea;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NImportStrategyDialog extends Window
{
    public enum ImportStrategy {
        FULL_REPLACE,    // Delete all old, add new
        DUPLICATE,       // Old behavior - rename duplicates
        OVERWRITE        // Replace areas with same name
    }
    
    private File selectedFile;
    
    public NImportStrategyDialog()
    {
        super(UI.scale(new Coord(300, 120)), L10n.get("import.title"));
        
        int btnWidth = UI.scale(90);
        int btnHeight = UI.scale(25);
        int spacing = UI.scale(10);
        
        // Label
        prev = add(new Label("Choose import strategy:"), UI.scale(10, 10));
        
        // Full Replace button
        prev = add(new Button(UI.scale(280), "Full Replace (delete old, add new)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.FULL_REPLACE);
            }
        }, prev.pos("bl").adds(0, 15));
        
        // Duplicate button  
        prev = add(new Button(UI.scale(280), "Duplicate (rename conflicts)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.DUPLICATE);
            }
        }, prev.pos("bl").adds(0, 5));
        
        // Overwrite button
        prev = add(new Button(UI.scale(280), "Overwrite (replace same names)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.OVERWRITE);
            }
        }, prev.pos("bl").adds(0, 5));
        
        pack();
    }
    
    private void executeImport(ImportStrategy strategy) {
        if(selectedFile != null) {
            pauseAreaSyncForImport();
            switch(strategy) {
                case FULL_REPLACE:
                    NUtils.getUI().core.config.replaceAreas(selectedFile);
                    break;
                case DUPLICATE:
                    NUtils.getUI().core.config.mergeAreas(selectedFile);
                    break;
                case OVERWRITE:
                    NUtils.getUI().core.config.overwriteAreas(selectedFile);
                    break;
            }
            if (!syncImportedAreasToDatabase(strategy)) {
                NConfig.needAreasUpdate();
            }
            if(NUtils.getGameUI().areas != null) {
                NUtils.getGameUI().areas.hide();
                NUtils.getGameUI().areas.show();
            }
        }
        hide();
    }

    private void pauseAreaSyncForImport() {
        if (NCore.databaseManager != null && NCore.databaseManager.getAreaService() != null) {
            NCore.databaseManager.getAreaService().pauseSyncForImport();
        }
    }

    private boolean syncImportedAreasToDatabase(ImportStrategy strategy) {
        if (!(Boolean) NConfig.get(NConfig.Key.ndbenable)) return false;
        if (NCore.databaseManager == null || !NCore.databaseManager.isReady()) return false;
        if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null ||
            NUtils.getGameUI().map.glob == null || NUtils.getGameUI().map.glob.map == null) return false;

        String profile = NUtils.getGameUI().getGenus();
        if (profile == null || profile.isEmpty()) profile = "global";

        Map<Integer, NArea> areas = new HashMap<>(NUtils.getGameUI().map.glob.map.areas);
        if (strategy == ImportStrategy.FULL_REPLACE) {
            NCore.databaseManager.getAreaService().replaceAreasToDatabaseAsync(areas, profile)
                .exceptionally(e -> {
                    System.err.println("Failed to replace imported areas in database: " + e.getMessage());
                    NConfig.needAreasUpdate();
                    return null;
                });
        } else {
            NCore.databaseManager.getAreaService().exportAreasToDatabaseAsync(areas, profile)
                .exceptionally(e -> {
                    System.err.println("Failed to save imported areas to database: " + e.getMessage());
                    NConfig.needAreasUpdate();
                    return null;
                });
        }
        return true;
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static void showDialog(File file)
    {
        NUtils.getGameUI().importDialog.selectedFile = file;
        NUtils.getGameUI().importDialog.show();
        NUtils.getGameUI().importDialog.raise();
        // Position relative to areas widget if it exists and is visible
        if(NUtils.getGameUI().areas != null && NUtils.getGameUI().areas.visible()) {
            NUtils.getGameUI().importDialog.c = NUtils.getGameUI().areas.c.add(
                (NUtils.getGameUI().areas.sz.x - NUtils.getGameUI().importDialog.sz.x) / 2,
                (NUtils.getGameUI().areas.sz.y - NUtils.getGameUI().importDialog.sz.y) / 2
            );
        }
    }
}

