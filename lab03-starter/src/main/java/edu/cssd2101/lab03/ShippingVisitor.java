package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;

/** Stateless shipping operation: books cost 250 cents, magazines 200, downloads zero. */
public final class ShippingVisitor implements ComponentVisitor<Long> {
    /** Creates a visitor with no mutable running total. */
    public ShippingVisitor() {}

    @Override
    public Long visit(Leaf leaf) {
        // TODO T3: implement the documented contract; see the acceptance tests.

        Material leafMaterial = leaf.material();
        if(leafMaterial.kind() == Material.Kind.BOOK){
            return 250L;
        }
        else if ((leafMaterial.kind() == Material.Kind.EBOOK) || (leafMaterial.kind() == Material.Kind.AUDIO) || (leafMaterial.kind() == Material.Kind.VIDEO)){
            return 0L;
        }
        else{
            return 200L;
        }
        //throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public Long visit(Bundle bundle) {
        // TODO T3: implement the documented contract; see the acceptance tests.
        //List<Material> items = new ArrayList<>();
        long total = 0L;
        for (Component child: bundle.children()){
            total = Math.addExact(total,child.accept(this));
        }
        return total;
        //throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public Long visit(AddOn addOn) {
        return addOn.wrapped().accept(this);
    }
}
