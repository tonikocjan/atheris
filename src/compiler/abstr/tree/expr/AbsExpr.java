package compiler.abstr.tree.expr;

import compiler.*;
import compiler.abstr.tree.AbsStmt;

/**
 * Izraz.
 * 
 * @author sliva
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
