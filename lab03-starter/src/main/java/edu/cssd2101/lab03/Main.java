package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;

/** Useful starting demonstration; extension methods are introduced by the milestones. */
public final class Main {
    private Main() {}

    /**
     * Creates validated values and a detached snapshot without invoking unfinished tasks.
     *
     * @param args unused arguments
     */
    public static void main(String[] args) {
        var store = new InMemoryCatalog();
        Material book = MaterialFactory.create("book", " b1 ", "Java Patterns", "24.99");
        store.add(book);
        System.out.println(
                "starter IDs=" + store.snapshot().materials().stream().map(Material::id).toList());
        System.out.println("leaf cents=" + new Leaf(book).priceCents());
        System.out.println("snapshot version=" + store.snapshot().version());
        System.out.println("Complete milestones T1-T7; acceptance tests are separate.");

        Material book1 = MaterialFactory.create("book", " b1 ", "Book 1", "24.99");
        Material book2 = MaterialFactory.create("book", " b2 ", "Book 2", "24.99");
        Material video = MaterialFactory.create("video", " v1 ", "Video 1", "12.99");
        Leaf leaf1 = new Leaf(book1);
        Leaf leaf2 = new Leaf(book2);
        Leaf leaf3 = new Leaf(video);
        List<Component> innerBundleList = new ArrayList<>();
        innerBundleList.add(leaf2);
        innerBundleList.add(leaf3);
        Bundle innerBundle = new Bundle("inner bundle", innerBundleList, 1000);
        AddOn innerAddOn = new AddOn("add on", leaf1,200);
        List<Component> outerBundleList = new ArrayList<>();
        outerBundleList.add(innerBundle);
        outerBundleList.add(innerAddOn);
        Bundle outerBundle = new Bundle("outer bundle", outerBundleList, 0);

        //test pricing
        System.out.println(outerBundle.priceCents());
        //test shipping
        ShippingVisitor visitor = new ShippingVisitor();
        System.out.println(visitor.visit(outerBundle));
        //test leaf order
        FlattenVisitor flatVisitor = new FlattenVisitor();
        System.out.println(flatVisitor.visit(outerBundle));
    }
}
