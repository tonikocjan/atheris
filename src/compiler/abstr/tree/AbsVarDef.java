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
	
	/** Is variable public / private (used for class members) */
	public final VisibilityEnum visibilityEnum;

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
		this.visibilityEnum = VisibilityEnum.Private;
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
		this.visibilityEnum = VisibilityEnum.Private;
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
	public AbsVarDef(Position pos, String name, AbsType type, boolean constant, VisibilityEnum visibilityEnum) {
		super(pos);
		this.name = name;
		this.type = type;
		this.isConstant = constant;
		this.visibilityEnum = visibilityEnum;
	}


	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
