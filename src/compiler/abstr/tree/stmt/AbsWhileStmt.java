package compiler.abstr.tree.stmt;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.expr.AbsExpr;

/**
 * Zanka brez eksplicitnega stevca.
 * 
 * @author sliva
 */
public class AbsWhileStmt extends AbsConditionalStmt {
	
	/** Pogoj. */
	public final AbsExpr cond;
	
	/** Jedro zanke. */
	public final AbsStmts body;
		
	/**
	 * Ustvari novo zanko brez eksplicitnega stevca.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param cond
	 *            Pogoj.
	 * @param body
	 *            Jedro zanke.
	 */
	public AbsWhileStmt(Position pos, AbsExpr cond, AbsStmts body) {
		super(pos);
		this.cond = cond;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
