package compiler.frames;

import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.seman.type.CanType;

/**
 *
 */
public class FrmStaticAccess implements FrmAccess {

    /**
     * Member definition.
     */
    private final AbsVarDef staticMember;

    /**
     * Parent type.
     */
    private final CanType parentType;

    /**
     *
     * @param staticMember
     * @param parentType
     */
    public FrmStaticAccess(AbsVarDef staticMember, CanType parentType) {
        this.staticMember = staticMember;
        this.parentType = parentType;
    }

    /**
     *
     * @return
     */
    public int offset() {
        return parentType.offsetForStaticMember(staticMember.getName());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "Todo";
    }
}
