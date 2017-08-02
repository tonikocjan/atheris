package compiler.ast.tree.def;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AbsDefs;
import compiler.ast.tree.type.AbsType;

import java.util.LinkedList;

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

    /** Conforming interfaces. */
    public final LinkedList<AbsType> conformances;

    /**
     *
     * @param pos
     * @param extendingType
     */
    public AbsExtensionDef(Position pos, String name, AbsType extendingType, AbsDefs definitions, LinkedList<AbsType> conformances) {
        super(pos, name);

        this.extendingType = extendingType;
        this.definitions = definitions;
        this.conformances = conformances;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
