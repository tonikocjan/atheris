package compiler.ast.tree.def;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AbsDefs;

/**
 * Interface.
 */
public class AbsInterfaceDef extends AbsTypeDef {

    /** Interface definitions. */
    public final AbsDefs definitions;

    /**
     *
     * @param pos
     * @param name
     */
    public AbsInterfaceDef(Position pos, String name, AbsDefs definitions) {
        super(pos, name);

        this.definitions = definitions;
    }

    @Override
    public void accept(ASTVisitor aSTVisitor) {
        aSTVisitor.visit(this);
    }
}
