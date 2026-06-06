package nurgling.actions.bots.silk;

import haven.Gob;

import java.lang.reflect.Field;

public class TransferSilkwormsFromHTablesToFeedingTest {
    public static void main(String[] args) {
        resolvesContainerGobByHashBeforeStaleId();
        fallsBackToContainerIdWhenHashMissing();
    }

    private static void resolvesContainerGobByHashBeforeStaleId() {
        Gob byHash = inertGob();
        Gob byId = inertGob();

        Gob resolved = TransferSilkwormsFromHTablesToFeeding.resolveContainerGob(
                "stable-hash",
                2L,
                hash -> byHash,
                id -> byId);

        if (resolved != byHash) {
            throw new AssertionError("container resolver must prefer stable gob hash over potentially stale id");
        }
    }

    private static void fallsBackToContainerIdWhenHashMissing() {
        Gob byId = inertGob();

        Gob resolved = TransferSilkwormsFromHTablesToFeeding.resolveContainerGob(
                null,
                2L,
                hash -> null,
                id -> byId);

        if (resolved != byId) {
            throw new AssertionError("container resolver must fall back to gob id when hash is missing");
        }
    }

    private static Gob inertGob() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            return (Gob) unsafe.allocateInstance(Gob.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("unable to allocate inert Gob for resolver test", e);
        }
    }
}
