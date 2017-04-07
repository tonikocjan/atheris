package compiler.frames;

import compiler.abstr.tree.def.AbsClassDef;

/**
 * Created by toni on 06/04/2017.
 */
public class FrmVirtualTableAccess extends FrmAccess  {

    /**
     *
     */
    public final AbsClassDef classDef;


    public FrmVirtualTableAccess(AbsClassDef classDef) {
        this.classDef = classDef;
    }

    @Override
    public String toString() {
        return null;
    }
}
