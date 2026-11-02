package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

class PatternsTest {
    private static Leaf leaf(String id, Material.Kind kind, long cents) {
        return new Leaf(new Material(id, id, kind, cents));
    }

    @Test
    void nestedPricingAndDecoratorPlacementAreObservable() {
        Leaf a = leaf("A", Material.Kind.BOOK, 1000), b = leaf("B", Material.Kind.EBOOK, 500);
        Bundle inner = new Bundle("Inner", List.of(a, b), 1000);
        Bundle outer = new Bundle("Outer", List.of(inner, a), 1000);
        assertEquals(1350, inner.priceCents());
        assertEquals(2115, outer.priceCents());
        assertEquals(2365, new AddOn("wrap", outer, 250).priceCents());
        assertEquals(
                2340,
                new Bundle("Outer", List.of(new AddOn("wrap", inner, 250), a), 1000).priceCents());
        assertEquals(1000, a.priceCents());
    }

    @Test
    void builderAndBundleDefendAgainstAliasing() {
        Leaf a = leaf("A", Material.Kind.BOOK, 1);
        var children = new ArrayList<Component>(List.of(a));
        Bundle direct = new Bundle("D", children, 0);
        children.clear();
        assertEquals(1, direct.children().size());
        var builder = new BundleBuilder("Study").add(a).discount(1000);
        Bundle first = builder.build();
        builder.add(a).discount(2000);
        assertEquals(1, first.children().size());
        assertEquals(1000, first.basisPoints());
        assertThrows(IllegalArgumentException.class, () -> builder.discount(10001));
        assertEquals(2000, builder.build().basisPoints());
        assertThrows(UnsupportedOperationException.class, () -> direct.children().clear());
    }

    @Test
    void invalidTreeInputsAreRejected() {
        Leaf a = leaf("A", Material.Kind.BOOK, 1);
        assertThrows(IllegalArgumentException.class, () -> new BundleBuilder("  "));
        assertThrows(IllegalArgumentException.class, () -> new BundleBuilder("Empty").build());
        assertThrows(IllegalArgumentException.class, () -> new Bundle(" ", List.of(a), 0));
        assertThrows(IllegalArgumentException.class, () -> new Bundle("B", List.of(a), -1));
        assertThrows(NullPointerException.class, () -> new Leaf(null));
        assertThrows(IllegalArgumentException.class, () -> new AddOn(" ", a, 1));
        assertThrows(IllegalArgumentException.class, () -> new AddOn("wrap", a, -1));
        assertThrows(NullPointerException.class, () -> new AddOn("wrap", null, 1));
    }

    @Test
    void arithmeticOverflowIsVisibleAndLeavesRemainValid() {
        Leaf a = leaf("A", Material.Kind.BOOK, Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> new AddOn("wrap", a, 1).priceCents());
        assertThrows(
                ArithmeticException.class, () -> new Bundle("B", List.of(a, a), 0).priceCents());
        assertEquals(Long.MAX_VALUE, a.priceCents());
    }

    @Test
    void visitorTraversesDecoratorsAndCountsRepeatedOccurrences() {
        Leaf book = leaf("B", Material.Kind.BOOK, 1), ebook = leaf("E", Material.Kind.EBOOK, 1);
        Component order =
                new AddOn(
                        "wrap",
                        new Bundle(
                                "B",
                                List.of(book, new Bundle("Inner", List.of(ebook, book), 0)),
                                0),
                        1);
        var visitor = new ShippingVisitor();
        assertEquals(500L, order.accept(visitor));
        assertEquals(500L, order.accept(visitor));
        assertEquals(List.of("B", "E", "B"), order.materials().stream().map(Material::id).toList());
        var iterator = order.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertThrows(UnsupportedOperationException.class, () -> order.materials().clear());
        for (Material.Kind kind : Material.Kind.values()) {
            long expected =
                    kind == Material.Kind.BOOK ? 250 : kind == Material.Kind.MAGAZINE ? 200 : 0;
            assertEquals(expected, leaf("A", kind, 0).accept(visitor));
        }
    }

    @Test
    void iteratorUsesInclusiveBoundsStableTiesAndExhaustionContract() {
        Snapshot snapshot =
                new Snapshot(
                        0,
                        List.of(
                                leaf("B", Material.Kind.BOOK, 100).material(),
                                leaf("A", Material.Kind.BOOK, 100).material(),
                                leaf("C", Material.Kind.EBOOK, 200).material()));
        var iterator = Traversals.priced(snapshot, Optional.of(Material.Kind.BOOK), 100, 100);
        assertEquals("A", iterator.next().id());
        assertEquals("B", iterator.next().id());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        var all = Traversals.priced(snapshot, Optional.empty(), 0, 200);
        var values = new ArrayList<Material>();
        all.forEachRemaining(values::add);
        assertEquals(3, values.size());
        assertThrows(
                IllegalArgumentException.class,
                () -> Traversals.priced(snapshot, Optional.empty(), -1, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> Traversals.priced(snapshot, Optional.empty(), 10, 9));
    }

    @ParameterizedTest
    @CsvSource({
        "0,Manager",
        "1500,Manager",
        "1501,Director",
        "2500,Director",
        "2501,VP",
        "4000,VP",
        "4001,declined",
        "10000,declined"
    })
    void approvalThresholdsChooseFirstHandler(int rate, String role) {
        var result = new DiscountChain().decide(10000, rate);
        assertEquals(role, result.approvedBy().orElse("declined"));
        assertEquals(role.equals("declined") ? 10000 : 10000 - rate, result.finalCents());
        assertEquals(rate, result.basisPoints());
    }

    @Test
    void chainNeverClampsInvalidRequests() {
        var chain = new DiscountChain();
        assertThrows(IllegalArgumentException.class, () -> chain.decide(100, -1));
        assertThrows(IllegalArgumentException.class, () -> chain.decide(100, 10001));
        assertThrows(IllegalArgumentException.class, () -> chain.decide(-1, 0));
    }
}
