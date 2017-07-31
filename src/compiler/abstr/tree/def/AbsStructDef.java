package compiler.abstr.tree.def;

import utils.Constants;
import compiler.Position;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsType;

import java.util.LinkedList;

/**
 *
 */
public class AbsStructDef extends AbsClassDef {

    /**
     *
     * @param name
     * @param pos
     * @param baseClass
     * @param definitions
     * @param defaultConstructor
     * @param constructors
     */
    public AbsStructDef(String name, Position pos, AbsType baseClass, LinkedList<AbsDef> definitions, LinkedList<AbsStmt> defaultConstructor, LinkedList<AbsFunDef> constructors) {
        super(name, pos, baseClass, definitions, defaultConstructor, constructors);

        // Create implicit constructor with all members
        LinkedList<AbsParDef> pars = new LinkedList<>();
        LinkedList<AbsStmt> stmts = new LinkedList<>();

        for (AbsDef def : definitions) {
            if (def instanceof AbsVarDef) {
                pars.add(new AbsParDef(def.position, def.getName(), ((AbsVarDef) def).type));

                AbsBinExpr initExpr = new AbsBinExpr(
                        def.position,
                        AbsBinExpr.ASSIGN,
                        new AbsBinExpr(
                                def.position,
                                AbsBinExpr.DOT,
                                new AbsVarNameExpr(
                                        def.position,
                                        Constants.selfParameterIdentifier),
                                new AbsVarNameExpr(
                                        def.position,
                                        def.getName())),
                        new AbsVarNameExpr(def.position, def.getName()));

                stmts.add(initExpr);
            }
        }

        AbsFunDef constructorDef = new AbsFunDef(
                pos,
                name, pars,
                new AbsAtomType(
                        pos,
                        AtomTypeKind.VOID),
                new AbsStmts(pos, stmts),
                true);
        constructorDef.setParentDefinition(this);

        boolean shouldAddImplicitConstructor = true;
        for (AbsFunDef constructor : constructors) {
            if (constructor.getStringRepresentation().equals(constructorDef.getStringRepresentation())) {
                shouldAddImplicitConstructor = false;
                break;
            }
        }

        if (shouldAddImplicitConstructor) {
            constructors.add(constructorDef);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "AbsStructDef " + position.toString() + ": " + getName();
    }
}
