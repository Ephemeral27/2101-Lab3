package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public final class Events implements EventSink {
    private final CopyOnWriteArrayList<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();

   
    public Events() {}

    
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
       
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
        Objects.requireNonNull(change);
        List<RuntimeException> failures = new ArrayList<>();
        for (Consumer<Change> listener : listeners) {
            try {
                listener.accept(change);
            } catch (RuntimeException e) {
                failures.add(e);
            }
        }
        return List.copyOf(failures);
    }
}