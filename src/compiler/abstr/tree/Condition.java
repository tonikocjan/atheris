package compiler.abstr.tree;

import compiler.abstr.tree.expr.AbsExpr;

/**
 * Simple structure holding condition and body which executed 
 * when condition is positive.
 */
public class Condition {
	
	/** Condition. */
	public final AbsExpr cond;
	
	/** Positive branch */
	public final AbsStmts body;
	
	public Condition(AbsExpr cond, AbsStmts body) {
		this.cond = cond;
		this.body = body;
	}
	
}