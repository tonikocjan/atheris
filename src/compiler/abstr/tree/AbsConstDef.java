package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Definicija konstante.
 * 
 * @author sliva
 */
public class AbsConstDef extends AbsDef {

	/** Ime spremenljivke. */
	public final String name;

	/** Opis tipa spremenljivke. */
	public final AbsType type;

	/**
	 * Ustvari novo definicijo konstante.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime spremenljivke.
	 * @param type
	 *            Opis tipa spremenljivke.
	 */
	public AbsConstDef(Position pos, String name, AbsType type) {
		super(pos);
		this.name = name;
		this.type = type;
	}


	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
