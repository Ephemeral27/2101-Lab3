import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static edu.cssd2101.lab03.JsonCatalogRepository.requireExactFields;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.ObjectMapper;



public class JsonCatalogRepositoryTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsExactFields() throws IOException {
        JsonNode node = mapper.readTree("""
        {
            "id": "BK001",
            "title": "Java",
            "kind": "BOOK",
            "cents": 2500
        }
        """);

        assertDoesNotThrow(() ->
                requireExactFields(
                        node,
                        Set.of("id", "title", "kind", "cents")
                )
        );
    }

    @Test
    void rejectsMissingField() throws IOException {
        JsonNode node = mapper.readTree("""
        {
            "id": "BK001",
            "title": "Java",
            "kind": "BOOK"
        }
        """);

        assertThrows(IOException.class, () ->
                requireExactFields(
                        node,
                        Set.of("id", "title", "kind", "cents")
                )
        );
    }

    @Test
    void rejectsUnexpectedField() throws IOException {
        JsonNode node = mapper.readTree("""
        {
            "id": "BK001",
            "title": "Java",
            "kind": "BOOK",
            "cents": 2500,
            "author": "John"
        }
        """);

        assertThrows(IOException.class, () ->
                requireExactFields(
                        node,
                        Set.of("id", "title", "kind", "cents")
                )
        );
    }


}
