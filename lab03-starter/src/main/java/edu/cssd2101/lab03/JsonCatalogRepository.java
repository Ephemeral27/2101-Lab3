package edu.cssd2101.lab03;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Local JSON snapshot adapter with strict schema and atomic replacement, or an explicit failure.
 * The caller owns the trusted destination path. This adapter opens and closes its own streams; it
 * has no executor or long-lived handle. One instance serializes its own operations, but independent
 * instances/processes require external coordination. Atomic rename is not fsync.
 */
public final class JsonCatalogRepository implements CatalogRepository {

    /** Maximum encoded document size: one mebibyte. */
    public static final int MAX_BYTES = 1_048_576;

    @FunctionalInterface
    interface Mover {
        void move(Path source, Path target) throws IOException;
    }

    private final Path path;
    private final Mover mover;

    private final ObjectMapper mapper =
            new ObjectMapper(
                    JsonFactory.builder()
                            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                            .build())
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * Creates a repository for the supplied destination.
     *
     * @param path destination path
     */
    public JsonCatalogRepository(Path path) {
        this(
                path,
                (source, target) ->
                        Files.move(
                                source,
                                target,
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING));
    }

    JsonCatalogRepository(Path path, Mover mover) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.mover = Objects.requireNonNull(mover);

        if (this.path.getFileName() == null) {
            throw new IllegalArgumentException("Destination needs a filename");
        }
    }

    public static void requireExactFields(
            JsonNode node,
            Set<String> expectedFields
    ) throws IOException {

        if (node == null || !node.isObject()) {
            throw new IOException("Expected a JSON object");
        }

        Set<String> actualFields = new HashSet<>();

        Iterator<String> names = node.fieldNames();

        while (names.hasNext()) {
            actualFields.add(names.next());
        }

        if (!actualFields.equals(expectedFields)) {
            throw new IOException("Invalid JSON fields");
        }
    }

    @Override
    public synchronized List<Material> load() throws IOException {

        // A missing file represents an empty inventory.
        if (Files.notExists(path)) {
            return List.of();
        }

        byte[] bytes;

        try (InputStream in = Files.newInputStream(path)) {
            bytes = in.readNBytes(MAX_BYTES + 1);
        }

        if (bytes.length > MAX_BYTES) {
            throw new IOException("File exceeds size limit");
        }

        JsonNode root = mapper.readTree(bytes);

        if (root == null || !root.isObject()) {
            throw new IOException("Expected a JSON object");
        }

        requireExactFields(root, Set.of("schema", "materials"));

        JsonNode schemaNode = root.get("schema");
        JsonNode materialsNode = root.get("materials");

        if (schemaNode == null
                || !schemaNode.isIntegralNumber()
                || !schemaNode.canConvertToInt()
                || schemaNode.intValue() != 1) {
            throw new IOException("Unsupported JSON schema");
        }

        if (materialsNode == null || !materialsNode.isArray()) {
            throw new IOException("Expected materials to be an array");
        }

        List<Material> materials = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (JsonNode node : materialsNode) {

            requireExactFields(
                    node,
                    Set.of("id", "title", "kind", "cents"));

            JsonNode idNode = node.get("id");
            JsonNode titleNode = node.get("title");
            JsonNode kindNode = node.get("kind");
            JsonNode centsNode = node.get("cents");

            if (!idNode.isTextual()
                    || !titleNode.isTextual()
                    || !kindNode.isTextual()
                    || !centsNode.isIntegralNumber()
                    || !centsNode.canConvertToLong()) {
                throw new IOException("Invalid material field types");
            }

            long cents = centsNode.longValue();

            if (cents < 0) {
                throw new IOException("Cents cannot be negative");
            }

            Material material;

            try {
                Material.Kind kind =
                        Material.Kind.valueOf(kindNode.textValue());

                material = new Material(
                        idNode.textValue(),
                        titleNode.textValue(),
                        kind,
                        cents);

            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid material data", e);
            }

            if (!ids.add(material.id())) {
                throw new IOException(
                        "Duplicate material ID",
                        new IllegalArgumentException("Duplicate ID: " + material.id()));
            }

            materials.add(material);
        }

        materials.sort(Comparator.comparing(Material::id));

        return List.copyOf(materials);
    }

    @Override
    public synchronized void save(List<Material> materials)
            throws IOException {

        if (materials == null) {
            throw new IllegalArgumentException("Materials cannot be null");
        }

        List<Material> copy = new ArrayList<>(materials);
        Set<String> ids = new HashSet<>();

        for (Material material : copy) {

            if (material == null) {
                throw new IllegalArgumentException("Material cannot be null");
            }

            if (!ids.add(material.id())) {
                throw new IllegalArgumentException(
                        "Duplicate material ID: " + material.id());
            }
        }

        copy.sort(Comparator.comparing(Material::id));

        ObjectNode root = mapper.createObjectNode();

        root.put("schema", 1);

        ArrayNode array = root.putArray("materials");

        for (Material material : copy) {

            ObjectNode item = array.addObject();

            item.put("id", material.id());
            item.put("title", material.title());
            item.put("kind", material.kind().name());
            item.put("cents", material.cents());
        }

        byte[] jsonBytes = mapper.writeValueAsBytes(root);

        if (jsonBytes.length > MAX_BYTES) {
            throw new IOException("File exceeds size limit");
        }

        Path parent = path.getParent();

        Files.createDirectories(parent);

        Path temp = Files.createTempFile(
                parent,
                "catalog-",
                ".tmp");

        boolean moved = false;

        try {
            Files.write(temp, jsonBytes);

            mover.move(temp, path);

            moved = true;

        } finally {
            if (!moved) {
                Files.deleteIfExists(temp);
            }
        }
    }
}