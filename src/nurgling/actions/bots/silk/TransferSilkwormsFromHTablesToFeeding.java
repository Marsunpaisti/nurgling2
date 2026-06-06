package nurgling.actions.bots.silk;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;

import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongFunction;

import static nurgling.areas.NContext.contcaps;

/**
 * Transfers silkworms from herbalist tables to feeding containers
 * Records available space in herbalist tables for future egg placement
 */
public class TransferSilkwormsFromHTablesToFeeding implements Action {
    private final int totalSilkwormsNeeded;
    private int totalEggsNeeded = 0;
    
    public TransferSilkwormsFromHTablesToFeeding(int totalSilkwormsNeeded) {
        this.totalSilkwormsNeeded = totalSilkwormsNeeded;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String worms = "Silkworm";
        NAlias wormsAlias = new NAlias(new ArrayList<>(List.of(worms)), new ArrayList<>(List.of("egg")));
        
        totalEggsNeeded = 0;
        ArrayList<Container> htableContainers = new ArrayList<>();
        ArrayList<Container> feedingContainers = new ArrayList<>();
        
        // Pre-populate feeding containers for efficiency
        NArea feedingArea = context.goToArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea != null) {
            feedingContainers = createContainersFromArea(feedingArea);
        }
        
        if (totalSilkwormsNeeded > 0) {
            int wormsTransferredTotal = 0;
            
            // Take silkworms from herbalist tables - use container-by-container approach
            NArea htablesArea = context.goToArea(Specialisation.SpecName.htable, "Silkworm Egg");
            if (htablesArea != null) {
                htableContainers = createContainersFromArea(htablesArea);
                
                // Process each herbalist table container individually
                for (Container htableContainer : htableContainers) {
                    if (wormsTransferredTotal >= totalSilkwormsNeeded) {
                        // Still need to check remaining containers for egg capacity only
                        if (!navigateToContainer(gui, htableContainer)) {
                            continue;
                        }
                        new OpenTargetContainer(htableContainer).run(gui);
                        
                        // Record free space for eggs
                        int freeSpace = gui.getInventory(htableContainer.cap).getFreeSpace();
                        totalEggsNeeded += freeSpace;
                        
                        new CloseTargetContainer(htableContainer).run(gui);
                        continue;
                    }
                    
                    if (!navigateToContainer(gui, htableContainer)) {
                        continue;
                    }
                    new OpenTargetContainer(htableContainer).run(gui);
                    
                    // Get all silkworm WItems from this container, excluding anything with "egg" in the name
                    ArrayList<WItem> silkwormItems = gui.getInventory(htableContainer.cap).getItems(wormsAlias);
                    
                    // Transfer silkworms from this container in batches based on inventory space
                    int wormsFromThisContainer = 0;
                    while (!silkwormItems.isEmpty() &&
                           wormsTransferredTotal + wormsFromThisContainer < totalSilkwormsNeeded) {
                        
                        // Take what fits in inventory
                        int inventorySpace = gui.getInventory().getFreeSpace();
                        int wormsToTake = Math.min(silkwormItems.size(), inventorySpace);
                        
                        if (wormsToTake == 0) {
                            // Inventory full - drop off and continue
                            if (!dropOffWormsToFeedingContainers(gui, feedingContainers, wormsAlias, context)) {
                                return Results.ERROR("Unable to drop off silkworms to feeding containers");
                            }
                            context.goToArea(Specialisation.SpecName.htable, "Silkworm Egg");

                            if (!navigateToContainer(gui, htableContainer)) {
                                return Results.ERROR("Unable to find herbalist table after returning from silkworm feeding area");
                            }
                            new OpenTargetContainer(htableContainer).run(gui);

                            // Refresh silkworm items list (may have changed)
                            silkwormItems = gui.getInventory(htableContainer.cap).getItems(wormsAlias);

                            continue;
                        }
                        
                        ArrayList<WItem> wormsToTakeBatch = new ArrayList<>();
                        for (int i = 0; i < wormsToTake; i++) {
                            wormsToTakeBatch.add(silkwormItems.get(i));
                        }
                        
                        new TakeWItemsFromContainer(htableContainer, wormsToTakeBatch).run(gui);
                        wormsFromThisContainer += wormsToTakeBatch.size();
                        
                        // Remove taken items from our tracking list
                        for (int i = 0; i < wormsToTake; i++) {
                            silkwormItems.remove(0);
                        }
                    }
                    
                    wormsTransferredTotal += wormsFromThisContainer;
                    
                    // Record free space for eggs (done once per container)
                    new OpenTargetContainer(htableContainer).run(gui);
                    int freeSpace = gui.getInventory(htableContainer.cap).getFreeSpace();
                    totalEggsNeeded += freeSpace;
                    
                    new CloseTargetContainer(htableContainer).run(gui);
                }
                
                // Drop off any remaining silkworms in inventory after processing all containers
                if (!gui.getInventory().getItems(wormsAlias).isEmpty()) {
                    if (!dropOffWormsToFeedingContainers(gui, feedingContainers, wormsAlias, context)) {
                        return Results.ERROR("Unable to drop off silkworms to feeding containers");
                    }
                }
            }
        }
        
        return Results.SUCCESS();
    }
    
    public int getTotalEggsNeeded() {
        return totalEggsNeeded;
    }
    
    private ArrayList<Container> createContainersFromArea(NArea area) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())));
        for (Gob gob : gobs) {
            Container cand = new Container(gob, contcaps.get(gob.ngob.name), area);
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }
        return containers;
    }
    
    private boolean dropOffWormsToFeedingContainers(NGameUI gui, ArrayList<Container> feedingContainers, NAlias wormsAlias, NContext context) throws InterruptedException {
        NArea feedingArea = context.goToArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea != null) {
            feedingContainers.clear();
            feedingContainers.addAll(createContainersFromArea(feedingArea));
        }
        
        for (Container feedingContainer : feedingContainers) {
            if (gui.getInventory().getItems(wormsAlias).isEmpty()) {
                break; // No more silkworms in inventory
            }
            
            if (!navigateToContainer(gui, feedingContainer)) {
                continue;
            }
            new OpenTargetContainer(feedingContainer).run(gui);
            
            int currentWorms = gui.getInventory(feedingContainer.cap).getItems(wormsAlias).size();
            int spaceAvailable = Math.max(0, 56 - currentWorms);
            
            if (spaceAvailable > 0) {
                new TransferToContainer(feedingContainer, wormsAlias).run(gui);
            }
            
            new CloseTargetContainer(feedingContainer).run(gui);
        }

        return gui.getInventory().getItems(wormsAlias).isEmpty();
    }

    private boolean navigateToContainer(NGameUI gui, Container container) throws InterruptedException {
        Gob target = findContainerGob(container);
        if (target == null) {
            return false;
        }
        return new PathFinder(target).run(gui).IsSuccess();
    }

    private Gob findContainerGob(Container container) {
        return resolveContainerGob(container.gobHash, container.gobid, Finder::findGob, Finder::findGob);
    }

    static Gob resolveContainerGob(String gobHash, long gobid, Function<String, Gob> byHash, LongFunction<Gob> byId) {
        if (gobHash != null && !gobHash.isEmpty()) {
            Gob byStableHash = byHash.apply(gobHash);
            if (byStableHash != null) {
                return byStableHash;
            }
        }
        return byId.apply(gobid);
    }
}
