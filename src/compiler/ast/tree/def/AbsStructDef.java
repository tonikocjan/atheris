package compiler.ast.tree.def;

import utils.Constants;
import compiler.Position;
import compiler.ast.tree.AbsStmt;
import compiler.ast.tree.AbsStmts;
import compiler.ast.tree.AtomTypeKind;
import compiler.ast.tree.expr.AbsBinExpr;
import compiler.ast.tree.expr.AbsVarNameExpr;
import compiler.ast.tree.type.AbsAtomType;
import compiler.ast.tree.type.AbsType;

import java.util.ArrayList;
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
    public AbsStructDef(String name, Position pos, AbsType baseClass, ArrayList<AbsDef> definitions, ArrayList<AbsStmt> defaultConstructor, ArrayList<AbsFunDef> constructors) {
        super(name, pos, baseClass, definitions, defaultConstructor, constructors);

        // Create implicit constructor with all members
        ArrayList<AbsParDef> pars = new ArrayList<>();
        ArrayList<AbsStmt> stmts = new ArrayList<>();

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
