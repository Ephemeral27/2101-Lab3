package edu.cssd2101.lab03;

import java.util.Iterator;
import java.util.List;

/** Sealed, immutable order tree: leaves, discounted bundles, and priced add-ons. */
public sealed interface Component extends Iterable<Material> permits Leaf, Bundle, AddOn {
    /**
     * Computes this node's final price using checked arithmetic.
     *
     * @return nonnegative cents
     * @throws ArithmeticException if a sum overflows a long
     */
    long priceCents();

    /**
     * Dispatches to the overload for this concrete node.
     *
     * @param visitor operation separated from the tree
     * @param <R> result type
     * @return visitor result
     */
    <R> R accept(ComponentVisitor<R> visitor);

    /**
     * Flattens leaves in left-to-right depth-first order, preserving repeated quantities.
     *
     * @return immutable material sequence
     */
    default List<Material> materials() {
        return accept(new FlattenVisitor());
    }

    /**
     * Traverses a detached immutable sequence; removal is unsupported.
     *
     * @return independent snapshot iterator
     */
    @Override
    default Iterator<Material> iterator() {
        return materials().iterator();
    }
}
