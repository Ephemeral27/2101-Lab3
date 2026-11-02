package edu.cssd2101.lab03;

/**
 * Double-dispatch operation with one overload for each concrete component kind.
 *
 * @param <R> result type
 */
public interface ComponentVisitor<R> {
    /**
     * Visits one material occurrence.
     *
     * @param leaf leaf node
     * @return computed result
     */
    R visit(Leaf leaf);

    /**
     * Visits a bundle, recursively if this operation requires it.
     *
     * @param bundle bundle node
     * @return computed result
     */
    R visit(Bundle bundle);

    /**
     * Visits one decorator.
     *
     * @param addOn decorator node
     * @return computed result
     */
    R visit(AddOn addOn);
}
