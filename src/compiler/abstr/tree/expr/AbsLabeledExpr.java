package compiler.abstr.tree.expr;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsLabeledExpr extends AbsExpr {
	
	/**
	 * Name of expression.
	 */
	public final String name;
	
	/**
	 * Expression.
	 */
	public final AbsExpr expr;

	/**
	 * Create new labeled (named) expr.
	 * @param pos
	 * @param expr
	 * @param name
	 */
	public AbsLabeledExpr(Position pos, AbsExpr expr, String name) {
		super(pos);
		
		this.expr = expr;
		this.name = name;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
