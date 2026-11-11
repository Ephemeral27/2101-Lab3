package edu.cssd2101.lab03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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


    /**

     * Demonstrates component composition, discount approval,

     * catalog events, searching, caching, and persistence.

     *

     * @throws IOException if temporary storage operations fail

     */

    private static void demonstrateT7() throws IOException {

        // Create materials for the T7 demonstration

        Material book = MaterialFactory.create("book", "B1", "Java Patterns", "24.99");

        Material ebook = MaterialFactory.create("ebook", "E1", "Java Programming", "20.00");

        // 2. Demonstrate component composition

        Component leaf = new Leaf(book);

        Component wrapped = new AddOn("Gift wrap", leaf, 900);

        System.out.println("wrapped cents=" + wrapped.priceCents());

        // 3. Create a discounted bundle

        Bundle bundle = new BundleBuilder("Java bundle")

                .add(new Leaf(book))

                .add(new Leaf(ebook))

                .discount(3000)

                .build();

        System.out.println("bundle cents=" + bundle.priceCents());

        // 4. Demonstrate shipping visitor

        long shipping = wrapped.accept(new ShippingVisitor());

        System.out.println("shipping cents=" + shipping);

        // 5. Demonstrate discount approval

        DiscountChain approvals = new DiscountChain();

        DiscountChain.Decision decision = approvals.decide(book.cents(), 2000);

        System.out.println("approval=" + decision.approvedBy().orElse("Declined"));

        // 6. Create catalog and event service

        InMemoryCatalog catalog = new InMemoryCatalog();

        Events events = new Events();

        var subscription = events.subscribe(

                change -> System.out.println("event version=" + change.version()));

        CatalogService service = new CatalogService(catalog, new JsonCatalogRepository(Path.of("catalog.json")), events);

        try {

            service.add(book);

            service.add(ebook);

        } finally {

            subscription.close();

        }

        // 7. Demonstrate prefix search and cache

        SearchService search = new SearchService(catalog, 3);

        List<Material> firstSearch = search.search("java", 10);

        search.search("java", 10);

        System.out.println(

                "prefix IDs=" +

                        firstSearch.stream()

                                .map(Material::id)

                                .toList()

        );

        LruCache.Stats stats = search.cacheStats();

        System.out.println(

                "cache hits=" + stats.hits()

                        + " misses=" + stats.misses()

        );

        // 8. Demonstrate persistence using a temporary directory

        Path directory = Files.createTempDirectory("lab03-demo-");

        try {

            Path file = directory.resolve("catalog.json");

            CatalogRepository repository = new JsonCatalogRepository(file);

            repository.save(catalog.snapshot().materials());

            InMemoryCatalog restored = new InMemoryCatalog();

            restored.replaceAll(repository.load());

            System.out.println("restored IDs=" + restored.snapshot().materials()

                                    .stream()

                                    .map(Material::id)

                                    .toList()

            );

            System.out.println("snapshot version=" + catalog.snapshot().version());

        } finally {

            Files.deleteIfExists(directory.resolve("catalog.json"));

            Files.deleteIfExists(directory);

        }

    }

}





