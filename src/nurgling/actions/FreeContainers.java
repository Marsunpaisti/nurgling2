package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainers implements Action
{
    ArrayList<Container> containers;
    NAlias pattern = null;

    public FreeContainers(ArrayList<Container> containers) {
        this.containers = containers;
    }

    public FreeContainers(ArrayList<Container> containers, NAlias pattern) {
        this.containers = containers;
        this.pattern = pattern;
    }

    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NContext context = new NContext(gui);
        NUtils.debugMsg(gui, "[FreeContainers] start containers=" + containers.size() + " pattern=" + (pattern != null ? pattern.toString() : "any"));

        for (Container container : containers)
        {
            Container.Space space;
            if ((space = container.getattr(Container.Space.class)).isReady())
            {
                NUtils.debugMsg(gui, "[FreeContainers] container " + container.cap + " hash=" + container.gobHash +
                        " free=" + space.getRes().get(Container.Space.FREESPACE) +
                        "/" + space.getRes().get(Container.Space.MAXSPACE));
                if (space.getRes().get(Container.Space.FREESPACE) == space.getRes().get(Container.Space.MAXSPACE)) {
                    NUtils.debugMsg(gui, "[FreeContainers] skip empty container hash=" + container.gobHash);
                    continue;
                }
            }

            navigateToTargetContainer(gui, container);

            NUtils.debugMsg(gui, "[FreeContainers] opening " + container.cap + " hash=" + container.gobHash);
            new OpenTargetContainer(container).run(gui);
            ArrayList<WItem> sourceItems = (pattern == null) ? gui.getInventory(container.cap).getItems() : gui.getInventory(container.cap).getItems(pattern);
            NUtils.debugMsg(gui, "[FreeContainers] opened " + container.cap + " items=" + sourceItems.size());
            for (WItem item : sourceItems)
            {
                String itemName = ((NGItem) item.item).name();
                double quality = ((NGItem) item.item).quality != null ? ((NGItem) item.item).quality : 1;
                boolean hasOutput = context.addOutItem(itemName, null, quality);
                NUtils.debugMsg(gui, "[FreeContainers] output lookup item=" + itemName + " q=" + quality + " found=" + hasOutput);
                if (hasOutput)
                    targets.add(itemName);
            }
            Results takeResult = new TakeItemsFromContainer(container, targets, pattern).run(gui);
            while (!takeResult.isSuccess)
            {
                NUtils.debugMsg(gui, "[FreeContainers] take failed/full inventory; transfer targets=" + targets);
                new TransferItems2(context, targets).run(gui);
                navigateToTargetContainer(gui, container);
                NUtils.debugMsg(gui, "[FreeContainers] reopening after transfer " + container.cap + " hash=" + container.gobHash);
                new OpenTargetContainer(container).run(gui);
                takeResult = new TakeItemsFromContainer(container, targets, pattern).run(gui);
            }
            NUtils.debugMsg(gui, "[FreeContainers] take complete; closing " + container.cap + " hash=" + container.gobHash);
            new CloseTargetContainer(container).run(gui);
        }
        NUtils.debugMsg(gui, "[FreeContainers] final transfer targets=" + targets);
        new TransferItems2(context, targets).run(gui);
        NUtils.debugMsg(gui, "[FreeContainers] complete");
        return Results.SUCCESS();
    }

    private void navigateToTargetContainer(NGameUI gui, Container container) throws InterruptedException {
        PathFinder pf;

        Gob gob = Finder.findGob(container.gobHash);
        if(gob!= null && PathFinder.isAvailable(gob)) {
            NUtils.debugMsg(gui, "[FreeContainers] path to container gob=" + gob.id + " rc=" + gob.rc + " hash=" + container.gobHash);
            pf = new PathFinder(gob);
            pf.isHardMode = true;
            pf.run(gui);
        }
        else
        {
            if(container.parent!=null)
            {
                NUtils.debugMsg(gui, "[FreeContainers] container unavailable locally; navigate parent area=" + container.parent.name + "#" + container.parent.id + " hash=" + container.gobHash);
                NUtils.navigateToArea(container.parent);
                gob = Finder.findGob(container.gobHash);
                if(gob!= null && PathFinder.isAvailable(gob)) {
                    NUtils.debugMsg(gui, "[FreeContainers] path to container after area nav gob=" + gob.id + " rc=" + gob.rc + " hash=" + container.gobHash);
                    pf = new PathFinder(gob);
                    pf.isHardMode = true;
                    pf.run(gui);
                }
                else {
                    NUtils.debugMsg(gui, "[FreeContainers] container still unavailable/unreachable after area nav hash=" + container.gobHash + " gob=" + (gob != null ? gob.id : "null"));
                }
            }
            else {
                NUtils.debugMsg(gui, "[FreeContainers] no parent area and container unavailable hash=" + container.gobHash + " gob=" + (gob != null ? gob.id : "null"));
            }
        }
    }
}
