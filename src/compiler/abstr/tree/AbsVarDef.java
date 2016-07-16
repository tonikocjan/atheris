package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Definicija spremenljivke.
 * 
 * @author sliva
 */
public class AbsVarDef extends AbsDef {

	/** Ime spremenljivke. */
	public final String name;

	/** Opis tipa spremenljivke. */
	public final AbsType type;
	
	/** Ali je spremenljivka konstantna */
	public final boolean isConstant;

	/**
	 * Ustvari novo definicijo spremenljivke.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime spremenljivke.
	 * @param type
	 *            Opis tipa spremenljivke.
	 */
	public AbsVarDef(Position pos, String name, AbsType type, boolean constant) {
		super(pos);
		this.name = name;
		this.type = type;
		this.isConstant = constant;
	}


	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
