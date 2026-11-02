package edu.cssd2101.lab03;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable catalog value. Equality compares every field; identity is the normalized ID.
 *
 * @param id case-insensitive ASCII ID, one to 32 letters, digits, or hyphens
 * @param title nonblank display title, at most 200 UTF-16 code units after stripping
 * @param kind distribution category
 * @param cents nonnegative price in cents
 */
public record Material(String id, String title, Kind kind, long cents) {
    /** Distribution categories; audio and video mean downloads in this model. */
    public enum Kind {
        /** Physical printed book. */
        BOOK,
        /** Downloadable electronic book. */
        EBOOK,
        /** Downloadable audio recording. */
        AUDIO,
        /** Downloadable video. */
        VIDEO,
        /** Physical periodical. */
        MAGAZINE
    }

    /**
     * Validates and normalizes a material before it becomes observable.
     *
     * @param id catalog ID
     * @param title display title
     * @param kind category
     * @param cents nonnegative price
     * @throws IllegalArgumentException for an invalid ID, title, or price
     * @throws NullPointerException for null text or kind
     */
    public Material {
        id = normalizeId(id);
        title = Objects.requireNonNull(title, "title").strip();
        if (title.isEmpty() || title.length() > 200) {
            throw new IllegalArgumentException("title must contain 1..200 code units");
        }
        Objects.requireNonNull(kind, "kind");
        if (cents < 0) {
            throw new IllegalArgumentException("negative cents");
        }
    }

    /**
     * Canonicalizes an ID using ASCII validation and locale-independent upper case.
     *
     * @param id raw identifier
     * @return normalized identifier
     * @throws IllegalArgumentException if the stripped ID does not match the policy
     * @throws NullPointerException if id is null
     */
    public static String normalizeId(String id) {
        String value = Objects.requireNonNull(id, "id").strip();
        if (!value.matches("[A-Za-z0-9-]{1,32}")) {
            throw new IllegalArgumentException("invalid ID");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * Produces an NFC, stripped, locale-independent lower-case search key. Internal whitespace and
     * accents remain significant; blank means match all.
     *
     * @param text query or title
     * @return canonical key
     * @throws NullPointerException if text is null
     */
    public static String searchKey(String text) {
        String lowered = Objects.requireNonNull(text, "text").strip().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lowered, Normalizer.Form.NFC);
    }
}
