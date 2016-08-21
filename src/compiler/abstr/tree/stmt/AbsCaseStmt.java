package compiler.abstr.tree.stmt;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.expr.AbsExpr;

public class AbsCaseStmt extends AbsStmt {

	/**
	 * Expressions.
	 */
	public final LinkedList<AbsExpr> exprs;
	
	/**
	 * Body to be executed if expression matches parent's subjectExpr.
	 */
	public final AbsStmts body;
	
	/**
	 * 
	 * @param pos
	 */
	public AbsCaseStmt(Position pos, LinkedList<AbsExpr> exprs, AbsStmts body) {
		super(pos);
		
		this.exprs = exprs;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
