package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseConstants;
import nurgling.actions.bots.cheese.CheeseInventoryOperations;

import java.util.ArrayList;

public class CreateTraysWithCurds implements Action {
    private final String curdType;
    private final int count;
    private final String cheeseTrayType = CheeseConstants.CHEESE_TRAY_NAME;
    NAlias cheeseTrayAlias = CheeseConstants.CHEESE_TRAY_ALIAS;
    private int lastTraysCreated = 0;

    public CreateTraysWithCurds(String curdType, int count) {
        this.curdType = curdType;
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        CreationDiagnostics diagnostics = new CreationDiagnostics(curdType, count);

        context.addInItem(curdType, null);
        context.addInItem(cheeseTrayType, null);

        int traysCreated = 0;
        
        // Step 1: Pre-fetch empty trays FIRST (before scanning curds)
        fetchEmptyTraysForBatch(gui, context, count, diagnostics);
        
        // Step 2: Scan all curd containers ONCE and cache item counts
        ArrayList<ContainerCurdInfo> curdContainers = scanCurdContainers(gui, context, diagnostics);
        gui.msg("Curd tray creation start for " + curdType + ": " + diagnostics.toSummary());
        
        while (traysCreated < count) {
            // 1. Find an empty tray (should be available from pre-fetch)
            WItem emptyTray = getNextEmptyTray(gui);
            if (emptyTray == null) {
                diagnostics.createdTrays = traysCreated;
                gui.error("Curd tray creation failed for " + curdType + ": no empty tray available (" + diagnostics.toSummary() + ")");
                break;
            }

            // 2. Make sure you have 4 curds
            ArrayList<WItem> curds = gui.getInventory().getItems(curdType);
            if (curds.size() < CheeseConstants.CURDS_PER_TRAY) {
                int needed = CheeseConstants.CURDS_PER_TRAY - curds.size();
                
                // Use cached data to check if we have enough curds
                int availableInStorage = calculateTotalCurds(curdContainers);
                
                // Only proceed if we have enough curds in storage
                if (availableInStorage < needed) {
                    diagnostics.createdTrays = traysCreated;
                    gui.error("Curd tray creation failed for " + curdType + ": not enough curds available to fill a tray (need " + needed + ", found " + availableInStorage + ", " + diagnostics.toSummary() + ")");
                    break;
                }
                
                // Take curds from containers using cached info (go to containers with curds first)
                Results curdRes = takeCurdsOptimized(gui, context, curdContainers, needed, diagnostics);
                if (!curdRes.isSuccess || gui.getInventory().getItems(curdType).size() < CheeseConstants.CURDS_PER_TRAY) {
                    diagnostics.createdTrays = traysCreated;
                    gui.error("Curd tray creation failed for " + curdType + ": failed to retrieve curds from storage (inventoryCurds=" + gui.getInventory().getItems(curdType).size() + ", " + diagnostics.toSummary() + ")");
                    break;
                }

                curds = gui.getInventory().getItems(curdType);
            }

            // 3. Use 4 curds on the tray
            for (int j = 0; j < CheeseConstants.CURDS_PER_TRAY; j++) {
                new UseItemOnItem(new NAlias(curdType), emptyTray).run(gui);
                int expected = curds.size() - (j + 1);
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), new NAlias(curdType), expected));
            }

            traysCreated++;
        }

        lastTraysCreated = traysCreated;
        diagnostics.createdTrays = traysCreated;
        gui.msg("Curd tray creation summary for " + curdType + ": " + diagnostics.toSummary());
        
        // Note: We do NOT clean up inventory here!
        // The calling code (ProcessCheeseOrderInBatches) is responsible for:
        // 1. Moving filled trays to racks via rackManager.handleTrayPlacement()
        // 2. Returning empty trays to storage after that
        // This prevents FreeInventory2 from interfering with tray placement logic.
        
        return Results.SUCCESS();
    }
    
    /**
     * Scan all curd containers ONCE and cache their item counts.
     * This avoids repeated PathFinder calls to check availability.
     */
    private ArrayList<ContainerCurdInfo> scanCurdContainers(NGameUI gui, NContext context, CreationDiagnostics diagnostics) throws InterruptedException {
        ArrayList<ContainerCurdInfo> result = new ArrayList<>();
        ArrayList<NContext.ObjectStorage> curdStorages = context.getInStorages(curdType);
        diagnostics.curdStorages = curdStorages != null ? curdStorages.size() : 0;
        
        if (curdStorages == null || curdStorages.isEmpty()) {
            gui.msg("Curd tray creation curd scan for " + curdType + ": no input storage configured (" + diagnostics.toSummary() + ")");
            return result;
        }
        
        for (NContext.ObjectStorage storage : curdStorages) {
            if (storage instanceof Container) {
                diagnostics.curdContainersScanned++;
                Container container = (Container) storage;
                Gob containerGob = Finder.findGob(container.gobid);
                if (containerGob != null) {
                    // Skip visually empty containers
                    if (containerGob.ngob.isContainerEmpty()) {
                        diagnostics.curdContainersSkippedEmpty++;
                        continue;
                    }
                    
                    new PathFinder(containerGob).run(gui);
                    new OpenTargetContainer(container).run(gui);
                    
                    ArrayList<WItem> curdsInContainer = gui.getInventory(container.cap).getItems(curdType);
                    int curdCount = curdsInContainer.size();
                    diagnostics.curdsFound += curdCount;
                    
                    result.add(new ContainerCurdInfo(container, curdCount));
                    
                    new CloseTargetContainer(container).run(gui);
                } else {
                    diagnostics.curdContainersMissingGob++;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Calculate total curds available across all scanned containers
     */
    private int calculateTotalCurds(ArrayList<ContainerCurdInfo> containers) {
        int total = 0;
        for (ContainerCurdInfo info : containers) {
            total += info.curdCount;
        }
        return total;
    }
    
    /**
     * Take curds from containers using cached info.
     * Goes directly to containers that have curds, skipping empty ones.
     */
    private Results takeCurdsOptimized(NGameUI gui, NContext context, ArrayList<ContainerCurdInfo> containers, int needed, CreationDiagnostics diagnostics) throws InterruptedException {
        int taken = 0;
        
        for (ContainerCurdInfo info : containers) {
            if (taken >= needed) break;
            if (info.curdCount <= 0) continue;
            
            Gob containerGob = Finder.findGob(info.container.gobid);
            if (containerGob == null) {
                diagnostics.curdContainersMissingGob++;
                continue;
            }
            
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(info.container).run(gui);
            
            // Get current curds in this container
            ArrayList<WItem> curdsInContainer = gui.getInventory(info.container.cap).getItems(curdType);
            int toTake = Math.min(curdsInContainer.size(), needed - taken);
            
            for (int i = 0; i < toTake && i < curdsInContainer.size(); i++) {
                WItem curd = curdsInContainer.get(i);
                curd.item.wdgmsg("transfer", haven.Coord.z);
                NUtils.addTask(new nurgling.tasks.ISRemoved(curd.item.wdgid()));
                taken++;
                diagnostics.curdsTaken++;
                info.curdCount--; // Update cached count
            }
            
            new CloseTargetContainer(info.container).run(gui);
        }
        
        return taken >= needed ? Results.SUCCESS() : Results.FAIL();
    }
    
    /**
     * Get the number of trays created in the last run
     * @return Number of trays created, or 0 if not yet run
     */
    public int getLastTraysCreated() {
        return lastTraysCreated;
    }

    /**
     * Pre-fetch empty trays for the entire batch, reserving 4 slots for curds
     */
    private void fetchEmptyTraysForBatch(NGameUI gui, NContext context, int traysNeeded, CreationDiagnostics diagnostics) throws InterruptedException {
        // Calculate how many empty trays we already have
        int emptyTraysInInventory = countEmptyTraysInInventory(gui);
        diagnostics.emptyTraysInInventory = emptyTraysInInventory;
        int traysToFetch = traysNeeded - emptyTraysInInventory;
        
        if (traysToFetch <= 0) {
            diagnostics.trayFetchLimit = 0;
            return;
        }
        
        // Calculate available space for cheese trays (1x2 each) and reserve space for curds
        // Use centralized cheese tray size constant
        int availableTraySlots = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
        diagnostics.availableTraySlots = availableTraySlots;
        int availableSlots = availableTraySlots - CheeseConstants.RESERVED_CURD_SLOTS; // Reserve slots for curds
        
        // Limit trays to fetch by available inventory space
        traysToFetch = Math.min(traysToFetch, availableSlots);
        diagnostics.trayFetchLimit = traysToFetch;
        
        if (traysToFetch <= 0) {
            gui.msg("Curd tray creation tray fetch for " + curdType + ": no inventory room for trays after reserving curd slots (" + diagnostics.toSummary() + ")");
            return;
        }
        
        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(cheeseTrayType);
        diagnostics.trayStorages = storages != null ? storages.size() : 0;
        if (storages == null || storages.isEmpty()) {
            gui.msg("Curd tray creation tray fetch for " + curdType + ": no Cheese Tray input storage configured (" + diagnostics.toSummary() + ")");
            return;
        }
        
        int traysObtained = 0;
        for (NContext.ObjectStorage storage : storages) {
            if (storage instanceof Container && traysObtained < traysToFetch) {
                diagnostics.trayContainersScanned++;
                Container container = (Container) storage;
                
                // Skip empty containers using visual flag
                Gob gob = Finder.findGob(container.gobid);
                if (gob != null && gob.ngob.isContainerEmpty()) {
                    diagnostics.trayContainersSkippedEmpty++;
                    continue;
                }
                
                int fetched = fetchMultipleTraysFromContainer(gui, container, traysToFetch - traysObtained, diagnostics);
                traysObtained += fetched;
                diagnostics.traysObtained += fetched;
            }
        }

        if (traysObtained < traysToFetch) {
            gui.msg("Curd tray creation tray fetch for " + curdType + ": fetched fewer trays than requested (" + diagnostics.toSummary() + ")");
        }
    }
    
    /**
     * Count empty trays currently in inventory
     */
    private int countEmptyTraysInInventory(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        int emptyCount = 0;
        for (WItem tray : trays) {
            if (CheeseUtils.isEmpty(tray)) {
                emptyCount++;
            }
        }
        return emptyCount;
    }
    
    /**
     * Count filled trays currently in inventory
     */
    private int countFilledTraysInInventory(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        int filledCount = 0;
        for (WItem tray : trays) {
            if (!CheeseUtils.isEmpty(tray)) {
                filledCount++;
            }
        }
        return filledCount;
    }
    
    /**
     * Fetch multiple empty trays from a specific container
     * @return number of empty trays actually obtained
     */
    private int fetchMultipleTraysFromContainer(NGameUI gui, Container container, int maxTrays, CreationDiagnostics diagnostics) throws InterruptedException {
            Gob containerGob = Finder.findGob(container.gobid);
            if (containerGob == null) {
                diagnostics.trayContainersMissingGob++;
                return 0;
            }
            
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(container).run(gui);
            
            // Get all cheese trays in this container
            ArrayList<WItem> trays = gui.getInventory(container.cap).getItems(cheeseTrayAlias);
            int emptyTraysFound = 0;
            int emptyTraysTransferred = 0;
            
            // First pass: count empty trays available
            for (WItem tray : trays) {
                if (CheeseUtils.isEmpty(tray)) {
                    emptyTraysFound++;
                }
            }
            diagnostics.emptyTraysFoundInStorage += emptyTraysFound;
            
            if (emptyTraysFound == 0) {
                new CloseTargetContainer(container).run(gui);
                return 0;
            }
            
            // Second pass: transfer empty trays up to maxTrays
            int traysToTake = Math.min(emptyTraysFound, maxTrays);
            
            for (WItem tray : trays) {
                if (emptyTraysTransferred >= traysToTake) break;
                
                if (CheeseUtils.isEmpty(tray)) {
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                    emptyTraysTransferred++;
                }
            }
            
            new CloseTargetContainer(container).run(gui);
            return emptyTraysTransferred;
    }

    private WItem getNextEmptyTray(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        for (WItem tray : trays) {
            // Use content inspection instead of resource name
            if (CheeseUtils.isEmpty(tray)) {
                return tray;
            }
        }
        return null;
    }
    
    /**
     * Cached container curd information to avoid repeated scanning
     */
    private static class ContainerCurdInfo {
        final Container container;
        int curdCount; // Mutable - updated when curds are taken
        
        ContainerCurdInfo(Container container, int curdCount) {
            this.container = container;
            this.curdCount = curdCount;
        }
    }

    private static class CreationDiagnostics {
        final String curdType;
        final int requestedTrays;
        int emptyTraysInInventory;
        int availableTraySlots;
        int trayFetchLimit;
        int trayStorages;
        int trayContainersScanned;
        int trayContainersSkippedEmpty;
        int trayContainersMissingGob;
        int emptyTraysFoundInStorage;
        int traysObtained;
        int curdStorages;
        int curdContainersScanned;
        int curdContainersSkippedEmpty;
        int curdContainersMissingGob;
        int curdsFound;
        int curdsTaken;
        int createdTrays;

        CreationDiagnostics(String curdType, int requestedTrays) {
            this.curdType = curdType;
            this.requestedTrays = requestedTrays;
        }

        String toSummary() {
            return "curdType=" + curdType + ", requestedTrays=" + requestedTrays +
                    ", emptyTraysInInventory=" + emptyTraysInInventory +
                    ", availableTraySlots=" + availableTraySlots +
                    ", trayFetchLimit=" + trayFetchLimit +
                    ", trayStorages=" + trayStorages +
                    ", trayContainersScanned=" + trayContainersScanned +
                    ", trayContainersSkippedEmpty=" + trayContainersSkippedEmpty +
                    ", trayContainersMissingGob=" + trayContainersMissingGob +
                    ", emptyTraysFoundInStorage=" + emptyTraysFoundInStorage +
                    ", traysObtained=" + traysObtained +
                    ", curdStorages=" + curdStorages +
                    ", curdContainersScanned=" + curdContainersScanned +
                    ", curdContainersSkippedEmpty=" + curdContainersSkippedEmpty +
                    ", curdContainersMissingGob=" + curdContainersMissingGob +
                    ", curdsFound=" + curdsFound +
                    ", curdsTaken=" + curdsTaken +
                    ", createdTrays=" + createdTrays;
        }
    }
}
