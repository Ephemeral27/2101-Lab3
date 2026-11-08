package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;

/** Stateless Visitor collecting every material occurrence in depth-first order. */
public final class FlattenVisitor implements ComponentVisitor<List<Material>> {
    /** Creates a reusable stateless visitor. */
    public FlattenVisitor() {}

    @Override
    public List<Material> visit(Leaf leaf) {
        return List.of(leaf.material());
    }

    @Override
    public List<Material> visit(Bundle bundle) {
        // TODO T3: implement the documented contract; see the acceptance tests.
        List<Material> resultMaterials = new ArrayList<>();
        for (Component child: bundle.children()){
            resultMaterials.addAll(child.accept(this));
        }
        return resultMaterials;
        //throw new UnsupportedOperationException("TODO T3");
    }

    @Override
    public List<Material> visit(AddOn addOn) {
        return addOn.wrapped().accept(this);
    }
}
