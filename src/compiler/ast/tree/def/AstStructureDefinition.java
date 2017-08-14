package compiler.ast.tree.def;

import compiler.ast.tree.AstStatement;
import compiler.ast.tree.AstStatements;
import compiler.ast.tree.expr.AstBinaryExpression;
import compiler.ast.tree.expr.AstVariableNameExpression;
import compiler.ast.tree.type.AstType;
import utils.Constants;
import compiler.Position;
import compiler.ast.tree.AtomTypeKind;
import compiler.ast.tree.type.AstAtomType;

import java.util.ArrayList;

public class AstStructureDefinition extends AstClassDefinition {

    public AstStructureDefinition(String name, Position pos, AstType baseClass, ArrayList<AstDefinition> definitions, ArrayList<AstStatement> defaultConstructor, ArrayList<AstFunctionDefinition> constructors) {
        super(name, pos, baseClass, definitions, defaultConstructor, constructors);

        // Create implicit constructor with all members
        ArrayList<AstParameterDefinition> pars = new ArrayList<>();
        ArrayList<AstStatement> stmts = new ArrayList<>();

        for (AstDefinition def : definitions) {
            if (def instanceof AstVariableDefinition) {
                pars.add(new AstParameterDefinition(def.position, def.getName(), ((AstVariableDefinition) def).type));

                AstBinaryExpression initExpr = new AstBinaryExpression(
                        def.position,
                        AstBinaryExpression.ASSIGN,
                        new AstBinaryExpression(
                                def.position,
                                AstBinaryExpression.DOT,
                                new AstVariableNameExpression(
                                        def.position,
                                        Constants.selfParameterIdentifier),
                                new AstVariableNameExpression(
                                        def.position,
                                        def.getName())),
                        new AstVariableNameExpression(def.position, def.getName()));

                stmts.add(initExpr);
            }
        }

        AstFunctionDefinition constructorDef = new AstFunctionDefinition(
                pos,
                name, pars,
                new AstAtomType(
                        pos,
                        AtomTypeKind.VOID),
                new AstStatements(pos, stmts),
                true);
        constructorDef.setParentDefinition(this);

        boolean shouldAddImplicitConstructor = true;
        for (AstFunctionDefinition constructor : constructors) {
            if (constructor.getStringRepresentation().equals(constructorDef.getStringRepresentation())) {
                shouldAddImplicitConstructor = false;
                break;
            }
        }

        if (shouldAddImplicitConstructor) {
            constructors.add(constructorDef);
        }
    }

    @Override
    public String toString() {
        return "AstStructureDefinition " + position.toString() + ": " + getName();
    }
}
