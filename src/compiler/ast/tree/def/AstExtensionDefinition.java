package compiler.ast.tree.def;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AstDefinitions;
import compiler.ast.tree.type.AstType;

import java.util.List;

public class AstExtensionDefinition extends AstTypeDefinition {

    public final AstType extendingType;
    public final AstDefinitions definitions;
    public final List<AstType> conformances;

    public AstExtensionDefinition(Position pos, String name, AstType extendingType, AstDefinitions definitions, List<AstType> conformances) {
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
