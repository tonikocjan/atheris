package compiler.abstr.tree.def;

import compiler.*;

/**
 * Definicija tipa.
 * 
 * @author sliva
 */
public abstract class AbsTypeDef extends AbsDef {

	/** Ime tipa. */
	public final String name;

	/**
	 * Ustvari novo definicijo tipa.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime tipa.
	 */
	public AbsTypeDef(Position pos, String name) {
		super(pos);
		this.name = name;
	}
	
}
