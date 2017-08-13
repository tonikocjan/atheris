package compiler.frames;

import compiler.ast.tree.def.AbsVarDef;
import compiler.seman.type.CanType;

/**
 *
 */
public class FrmStaticAccess extends FrmAccess {

    private final AbsVarDef staticMemberDefinition;
    private final CanType parentType;

    public FrmStaticAccess(AbsVarDef staticMember, CanType parentType) {
        this.staticMemberDefinition = staticMember;
        this.parentType = parentType;
    }

    public int offsetForStaticMember() {
        return parentType.offsetForStaticMember(staticMemberDefinition.getName());
    }

    @Override
    public String toString() {
        return "Todo";
    }
}
