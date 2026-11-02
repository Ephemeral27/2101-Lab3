package edu.cssd2101.lab03;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Monitor-guarded bounded access-order cache. Keys and values must be immutable or externally
 * owned.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class LruCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long hits;
    private long misses;
    private long evictions;

    /**
     * Creates an empty bounded cache.
     *
     * @param capacity strictly positive maximum entry count
     * @throws IllegalArgumentException for a nonpositive capacity
     */
    public LruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    /**
     * Looks up and promotes a hit to most recently used.
     *
     * @param key nonnull key
     * @return cached value, or empty on a miss
     */
    public synchronized Optional<V> get(K key) {
        // TODO T5: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T5");
    }

    /**
     * Inserts or updates, evicting at most one least-recently-used entry afterward.
     *
     * @param key nonnull key
     * @param value nonnull value
     */
    public synchronized void put(K key, V value) {
        // TODO T5: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T5");
    }

    /** Removes entries while retaining lifetime hit, miss, and eviction counts. */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * Returns current access order without promoting any entry.
     *
     * @return least to most recently used keys
     */
    public synchronized List<K> keys() {
        return List.copyOf(entries.keySet());
    }

    /**
     * Captured cache counters; invalidation is distinct from capacity eviction.
     *
     * @param hits successful lookups
     * @param misses unsuccessful lookups
     * @param evictions capacity-driven removals
     * @param size current entry count
     */
    public record Stats(long hits, long misses, long evictions, int size) {}

    /**
     * Obtains counters and size atomically.
     *
     * @return immutable statistics
     */
    public synchronized Stats stats() {
        return new Stats(hits, misses, evictions, entries.size());
    }
}
