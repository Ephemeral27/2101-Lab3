package edu.cssd2101.lab03;

/** Stateless shipping operation: books cost 250 cents, magazines 200, downloads zero. */
public final class ShippingVisitor implements ComponentVisitor<Long> {
    /** Creates a visitor with no mutable running total. */
    public ShippingVisitor() {}

    @Override
    public Long visit(Leaf leaf) {
        // TODO T3: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public Long visit(Bundle bundle) {
        // TODO T3: implement the documented contract; see the acceptance tests.
        throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public Long visit(AddOn addOn) {
        return addOn.wrapped().accept(this);
    }
}
