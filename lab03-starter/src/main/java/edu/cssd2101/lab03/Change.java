package edu.cssd2101.lab03;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable event captured in the same critical section as its mutation.
 *
 * @param version strictly positive committed version
 * @param before previous value, empty for addition
 * @param after new value, empty for removal
 */
public record Change(long version, Optional<Material> before, Optional<Material> after) {
    /**
     * Validates a real change for one identity.
     *
     * @param version committed version
     * @param before previous value
     * @param after new value
     * @throws IllegalArgumentException for an invalid or unchanged transition
     * @throws NullPointerException for null optionals
     */
    public Change {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        if (version <= 0 || before.equals(after)) {
            throw new IllegalArgumentException("not a change");
        }
        if (before.isPresent()
                && after.isPresent()
                && !before.get().id().equals(after.get().id())) {
            throw new IllegalArgumentException("identity cannot change");
        }
    }
}
