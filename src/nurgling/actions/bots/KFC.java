package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

/**
 * KFC - Chicken and Duck Manager Bot
 * Manages chicken coops and incubators: replaces low-quality males/females with better ones,
 * transfers babies to incubators, and processes low-quality birds.
 */
public class KFC implements Action {

    enum BirdSpecies {
        CHICKEN,
        DUCK
    }

    enum BirdType {
        MALE,
        FEMALE,
        BABY,
        EGG,
        DEAD_MALE,
        DEAD_FEMALE,
        PLUCKED_MALE,
        PLUCKED_FEMALE,
        CLEANED
    }

    private static final EnumMap<BirdSpecies, EnumMap<BirdType, List<String>>> BIRD_RESOURCES = new EnumMap<>(BirdSpecies.class);

    static {
        addBird(BirdSpecies.CHICKEN, BirdType.MALE, "gfx/invobjs/rooster");
        addBird(BirdSpecies.CHICKEN, BirdType.FEMALE, "gfx/invobjs/hen");
        addBird(BirdSpecies.CHICKEN, BirdType.BABY, "gfx/invobjs/chick");
        addBird(BirdSpecies.CHICKEN, BirdType.EGG, "gfx/invobjs/egg-chicken");
        addBird(BirdSpecies.CHICKEN, BirdType.DEAD_MALE, "gfx/invobjs/rooster-dead");
        addBird(BirdSpecies.CHICKEN, BirdType.DEAD_FEMALE, "gfx/invobjs/hen-dead");
        addBird(BirdSpecies.CHICKEN, BirdType.PLUCKED_MALE, "gfx/invobjs/chicken-plucked");
        addBird(BirdSpecies.CHICKEN, BirdType.PLUCKED_FEMALE, "gfx/invobjs/chicken-plucked");
        addBird(BirdSpecies.CHICKEN, BirdType.CLEANED, "gfx/invobjs/chicken-cleaned");

        addBird(BirdSpecies.DUCK, BirdType.MALE, "gfx/invobjs/duckdrake");
        addBird(BirdSpecies.DUCK, BirdType.FEMALE, "gfx/invobjs/duckhen");
        addBird(BirdSpecies.DUCK, BirdType.BABY, "gfx/invobjs/duckling");
        addBird(BirdSpecies.DUCK, BirdType.EGG, "gfx/invobjs/egg-duck");
        addBird(BirdSpecies.DUCK, BirdType.DEAD_MALE, "gfx/invobjs/duckdrake-dead");
        addBird(BirdSpecies.DUCK, BirdType.DEAD_FEMALE, "gfx/invobjs/duckhen-dead");
        addBird(BirdSpecies.DUCK, BirdType.PLUCKED_MALE, "gfx/invobjs/duckdrake-plucked");
        addBird(BirdSpecies.DUCK, BirdType.PLUCKED_FEMALE, "gfx/invobjs/duckhen-plucked");
        addBird(BirdSpecies.DUCK, BirdType.CLEANED, "gfx/invobjs/duck-cleaned");
    }

    private static void addBird(BirdSpecies species, BirdType type, String... resources) {
        BIRD_RESOURCES.computeIfAbsent(species, k -> new EnumMap<>(BirdType.class)).put(type, Arrays.asList(resources));
    }

    static boolean isBirdResource(String resourceName, BirdSpecies species, BirdType type) {
        if (resourceName == null) {
            return false;
        }
        List<String> resources = BIRD_RESOURCES.get(species).get(type);
        return resources != null && resources.contains(resourceName);
    }

    // Coop info class
    private static class CoopInfo {
        String gobHash;
        BirdSpecies species;
        double maleQuality;
        ArrayList<Float> femaleQualities = new ArrayList<>();

        public CoopInfo(String gobHash, BirdSpecies species, double maleQuality) {
            this.gobHash = gobHash;
            this.species = species;
            this.maleQuality = maleQuality;
        }
    }

    // Incubator info class
    private static class IncubatorInfo {
        String gobHash;
        double birdQuality;

        public IncubatorInfo(String gobHash, double birdQuality) {
            this.gobHash = gobHash;
            this.birdQuality = birdQuality;
        }
    }

    // Maximum babies per incubator
    private static final int MAX_BABIES_PER_INCUBATOR = 24;
    private static final String COOP_WINDOW = "Chicken Coop";
    
    // Comparator for sorting incubators by quality
    Comparator<IncubatorInfo> incubatorComparator = (o1, o2) -> Double.compare(o1.birdQuality, o2.birdQuality);

    // Comparator for sorting coops
    Comparator<CoopInfo> coopComparator = (o1, o2) -> {
        int res = Double.compare(o1.maleQuality, o2.maleQuality);
        if (res == 0) {
            if (!o1.femaleQualities.isEmpty() && !o2.femaleQualities.isEmpty()) {
                double avgQuality1 = o1.femaleQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                double avgQuality2 = o2.femaleQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                res = Double.compare(avgQuality1, avgQuality2);
            }
        }
        return res;
    };

    NContext context;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        context = new NContext(gui);
        
        // Validate required areas
        NArea.Specialisation chickenSpec = new NArea.Specialisation(Specialisation.SpecName.chicken.toString());
        NArea.Specialisation incubatorSpec = new NArea.Specialisation(Specialisation.SpecName.incubator.toString());
        NArea.Specialisation swillSpec = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        NArea.Specialisation waterSpec = new NArea.Specialisation(Specialisation.SpecName.water.toString());
        
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(chickenSpec);
        req.add(incubatorSpec);
        
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swillSpec);
        opt.add(waterSpec);
        
        if (!new Validator(req, opt).run(gui).IsSuccess()) {
            return Results.FAIL();
        }
        
        // Resolve areas (local first, then global) without navigating — bot navigates explicitly below
        NArea chickenArea = context.findArea(Specialisation.SpecName.chicken);
        NArea incubatorArea = context.findArea(Specialisation.SpecName.incubator);
        NArea swillArea = context.findArea(Specialisation.SpecName.swill);
        NArea waterArea = context.findArea(Specialisation.SpecName.water);
        
        if (chickenArea == null) {
            return Results.ERROR("Chicken area not found!");
        }
        if (incubatorArea == null) {
            return Results.ERROR("Incubator area not found!");
        }
        
        // Navigate to chicken area and collect coop hashes
        NUtils.navigateToArea(chickenArea);
        ArrayList<String> coopHashes = new ArrayList<>();
        for (Gob cc : Finder.findGobs(chickenArea, new NAlias("gfx/terobjs/chickencoop"))) {
            if (cc.ngob != null && cc.ngob.hash != null) {
                coopHashes.add(cc.ngob.hash);
            }
        }

        // Navigate to incubator area and collect incubator hashes
        NUtils.navigateToArea(incubatorArea);
        ArrayList<String> incubatorHashes = new ArrayList<>();
        for (Gob cc : Finder.findGobs(incubatorArea, new NAlias("gfx/terobjs/chickencoop"))) {
            if (cc.ngob != null && cc.ngob.hash != null) {
                incubatorHashes.add(cc.ngob.hash);
            }
        }

        // Fill chicken coops and incubators with fluids
        if (swillArea != null || waterArea != null) {
            ArrayList<Container> containers = getContainersFromHashes(coopHashes, chickenArea);
            ArrayList<Container> ccontainers = getContainersFromHashes(incubatorHashes, incubatorArea);
            
            if (swillArea != null) {
                new FillFluid(containers, swillArea.getRCArea(), new NAlias("swill"), 2).run(gui);
                new FillFluid(ccontainers, swillArea.getRCArea(), new NAlias("swill"), 2).run(gui);
            }
            if (waterArea != null) {
                new FillFluid(containers, waterArea.getRCArea(), new NAlias("water"), 1).run(gui);
                new FillFluid(ccontainers, waterArea.getRCArea(), new NAlias("water"), 1).run(gui);
            }
        }

        // Read coop contents and sort them
        EnumMap<BirdSpecies, ArrayList<CoopInfo>> coopInfos = new EnumMap<>(BirdSpecies.class);
        EnumMap<BirdSpecies, ArrayList<IncubatorInfo>> qmales = new EnumMap<>(BirdSpecies.class);
        EnumMap<BirdSpecies, ArrayList<IncubatorInfo>> qfemales = new EnumMap<>(BirdSpecies.class);
        for (BirdSpecies species : BirdSpecies.values()) {
            coopInfos.put(species, new ArrayList<>());
            qmales.put(species, new ArrayList<>());
            qfemales.put(species, new ArrayList<>());
        }
        
        // Navigate to chicken area and read coop contents
        NUtils.navigateToArea(chickenArea);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            for (BirdSpecies species : BirdSpecies.values()) {
                WItem male = getBirdItem(gui.getInventory(COOP_WINDOW), species, BirdType.MALE);
                double maleQuality = male != null ? itemQuality(male) : -1;
                CoopInfo coopInfo = new CoopInfo(hash, species, maleQuality);

                ArrayList<WItem> females = getBirdItems(gui.getInventory(COOP_WINDOW), species, BirdType.FEMALE);
                for (WItem female : females) {
                    coopInfo.femaleQualities.add(itemQuality(female));
                }
                coopInfo.femaleQualities.sort(Float::compareTo);

                if (maleQuality != -1 || !coopInfo.femaleQualities.isEmpty()) {
                    coopInfos.get(species).add(coopInfo);
                }
            }

            new CloseTargetContainer(COOP_WINDOW).run(gui);
        }

        for (BirdSpecies species : BirdSpecies.values()) {
            coopInfos.get(species).sort(coopComparator.reversed());
        }

        // Navigate to incubator area and read contents
        NUtils.navigateToArea(incubatorArea);
        for (String hash : incubatorHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            for (BirdSpecies species : BirdSpecies.values()) {
                ArrayList<WItem> males = getBirdItems(gui.getInventory(COOP_WINDOW), species, BirdType.MALE);
                for (WItem male : males) {
                    qmales.get(species).add(new IncubatorInfo(hash, itemQuality(male)));
                }

                ArrayList<WItem> females = getBirdItems(gui.getInventory(COOP_WINDOW), species, BirdType.FEMALE);
                for (WItem female : females) {
                    qfemales.get(species).add(new IncubatorInfo(hash, itemQuality(female)));
                }
            }

            new CloseTargetContainer(COOP_WINDOW).run(gui);
        }

        for (BirdSpecies species : BirdSpecies.values()) {
            Results maleResult = processMales(gui, species, coopInfos.get(species), qmales.get(species));
            if (!maleResult.IsSuccess()) {
                return maleResult;
            }

            Results femaleResult = processFemales(gui, species, coopInfos.get(species), qfemales.get(species));
            if (!femaleResult.IsSuccess()) {
                return femaleResult;
            }
        }

        transferBabies(gui, coopHashes, incubatorHashes);

        if (coopHashes.isEmpty()) {
            return Results.ERROR("No chicken coops found!");
        }

        EnumMap<BirdSpecies, Double> eggThresholds = new EnumMap<>(BirdSpecies.class);
        for (BirdSpecies species : BirdSpecies.values()) {
            ArrayList<CoopInfo> speciesCoops = coopInfos.get(species);
            if (speciesCoops.isEmpty()) {
                continue;
            }
            CoopInfo bestCoop = speciesCoops.get(0);
            if (bestCoop.femaleQualities.isEmpty()) {
                if (species == BirdSpecies.CHICKEN) {
                    return Results.ERROR("No hens in best coop");
                }
                continue;
            }
            eggThresholds.put(species, (double) bestCoop.femaleQualities.get(0));
        }

        collectAndDisposeLowQualityEggs(gui, coopHashes, eggThresholds);

        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }
    
    private ArrayList<Container> getContainersFromHashes(ArrayList<String> hashes, NArea area) {
        ArrayList<Container> containers = new ArrayList<>();
        for (String hash : hashes) {
            Gob gob = Finder.findGob(hash);
            if (gob != null) {
                Container cand = new Container(gob, COOP_WINDOW, area);
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }
        }
        return containers;
    }
    
    private void transferBabies(NGameUI gui, ArrayList<String> coopHashes, ArrayList<String> incubatorHashes) throws InterruptedException {
        // Collect chicks and ducklings from chicken coops.
        context.goToArea(Specialisation.SpecName.chicken);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, gob).run(gui).IsSuccess())) {
                continue;
            }
            
            ArrayList<WItem> babies = getBabyItems(gui.getInventory(COOP_WINDOW));
            for (WItem baby : babies) {
                baby.item.wdgmsg("transfer", Coord.z);
            }
            
            new CloseTargetContainer(COOP_WINDOW).run(gui);
            
            // If inventory getting full, transfer to incubators (don't kill yet)
            if (shouldDropOffItems(gui)) {
                transferBabiesToIncubators(gui, incubatorHashes);
                context.goToArea(Specialisation.SpecName.chicken);
            }
        }
        
        // Transfer all remaining babies to incubators (fills all available space)
        transferBabiesToIncubators(gui, incubatorHashes);
        
        // Only after ALL incubators are full, kill excess babies
        killExcessBabies(gui);
    }
    
    private void transferBabiesToIncubators(NGameUI gui, ArrayList<String> incubatorHashes) throws InterruptedException {
        NAlias babyAlias = babyAlias();
        ArrayList<WItem> babies = getBabyItems(gui.getInventory());
        if (babies.isEmpty()) return;
        
        context.goToArea(Specialisation.SpecName.incubator);
        for (String hash : incubatorHashes) {
            babies = getBabyItems(gui.getInventory());
            if (babies.isEmpty()) break;
            
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            // ItemCount is alias-based, so use precise baby display names and exclude eggs.
            Container incubatorContainer = new Container(gob, COOP_WINDOW, null);
            Container.ItemCount itemCount = incubatorContainer.initItemCount(babyAlias, MAX_BABIES_PER_INCUBATOR);
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(incubatorContainer).run(gui).IsSuccess())) {
                continue;
            }
            
            // Update ItemCount to get current baby count
            itemCount.update();
            int canAdd = itemCount.getNeeded();
            
            if (canAdd <= 0) {
                new CloseTargetContainer(incubatorContainer).run(gui);
                continue;
            }
            
            // Transfer babies to incubator (up to limit)
            int transferred = 0;
            for (WItem baby : babies) {
                if (transferred >= canAdd) break;
                if (gui.getInventory(COOP_WINDOW).getNumberFreeCoord(new Coord(2, 2)) > 0) {
                    baby.item.wdgmsg("transfer", Coord.z);
                    transferred++;
                } else {
                    break;
                }
            }
            
            new CloseTargetContainer(incubatorContainer).run(gui);
        }
    }
    
    /**
     * Kill excess chicks and ducklings that couldn't fit in incubators.
     * Wring neck -> wait for "A Bloody Mess" -> drop on ground
     */
    private void killExcessBabies(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> babies = getBabyItems(gui.getInventory());
        
        while (!babies.isEmpty()) {
            WItem baby = babies.get(0);
            
            // Wring neck
            new SelectFlowerAction("Wring neck", baby).run(gui);
            
            // Wait for "A Bloody Mess" to appear
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("A Bloody Mess"), 1));
            
            // Drop the bloody mess on ground
            WItem bloodyMess = gui.getInventory().getItem(new NAlias("A Bloody Mess"));
            if (bloodyMess != null) {
                NUtils.drop(bloodyMess);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return gui.getInventory().getItems(new NAlias("A Bloody Mess")).isEmpty();
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });
            }
            
            // Get remaining babies
            babies = getBabyItems(gui.getInventory());
        }
    }
    
    /**
     * Collect eggs with quality BELOW threshold and dispose them via FreeInventory2 (like Butcher).
     * Good quality eggs stay in coops for hatching.
     */
    private void collectAndDisposeLowQualityEggs(NGameUI gui, ArrayList<String> coopHashes, EnumMap<BirdSpecies, Double> qualityThresholds) throws InterruptedException {
        if (qualityThresholds.isEmpty()) return;

        context.goToArea(Specialisation.SpecName.chicken);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, gob).run(gui).IsSuccess())) {
                continue;
            }
            
            for (BirdSpecies species : qualityThresholds.keySet()) {
                double qualityThreshold = qualityThresholds.get(species);
                ArrayList<WItem> eggs = getBirdItems(gui.getInventory(COOP_WINDOW), species, BirdType.EGG);
                for (WItem egg : eggs) {
                    if (itemQuality(egg) < qualityThreshold) {
                        egg.item.wdgmsg("transfer", Coord.z);
                    }
                }
            }
            
            new CloseTargetContainer(COOP_WINDOW).run(gui);
            
            // If inventory getting full, dispose via FreeInventory2 and return to chicken area
            if (shouldDropOffItems(gui)) {
                new FreeInventory2(context).run(gui);
                context.goToArea(Specialisation.SpecName.chicken);
            }
        }
    }


    private Results processMales(NGameUI gui, BirdSpecies species, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qmales) throws InterruptedException {
        // Sort males by quality (best to worst)
        qmales.sort(incubatorComparator.reversed());

        for (IncubatorInfo maleInfo : qmales) {
            // Navigate to incubator area and open the coop with male
            context.goToArea(Specialisation.SpecName.incubator);
            
            Gob maleGob = Finder.findGob(maleInfo.gobHash);
            if (maleGob == null) continue;
            
            new PathFinder(maleGob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, maleGob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get male from coop inventory
            WItem male = getBirdItem(gui.getInventory(COOP_WINDOW), species, BirdType.MALE, maleInfo.birdQuality);
            if (male == null) {
                new CloseTargetContainer(COOP_WINDOW).run(gui);
                continue;
            }
            double maleQuality = itemQuality(male);

            Coord pos = male.c.div(Inventory.sqsz);
            male.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory(COOP_WINDOW).isSlotFree(finalPos1);
                }
            });
            new CloseTargetContainer(COOP_WINDOW).run(gui);

            // Find coop with worse male of same species and replace it
            for (CoopInfo coopInfo : coopInfos) {
                if (coopInfo.maleQuality < maleQuality && coopInfo.maleQuality != -1) {
                    male = getBirdItem(gui.getInventory(), species, BirdType.MALE);
                    if (male == null) break;

                    // Navigate to chicken area and open coop for replacement
                    context.goToArea(Specialisation.SpecName.chicken);
                    
                    Gob coopGob = Finder.findGob(coopInfo.gobHash);
                    if (coopGob == null) continue;
                    
                    new PathFinder(coopGob).run(gui);
                    if (!(new OpenTargetContainer(COOP_WINDOW, coopGob).run(gui).IsSuccess())) {
                        return Results.FAIL();
                    }

                    // Get current male in coop
                    WItem oldMale = getBirdItem(gui.getInventory(COOP_WINDOW), species, BirdType.MALE, coopInfo.maleQuality);
                    if (oldMale == null) {
                        new CloseTargetContainer(COOP_WINDOW).run(gui);
                        continue;
                    }

                    // Replace male
                    pos = oldMale.c.div(Inventory.sqsz);
                    oldMale.item.wdgmsg("transfer", Coord.z);
                    Coord finalPos = pos;
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return gui.getInventory(COOP_WINDOW).isSlotFree(finalPos);
                        }
                    });

                    NUtils.takeItemToHand(male);
                    gui.getInventory(COOP_WINDOW).dropOn(pos);

                    // Update quality
                    coopInfo.maleQuality = maleQuality;
                    maleQuality = itemQuality(oldMale);
                    new CloseTargetContainer(COOP_WINDOW).run(gui);
                }
            }

            // Process the male (butcher it)
            male = getBirdItem(gui.getInventory(), species, BirdType.MALE);
            if (male != null) {
                butcherBird(gui, male, species, BirdType.MALE);
            }
        }
        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }

    private Results processFemales(NGameUI gui, BirdSpecies species, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qfemales) throws InterruptedException {
        // Sort females by quality (best to worst)
        qfemales.sort(incubatorComparator.reversed());

        for (IncubatorInfo femaleInfo : qfemales) {
            // Navigate to incubator area and open coop with female
            context.goToArea(Specialisation.SpecName.incubator);
            
            Gob femaleGob = Finder.findGob(femaleInfo.gobHash);
            if (femaleGob == null) continue;
            
            new PathFinder(femaleGob).run(gui);
            if (!(new OpenTargetContainer(COOP_WINDOW, femaleGob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get female from coop inventory
            WItem female = getBirdItem(gui.getInventory(COOP_WINDOW), species, BirdType.FEMALE, femaleInfo.birdQuality);
            if (female == null) {
                new CloseTargetContainer(COOP_WINDOW).run(gui);
                continue;
            }
            float femaleQuality = itemQuality(female);

            Coord pos = female.c.div(Inventory.sqsz);
            female.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory(COOP_WINDOW).isSlotFree(finalPos1);
                }
            });
            new CloseTargetContainer(COOP_WINDOW).run(gui);

            // Find coop with worse female of same species and replace it
            for (CoopInfo coopInfo : coopInfos) {
                for (int i = 0; i < coopInfo.femaleQualities.size(); i++) {
                    if (coopInfo.femaleQualities.get(i) < femaleQuality) {
                        female = getBirdItem(gui.getInventory(), species, BirdType.FEMALE);
                        if (female == null) break;

                        // Navigate to chicken area and open coop for replacement
                        context.goToArea(Specialisation.SpecName.chicken);
                        
                        Gob coopGob = Finder.findGob(coopInfo.gobHash);
                        if (coopGob == null) continue;
                        
                        new PathFinder(coopGob).run(gui);
                        if (!(new OpenTargetContainer(COOP_WINDOW, coopGob).run(gui).IsSuccess())) {
                            return Results.FAIL();
                        }

                        // Get current female in coop
                        WItem oldFemale = getBirdItem(gui.getInventory(COOP_WINDOW), species, BirdType.FEMALE, coopInfo.femaleQualities.get(i));
                        if (oldFemale == null) {
                            new CloseTargetContainer(COOP_WINDOW).run(gui);
                            continue;
                        }

                        // Replace female
                        pos = oldFemale.c.div(Inventory.sqsz);
                        oldFemale.item.wdgmsg("transfer", Coord.z);
                        Coord finalPos = pos;
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return gui.getInventory(COOP_WINDOW).isSlotFree(finalPos);
                            }
                        });

                        NUtils.takeItemToHand(female);
                        gui.getInventory(COOP_WINDOW).dropOn(pos);

                        // Update quality
                        coopInfo.femaleQualities.set(i, femaleQuality);
                        femaleQuality = itemQuality(oldFemale);
                        new CloseTargetContainer(COOP_WINDOW).run(gui);
                        break;
                    }
                }
            }

            // Process the female (butcher it)
            female = getBirdItem(gui.getInventory(), species, BirdType.FEMALE);
            if (female != null) {
                butcherBird(gui, female, species, BirdType.FEMALE);
            }
        }
        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }
    
    /**
     * Butcher a bird - wring neck, pluck, clean, butcher.
     * Similar to Butcher bot approach with FreeInventory2 and return to area.
     */
    private void butcherBird(NGameUI gui, WItem bird, BirdSpecies species, BirdType liveType) throws InterruptedException {
        // Check inventory space before butchering
        if (gui.getInventory().getNumberFreeCoord(new Coord(1, 1)) < 2) {
            new FreeInventory2(context).run(gui);
        }

        BirdType deadType = liveType == BirdType.MALE ? BirdType.DEAD_MALE : BirdType.DEAD_FEMALE;
        BirdType pluckedType = liveType == BirdType.MALE ? BirdType.PLUCKED_MALE : BirdType.PLUCKED_FEMALE;
        
        int deadCount = countBirdItems(gui.getInventory(), species, deadType);
        new SelectFlowerAction("Wring neck", bird).run(gui);
        waitForBirdItem(gui.getInventory(), species, deadType, deadCount + 1);

        WItem deadBird = getBirdItem(gui.getInventory(), species, deadType);
        if (deadBird == null) return;

        Boolean skipPluckCocks = (Boolean) NConfig.get(NConfig.Key.skipPluckingCocksInKFC);
        boolean isChickenCock = species == BirdSpecies.CHICKEN && liveType == BirdType.MALE;
        if (skipPluckCocks != null && skipPluckCocks && isChickenCock) {
            // Leave as Dead Cock for creamy cock recipe
        } else {
            int pluckedCount = countBirdItems(gui.getInventory(), species, pluckedType);
            new SelectFlowerAction("Pluck", deadBird).run(gui);
            waitForBirdItem(gui.getInventory(), species, pluckedType, pluckedCount + 1);

            WItem plucked = getBirdItem(gui.getInventory(), species, pluckedType);
            if (plucked == null) return;

            int cleanedCount = countBirdItems(gui.getInventory(), species, BirdType.CLEANED);
            new SelectFlowerAction("Clean", plucked).run(gui);
            waitForBirdItem(gui.getInventory(), species, BirdType.CLEANED, cleanedCount + 1);

            WItem cleaned = getBirdItem(gui.getInventory(), species, BirdType.CLEANED);
            if (cleaned == null) return;

            Boolean skipButcher = (Boolean) NConfig.get(NConfig.Key.skipButcherInKFC);
            if (skipButcher == null || !skipButcher) {
                int beforeButcher = countBirdItems(gui.getInventory(), species, BirdType.CLEANED);
                new SelectFlowerAction("Butcher", cleaned).run(gui);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return countBirdItems(gui.getInventory(), species, BirdType.CLEANED) < beforeButcher;
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });
            }
        }

        // Drop off if insufficient space for another bird
        if (shouldDropOffItems(gui)) {
            new FreeInventory2(context).run(gui);
        }
    }

    private NAlias babyAlias() {
        return new NAlias(new ArrayList<>(List.of("Chick", "Duckling")), new ArrayList<>(List.of("Egg")));
    }

    private ArrayList<WItem> getBabyItems(NInventory inventory) throws InterruptedException {
        ArrayList<WItem> babies = new ArrayList<>();
        for (BirdSpecies species : BirdSpecies.values()) {
            babies.addAll(getBirdItems(inventory, species, BirdType.BABY));
        }
        return babies;
    }

    private ArrayList<WItem> getBirdItems(NInventory inventory, BirdSpecies species, BirdType type) throws InterruptedException {
        ArrayList<WItem> birds = new ArrayList<>();
        for (WItem item : inventory.getItems()) {
            if (isBirdItem(item, species, type)) {
                birds.add(item);
            }
        }
        return birds;
    }

    private WItem getBirdItem(NInventory inventory, BirdSpecies species, BirdType type) throws InterruptedException {
        ArrayList<WItem> birds = getBirdItems(inventory, species, type);
        return birds.isEmpty() ? null : birds.get(0);
    }

    private WItem getBirdItem(NInventory inventory, BirdSpecies species, BirdType type, double quality) throws InterruptedException {
        for (WItem item : getBirdItems(inventory, species, type)) {
            if (Double.compare(itemQuality(item), quality) == 0) {
                return item;
            }
        }
        return null;
    }

    private int countBirdItems(NInventory inventory, BirdSpecies species, BirdType type) throws InterruptedException {
        return getBirdItems(inventory, species, type).size();
    }

    private void waitForBirdItem(NInventory inventory, BirdSpecies species, BirdType type, int minCount) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                try {
                    return countBirdItems(inventory, species, type) >= minCount;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        });
    }

    private boolean isBirdItem(WItem item, BirdSpecies species, BirdType type) {
        return isBirdResource(itemResource(item), species, type);
    }

    private String itemResource(WItem item) {
        try {
            NGItem ngItem = (NGItem) item.item;
            return ngItem.res == null ? null : ngItem.res.get().name;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private float itemQuality(WItem item) {
        Float quality = ((NGItem) item.item).quality;
        return quality == null ? -1 : quality;
    }

    /**
     * Checks if inventory drop-off is needed based on available space.
     * Only drops off if insufficient space for another bird + buffer.
     *
     * @param gui Game UI interface
     * @return true if drop-off needed, false if can continue batching
     */
    private boolean shouldDropOffItems(NGameUI gui) throws InterruptedException {
        // Bird is 2x2 (4 cells) plus extra product buffer.
        int availableSpaceForBird = gui.getInventory().getNumberFreeCoord(new Coord(2, 2));
        return availableSpaceForBird <= 2;
    }
}
