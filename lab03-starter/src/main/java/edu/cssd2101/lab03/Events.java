package edu.cssd2101.lab03;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Synchronous Observer adapter with snapshot iteration and explicit unsubscription. */
public final class Events implements EventSink {
    private final CopyOnWriteArrayList<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();

    /** Creates an event bus without observers. */
    public Events() {}

    /** Subscription owned by its caller; closing more than once is harmless. */
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        /** Removes this registration without checked exceptions. */
        @Override
        void close();
    }

    /**
     * Adds one independent registration. Callbacks may execute concurrently across publishers.
     *
     * @param listener nonnull callback; synchronize its mutable state if needed
     * @return caller-owned subscription
     */
    public Subscription subscribe(Consumer<Change> listener) {
        Objects.requireNonNull(listener);
        Consumer<Change> registration = change -> listener.accept(change);
        listeners.add(registration);
        return () -> listeners.remove(registration);
    }

    @Override
    public List<RuntimeException> publish(Change change) {
        // TODO T4: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T4");
    }
}
