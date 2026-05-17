package com.rfizzle.meridian.anvil;

/**
 * One-stop registration hook for every MVP {@link AnvilHandler}. Called from
 * {@link com.rfizzle.meridian.Meridian#onInitialize} so the dispatcher chain is
 * populated before any anvil interaction fires.
 *
 * <p>Order matters — {@link AnvilDispatcher} walks handlers top-to-bottom and the first
 * non-empty result wins. Prismatic Web (T-4.1.4) and iron-block anvil repair (S-4.2) key off
 * disjoint right-slot items (web vs. iron block), so their relative order is behaviour-neutral;
 * they are listed in task-introduction order to keep the registration site readable. The tome
 * families (Story S-5.2) slot in below them. The Extraction Tome has two handlers keyed on the
 * same right-slot item — {@link ExtractionTomeHandler} owns the enchanted-left case,
 * {@link ExtractionTomeFuelSlotRepairHandler} owns the damaged-unenchanted-left case — so the
 * extraction path registers <em>first</em> and the repair path falls through when the left has
 * nothing to salvage.
 */
public final class MeridianAnvilHandlers {

    private static boolean registered = false;

    private MeridianAnvilHandlers() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        AnvilDispatcher.register(new PrismaticWebHandler());
        AnvilDispatcher.register(new IronBlockAnvilRepairHandler());
        AnvilDispatcher.register(new ScrapTomeHandler());
        AnvilDispatcher.register(new ImprovedScrapTomeHandler());
        AnvilDispatcher.register(new ExtractionTomeHandler());
        AnvilDispatcher.register(new ExtractionTomeFuelSlotRepairHandler());
    }
}
