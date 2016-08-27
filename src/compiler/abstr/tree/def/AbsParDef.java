package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.type.AbsType;

/**
 * Opis parametra funckije.
 * 
 * @author toni kocjan
 *
 */
public class AbsParDef extends AbsDef {

	/** Ime parametra. */
	public final String name;

	/** Tip parametra. */
	public final AbsType type;

	/**
	 * Ustvari nov opis parametra.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime parametra.
	 * @param type
	 *            Tip parametra.
	 */
	public AbsParDef(Position pos, String name, AbsType type) {
		super(pos);
		this.name = name;
		this.type = type;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

	@Override
	public String getName() {
		return name;
	}

}
