package edu.cssd2101.lab03;

import java.util.Objects;

/**
 * Decorator adding a fixed fee to any component, including an entire bundle.
 *
 * @param label nonblank feature name
 * @param wrapped existing immutable component
 * @param feeCents nonnegative surcharge
 */
public record AddOn(String label, Component wrapped, long feeCents) implements Component {
    /**
     * Validates the decorator; repeated add-ons are allowed and charged independently.
     *
     * @param label feature name
     * @param wrapped delegated component
     * @param feeCents nonnegative surcharge
     * @throws IllegalArgumentException for a blank label or negative fee
     * @throws NullPointerException for null label or wrapped node
     */
    public AddOn {
        label = Objects.requireNonNull(label).strip();
        Objects.requireNonNull(wrapped);
        if (label.isEmpty() || feeCents < 0) {
            throw new IllegalArgumentException("invalid add-on");
        }
    }

    @Override
    public long priceCents() {
        // TODO T3: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public <R> R accept(ComponentVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
