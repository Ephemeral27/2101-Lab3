package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class CatalogTest {
    private static Material m(String id, long cents) {
        return new Material(id, "Title", Material.Kind.BOOK, cents);
    }

    @Test
    void mutationsReturnExactBeforeAfterAndVersion() {
        var store = new InMemoryCatalog();
        Material before = m("a", 100);
        Material after = m("a", 200);
        Change add = store.add(before).orElseThrow();
        assertEquals(1, add.version());
        assertTrue(add.before().isEmpty());
        Change update = store.update(after).orElseThrow();
        assertEquals(Optional.of(before), update.before());
        assertEquals(Optional.of(after), update.after());
        assertEquals(2, update.version());
        Change remove = store.remove(" a ").orElseThrow();
        assertEquals(3, remove.version());
        assertTrue(remove.after().isEmpty());
        assertEquals(List.of(), store.snapshot().materials());
    }

    @Test
    void noOpAndRejectedMutationsPreserveState() {
        var store = new InMemoryCatalog();
        Material a = m("A", 1);
        store.add(a);
        Snapshot before = store.snapshot();
        assertTrue(store.add(m("a", 2)).isEmpty());
        assertTrue(store.update(a).isEmpty());
        assertTrue(store.update(m("Z", 1)).isEmpty());
        assertTrue(store.remove("Z").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> store.remove("?"));
        assertThrows(NullPointerException.class, () -> store.add(null));
        assertThrows(NullPointerException.class, () -> store.update(null));
        assertEquals(before, store.snapshot());
    }

    @Test
    void snapshotsDetachAndOrderById() {
        var store = new InMemoryCatalog();
        store.add(m("B", 1));
        store.add(m("A", 1));
        Snapshot snapshot = store.snapshot();
        store.remove("A");
        assertEquals(List.of("A", "B"), snapshot.materials().stream().map(Material::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.materials().clear());
        assertEquals(2, snapshot.version());
        assertEquals(3, store.snapshot().version());
    }

    @Test
    void replacementIsAllOrNothingAndCopiesCallerCollection() {
        var store = new InMemoryCatalog();
        store.add(m("OLD", 1));
        Snapshot before = store.snapshot();
        assertThrows(
                IllegalArgumentException.class,
                () -> store.replaceAll(List.of(m("A", 1), m("a", 2))));
        assertEquals(before, store.snapshot());
        var source = new ArrayList<>(List.of(m("NEW", 2)));
        Snapshot replaced = store.replaceAll(source);
        source.clear();
        assertEquals(2, replaced.version());
        assertEquals(1, store.snapshot().materials().size());
        assertEquals(replaced, store.replaceAll(replaced.materials()));
        assertEquals(3, store.replaceAll(List.of()).version());
    }

    @Test
    void changeRejectsContradictoryTransitions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Change(0, Optional.empty(), Optional.of(m("A", 1))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Change(1, Optional.empty(), Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Change(1, Optional.of(m("A", 1)), Optional.of(m("B", 1))));
        assertThrows(NullPointerException.class, () -> new Change(1, null, Optional.empty()));
    }
}
