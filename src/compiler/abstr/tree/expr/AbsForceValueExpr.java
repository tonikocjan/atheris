package compiler.abstr.tree.expr;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsForceValueExpr extends AbsExpr {

	/**
	 * 
	 */
	public final AbsExpr subExpr;
	
	/**
	 * 
	 * @param pos
	 * @param subExpr
	 */
	public AbsForceValueExpr(Position pos, AbsExpr subExpr) {
		super(pos);
		
		this.subExpr = subExpr;
	}

	/**
	 * 
	 */
	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}
	
}
