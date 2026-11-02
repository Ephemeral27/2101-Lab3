package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class SmokeTest {
    @Test
    void starterCreatesValuesAndAddsUniqueIds() {
        var store = new InMemoryCatalog();
        Material material = MaterialFactory.create("book", " b1 ", "Java Patterns", "24.99");
        assertEquals(2499, material.cents());
        assertTrue(store.add(material).isPresent());
        assertTrue(store.add(material).isEmpty());
        assertEquals(
                List.of("B1"), store.snapshot().materials().stream().map(Material::id).toList());
        assertEquals(1, store.snapshot().version());
    }

    @Test
    void baselineSnapshotsAndLeavesAreImmutable() {
        var store = new InMemoryCatalog();
        Material material = new Material("A", "A", Material.Kind.BOOK, 0);
        store.add(material);
        Snapshot before = store.snapshot();
        store.add(new Material("B", "B", Material.Kind.EBOOK, 10));
        assertEquals(1, before.materials().size());
        assertThrows(UnsupportedOperationException.class, () -> before.materials().clear());
        assertEquals(0, new Leaf(material).priceCents());
    }
}
