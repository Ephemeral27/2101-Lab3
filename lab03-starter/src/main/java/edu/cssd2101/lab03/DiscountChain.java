package edu.cssd2101.lab03;

import java.util.Optional;


public final class DiscountChain {
    private record Handler(String role, int maximum, Handler next) {
        Optional<String> approve(int rate) {
            if (rate <= maximum) {
                return Optional.of(role);
            }
            return next == null ? Optional.empty() : next.approve(rate);
        }
    }

    private final Handler first =
            new Handler(
                    "Manager", 1500, new Handler("Director", 2500, new Handler("VP", 4000, null)));

    
    public DiscountChain() {}

    /**
     * Decision retains the requested rate and final price without mutating any material.
     *
     * @param approvedBy approving role, or empty when declined
     * @param basisPoints original requested rate
     * @param finalCents approved discounted price, or original price when declined
     */
    public record Decision(Optional<String> approvedBy, int basisPoints, long finalCents) {}

    /**
     * Selects the first authorized handler; invalid input is rejected rather than clamped.
     *
     * @param priceCents nonnegative original price
     * @param basisPoints request in 0..10000
     * @return immutable decision
     * @throws IllegalArgumentException for an invalid amount or rate
     */
    public Decision decide(long priceCents, int basisPoints) {
        if (priceCents < 0) {
            throw new IllegalArgumentException("negative price");
        }
        if (basisPoints < 0 || basisPoints > 10000) {
            throw new IllegalArgumentException("basisPoints must be 0..10000");
        }
        Optional<String> approver = first.approve(basisPoints);
        long finalCents = approver.isPresent()
                ? Money.discounted(priceCents, basisPoints)
                : priceCents;
        return new Decision(approver, basisPoints, finalCents);
    }
}