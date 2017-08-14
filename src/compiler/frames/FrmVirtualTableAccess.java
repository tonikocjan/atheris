package compiler.frames;

import compiler.ast.tree.def.AstClassDefinition;

public class FrmVirtualTableAccess extends FrmAccess  {

    private static int heapOffset = 4;

    public final AstClassDefinition classDef;
    public final int size;
    public final int location;
    public final FrmLabel label;

    public FrmVirtualTableAccess(AstClassDefinition classDef, int size) {
        this.classDef = classDef;
        this.size = size;
        this.location = heapOffset;
        this.label = FrmLabel.newNamedLabel(classDef.getName());

        heapOffset += size;
    }

    @Override
    public String toString() {
        return null;
    }
}
