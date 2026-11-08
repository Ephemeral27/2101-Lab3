package edu.cssd2101.lab03;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

/** Iterator factory for deterministic selection over detached catalog snapshots. */
public final class Traversals {
    private Traversals() {}

    /**
     * Filters by inclusive price bounds and optional category, then sorts by price and ID.
     *
     * @param snapshot immutable source
     * @param kind desired kind, or empty for every kind
     * @param minimum inclusive minimum cents
     * @param maximum inclusive maximum cents
     * @return immutable-list iterator; next after exhaustion throws NoSuchElementException
     * @throws IllegalArgumentException for negative or reversed bounds
     */
    public static Iterator<Material> priced(
            Snapshot snapshot, Optional<Material.Kind> kind, long minimum, long maximum) {
        // TODO T3: implement the documented contract; see the acceptance tests.
        if (minimum < 0 || maximum <0 || maximum < minimum){
            throw new IllegalArgumentException("minimum cannot be larger than mazimum and neither parameter can be negative");
        }
        Iterator<Material> result = snapshot.materials().stream().filter(snapItem-> snapItem.cents() >=minimum && snapItem.cents()<=maximum && (kind.isEmpty() || snapItem.kind() == kind.get())).sorted(Comparator.comparingLong(Material::cents).thenComparing(Material::id)).toList().iterator();
        return result;
        //throw new UnsupportedOperationException("TODO T3");
    }
}
