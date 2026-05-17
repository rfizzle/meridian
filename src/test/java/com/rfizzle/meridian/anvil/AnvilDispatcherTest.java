package com.rfizzle.meridian.anvil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for {@link AnvilDispatcher}. T-4.1.1 verified the no-handler path; T-4.1.2 layers
 * the ordered handler list and the "first non-empty wins, short-circuit the rest" rule on top.
 *
 * <p>Stub handlers are constructed with lambdas that touch only their own {@link AtomicInteger}
 * counters and never deref the {@code ItemStack}/{@code Player} arguments — that lets the tests
 * pass {@code null} for those arguments and skip a full Minecraft bootstrap, which would otherwise
 * be needed to construct {@code ItemStack.EMPTY}.
 */
class AnvilDispatcherTest {

    @BeforeEach
    @AfterEach
    void resetHandlers() {
        AnvilDispatcher.clear();
    }

    @Test
    void handle_withNoHandlers_returnsEmpty() {
        Optional<AnvilResult> result = AnvilDispatcher.handle(null, null, null, null, 0);
        assertNotNull(result, "handle must return a non-null Optional, even when no handlers claim the pair");
        assertTrue(result.isEmpty(),
                "with no handlers registered the dispatcher must return Optional.empty — "
                        + "a non-empty result would leak into the anvil output slot");
    }

    @Test
    void handle_firstHandlerClaims_secondNeverConsulted() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        AnvilResult canned = new AnvilResult(null, 7, 1);

        AnvilDispatcher.register((left, right, player) -> {
            firstCalls.incrementAndGet();
            return Optional.of(canned);
        });
        AnvilDispatcher.register((left, right, player) -> {
            secondCalls.incrementAndGet();
            return Optional.of(new AnvilResult(null, 99, 99));
        });

        Optional<AnvilResult> result = AnvilDispatcher.handle(null, null, null, null, 0);

        assertTrue(result.isPresent(), "first handler returned a result; dispatcher must surface it");
        assertSame(canned, result.get(),
                "dispatcher must return the first handler's instance verbatim, not the second handler's");
        assertEquals(1, firstCalls.get(), "first handler should be invoked exactly once");
        assertEquals(0, secondCalls.get(),
                "second handler must not be consulted once the first claims the pair — "
                        + "otherwise multiple handlers could fight over the same output slot");
    }

    @Test
    void handle_firstHandlerEmpty_secondHandlerFires() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        AnvilResult canned = new AnvilResult(null, 12, 2);

        AnvilDispatcher.register((left, right, player) -> {
            firstCalls.incrementAndGet();
            return Optional.empty();
        });
        AnvilDispatcher.register((left, right, player) -> {
            secondCalls.incrementAndGet();
            return Optional.of(canned);
        });

        Optional<AnvilResult> result = AnvilDispatcher.handle(null, null, null, null, 0);

        assertTrue(result.isPresent(), "second handler returned a result; dispatcher must surface it");
        assertSame(canned, result.get(),
                "dispatcher must return the second handler's instance after the first declines");
        assertEquals(1, firstCalls.get(), "first handler invoked exactly once before declining");
        assertEquals(1, secondCalls.get(), "second handler invoked exactly once after the first declined");
    }

    @Test
    void handle_allHandlersDecline_returnsEmpty() {
        AnvilDispatcher.register((left, right, player) -> Optional.empty());
        AnvilDispatcher.register((left, right, player) -> Optional.empty());

        Optional<AnvilResult> result = AnvilDispatcher.handle(null, null, null, null, 0);

        assertTrue(result.isEmpty(),
                "every handler declined; dispatcher must yield to vanilla by returning empty");
    }

    @Test
    void handlers_snapshotIsUnmodifiable() {
        AnvilDispatcher.register((left, right, player) -> Optional.empty());

        List<AnvilHandler> snapshot = AnvilDispatcher.handlers();
        assertEquals(1, snapshot.size(), "snapshot reflects registered handlers");

        try {
            snapshot.add((left, right, player) -> Optional.empty());
            throw new AssertionError("snapshot must be unmodifiable so callers cannot mutate the live chain");
        } catch (UnsupportedOperationException expected) {
            // pass
        }
    }
}
