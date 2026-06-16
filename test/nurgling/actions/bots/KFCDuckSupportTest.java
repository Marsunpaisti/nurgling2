package nurgling.actions.bots;

public class KFCDuckSupportTest {
    public static void main(String[] args) {
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.MALE, "gfx/invobjs/duckdrake");
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.FEMALE, "gfx/invobjs/duckhen");
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.BABY, "gfx/invobjs/duckling");
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.EGG, "gfx/invobjs/egg-duck");

        assertMatch(KFC.BirdSpecies.CHICKEN, KFC.BirdType.MALE, "gfx/invobjs/rooster");
        assertMatch(KFC.BirdSpecies.CHICKEN, KFC.BirdType.FEMALE, "gfx/invobjs/hen");
        assertMatch(KFC.BirdSpecies.CHICKEN, KFC.BirdType.BABY, "gfx/invobjs/chick");
        assertMatch(KFC.BirdSpecies.CHICKEN, KFC.BirdType.EGG, "gfx/invobjs/egg-chicken");

        assertNoCrossSpecies(KFC.BirdSpecies.CHICKEN, KFC.BirdType.MALE, "gfx/invobjs/duckdrake");
        assertNoCrossSpecies(KFC.BirdSpecies.DUCK, KFC.BirdType.FEMALE, "gfx/invobjs/hen");
        assertNoCrossSpecies(KFC.BirdSpecies.CHICKEN, KFC.BirdType.EGG, "gfx/invobjs/egg-duck");
        assertNoCrossSpecies(KFC.BirdSpecies.DUCK, KFC.BirdType.BABY, "gfx/invobjs/chick");

        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.DEAD_MALE, "gfx/invobjs/duckdrake-dead");
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.PLUCKED_FEMALE, "gfx/invobjs/duckhen-plucked");
        assertMatch(KFC.BirdSpecies.DUCK, KFC.BirdType.CLEANED, "gfx/invobjs/duck-cleaned");
    }

    private static void assertMatch(KFC.BirdSpecies species, KFC.BirdType type, String resource) {
        if (!KFC.isBirdResource(resource, species, type)) {
            throw new AssertionError(resource + " must match " + species + " " + type);
        }
    }

    private static void assertNoCrossSpecies(KFC.BirdSpecies species, KFC.BirdType type, String resource) {
        if (KFC.isBirdResource(resource, species, type)) {
            throw new AssertionError(resource + " must not match " + species + " " + type);
        }
    }
}
