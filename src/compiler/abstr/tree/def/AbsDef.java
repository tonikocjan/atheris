package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.tree.AbsStmt;

/**
 * Definicija.
 * 
 * @author toni kocjan
 */
public abstract class AbsDef extends AbsStmt {
	
	/**
	 * Parent definition.
	 */
	private AbsDef parentDef;

	/**
	 * Create new definition.
	 *
	 * @param pos
	 *            Position.
	 */
	public AbsDef(Position pos) {
		super(pos);
		
		this.parentDef = null;
	}
	
	/**
	 * Crate new definition.
	 *
	 * @param pos
	 *            Position.
	 * @param parent parent definition for this definition
	 */
	public AbsDef(Position pos, AbsDef parent) {
		super(pos);
		
		this.parentDef = parent;
	}

	/**
	 * 
	 * @param parent
	 */
	public void setParentDefinition(AbsDef parent) {
		this.parentDef = parent;
	}
	
	/**
	 * 
	 * @return
	 */
	public AbsDef getParemtDefinition() {
		return this.parentDef;
	}
	
	/**
	 * Name of the definition.
	 * @return name
	 */
	public abstract String getName();
}
