package compiler.ast.tree.def;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AstDefinitions;

public class AstInterfaceDefinition extends AstTypeDefinition {

    public final AstDefinitions definitions;

    public AstInterfaceDefinition(Position pos, String name, AstDefinitions definitions) {
        super(pos, name);

        this.definitions = definitions;
    }

    @Override
    public void accept(ASTVisitor aSTVisitor) {
        aSTVisitor.visit(this);
    }
}
