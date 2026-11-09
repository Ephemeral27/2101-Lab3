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
        Objects.requireNonNull(material);
        Material previousMaterial = materials.get(material.id());
        
        if (previousMaterial == null || previousMaterial.equals(material)) {
        	return Optional.empty();
        }
        
        long updatedVersion = Math.incrementExact(version);
        materials.put(material.id(), material);
        version = updatedVersion;
        return Optional.of(new Change(updatedVersion, Optional.of(previousMaterial), Optional.of(material)));
    }

    @Override
    public synchronized Optional<Change> remove(String id) {
        // TODO T2: implement the documented contract; see the acceptance tests.
        String catalogId = Material.normalizeId(id);
        Material removedMaterial = materials.get(catalogId);
        
        if (removedMaterial == null) {
        	return Optional.empty();
        }
        
        long removalVersion = Math.incrementExact(version);
        materials.remove(catalogId);
        version = removalVersion;
        
        return Optional.of(new Change(removalVersion, Optional.of(removedMaterial), Optional.empty()));
    }

    @Override
    public synchronized Snapshot snapshot() {
        return new Snapshot(version, new ArrayList<>(materials.values()));
    }

    @Override
    public synchronized Snapshot replaceAll(List<Material> replacement) {
        // TODO T2: implement the documented contract; see the acceptance tests.
        Objects.requireNonNull(replacement);
        Map<String, Material> replacementMaterials = new LinkedHashMap<>();
        
        for (Material incomingMaterial : replacement) {
        	Objects.requireNonNull(incomingMaterial);
        	
        	if (replacementMaterials.putIfAbsent(incomingMaterial.id(), incomingMaterial) != null) {
        		throw new IllegalArgumentException("Duplicate material ID: " + incomingMaterial.id());
        	}
        }
        
        Snapshot replacementSnapshot = new Snapshot(version, new ArrayList<>(replacementMaterials.values()));
        
        if (replacementSnapshot.materials().equals(snapshot().materials())) {
        	return snapshot();
        }
        
        long replacementVersion = Math.incrementExact(version);
        materials.clear();
        materials.putAll(replacementMaterials);
        version = replacementVersion;
        return snapshot();
    }
}
