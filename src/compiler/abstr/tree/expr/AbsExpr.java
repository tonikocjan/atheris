package compiler.abstr.tree.expr;

import compiler.*;
import compiler.abstr.tree.AbsStmt;

/**
 * Expression.
 * 
 * @author toni kocjan
 */
public abstract class AbsExpr extends AbsStmt {

	/**
	 * Ustvari nov izraz.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 */
	public AbsExpr(Position pos) {
		super(pos);
	}

}
