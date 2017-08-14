package compiler.ast.tree.type;

import compiler.Position;
import compiler.ast.ASTVisitor;

public class AstOptionalType extends AstType {

	public final AstType childType;
	public final boolean isForced;

	public AstOptionalType(Position pos, AstType childType, boolean isForced) {
		super(pos);
		
		this.childType = childType;
		this.isForced = isForced;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

    @Override
    public String getName() {
        return childType.getName();
    }
}
