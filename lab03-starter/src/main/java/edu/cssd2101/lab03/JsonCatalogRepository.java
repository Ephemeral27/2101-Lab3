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
import java.util.*;

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
     * Configures a repository without creating or truncating storage.
     *
     * @param path trusted destination, resolved to an absolute normalized path
     * @throws IllegalArgumentException if the path has no filename
     * @throws NullPointerException if path is null
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
            throw new IllegalArgumentException("destination needs a filename");
        }
    }


    public static void requireExactFields(
            JsonNode node,
            Set<String> expectedFields
    ) throws IOException {

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
        // TODO T6: implement the documented contract; see the acceptance tests.
        //throw new UnsupportedOperationException("TODO T6");
            byte[] bytes;

            // 1. Read the file, enforcing the size limit.
            try (InputStream in = Files.newInputStream(path)) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }

            if (bytes.length > MAX_BYTES) {
                throw new IOException("File exceeds size limit");
            }

            // 2. Parse the JSON.
            JsonNode root = mapper.readTree(bytes);

            if (root == null || !root.isObject()) {
                throw new IOException("Expected a JSON object");
            }

            // 3. Validate the root fields.
            requireExactFields(root, Set.of("materials"));

            JsonNode materialsNode = root.get("materials");

            if (materialsNode == null || !materialsNode.isArray()) {
                throw new IOException("Expected materials to be an array");
            }

            // 4. Parse and validate every material.
            List<Material> materials = new ArrayList<>();
            Set<String> ids = new HashSet<>();

            for (JsonNode node : materialsNode) {
                if (!node.isObject()) {
                    throw new IOException("Expected a material object");
                }

                requireExactFields(node, Set.of("id", "title", "kind", "cents"));

                JsonNode idNode = node.get("id");
                JsonNode titleNode = node.get("title");
                JsonNode kindNode = node.get("kind");
                JsonNode centsNode = node.get("cents");

                if (!idNode.isTextual() || !titleNode.isTextual() || !kindNode.isTextual()
                        || !centsNode.isIntegralNumber() || !centsNode.canConvertToLong()) {
                    throw new IOException("Invalid material field types");
                }

                long cents = centsNode.longValue();

                if (cents < 0) {
                    throw new IOException("Cents cannot be negative");
                }

                Material material;

                try {
                    Material.Kind kind = Material.Kind.valueOf(kindNode.textValue());

                    material = new Material(idNode.textValue(), titleNode.textValue(), kind, cents);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid material data", e);
                }

                if (!ids.add(material.id())) {
                    throw new IOException("Duplicate material ID");
                }

                materials.add(material);
            }

            // 5. Sort and return.
            materials.sort(Comparator.comparing(Material::id));

            return materials;
        }






    private static void fields(JsonNode node, Set<String> expected) throws IOException {
        if (node == null || !node.isObject()) {
            throw new IOException("expected JSON object");
        }
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        if (!names.equals(expected)) {
            throw new IOException("missing or unknown fields");
        }
    }

    @Override
    public synchronized void save(List<Material> materials) throws IOException {
        // TODO T6: implement the documented contract; see the acceptance tests.
        //throw new UnsupportedOperationException("TODO T6");
            // 1. Validate the list and create a copy.
            if (materials == null) {
                throw new IOException("Materials list cannot be null");
            }

            List<Material> copy = new ArrayList<>(materials);

            // 2. Check for null materials and duplicate IDs.
            Set<String> ids = new HashSet<>();

            for (Material material : copy) {

                if (material == null) {
                    throw new IOException("Material cannot be null");
                }

                if (!ids.add(material.id())) {
                    throw new IOException("Duplicate material ID");
                }
            }

            // 3. Sort the materials by ID.
            copy.sort(Comparator.comparing(Material::id));

            // 4. Create the JSON structure.
            ObjectNode root = mapper.createObjectNode();

            ArrayNode array = root.putArray("materials");

            for (Material material : copy) {

                ObjectNode item = array.addObject();

                item.put("id", material.id());
                item.put("title", material.title());
                item.put("kind", material.kind().name());
                item.put("cents", material.cents());
            }

            // 5. Convert the JSON structure into bytes.
            byte[] jsonBytes = mapper.writeValueAsBytes(root);

            // 6. Check the file size.
            if (jsonBytes.length > MAX_BYTES) {
                throw new IOException("File exceeds size limit");
            }

            // 7. Create a temporary file in the same directory.
            Path parent = path.getParent();

            Path temp = Files.createTempFile(
                    parent,
                    "catalog-",
                    ".tmp"
            );

            boolean moved = false;

            try {

                // 8. Write the JSON into the temporary file.
                Files.write(temp, jsonBytes);

                // 9. Replace the original file atomically.
                mover.move(temp, path);

                moved = true;

            } finally {

                // 10. Delete the temporary file if the move failed.
                if (!moved) {
                    Files.deleteIfExists(temp);
                }
            }
        }

    }

