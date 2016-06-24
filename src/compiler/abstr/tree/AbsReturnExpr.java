package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsReturnExpr extends AbsExpr {

	/** Expression, ki ga stavek vrača */
	public AbsExpr expr = null; 
	
	public AbsReturnExpr(Position pos, AbsExpr expr) {
		super(pos);
		
		this.expr = expr;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
