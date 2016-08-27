package compiler.abstr.tree;

import compiler.Position;

/**
 * Statement.
 * @author toni kocjan
 *
 */
public abstract class AbsStmt extends AbsTree { 
	
	/**
	 * Create new statement.
	 * 
	 * @param pos
	 *            Position.
	 */
	public AbsStmt(Position pos) {
		super(pos);
	}
	
}
