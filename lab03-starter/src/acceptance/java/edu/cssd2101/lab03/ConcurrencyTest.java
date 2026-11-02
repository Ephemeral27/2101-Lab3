package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Timeout(15)
class ConcurrencyTest {
    private static Material m(String id) {
        return new Material(id, "Java " + id, Material.Kind.BOOK, 1);
    }

    @Test
    void simultaneousSameIdHasExactlyOneWinner() throws Exception {
        int workers = 40;
        var store = new InMemoryCatalog();
        var ready = new CountDownLatch(workers);
        var start = new CountDownLatch(1);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var results = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < workers; i++) {
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(3, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("start timeout");
                                    }
                                    return store.add(m("same")).isPresent();
                                }));
            }
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();
            int winners = 0;
            for (var result : results) {
                if (result.get(3, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertEquals(1, winners);
            assertEquals(1, store.snapshot().version());
            assertEquals(1, store.snapshot().materials().size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentAddsAndSnapshotsRetainAtomicVersionContentPairs() throws Exception {
        int workers = 40;
        var store = new InMemoryCatalog();
        var start = new CountDownLatch(1);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var results = new ArrayList<Future<Snapshot>>();
        try {
            for (int i = 0; i < workers; i++) {
                String id = "ID" + i;
                results.add(
                        executor.submit(
                                () -> {
                                    if (!start.await(3, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("start timeout");
                                    }
                                    store.add(m(id));
                                    return store.snapshot();
                                }));
            }
            start.countDown();
            for (var result : results) {
                Snapshot snapshot = result.get(3, TimeUnit.SECONDS);
                assertEquals(snapshot.version(), snapshot.materials().size());
                assertEquals(
                        snapshot.materials().size(),
                        snapshot.materials().stream().map(Material::id).distinct().count());
            }
            assertEquals(workers, store.snapshot().version());
            assertEquals(workers, store.snapshot().materials().size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    @Test
    void cacheNeverExceedsCapacityUnderConcurrentUseAndFreshSearchSeesCompletedWrite()
            throws Exception {
        var cache = new LruCache<Integer, Integer>(3);
        var store = new InMemoryCatalog();
        var search = new SearchService(store, 2);
        search.search("java", 7);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                int key = i;
                tasks.add(
                        executor.submit(
                                () -> {
                                    cache.put(key, key);
                                    cache.get(key);
                                    assertTrue(cache.stats().size() <= 3);
                                    store.add(m("ID" + key));
                                    assertTrue(search.search("java", 7).size() <= 7);
                                }));
            }
            for (var task : tasks) {
                task.get(3, TimeUnit.SECONDS);
            }
        }
        assertEquals(30, search.search("java", 100).size());
        assertEquals(3, cache.stats().size());
    }
}
