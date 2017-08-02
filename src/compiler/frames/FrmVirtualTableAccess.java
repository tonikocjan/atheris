package compiler.frames;

import compiler.ast.tree.def.AbsClassDef;

/**
 * Created by toni on 06/04/2017.
 */
public class FrmVirtualTableAccess implements FrmAccess  {

    private static int heapOffset = 4;

    /**
     *
     */
    public final AbsClassDef classDef;
    public final int size;
    public final int location;

    public final FrmLabel label;

    public FrmVirtualTableAccess(AbsClassDef classDef, int size) {
        this.classDef = classDef;
        this.size = size;
        this.location = heapOffset;
        this.label = FrmLabel.newLabel(classDef.getName());

        heapOffset += size;
    }

    @Override
    public String toString() {
        return null;
    }
}
