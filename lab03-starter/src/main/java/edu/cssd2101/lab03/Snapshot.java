package edu.cssd2101.lab03;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Detached, immutable catalog state from one logical instant, ordered by ID.
 *
 * @param version nonnegative mutation counter
 * @param materials unique, nonnull immutable values
 */
public record Snapshot(long version, List<Material> materials) {
    /**
     * Copies, validates, and sorts the collection.
     *
     * @param version nonnegative counter
     * @param materials values to detach
     * @throws IllegalArgumentException for duplicate IDs or a negative version
     * @throws NullPointerException for a null list or element
     */
    public Snapshot {
        if (version < 0) {
            throw new IllegalArgumentException("negative version");
        }
        materials = List.copyOf(materials);
        var ids = new HashSet<String>();
        for (Material material : materials) {
            if (!ids.add(material.id())) {
                throw new IllegalArgumentException("duplicate ID: " + material.id());
            }
        }
        materials = materials.stream().sorted(Comparator.comparing(Material::id)).toList();
    }
}
