package edu.cssd2101.lab03;

import java.util.List;

/** Event publication port; ordinary observer failures do not roll back committed data. */
@FunctionalInterface
public interface EventSink {
    /**
     * Delivers a committed change and reports ordinary observer failures.
     *
     * @param change nonnull immutable change
     * @return immutable failure list
     */
    List<RuntimeException> publish(Change change);
}
