package edu.cssd2101.lab03;

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
    }
}
