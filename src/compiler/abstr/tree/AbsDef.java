package compiler.abstr.tree;

import compiler.*;

/**
 * Definicija.
 * 
 * @author sliva
 */
public abstract class AbsDef extends AbsStmt {

	/**
	 * Ustvari novo definicijo.
	 *
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 */
	public AbsDef(Position pos) {
		super(pos);
	}

}
