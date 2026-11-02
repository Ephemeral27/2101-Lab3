package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;

class ValuesTest {
    @Test
    void normalizesIdentityAndKeepsDisplayTitle() {
        Material material = new Material(" b-1 ", " Java  Patterns ", Material.Kind.BOOK, 0);
        assertEquals("B-1", material.id());
        assertEquals("Java  Patterns", material.title());
        assertEquals("école", Material.searchKey(" E\u0301COLE "));
        assertEquals("", Material.searchKey("  "));
    }

    @Test
    void caseRulesDoNotDependOnTurkishDefaultLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("ID", Material.normalizeId("id"));
            assertEquals("intro", Material.searchKey("INTRO"));
        } finally {
            Locale.setDefault(old);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "bad id", "é", "x_y", "abcdefghijklmnopqrstuvwxyz0123456789"})
    void rejectsInvalidIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> Material.normalizeId(id));
    }

    @Test
    void rejectsInvalidMaterialFields() {
        assertThrows(
                NullPointerException.class,
                () -> new Material(null, "Title", Material.Kind.BOOK, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Material("A", " ", Material.Kind.BOOK, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Material("A", "x".repeat(201), Material.Kind.BOOK, 0));
        assertThrows(NullPointerException.class, () -> new Material("A", "Title", null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Material("A", "Title", Material.Kind.BOOK, -1));
        assertThrows(NullPointerException.class, () -> Material.searchKey(null));
    }

    @Test
    void roundsTiesToEvenAndRejectsOverflow() {
        assertEquals(100, Money.cents("1.005"));
        assertEquals(102, Money.cents("1.015"));
        assertEquals(2, Money.discounted(5, 5000));
        assertEquals(4, Money.discounted(7, 5000));
        assertEquals(Long.MAX_VALUE, Money.discounted(Long.MAX_VALUE, 0));
        assertEquals(0, Money.discounted(Long.MAX_VALUE, 10000));
        assertThrows(IllegalArgumentException.class, () -> Money.cents("-0.001"));
        assertThrows(NumberFormatException.class, () -> Money.cents("NaN"));
        assertThrows(ArithmeticException.class, () -> Money.cents("92233720368547758.08"));
        assertThrows(IllegalArgumentException.class, () -> Money.discounted(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Money.discounted(0, -1));
        assertThrows(IllegalArgumentException.class, () -> Money.discounted(0, 10001));
    }

    @Test
    void factoryHandlesEveryCategoryAndRejectsUnknownKinds() {
        for (Material.Kind kind : Material.Kind.values()) {
            assertEquals(
                    kind,
                    MaterialFactory.create(
                                    " " + kind.name().toLowerCase(Locale.ROOT) + " ",
                                    "A",
                                    "Title",
                                    "2.50")
                            .kind());
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialFactory.create("DVD", "A", "Title", "1"));
    }

    @Test
    void snapshotValidatesBeforePublishingAnyValues() {
        Material a = new Material("A", "A", Material.Kind.BOOK, 1);
        assertThrows(IllegalArgumentException.class, () -> new Snapshot(-1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Snapshot(0, List.of(a, a)));
        assertThrows(NullPointerException.class, () -> new Snapshot(0, null));
    }
}
