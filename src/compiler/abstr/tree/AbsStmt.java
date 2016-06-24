package compiler.abstr.tree;

import compiler.Position;

public abstract class AbsStmt extends AbsTree { 
	
	/**
	 * Ustvari nov stavek.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 */
	public AbsStmt(Position pos) {
		super(pos);
	}
	
}
