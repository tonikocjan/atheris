package compiler.abstr.tree.def;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;

/**
 *
 */
public class AbsExtensionDef extends AbsTypeDef {

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
    public AbsExtensionDef(Position pos, String name, AbsType extendingType, AbsDefs definitions) {
        super(pos, name);

        this.extendingType = extendingType;
        this.definitions = definitions;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
