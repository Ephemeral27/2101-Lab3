package edu.cssd2101.lab03;

import java.util.List;
import java.util.Optional;

/**
 * Catalog port. Each operation is atomic; successful mutations advance a version once. No-op
 * additions, updates, removals, and equal replacements preserve the version. Implementations must
 * return immutable detached snapshots and changes.
 */
public interface Catalog {
    /**
     * Inserts only when the normalized ID is absent.
     *
     * @param material nonnull value
     * @return committed change, or empty when that ID already exists
     */
    Optional<Change> add(Material material);

    /**
     * Replaces an existing ID only when its value differs.
     *
     * @param material nonnull replacement
     * @return committed change, or empty for absent/equal value
     */
    Optional<Change> update(Material material);

    /**
     * Removes one identity.
     *
     * @param id identifier following Material's normalization policy
     * @return committed change, or empty when absent
     */
    Optional<Change> remove(String id);

    /**
     * Returns state from one logical instant.
     *
     * @return immutable snapshot
     */
    Snapshot snapshot();

    /**
     * Validates all input before atomically replacing the inventory.
     *
     * @param materials complete replacement with unique IDs
     * @return state after replacement
     * @throws IllegalArgumentException for duplicate identities
     * @throws NullPointerException for a null list or element
     */
    Snapshot replaceAll(List<Material> materials);
}
