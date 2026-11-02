package edu.cssd2101.lab03;

import java.io.IOException;
import java.util.List;

/** Persistence port for complete inventories; schema and paths belong to its adapter. */
public interface CatalogRepository {
    /**
     * Loads a complete validated inventory; missing storage means an empty inventory.
     *
     * @return immutable values
     * @throws IOException for unreadable or malformed existing storage
     */
    List<Material> load() throws IOException;

    /**
     * Writes a complete inventory; validation failure preserves existing bytes.
     *
     * @param materials unique nonnull values
     * @throws IOException if replacement cannot complete
     * @throws IllegalArgumentException for duplicate identities
     */
    void save(List<Material> materials) throws IOException;
}
