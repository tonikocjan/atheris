package compiler.abstr.tree.expr;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsReturnExpr extends AbsExpr {

	/** Expression, ki ga stavek vrača */
	public AbsExpr expr = null; 
	
	public AbsReturnExpr(Position pos, AbsExpr expr) {
		super(pos);
		
		this.expr = expr;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
