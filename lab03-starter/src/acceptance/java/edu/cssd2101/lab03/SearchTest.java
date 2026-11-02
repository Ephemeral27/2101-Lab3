package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class SearchTest {
    private static Material m(String id, String title) {
        return new Material(id, title, Material.Kind.BOOK, 100);
    }

    @Test
    void trieUsesUnicodeNormalizationAndDeterministicPrefixOrder() {
        var index =
                new PrefixIndex(
                        new Snapshot(
                                3,
                                List.of(
                                        m("B", "Java Streams"),
                                        m("A", "Java Patterns"),
                                        m("C", "École"),
                                        m("D", "😀 Java"))));
        assertEquals(
                List.of("A", "B"), index.search(" JAVA ", 7).stream().map(Material::id).toList());
        assertEquals(List.of("C"), index.search("E\u0301", 7).stream().map(Material::id).toList());
        assertEquals(List.of("D"), index.search("😀", 1).stream().map(Material::id).toList());
        assertEquals(4, index.search("", 99).size());
        assertEquals(1, index.search("java", 1).size());
        assertTrue(index.search("java", 0).isEmpty());
        assertTrue(index.search("ava", 10).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> index.search("a", -1));
        assertThrows(NullPointerException.class, () -> index.search(null, 1));
        assertThrows(UnsupportedOperationException.class, () -> index.search("", 9).clear());
    }

    @Test
    void lruPromotesHitsUpdatesWithoutEvictionAndCountsMisses() {
        var cache = new LruCache<String, Integer>(2);
        cache.put("A", 1);
        cache.put("B", 2);
        assertEquals(1, cache.get("A").orElseThrow());
        cache.put("C", 3);
        assertEquals(List.of("A", "C"), cache.keys());
        assertTrue(cache.get("B").isEmpty());
        cache.put("A", 4);
        assertEquals(List.of("C", "A"), cache.keys());
        assertEquals(new LruCache.Stats(1, 1, 1, 2), cache.stats());
        assertEquals(4, cache.get("A").orElseThrow());
        cache.clear();
        assertEquals(new LruCache.Stats(2, 1, 1, 0), cache.stats());
    }

    @Test
    void lruRejectsInvalidInputsWithoutChangingEntries() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache<String, String>(0));
        var cache = new LruCache<String, String>(1);
        cache.put("A", "value");
        assertThrows(NullPointerException.class, () -> cache.put("B", null));
        assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
        assertThrows(NullPointerException.class, () -> cache.get(null));
        assertEquals(List.of("A"), cache.keys());
        assertEquals(new LruCache.Stats(0, 0, 0, 1), cache.stats());
        assertThrows(UnsupportedOperationException.class, () -> cache.keys().clear());
    }

    @Test
    void cacheIncludesEveryLimitAndCachesEmptyResults() {
        var store = new InMemoryCatalog();
        store.add(m("A", "Java A"));
        store.add(m("B", "Java B"));
        var search = new SearchService(store, 4);
        assertEquals(1, search.search("java", 1).size());
        assertEquals(2, search.search("java", 7).size());
        search.search(" JAVA ", 7);
        search.search("missing", 3);
        search.search("missing", 3);
        assertEquals(new LruCache.Stats(2, 3, 0, 3), search.cacheStats());
        assertEquals(1, search.builds());
    }

    @Test
    void everyMutationInvalidatesIncludingRenameRemovalAndRestore() {
        var store = new InMemoryCatalog();
        var search = new SearchService(store, 3);
        assertTrue(search.search("java", 7).isEmpty());
        store.add(m("A", "Java"));
        assertEquals(1, search.search("java", 7).size());
        store.update(m("A", "Kotlin"));
        assertTrue(search.search("java", 7).isEmpty());
        assertEquals(1, search.search("kotlin", 7).size());
        store.remove("A");
        assertTrue(search.search("kotlin", 7).isEmpty());
        store.replaceAll(List.of(m("B", "Java")));
        assertEquals("B", search.search("java", 7).getFirst().id());
        assertEquals(5, search.builds());
        assertEquals(0, search.cacheStats().hits());
    }

    @Test
    void noOpAndInvalidQueryPreserveCacheGenerationAndStatistics() {
        var store = new InMemoryCatalog();
        Material a = m("A", "Java");
        store.add(a);
        var search = new SearchService(store, 1);
        search.search("java", 7);
        var stats = search.cacheStats();
        assertThrows(IllegalArgumentException.class, () -> search.search("java", -1));
        assertThrows(NullPointerException.class, () -> search.search(null, 1));
        assertEquals(stats, search.cacheStats());
        store.add(a);
        store.update(a);
        store.remove("absent");
        search.search("java", 7);
        assertEquals(1, search.builds());
        assertEquals(1, search.cacheStats().hits());
    }

    @Test
    void boundedDatasetMatchesIndependentLinearSearchOracle() {
        var store = new InMemoryCatalog();
        for (int i = 0; i < 100; i++) {
            store.add(m("ID" + i, i % 2 == 0 ? "Java " + i : "Kotlin " + i));
        }
        var search = new SearchService(store, 3);
        for (String prefix : List.of("", "j", "java 1", "kotlin", "missing")) {
            for (int limit : List.of(0, 1, 7, 200)) {
                var expected =
                        store.snapshot().materials().stream()
                                .filter(
                                        m ->
                                                Material.searchKey(m.title())
                                                        .startsWith(Material.searchKey(prefix)))
                                .limit(limit)
                                .toList();
                assertEquals(expected, search.search(prefix, limit));
            }
        }
        assertTrue(search.cacheStats().size() <= 3);
    }
}
