package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Mutable, thread-confined Builder whose completed bundles retain independent copies. */
public final class BundleBuilder {
    private final String name;
    private final List<Component> children = new ArrayList<>();
    private int basisPoints;

    /**
     * Starts a named order bundle.
     *
     * @param name nonblank bundle name
     * @throws IllegalArgumentException for a blank name
     */
    public BundleBuilder(String name) {
        this.name = Objects.requireNonNull(name).strip();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("blank name");
        }
    }

    /**
     * Appends one occurrence, preserving insertion order.
     *
     * @param component existing node
     * @return this builder
     */
    public BundleBuilder add(Component component) {
        children.add(Objects.requireNonNull(component));
        return this;
    }

    /**
     * Changes the bundle discount after validation.
     *
     * @param value basis points in 0..10000
     * @return this builder
     * @throws IllegalArgumentException if outside the range
     */
    public BundleBuilder discount(int value) {
        Money.discounted(0, value);
        basisPoints = value;
        return this;
    }

    /**
     * Builds without resetting the builder; subsequent edits cannot affect this result.
     *
     * @return independent immutable bundle
     * @throws IllegalArgumentException if no children were added
     */
    public Bundle build() {
        // TODO T3: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T3");
    }
}
