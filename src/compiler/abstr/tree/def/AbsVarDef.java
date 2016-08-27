package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.VisibilityKind;
import compiler.abstr.tree.type.AbsType;

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
	
	/** Is variable public / private (used for class members) */
	public final VisibilityKind visibilityKind;

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
	public AbsVarDef(Position pos, String name, AbsType type) {
		super(pos);
		this.name = name;
		this.type = type;
		this.isConstant = false;
		this.visibilityKind = VisibilityKind.Public;
	}
	
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
		this.visibilityKind = VisibilityKind.Public;
	}
	
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
	public AbsVarDef(Position pos, String name, AbsType type, boolean constant, VisibilityKind visibilityKind) {
		super(pos);
		this.name = name;
		this.type = type;
		this.isConstant = constant;
		this.visibilityKind = visibilityKind;
	}


	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

	@Override
	public String getName() {
		return name;
	}

}
