package compiler.abstr.tree.stmt;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.expr.AbsExpr;

public class AbsCaseStmt extends AbsStmt {

	/**
	 * Expression.
	 */
	public final AbsExpr expr;
	
	/**
	 * Body to be executed if expression matches parent's subjectExpr.
	 */
	public final AbsStmts body;
	
	/**
	 * 
	 * @param pos
	 */
	public AbsCaseStmt(Position pos, AbsExpr expr, AbsStmts body) {
		super(pos);
		
		this.expr = expr;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
