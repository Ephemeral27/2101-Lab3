package edu.cssd2101.lab03;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class RepositoryTest {
    @TempDir Path directory;

    private static Material m(String id) {
        return new Material(id, "Quotes \" \n 😀", Material.Kind.BOOK, Long.MAX_VALUE);
    }

    @Test
    void missingMeansEmptyAndRoundTripPreservesAllCategories() throws Exception {
        Path path = directory.resolve("nested/catalog.json");
        var repository = new JsonCatalogRepository(path);
        assertEquals(List.of(), repository.load());
        assertFalse(Files.exists(path));
        var values =
                java.util.Arrays.stream(Material.Kind.values())
                        .map(k -> new Material(k.name(), "é \"title\" \n 😀", k, 123))
                        .toList();
        repository.save(values);
        assertEquals(new Snapshot(0, values).materials(), repository.load());
        assertThrows(UnsupportedOperationException.class, () -> repository.load().clear());
        repository.save(List.of(m("A")));
        assertEquals(List.of(m("A")), repository.load());
        repository.save(List.of());
        assertEquals(List.of(), repository.load());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "{",
                "null",
                "[]",
                "{}",
                "{\"schema\":2,\"materials\":[]}",
                "{\"schema\":1,\"materials\":null}",
                "{\"schema\":1.0,\"materials\":[]}",
                "{\"schema\":1,\"materials\":[],\"extra\":0}",
                "{\"schema\":1,\"schema\":1,\"materials\":[]}",
                "{\"schema\":1,\"materials\":[]} {}",
                "{\"schema\":1,\"materials\":[null]}",
                "{\"schema\":1,\"materials\":[{\"id\":\"A\",\"title\":\"A\",\"kind\":\"BOOK\"}]}",
                "{\"schema\":1,\"materials\":[{\"id\":\"A\",\"title\":\"A\",\"kind\":\"NOPE\",\"cents\":1}]}",
                "{\"schema\":1,\"materials\":[{\"id\":\"A\",\"title\":\"A\",\"kind\":\"BOOK\",\"cents\":-1}]}",
                "{\"schema\":1,\"materials\":[{\"id\":\"A\",\"title\":\"A\",\"kind\":\"BOOK\",\"cents\":1.5}]}",
                "{\"schema\":1,\"materials\":[{\"id\":\"A\",\"title\":\"A\",\"kind\":\"BOOK\",\"cents\":9223372036854775808}]}",
                "{\"schema\":1,\"materials\":[{\"id\":7,\"title\":\"A\",\"kind\":\"BOOK\",\"cents\":1}]}"
            })
    void malformedExistingFilesFailWithoutChangingBytes(String json) throws Exception {
        Path path = directory.resolve("bad.json");
        Files.writeString(path, json);
        byte[] before = Files.readAllBytes(path);
        assertThrows(IOException.class, () -> new JsonCatalogRepository(path).load());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    void rejectsDuplicateNormalizedIds() throws Exception {
        Path path = directory.resolve("bad.json");
        String item = "{\"id\":\"%s\",\"title\":\"A\",\"kind\":\"BOOK\",\"cents\":1}";
        Files.writeString(
                path,
                "{\"schema\":1,\"materials\":["
                        + item.formatted("a")
                        + ","
                        + item.formatted("A")
                        + "]}");
        IOException failure =
                assertThrows(IOException.class, () -> new JsonCatalogRepository(path).load());
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test
    void invalidSaveAndFailedAtomicMovePreserveOldBytesAndCleanTemporaryFile() throws Exception {
        Path path = directory.resolve("catalog.json");
        var good = new JsonCatalogRepository(path);
        good.save(List.of(m("OLD")));
        byte[] before = Files.readAllBytes(path);
        assertThrows(IllegalArgumentException.class, () -> good.save(List.of(m("A"), m("a"))));
        assertArrayEquals(before, Files.readAllBytes(path));
        var broken =
                new JsonCatalogRepository(
                        path,
                        (source, target) -> {
                            throw new AtomicMoveNotSupportedException(
                                    source.toString(), target.toString(), "injected");
                        });
        assertThrows(AtomicMoveNotSupportedException.class, () -> broken.save(List.of(m("NEW"))));
        assertArrayEquals(before, Files.readAllBytes(path));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void oversizedInputAndOutputFailExplicitly() throws Exception {
        Path path = directory.resolve("catalog.json");
        var repository = new JsonCatalogRepository(path);
        Files.write(path, new byte[JsonCatalogRepository.MAX_BYTES + 1]);
        assertThrows(IOException.class, repository::load);
        byte[] before = Files.readAllBytes(path);
        var values = new java.util.ArrayList<Material>();
        for (int i = 0; i < 5000; i++) {
            values.add(new Material("ID" + i, "x".repeat(200), Material.Kind.BOOK, 1));
        }
        assertThrows(IOException.class, () -> repository.save(values));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    void unreadableShapeIsNotTreatedAsAnEmptyInventory() throws Exception {
        Path path = directory.resolve("directory");
        Files.createDirectory(path);
        assertThrows(IOException.class, () -> new JsonCatalogRepository(path).load());
    }
}
