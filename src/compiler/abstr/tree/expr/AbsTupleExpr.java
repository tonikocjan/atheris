package compiler.abstr.tree.expr;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsExprs;

public class AbsTupleExpr extends AbsExpr {

	/**
	 * Expressions inside tuple expression.
	 */
	public final AbsExprs expressions;
	
	/**
	 * Create new tuple expression.
	 * @param pos
	 * @param expressions
	 */
	public AbsTupleExpr(Position pos, LinkedList<AbsExpr> expressions) {
		super(pos);
		
		// TODO: position
		this.expressions = new AbsExprs(position, expressions);
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
