package compiler.abstr.tree.def;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.type.AbsType;

/**
 *
 */
public class AbsExtensionDef extends AbsDef {

    /**
     *
     */
    public final AbsType extendingType;

    /**
     * New definitions.
     */
    public final AbsDefs definitions;

    /**
     *
     * @param pos
     * @param extendingType
     */
    public AbsExtensionDef(Position pos, AbsType extendingType, AbsDefs definitions) {
        super(pos, "");

        this.extendingType = extendingType;
        this.definitions = definitions;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
