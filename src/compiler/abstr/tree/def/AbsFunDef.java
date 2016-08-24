package compiler.abstr.tree.def;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.type.AbsType;

/**
 * Definicija funckije.
 * 
 * @author sliva
 */
public class AbsFunDef extends AbsDef {

	/** Ime funkcije. */
	public final String name;
	
	/** Seznam parametrov. */
	private final LinkedList<AbsParDef> pars;

	/** Opis tipa rezultata funkcije. */
	public final AbsType type;
	
	/** Jedro funkcije. */
	public final AbsStmts func;

	/**
	 * Ustvari novo definicijo funckije.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime funkcije.
	 * @param pars
	 *            Seznam parametrov.
	 * @param type
	 *            Opis tipa rezultata funkcije.
	 * @param stmts
	 *            Jedro funkcije.
	 */
	public AbsFunDef(Position pos, String name, LinkedList<AbsParDef> pars, AbsType type, AbsStmts stmts) {
		super(pos);
		this.name = name;
		this.pars = pars;
		this.type = type;
		this.func = stmts;
	}

	public LinkedList<AbsParDef> getParamaters() {
		return pars;
	}

	/**
	 * Vrne stevilo parametrov funkcije.
	 * 
	 * @return Stevilo parametrov funkcije.
	 */
	public int numPars() {
		return pars.size();
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

	@Override
	public String getName() {
		return name;
	}
}
