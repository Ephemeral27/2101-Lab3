package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable-after-construction prefix trie over Unicode code points, ordered by catalog ID. */
public final class PrefixIndex {
    private static final class Node {
        private final Map<Integer, Node> children = new HashMap<>();
        private final List<Material> matches = new ArrayList<>();
    }

    private final Node root = new Node();

    /**
     * Builds an index from one validated snapshot, storing matches at every prefix node.
     *
     * @param snapshot source whose ID order defines deterministic result order
     */
    public PrefixIndex(Snapshot snapshot) {
        for (Material material : snapshot.materials()) {
            Node current = root;
            current.matches.add(material);
            for (int point : Material.searchKey(material.title()).codePoints().toArray()) {
                current = current.children.computeIfAbsent(point, unused -> new Node());
                current.matches.add(material);
            }
        }
    }

    /**
     * Finds the entire-title prefix; an empty key matches every indexed item.
     *
     * @param prefix raw query, normalized using Material.searchKey
     * @param limit nonnegative maximum results
     * @return detached immutable list, including an empty result for zero limit
     * @throws IllegalArgumentException for negative limits
     * @throws NullPointerException for a null query
     */
    public List<Material> search(String prefix, int limit) {
        // TODO T5: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T5");
    }
}
