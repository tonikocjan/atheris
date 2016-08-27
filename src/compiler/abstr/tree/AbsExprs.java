package compiler.abstr.tree;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.expr.AbsExpr;

/**
 * List of expressions..
 * 
 * @author toni kocjan
 */
public class AbsExprs extends AbsExpr {

	/** Expresisons. */
	public final LinkedList<AbsExpr> expressions;

	/**
	 * Create new expressions list.
	 * 
	 * @param pos
	 *            Position.
	 * @param exprs
	 *            Expressions.
	 */
	public AbsExprs(Position pos, LinkedList<AbsExpr> exprs) {
		super(pos);
		
		this.expressions = exprs;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
