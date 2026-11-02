package edu.cssd2101.lab03;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Application use cases depending on three ports supplied by the composition root. */
public final class CatalogService {
    private final Catalog catalog;
    private final CatalogRepository repository;
    private final EventSink events;

    /**
     * Outcome distinguishes a committed mutation from observer delivery failures.
     *
     * @param changed whether inventory changed
     * @param observerFailures number of failed callbacks
     */
    public record Result(boolean changed, int observerFailures) {}

    /**
     * Borrows dependencies; this service does not close externally supplied resources.
     *
     * @param catalog inventory port
     * @param repository persistence port
     * @param events event publication port
     */
    public CatalogService(Catalog catalog, CatalogRepository repository, EventSink events) {
        this.catalog = Objects.requireNonNull(catalog);
        this.repository = Objects.requireNonNull(repository);
        this.events = Objects.requireNonNull(events);
    }

    /**
     * Adds and then publishes outside the catalog lock.
     *
     * @param material new value
     * @return mutation and delivery result
     */
    public Result add(Material material) {
        return report(catalog.add(material));
    }

    /**
     * Updates and then publishes outside the catalog lock.
     *
     * @param material replacement value
     * @return mutation and delivery result
     */
    public Result update(Material material) {
        return report(catalog.update(material));
    }

    /**
     * Removes and then publishes outside the catalog lock.
     *
     * @param id normalized or raw ID
     * @return mutation and delivery result
     */
    public Result remove(String id) {
        return report(catalog.remove(id));
    }

    private Result report(Optional<Change> change) {
        return change.map(value -> new Result(true, events.publish(value).size()))
                .orElse(new Result(false, 0));
    }

    /**
     * Exports a single snapshot; later concurrent edits are outside this save operation.
     *
     * @throws IOException if persistence fails; inventory remains unchanged
     */
    public void save() throws IOException {
        repository.save(catalog.snapshot().materials());
    }

    /**
     * Loads and validates before replacing the inventory; no per-material events are emitted. This
     * administrative operation requires callers to coordinate competing user edits.
     *
     * @return restored snapshot with a local version, never a persisted version
     * @throws IOException if loading fails; inventory remains unchanged
     */
    public Snapshot restore() throws IOException {
        return catalog.replaceAll(repository.load());
    }
}
