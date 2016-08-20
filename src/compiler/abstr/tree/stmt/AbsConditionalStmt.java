package compiler.abstr.tree.stmt;

import compiler.Position;
import compiler.abstr.tree.AbsStmt;

/**
 * Conditional statement.
 * @author toni
 *
 */
public abstract class AbsConditionalStmt extends AbsStmt {

	/**
	 * 
	 * @param pos
	 */
	public AbsConditionalStmt(Position pos) {
		super(pos);
	}

}
