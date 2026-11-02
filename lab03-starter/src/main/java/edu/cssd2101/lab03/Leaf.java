package edu.cssd2101.lab03;

import java.util.Objects;

/**
 * A single immutable material occurrence in an order.
 *
 * @param material nonnull catalog value
 */
public record Leaf(Material material) implements Component {
    /**
     * Validates the leaf.
     *
     * @param material nonnull material
     */
    public Leaf {
        Objects.requireNonNull(material);
    }

    @Override
    public long priceCents() {
        return material.cents();
    }

    @Override
    public <R> R accept(ComponentVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
