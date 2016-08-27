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
	 * Definition name.
	 */
	public final String name;
	
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
	public AbsDef(Position pos, String name) {
		super(pos);
		
		this.parentDef = null;
		this.name = name;
	}
	
	/**
	 * Crate new definition.
	 *
	 * @param pos
	 *            Position.
	 * @param parent 
	 * 			  Parent definition for this definition
	 */
	public AbsDef(Position pos, String name, AbsDef parent) {
		this(pos, name);
		
		this.parentDef = parent;
	}

	/**
	 * Set parent definition.
	 * @param parent
	 */
	public void setParentDefinition(AbsDef parent) {
		this.parentDef = parent;
	}
	
	/**
	 * Get parent definition.
	 * @return Parent definition.
	 */
	public AbsDef getParemtDefinition() {
		return this.parentDef;
	}
	
	/**
	 * Get name of the definition.
	 * @return name
	 */
	public String getName() {
		return name;
	}
}
