package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Monitor-guarded adapter; map, values, and version share one synchronization policy. */
public final class InMemoryCatalog implements Catalog {
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private long version;

    /** Creates an empty catalog at version zero. */
    public InMemoryCatalog() {}

    @Override
    public synchronized Optional<Change> add(Material material) {
        Objects.requireNonNull(material);
        if (materials.containsKey(material.id())) {
            return Optional.empty();
        }
        long next = Math.incrementExact(version);
        materials.put(material.id(), material);
        version = next;
        return Optional.of(new Change(next, Optional.empty(), Optional.of(material)));
    }

    @Override
    public synchronized Optional<Change> update(Material material) {
        // TODO T2: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T2");
    }

    @Override
    public synchronized Optional<Change> remove(String id) {
        // TODO T2: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T2");
    }

    @Override
    public synchronized Snapshot snapshot() {
        return new Snapshot(version, new ArrayList<>(materials.values()));
    }

    @Override
    public synchronized Snapshot replaceAll(List<Material> replacement) {
        // TODO T2: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T2");
    }
}
