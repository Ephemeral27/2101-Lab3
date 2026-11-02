package edu.cssd2101.lab03;

import java.util.List;
import java.util.Objects;

/**
 * Composite that discounts the sum of already-priced children once at this node.
 *
 * @param name nonblank display label
 * @param children nonempty copied child sequence
 * @param basisPoints discount in 0..10000
 */
public record Bundle(String name, List<Component> children, int basisPoints) implements Component {
    /**
     * Creates an immutable acyclic-by-construction tree node.
     *
     * @param name display label
     * @param children previously constructed children
     * @param basisPoints discount in 0..10000
     * @throws IllegalArgumentException for a blank name, empty children, or invalid rate
     * @throws NullPointerException for null name, children, or child
     */
    public Bundle {
        name = Objects.requireNonNull(name).strip();
        children = List.copyOf(children);
        if (name.isEmpty() || children.isEmpty()) {
            throw new IllegalArgumentException("empty bundle");
        }
        Money.discounted(0, basisPoints);
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
