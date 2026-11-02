package edu.cssd2101.lab03;

import java.util.Locale;
import java.util.Objects;

/** Converts external text into validated immutable domain values. */
public final class MaterialFactory {
    private MaterialFactory() {}

    /**
     * Creates one of the five supported material categories.
     *
     * @param kind category name, ignoring surrounding whitespace and case
     * @param id catalog identifier
     * @param title display title
     * @param dollars decimal dollar text
     * @return validated material
     * @throws IllegalArgumentException if any text or amount is invalid
     * @throws NullPointerException if an argument is null
     * @throws ArithmeticException if the rounded cents overflow
     */
    public static Material create(String kind, String id, String title, String dollars) {
        Material.Kind category =
                Material.Kind.valueOf(
                        Objects.requireNonNull(kind).strip().toUpperCase(Locale.ROOT));
        return new Material(id, title, category, Money.cents(dollars));
    }
}
