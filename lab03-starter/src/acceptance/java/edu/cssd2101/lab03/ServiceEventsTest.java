package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(10)
class ServiceEventsTest {
    private static Material m(String id, long cents) {
        return new Material(id, "Title", Material.Kind.BOOK, cents);
    }

    private static class MemoryRepository implements CatalogRepository {
        private List<Material> values = List.of();
        private boolean broken;

        @Override
        public List<Material> load() throws IOException {
            if (broken) {
                throw new IOException("injected");
            }
            return values;
        }

        @Override
        public void save(List<Material> values) throws IOException {
            if (broken) {
                throw new IOException("injected");
            }
            this.values = List.copyOf(values);
        }
    }

    @Test
    void serviceUsesPortsAndPublishesOnlyCommittedMutations() throws Exception {
        var store = new InMemoryCatalog();
        var repository = new MemoryRepository();
        var events = new Events();
        var history = new ArrayList<Change>();
        events.subscribe(history::add);
        var service = new CatalogService(store, repository, events);
        assertTrue(service.add(m("A", 1)).changed());
        assertFalse(service.add(m("A", 2)).changed());
        assertTrue(service.update(m("A", 2)).changed());
        assertFalse(service.update(m("A", 2)).changed());
        assertTrue(service.remove("A").changed());
        assertFalse(service.remove("A").changed());
        assertEquals(List.of(1L, 2L, 3L), history.stream().map(Change::version).toList());
        service.add(m("B", 3));
        service.save();
        store.replaceAll(List.of());
        assertEquals(List.of(m("B", 3)), service.restore().materials());
        assertEquals(4, history.size());
    }

    @Test
    void observerFailureIsReportedAfterCommitAndDoesNotStopLaterObservers() {
        var store = new InMemoryCatalog();
        var events = new Events();
        var calls = new AtomicInteger();
        events.subscribe(
                change -> {
                    throw new IllegalStateException("observer failure");
                });
        events.subscribe(change -> calls.incrementAndGet());
        var service = new CatalogService(store, new MemoryRepository(), events);
        assertEquals(new CatalogService.Result(true, 1), service.add(m("A", 1)));
        assertEquals(1, calls.get());
        assertEquals(1, store.snapshot().materials().size());
    }

    @Test
    void listenerChangesApplyToNextPublicationAndClosingIsIdempotent() {
        var events = new Events();
        var second = new AtomicReference<Events.Subscription>();
        var calls = new ArrayList<String>();
        events.subscribe(
                change -> {
                    calls.add("first");
                    second.get().close();
                });
        second.set(events.subscribe(change -> calls.add("second")));
        Change change = new InMemoryCatalog().add(m("A", 1)).orElseThrow();
        events.publish(change);
        events.publish(change);
        second.get().close();
        assertEquals(List.of("first", "second", "first"), calls);
        assertThrows(NullPointerException.class, () -> events.subscribe(null));
        assertThrows(NullPointerException.class, () -> events.publish(null));
    }

    @Test
    void callbacksRunAfterCatalogLockIsReleased() throws Exception {
        var store = new InMemoryCatalog();
        var events = new Events();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            events.subscribe(
                    change -> {
                        try {
                            assertEquals(
                                    1,
                                    executor.submit(() -> store.snapshot().materials().size())
                                            .get(2, TimeUnit.SECONDS));
                        } catch (Exception failure) {
                            throw new IllegalStateException(failure);
                        }
                    });
            var result = new CatalogService(store, new MemoryRepository(), events).add(m("A", 1));
            assertEquals(0, result.observerFailures());
        }
    }

    @Test
    void loadAndSaveFailuresPreserveTheInventory() throws Exception {
        var store = new InMemoryCatalog();
        var repository = new MemoryRepository();
        var service = new CatalogService(store, repository, new Events());
        service.add(m("A", 1));
        Snapshot before = store.snapshot();
        repository.broken = true;
        assertThrows(IOException.class, service::save);
        assertThrows(IOException.class, service::restore);
        assertEquals(before, store.snapshot());
        repository.broken = false;
        repository.values = List.of(m("B", 1), m("b", 2));
        assertThrows(IllegalArgumentException.class, service::restore);
        assertEquals(before, store.snapshot());
    }
}
